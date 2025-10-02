package app.vishwakarma.neuroXcamera.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import app.vishwakarma.neuroXcamera.AutoFinishOnSleep
import app.vishwakarma.neuroXcamera.CapturedItem
import app.vishwakarma.neuroXcamera.util.EphemeralSharedPrefsNamespace
import app.vishwakarma.neuroXcamera.util.getPrefs

open class SecureMainActivity : MainActivity(), SecureActivity {
    val capturedItems = ArrayList<CapturedItem>()
    val ephemeralPrefsNamespace = EphemeralSharedPrefsNamespace()

    private val autoFinisher = AutoFinishOnSleep(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoFinisher.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoFinisher.stop()
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return ephemeralPrefsNamespace.getPrefs(this, name, mode, cloneOriginal = true)
    }
}
