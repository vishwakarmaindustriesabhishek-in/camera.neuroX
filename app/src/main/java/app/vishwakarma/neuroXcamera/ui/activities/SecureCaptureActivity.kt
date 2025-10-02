package app.vishwakarma.neuroXcamera.ui.activities

import android.content.SharedPreferences
import app.vishwakarma.neuroXcamera.util.EphemeralSharedPrefsNamespace
import app.vishwakarma.neuroXcamera.util.getPrefs

class SecureCaptureActivity : CaptureActivity(), SecureActivity {
    val ephemeralPrefsNamespace = EphemeralSharedPrefsNamespace()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return ephemeralPrefsNamespace.getPrefs(this, name, mode, cloneOriginal = true)
    }
}
