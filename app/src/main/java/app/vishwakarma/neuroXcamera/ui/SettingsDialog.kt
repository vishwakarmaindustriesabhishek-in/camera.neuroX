package app.vishwakarma.neuroXcamera.ui

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.annotation.StringRes
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import app.vishwakarma.neuroXcamera.CamConfig
import app.vishwakarma.neuroXcamera.R
import app.vishwakarma.neuroXcamera.databinding.SettingsBinding
import app.vishwakarma.neuroXcamera.ui.activities.MainActivity
import app.vishwakarma.neuroXcamera.ui.activities.MoreSettings
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import java.util.Collections
import kotlin.math.max

@SuppressLint("ClickableViewAccessibility")
class SettingsDialog(val mActivity: MainActivity, themedContext: Context) :
    Dialog(themedContext) {
    val camConfig = mActivity.camConfig

    private val binding: SettingsBinding by lazy { SettingsBinding.inflate(layoutInflater) }
    private var dialog: View
    var locToggle: ToggleButton
    private var flashToggle: ImageView
    private var aRToggle: ToggleButton
    var torchToggle: ToggleButton
    private var gridToggle: ImageView
    var videoQualitySpinner: Spinner
    private lateinit var vQAdapter: ArrayAdapter<String>
    private var focusTimeoutSpinner: Spinner
    private var timerSpinner: Spinner

    var mScrollView: ScrollView
    var mScrollViewContent: View

    var includeAudioToggle: MaterialSwitch
    var enableEISToggle: MaterialSwitch

    var selfIlluminationToggle: MaterialSwitch

    var waitForFocusLockSwitch: MaterialSwitch

    private val timeOptions = mActivity.resources.getStringArray(R.array.time_options)

    private var includeAudioSetting: View
    private var enableEISSetting: View
    private var selfIlluminationSetting: View
    private var videoQualitySetting: View
    private var timerSetting: View

    var settingsFrame: View

    private var moreSettingsButton: View

    private val tabSelectedColor =
        MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)

    private fun getString(@StringRes id: Int) = mActivity.getString(id)

    init {
        setContentView(binding.root)

        dialog = binding.settingsDialog
        dialog.setOnClickListener {}

        moreSettingsButton = binding.moreSettings
        moreSettingsButton.setOnClickListener {
            if (!mActivity.videoCapturer.isRecording) {
                MoreSettings.start(mActivity)
            } else {
                mActivity.showMessage(getString(R.string.more_settings_unavailable_during_recording))
            }
        }

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0f)

        setOnDismissListener {
            mActivity.settingsIcon.visibility = View.VISIBLE
        }

        val background: View = binding.background
        background.setOnClickListener {
            slideDialogUp()
        }

        val rootView = binding.root
        rootView.setOnInterceptTouchEventListener(
            object : SettingsFrameLayout.OnInterceptTouchEventListener {

                override fun onInterceptTouchEvent(
                    view: SettingsFrameLayout?,
                    ev: MotionEvent?,
                    disallowIntercept: Boolean
                ): Boolean {
                    return mActivity.gestureDetector.onTouchEvent(ev!!)
                }

                override fun onTouchEvent(
                    view: SettingsFrameLayout?,
                    event: MotionEvent?
                ): Boolean {
                    return false
                }
            }
        )

        settingsFrame = binding.settingsFrame

        rootView.viewTreeObserver.addOnPreDrawListener(
            object : OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    rootView.viewTreeObserver.removeOnPreDrawListener(this)

                    settingsFrame.layoutParams =
                        (settingsFrame.layoutParams as ViewGroup.MarginLayoutParams).let {
                            val marginTop =
                                (mActivity.rootView.layoutParams as ViewGroup.MarginLayoutParams).topMargin
                            it.height = (marginTop + (rootView.measuredWidth * 4 / 3))
                            it
                        }

                    return true
                }
            }
        )

        locToggle = binding.locationToggle
        locToggle.setOnClickListener {
            if (mActivity.videoCapturer.isRecording) {
                locToggle.isChecked = !locToggle.isChecked
                mActivity.showMessage(
                    getString(R.string.toggle_geo_tagging_unsupported_while_recording)
                )
            } else {
                camConfig.requireLocation = locToggle.isChecked
            }
        }

        flashToggle = binding.flashToggleOption
        flashToggle.setOnClickListener {
            if (mActivity.requiresVideoModeOnly) {
                mActivity.showMessage(
                    getString(R.string.flash_switch_unsupported)
                )
            } else {
                camConfig.toggleFlashMode()
            }
        }

        aRToggle = binding.aspectRatioToggle
        aRToggle.setOnClickListener {
            if (camConfig.isVideoMode) {
                aRToggle.isChecked = true
                mActivity.showMessage(
                    getString(R.string.four_by_three_unsupported_in_video)
                )
            } else {
                camConfig.toggleAspectRatio()
            }
        }

        torchToggle = binding.torchToggleOption
        torchToggle.setOnClickListener {
            if (camConfig.isFlashAvailable) {
                camConfig.toggleTorchState()
            } else {
                torchToggle.isChecked = false
                mActivity.showMessage(
                    getString(R.string.flash_unavailable_in_current_mode)
                )
            }
        }

        gridToggle = binding.gridToggleOption
        gridToggle.setOnClickListener {
            camConfig.gridType = when (camConfig.gridType) {
                CamConfig.GridType.NONE -> CamConfig.GridType.THREE_BY_THREE
                CamConfig.GridType.THREE_BY_THREE -> CamConfig.GridType.FOUR_BY_FOUR
                CamConfig.GridType.FOUR_BY_FOUR -> CamConfig.GridType.GOLDEN_RATIO
                CamConfig.GridType.GOLDEN_RATIO -> CamConfig.GridType.NONE
            }
            updateGridToggleUI()
        }

        videoQualitySpinner = binding.videoQualitySpinner

        videoQualitySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {

                    val choice = vQAdapter.getItem(position) as String
                    updateVideoQuality(choice)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        if (mActivity.requiresVideoModeOnly) {
            binding.waitForFocusLockSetting.visibility = View.GONE
        }

        waitForFocusLockSwitch = binding.waitForFocusLockSwitch
        waitForFocusLockSwitch.isChecked = camConfig.waitForFocusLock
        waitForFocusLockSwitch.setOnClickListener {
            camConfig.waitForFocusLock = waitForFocusLockSwitch.isChecked
            if (camConfig.cameraProvider != null) {
                camConfig.startCamera(true)
            }
        }

        selfIlluminationToggle = binding.selfIlluminationSwitch
        selfIlluminationToggle.setOnCheckedChangeListener { _, isChecked ->
            camConfig.selfIlluminate = isChecked
        }
        binding.selfIlluminationSwitchContainer.setOnTouchListener { _, event ->
            event.setLocation(0f, 0f)
            selfIlluminationToggle.dispatchTouchEvent(event)
            true
        }

        focusTimeoutSpinner = binding.focusTimeoutSpinner
        focusTimeoutSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {

                    val selectedOption = focusTimeoutSpinner.selectedItem.toString()
                    updateFocusTimeout(selectedOption)

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        focusTimeoutSpinner.setSelection(2)

        timerSpinner = binding.timerSpinner
        timerSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {

                    val selectedOption = timerSpinner.selectedItem.toString()

                    if (selectedOption == "Off") {
                        mActivity.timerDuration = 0
                        mActivity.cbText.visibility = View.INVISIBLE
                    } else {

                        try {
                            val durS = selectedOption.substring(0, selectedOption.length - 1)
                            val dur = durS.toInt()

                            mActivity.timerDuration = dur

                            mActivity.cbText.text = selectedOption
                            mActivity.cbText.visibility = View.VISIBLE

                        } catch (exception: Exception) {

                            mActivity.showMessage(
                                getString(R.string.unexpected_error_while_setting_focus_timeout)
                            )

                        }

                    }

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        mScrollView = binding.settingsScrollview
        mScrollViewContent = binding.settingsScrollviewContent

        includeAudioSetting = binding.includeAudioSetting
        enableEISSetting = binding.enableEisSetting
        selfIlluminationSetting = binding.selfIlluminationSetting
        videoQualitySetting = binding.videoQualitySetting
        timerSetting = binding.timerSetting

        includeAudioToggle = binding.includeAudioSwitch
        includeAudioToggle.setOnCheckedChangeListener { _, _ ->
            if (mActivity.videoCapturer.isRecording) {
                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                    // Inform the user why enabling this option isn't possible
                    mActivity.showMessage(context.getString(R.string.audio_permission_failed_in_recording))

                    // Ensure the option is visually off
                    includeAudioToggle.isChecked = false
                    return@setOnCheckedChangeListener
                }

                if (!mActivity.videoCapturer.includeAudio) {
                    mActivity.showMessage("Enabling audio while recording is not currently supported when it was disabled at the start")
                    includeAudioToggle.isChecked = false
                    return@setOnCheckedChangeListener
                }

                if  (includeAudioToggle.isChecked) {
                    mActivity.videoCapturer.unmuteRecording()
                } else {
                    mActivity.videoCapturer.muteRecording()
                }
            }
        }

        includeAudioToggle.setOnClickListener {
            mActivity.micOffIcon.visibility = if (includeAudioToggle.isChecked) {
                View.GONE
            } else {
                View.VISIBLE
            }

            camConfig.includeAudio = includeAudioToggle.isChecked
        }
        binding.includeAudioSwitchContainer.setOnTouchListener { _, event ->
            event.setLocation(0f, 0f)
            includeAudioToggle.dispatchTouchEvent(event)
            true
        }

        enableEISToggle = binding.enableEisSwitch
        enableEISToggle.setOnCheckedChangeListener { _, isChecked ->
            camConfig.enableEIS = isChecked
            camConfig.startCamera(true)
        }
        binding.enableEisSwitchContainer.setOnTouchListener { _, event ->
            event.setLocation(0f, 0f)
            enableEISToggle.dispatchTouchEvent(event)
            true
        }

        window?.attributes?.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        var backgroundColor = ContextCompat.getColor(context, android.R.color.black)
        backgroundColor = ColorUtils.setAlphaComponent(backgroundColor, 150)
        val settingsDialogBackgroundDrawable =
            ContextCompat.getDrawable(context, R.drawable.settings_bg)
        settingsDialogBackgroundDrawable?.setTint(backgroundColor)
        binding.settingsDialog.background = settingsDialogBackgroundDrawable

        val moreSettingsBackgroundDrawable =
            ContextCompat.getDrawable(context, R.drawable.settings_bg)
        moreSettingsBackgroundDrawable?.setTint(backgroundColor)
        binding.moreSettings.background = moreSettingsBackgroundDrawable
    }

    private fun resize() {
        mScrollViewContent.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                mScrollViewContent.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val settingsDialogHorizontalMargin =
                    mActivity.resources.getDimensionPixelSize(R.dimen.settings_dialog_horizontal_margin)
                val moreSettingsButtonTopPadding =
                    (8 * mActivity.resources.displayMetrics.density).toInt()
                val totalDialogHeight = moreSettingsButton.height + moreSettingsButtonTopPadding +
                        dialog.height
                val availableWidth = dialog.width - (settingsDialogHorizontalMargin * 4)
                val availableHeight = availableWidth - (totalDialogHeight - mScrollView.height)

                val height = if (mScrollViewContent.height < mScrollView.height) {
                    mScrollViewContent.height
                } else {
                    max(
                        mScrollView.height.coerceAtMost(availableHeight),
                        mScrollViewContent.height.coerceAtMost(availableHeight),
                    )
                }
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height,
                )

                mScrollView.layoutParams = lp
            }
        })
    }

    fun showOnlyRelevantSettings() {
        if (camConfig.isVideoMode) {
            includeAudioSetting.visibility = View.VISIBLE
            enableEISSetting.visibility = View.GONE
            videoQualitySetting.visibility = View.VISIBLE
            enableEISSetting.visibility = if (camConfig.isVideoStabilizationSupported()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            includeAudioSetting.visibility = View.GONE
            enableEISSetting.visibility = View.GONE
            videoQualitySetting.visibility = View.GONE
        }

        selfIlluminationSetting.visibility =
            if (camConfig.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                View.VISIBLE
            } else {
                View.GONE
            }

        timerSetting.visibility = if (camConfig.isVideoMode) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }


    fun updateFocusTimeout(selectedOption: String) {

        if (selectedOption == "Off") {
            camConfig.focusTimeout = 0
        } else {

            try {
                val durS = selectedOption.substring(0, selectedOption.length - 1)
                val dur = durS.toLong()

                camConfig.focusTimeout = dur

            } catch (exception: Exception) {

                mActivity.showMessage(
                    getString(R.string.unexpected_error_while_setting_focus_timeout)
                )

            }
        }

        focusTimeoutSpinner.setSelection(timeOptions.indexOf(selectedOption), false)
    }

    fun updateVideoQuality(choice: String, resCam: Boolean = true) {

        val quality = titleToQuality(choice)

        if (quality == camConfig.videoQuality) return

        camConfig.videoQuality = quality

        if (resCam) {
            camConfig.startCamera(true)
        } else {
            videoQualitySpinner.setSelection(getAvailableQTitles().indexOf(choice))

        }
    }

    fun titleToQuality(title: String): Quality {
        return when (title) {
            "2160p (UHD)" -> Quality.UHD
            "1080p (FHD)" -> Quality.FHD
            "720p (HD)" -> Quality.HD
            "480p (SD)" -> Quality.SD
            else -> {
                Log.e("TAG", "Unknown quality: $title")
                Quality.SD
            }
        }
    }

    private var wasSelfIlluminationOn = false

    fun selfIllumination() {

        if (camConfig.selfIlluminate) {

            val colorFrom: Int = Color.BLACK
            val colorTo: Int = mActivity.getColor(R.color.self_illumination_light)

            val colorAnimation1 = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation1.duration = 300
            colorAnimation1.addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                mActivity.previewView.setBackgroundColor(color)
                mActivity.rootView.setBackgroundColor(color)
                mActivity.bottomOverlay.setBackgroundColor(color)
            }

            val colorAnimation2 = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, Color.BLACK)
            colorAnimation2.duration = 300

            val selectedTextColor =
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimary)
            val colorAnimation3 = ValueAnimator.ofObject(ArgbEvaluator(), selectedTextColor, Color.WHITE)
            colorAnimation3.duration = 300

            var currentUnselectedColor = Color.WHITE
            colorAnimation2.addUpdateListener { animator ->
                currentUnselectedColor = animator.animatedValue as Int
            }
            colorAnimation3.addUpdateListener { animator ->
                mActivity.tabLayout.setTabTextColors(
                    currentUnselectedColor,
                    animator.animatedValue as Int
                )
            }

            val colorAnimation4 =
                ValueAnimator.ofObject(ArgbEvaluator(), tabSelectedColor, Color.BLACK)
            colorAnimation4.duration = 300
            colorAnimation4.addUpdateListener { animator ->
                mActivity.tabLayout.setSelectedTabIndicatorColor(animator.animatedValue as Int)
            }

            colorAnimation1.start()
            colorAnimation2.start()
            colorAnimation3.start()
            colorAnimation4.start()

            setBrightness(1f)

        } else if (wasSelfIlluminationOn) {

            val colorFrom: Int = mActivity.getColor(R.color.self_illumination_light)
            val colorTo: Int = Color.BLACK

            val colorAnimation1 = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
            colorAnimation1.duration = 300
            colorAnimation1.addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                mActivity.previewView.setBackgroundColor(color)
                mActivity.rootView.setBackgroundColor(color)
                mActivity.bottomOverlay.setBackgroundColor(color)
            }

            val colorAnimation2 = ValueAnimator.ofObject(ArgbEvaluator(), Color.BLACK, Color.WHITE)
            colorAnimation2.duration = 300

            val selectedTextColor =
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimary)
            val colorAnimation3 = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, selectedTextColor)
            colorAnimation3.duration = 300

            var currentUnselectedTextColor = Color.BLACK
            colorAnimation2.addUpdateListener { animator ->
                currentUnselectedTextColor = animator.animatedValue as Int
            }
            colorAnimation3.addUpdateListener { animator ->
                mActivity.tabLayout.setTabTextColors(
                    currentUnselectedTextColor,
                    animator.animatedValue as Int
                )
            }

            val colorAnimation4 = ValueAnimator.ofObject(ArgbEvaluator(), Color.BLACK, tabSelectedColor)
            colorAnimation4.duration = 300
            colorAnimation4.addUpdateListener { animator ->
                mActivity.tabLayout.setSelectedTabIndicatorColor(animator.animatedValue as Int)
            }

            colorAnimation1.start()
            colorAnimation2.start()
            colorAnimation3.start()
            colorAnimation4.start()

            setBrightness(getSystemBrightness())
        }

        wasSelfIlluminationOn = camConfig.selfIlluminate
    }

    private val slideDownAnimation: Animation by lazy {
        val anim = AnimationUtils.loadAnimation(
            mActivity,
            R.anim.slide_down
        )

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                moreSettingsButton.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })

        anim
    }

    val dismissHandler = Handler(Looper.myLooper()!!)
    val dismissCallback = Runnable {
        dismiss()
    }

    private val slideUpAnimation: Animation by lazy {
        val anim = AnimationUtils.loadAnimation(
            mActivity,
            R.anim.slide_up
        )

        anim.setAnimationListener(
            object : Animation.AnimationListener {

                override fun onAnimationStart(p0: Animation?) {
                    moreSettingsButton.visibility = View.INVISIBLE
                }

                override fun onAnimationEnd(p0: Animation?) {
                    dismissHandler.removeCallbacks(dismissCallback)
                    dismissHandler.post(
                        dismissCallback
                    )
                }

                override fun onAnimationRepeat(p0: Animation?) {}

            }
        )

        anim
    }

    private fun getSystemBrightness(): Float {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            -1
        ) / 255f
    }

    private fun setBrightness(brightness: Float) {

        val layout = mActivity.window.attributes
        layout.screenBrightness = brightness
        mActivity.window.attributes = layout

        window?.let {
            val dialogLayout = it.attributes
            dialogLayout.screenBrightness = brightness
            it.attributes = dialogLayout
        }

    }

    private fun slideDialogDown() {
        settingsFrame.startAnimation(slideDownAnimation)
    }

    fun slideDialogUp() {
        settingsFrame.startAnimation(slideUpAnimation)
    }

    private fun getAvailableQualities(): List<Quality> {
        val cameraInfo = camConfig.camera?.cameraInfo ?: return Collections.emptyList()
        return Recorder.getVideoCapabilities(cameraInfo).getSupportedQualities(DynamicRange.SDR)
    }

    private fun getAvailableQTitles(): List<String> {
        val titles = arrayListOf<String>()

        getAvailableQualities().forEach {
            titles.add(getTitleFor(it))
        }

        return titles
    }

    private fun getTitleFor(quality: Quality): String {
        return when (quality) {
            Quality.UHD -> "2160p (UHD)"
            Quality.FHD -> "1080p (FHD)"
            Quality.HD -> "720p (HD)"
            Quality.SD -> "480p (SD)"
            else -> {
                Log.i("TAG", "Unknown constant: $quality")
                "Unknown"
            }
        }
    }

    fun updateGridToggleUI() {
        mActivity.previewGrid.postInvalidate()
        gridToggle.setImageResource(
            when (camConfig.gridType) {
                CamConfig.GridType.NONE -> R.drawable.grid_off_circle
                CamConfig.GridType.THREE_BY_THREE -> R.drawable.grid_3x3_circle
                CamConfig.GridType.FOUR_BY_FOUR -> R.drawable.grid_4x4_circle
                CamConfig.GridType.GOLDEN_RATIO -> R.drawable.grid_goldenratio_circle
            }
        )
    }

    fun updateFlashMode() {
        flashToggle.setImageResource(
            if (camConfig.isFlashAvailable) {
                when (camConfig.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> R.drawable.flash_on_circle
                    ImageCapture.FLASH_MODE_AUTO -> R.drawable.flash_auto_circle
                    else -> R.drawable.flash_off_circle
                }
            } else {
                R.drawable.flash_off_circle
            }
        )
    }

    override fun show() {

        this.resize()

        updateFlashMode()

        if (camConfig.isVideoMode) {
            aRToggle.isChecked = true
        } else {
            aRToggle.isChecked = camConfig.aspectRatio == AspectRatio.RATIO_16_9
        }

        torchToggle.isChecked = camConfig.isTorchOn

        updateGridToggleUI()

        mActivity.settingsIcon.visibility = View.INVISIBLE
        super.show()

        slideDialogDown()
    }

    fun reloadQualities() {

        val titles = getAvailableQTitles()

        vQAdapter = ArrayAdapter<String>(
            mActivity,
            android.R.layout.simple_spinner_item,
            titles
        )

        vQAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        videoQualitySpinner.adapter = vQAdapter

        if (camConfig.videoQuality != Quality.HIGHEST) {
            videoQualitySpinner.setSelection(titles.indexOf(getTitleFor(camConfig.videoQuality)))
        }
    }
}
