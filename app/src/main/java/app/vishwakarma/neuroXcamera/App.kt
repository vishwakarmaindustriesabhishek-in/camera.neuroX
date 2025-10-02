/*
 * MIT License
 *
 * Copyright (c) 2025 Vishwakarma Industries
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * NeuroX.AI - AI-Powered Camera Application
 * Developed by Vishwakarma Industries
 */

package app.vishwakarma.neuroXcamera

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import app.vishwakarma.neuroXcamera.ktx.isSystemApp
import app.vishwakarma.neuroXcamera.ui.activities.MainActivity
import com.google.android.material.color.DynamicColors

class App : Application() {

    companion object {
        private const val STALE_LOCATION_THRESHOLD = 11 * 1000L
    }

    private var activity: MainActivity? = null
    private var location: Location? = null

    private var isLocationFetchInProgress = false

    private val locationManager by lazy {
        getSystemService(LocationManager::class.java)!!
    }

    private val locationListener: LocationListener by lazy {
        object : LocationListener {
            override fun onLocationChanged(changedLocation: Location) {
                location = listOf(location, changedLocation).getOptimalLocation()
            }

            override fun onProviderDisabled(provider: String) {
                if (!isAnyLocationProvideActive()) {
                    activity?.indicateLocationProvidedIsDisabled()
                }
            }

            override fun onLocationChanged(locations: MutableList<Location>) {
                val location = locations.getOptimalLocation()
                if (location != null) {
                    this@App.location = location
                }
            }

            override fun onProviderEnabled(provider: String) {}
        }
    }

    private val autoSleepDuration: Long = 5 * 60 * 1000 // 5 minutes
    private val autoSleepTimer = object : CountDownTimer(
        autoSleepDuration,
        autoSleepDuration / 2
    ) {
        override fun onTick(milliLeft: Long) {}

        override fun onFinish() {
            activity?.enableAutoSleep()
        }
    }

    private val activityLifeCycleHelper by lazy {
        ActivityLifeCycleHelper { activity ->
            if (activity != null) activity.disableAutoSleep() else this.activity?.enableAutoSleep()
            this.activity = activity
        }
    }

    fun isAnyLocationProvideActive(): Boolean {
        if (!locationManager.isLocationEnabled) return false
        val providers = locationManager.allProviders

        providers.forEach {
            if (locationManager.isProviderEnabled(it)) return true
        }
        return false
    }

    fun List<Location?>.getOptimalLocation(): Location? {
        if (isNullOrEmpty()) return null

        var optimalLocation: Location? = null
        forEach { location ->
            if (location != null) {
                if (optimalLocation == null) {
                    optimalLocation = location
                    return@forEach
                }

                val timeDifference = location.time - optimalLocation.time

                // If the location is older than STALE_LOCATION_THRESHOLD ms
                if (timeDifference > STALE_LOCATION_THRESHOLD) {
                    optimalLocation = location
                } else {
                    // Compare their accuracy instead of time if the difference is below
                    // threshold
                    if (location.accuracy > optimalLocation.accuracy) {
                        optimalLocation = location
                    }
                }
            }
        }
        return optimalLocation
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityLifeCycleHelper)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestLocationUpdates(reAttach: Boolean = false) {
        if (!isLocationEnabled()) {
            activity?.indicateLocationProvidedIsDisabled()
        }
        if (isLocationFetchInProgress) {
            if (!reAttach) return
            dropLocationUpdates()
        }
        isLocationFetchInProgress = true
        if (location == null) {
            val providers = if (applicationInfo.isSystemApp() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf<String>(LocationManager.FUSED_PROVIDER)
            } else {
                locationManager.allProviders
            }
            val locations = providers.map {
                locationManager.getLastKnownLocation(it)
            }
            val fetchedLocation = locations.getOptimalLocation()
            if (fetchedLocation != null) {
                location = fetchedLocation
            }
        }

        locationManager.allProviders.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider,
                2000,
                10f,
                locationListener
            )
        }
    }

    fun dropLocationUpdates() {
        isLocationFetchInProgress = false
        locationManager.removeUpdates(locationListener)
    }

    fun getLocation(): Location? = location

    private fun isLocationEnabled(): Boolean = locationManager.isLocationEnabled

    fun shouldAskForLocationPermission() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(activityLifeCycleHelper)
    }

    private fun AppCompatActivity.disableAutoSleep() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        resetPreventScreenFromSleeping()
    }

    fun resetPreventScreenFromSleeping() {
        autoSleepTimer.cancel()
        autoSleepTimer.start()
    }

    private fun AppCompatActivity.enableAutoSleep() {
        autoSleepTimer.cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
