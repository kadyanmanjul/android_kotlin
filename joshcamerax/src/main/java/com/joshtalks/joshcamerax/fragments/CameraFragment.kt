package com.joshtalks.joshcamerax.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshcamerax.R
import com.joshtalks.joshcamerax.analyzer.LuminosityAnalyzer
import com.joshtalks.joshcamerax.databinding.FragmentCameraBinding
import com.joshtalks.joshcamerax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera) {
    companion object {
        private const val TAG = "JoshCameraX"
        const val KEY_FLASH = "sPrefFlashCamera"
    }

    private lateinit var displayManager: DisplayManager
    private lateinit var prefs: SharedPrefsManager
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var flashMode by Delegates.observable(FlashMode.OFF.ordinal) { _, _, new ->
        binding.buttonFlash.setImageResource(
            when (new) {
                FlashMode.ON.ordinal -> R.drawable.ic_flash_on
                else -> R.drawable.ic_flash_off
            }
        )
    }

    /**
     * A display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                preview.setTargetRotation(view.display.rotation)
                imageCapture.setTargetRotation(view.display.rotation)
                imageAnalyzer.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsManager.newInstance(requireContext())
        flashMode = prefs.getInt(KEY_FLASH, FlashMode.OFF.ordinal)
        initViews()

        displayManager =
            requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        binding.fragment = this // setting the variable for XML
        binding.viewFinder.addOnAttachStateChangeListener(object :
            View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) =
                displayManager.registerDisplayListener(displayListener, null)

            override fun onViewAttachedToWindow(v: View) =
                displayManager.unregisterDisplayListener(displayListener)
        })

        // This SimpleOnScaleGestureListener adds a fun gesture to switch between video and photo
        // TODO later as reqiure to change whole dependencies and code restructure
        enableZoomFeature()

    }

    override fun onResume() {
        super.onResume()
        initViews()
    }

    private fun enableZoomFeature() {
       /* val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = CameraX.getCameraInfo(lensFacing)?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0F
                val delta = detector.scaleFactor
                CameraX.getCameraControl(lensFacing).setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, listener)

        binding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }*/
    }

    /**
     * Create some initial states
     * */
    private fun initViews() {
        adjustInsets()
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {
        binding.viewFinder.fitSystemWindows()
        binding.fabTakePicture.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                view.bottomMargin = windowInsets.systemWindowInsetBottom
            else view.endMargin = windowInsets.systemWindowInsetRight
        }
    }

    /**
     * Turns on or off the flashlight
     * */
    fun toggleFlash()=
        binding.buttonFlash.toggleButton(
            flashMode == FlashMode.ON.ordinal,
            360f,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on
        ) { flag ->

            flashMode = if (flag) FlashMode.ON.ordinal else FlashMode.OFF.ordinal
            prefs.putInt(KEY_FLASH, flashMode)
            imageCapture.flashMode = getFlashMode()
            Log.d(TAG, "toggleFlash() called with: flag = $flag flashmode  is $flashMode")
        }


    /**
     * Change the facing of camera
     *  toggleButton() function is an Extension function made to animate button rotation
     * */
    @SuppressLint("RestrictedApi")
    fun toggleCamera() = binding.buttonSwitchCamera.toggleButton(
        lensFacing == CameraX.LensFacing.BACK, 180f,
        R.drawable.ic_rotate_camera, R.drawable.ic_rotate_camera
    ) {
        lensFacing = if (it) CameraX.LensFacing.BACK else CameraX.LensFacing.FRONT

        CameraX.getCameraWithLensFacing(lensFacing)
        recreateCamera()
    }

    /**
     * Change the fragment to video
     *  toggleVideo() function is an function made to fragment transation
     * */
    @SuppressLint("RestrictedApi")
    fun toggleVideo() = view?.let { Navigation.findNavController(it).navigate(R.id.action_camera_to_video) }

    /**
     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
     * */
    private fun recreateCamera() {
        CameraX.unbindAll()
        startCamera()
    }

    /**
     * Navigate to PreviewFragment
     * */
    fun openPreview() {
        if (!outputDirectory.listFiles().isNullOrEmpty())
            view?.let { Navigation.findNavController(it).navigate(R.id.action_camera_to_preview) }
    }


    override fun onPermissionGranted() {
        // Each time apps is coming to foreground the need permission check is being processed
        binding.viewFinder.let { vf ->
            vf.post {
                // Setting current display ID
                displayId = vf.display.displayId
                recreateCamera()
                lifecycleScope.launch(Dispatchers.Main) {
                    // Do on IO Dispatcher
                    // Check if there are any photos or videos in the app directory and preview the last one
                    outputDirectory.listFiles()?.lastOrNull()?.let {
                        setGalleryThumbnail(it)
                    }
                        ?: binding.buttonGallery.setImageResource(R.drawable.ic_no_picture) // or the default placeholder
                }
            }
        }
    }

    private fun startCamera() {
        // This is the Texture View where the camera will be rendered
        val viewFinder = binding.viewFinder

        // The ratio for the output image and preview
        val ratio = AspectRatio.RATIO_4_3

        // The Configuration of how we want to preview the camera
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(ratio) // setting the aspect ration
            setLensFacing(lensFacing) // setting the lens facing (front or back)
            setTargetRotation(viewFinder.display.rotation) // setting the rotation of the camera
        }.build()

        // Create an instance of Camera Preview
        preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        // The Configuration of how we want to capture the image
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(ratio) // setting the aspect ration
            setLensFacing(lensFacing) // setting the lens facing (front or back)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY) // setting to have pictures with highest quality possible
            setTargetRotation(viewFinder.display.rotation) // setting the rotation of the camera
            setFlashMode(getFlashMode()) // setting the flash
        }

        imageCapture = ImageCapture(imageCaptureConfig.build())
        binding.imageCapture = imageCapture

        // The Configuration for image analyzing
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Create an Image Analyzer Use Case instance for luminosity analysis
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            // Use a worker thread for image analysis to prevent glitches
            val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
            setAnalyzer(ThreadExecutor(Handler(analyzerThread.looper)), LuminosityAnalyzer())
        }

        // Check for lens facing to add or not the Image Analyzer Use Case
        if (lensFacing == CameraX.LensFacing.BACK) {
            CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture, analyzerUseCase)
        } else {
            CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture)
        }
    }

    private fun getFlashMode() = when (flashMode) {
        FlashMode.ON.ordinal -> FlashMode.ON
        FlashMode.AUTO.ordinal -> FlashMode.AUTO
        else -> FlashMode.OFF
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    fun takePicture(imageCapture: ImageCapture) = lifecycleScope.launch(Dispatchers.Main) {
        try {
            captureImage(imageCapture)
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private fun captureImage(imageCapture: ImageCapture) {
        // Create the output file
        val imageFile = File(outputDirectory, "IMG_${SimpleDateFormat("yyyyMMdd_HHmmSS", Locale.ENGLISH).format(
            Date()
        )}.jpg")
        // Capture the image, first parameter is the file where the image should be stored, the second parameter is the callback after taking a photo
        imageCapture.takePicture(
            imageFile,
            requireContext().mainExecutor(),
            object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) { // the resulting file of taken photo
                    // Create small preview
                    setGalleryThumbnail(file)
                    openPreview()
                    val msg = "Photo saved in ${file.absolutePath}"
                    Log.d("CameraXDemo", msg)
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    // This function is called if there is some error during capture process
                    val msg = "Photo capture failed: $message"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.e("CameraXApp", msg)
                    cause?.printStackTrace()
                }
            })
    }

    private fun setGalleryThumbnail(file: File) = binding.buttonGallery.let {
        // Do the work on view's thread, this is needed, because the function is called in a Coroutine Scope's IO Dispatcher
        it.post {
            Glide.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .into(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onBackPressed() {
        requireActivity().finish()
        }
}