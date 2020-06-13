package com.joshtalks.joshcamerax.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.animation.doOnCancel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshcamerax.R
import com.joshtalks.joshcamerax.VideoTrimmerActivity
import com.joshtalks.joshcamerax.databinding.FragmentVideoBinding
import com.joshtalks.joshcamerax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


@SuppressLint("RestrictedApi")
class VideoFragment : BaseFragment<FragmentVideoBinding>(R.layout.fragment_video) {
    companion object {
        private const val TAG = "CameraXDemo"
    }

    private lateinit var displayManager: DisplayManager
    private lateinit var prefs: SharedPrefsManager
    private lateinit var preview: Preview
    private lateinit var videoCapture: VideoCapture

    private var displayId = -1
    private var duration = 0
    private var lensFacing = CameraX.LensFacing.BACK
    private var timer = Timer()
    private var absoulutePath: String? = null
    private var flashMode by Delegates.observable(FlashMode.OFF.ordinal) { _, _, new ->
        binding.buttonFlash.setImageResource(
            when (new) {
                FlashMode.ON.ordinal -> R.drawable.ic_flash_on
                else -> R.drawable.ic_flash_off
            }
        )
    }
    private var isTorchOn = false
    private var isRecording = false
    private var backpressed = false

    private val animateRecord by lazy {
        ObjectAnimator.ofFloat(binding.fabRecordVideo, View.ALPHA, 1f, 0.5f).apply {
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            doOnCancel { binding.fabRecordVideo.alpha = 1f }
        }
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
            if (displayId == this@VideoFragment.displayId) {
                preview.setTargetRotation(view.display.rotation)
                videoCapture.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsManager.newInstance(requireContext())
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
        binding.fabRecordVideo.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                view.bottomMargin = windowInsets.systemWindowInsetBottom
            else view.endMargin = windowInsets.systemWindowInsetRight
        }
        /*binding.buttonFlash.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.systemWindowInsetTop
        }*/
    }

    /**
     * Change the facing of camera
     *  toggleButton() function is an Extension function made to animate button rotation
     * */
    fun toggleCamera() = binding.buttonSwitchCamera.toggleButton(
        lensFacing == CameraX.LensFacing.BACK, 180f,
        R.drawable.ic_rotate_camera, R.drawable.ic_rotate_camera
    ) {
        lensFacing = if (it) CameraX.LensFacing.BACK else CameraX.LensFacing.FRONT

        CameraX.getCameraWithLensFacing(lensFacing)
        recreateCamera()
    }

    /**
     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
     * */
    private fun recreateCamera() {
        CameraX.unbindAll()
        startCamera()
    }

    private fun startCamera() {
        // This is the Texture View where the camera will be rendered
        val viewFinder = binding.viewFinder

        // The ratio for the output video and preview
        val ratio = AspectRatio.RATIO_16_9

        // The Configuration of how we want to preview the camera
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(ratio) // setting the aspect ration
            setLensFacing(lensFacing) // setting the lens facing (front or back)
            setTargetRotation(viewFinder.display.rotation)// setting the rotation of the camera
        }.build()

        // Create an instance of Camera Preview
        preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        // The Configuration of how we want to capture the video
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetAspectRatio(ratio) // setting the aspect ration
            setLensFacing(lensFacing) // setting the lens facing (front or back)
            setVideoFrameRate(24) // setting the frame rate to 24 fps
            setTargetRotation(viewFinder.display.rotation) // setting the rotation of the camera
        }.build()

        videoCapture = VideoCapture(videoCaptureConfig)

        binding.fabRecordVideo.setOnClickListener { recordVideo(videoCapture) }

        // Add all the use cases to the CameraX
        CameraX.bindToLifecycle(viewLifecycleOwner, preview, videoCapture)
    }

    /**
     * Navigate to PreviewFragment
     * */
    fun openPreview() {
        if (!outputDirectory.listFiles().isNullOrEmpty())
            view?.let { Navigation.findNavController(it).navigate(R.id.action_video_to_preview) }
    }

    /**
     * Navigate to CameraFragment
     * */
    fun toggleToCamera() = view?.let { Navigation.findNavController(it).navigate(R.id.action_video_to_camera) }


    private fun recordVideo(videoCapture: VideoCapture) {
        // Create the output file
        val f =
            File(Environment.getExternalStorageDirectory().toString() + File.separator + "JoshSkill" + File.separator + "Media" + "/JoshApp/cached")
        if (!f.exists()) {
            f.mkdirs()
        }
        val desstination =
            File(f.absolutePath + File.separator + "record.mp4")
        try {
            desstination.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val videoFile = File(outputDirectory, "${SimpleDateFormat("yyyyMMdd_HHmmSS", Locale.ENGLISH).format(Date())}record.mp4")
        absoulutePath=videoFile.absolutePath

        if (!isRecording) {
            try {
                timer.let {
                    timer = Timer()
                    timer.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            activity?.runOnUiThread {
                                duration += 1000
                                binding.tvRecordView.text = String.format(
                                    Locale.getDefault(), "%02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
                                    TimeUnit.MILLISECONDS.toSeconds(duration.toLong())
                                )
                            }
                        }
                    }, 1000, 1000)
                }
            } catch (e:Exception) {
            e.printStackTrace()
        }
                binding.tvRecordView.visibility = View.VISIBLE
            animateRecord.start()
            // Capture the video, first parameter is the file where the video should be stored, the second parameter is the callback after racording a video
            videoCapture.startRecording(
                videoFile,
                requireContext().mainExecutor(),
                object : VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {

                        if(backpressed.not()) {
                            // Create small preview
                            setGalleryThumbnail(file)
                            val msg = "Video saved in ${file.absolutePath}"
                            Log.d(
                                "CameraXDemo",
                                msg + "  " + Uri.fromFile(file) + "  " + file + "  " + file.absolutePath
                            )

                            activity?.let {
                                VideoTrimmerActivity.startTrimmerActivity(
                                    it, VIDEO_SELECT,
                                    Uri.fromFile(file),
                                    desstination,
                                    file.absolutePath
                                )
                            }
                        }

                    }

                    override fun onError(
                        videoCaptureError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                        // This function is called if there is some error during the video recording process
                        animateRecord.cancel()
                        val msg = "Video capture failed: $message"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        Log.e("CameraXApp", msg)
                        cause?.printStackTrace()
                    }
                })
        } else {
            animateRecord.cancel()
            videoCapture.stopRecording()
            timer.cancel()
            binding.tvRecordView.visibility=View.GONE
        }
        isRecording = !isRecording
    }

    /**
     * Turns on or off the flashlight
     * */
    fun toggleFlash() {
        binding.buttonFlash.toggleButton(
            flashMode == FlashMode.ON.ordinal,
            360f,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on
        ) { flag ->
            isTorchOn = flag
            flashMode = if (flag) FlashMode.ON.ordinal else FlashMode.OFF.ordinal
            preview.enableTorch(flag)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onPause() {
        binding.tvRecordView.text = "00:00"
        binding.tvRecordView.visibility = View.GONE
        timer.cancel()
        super.onPause()
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
                preview.enableTorch(isTorchOn)
            }
        }
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

    override fun onBackPressed() {
        if(backpressed.not()) {
            backpressed = true
            val uiHandler = Handler(Looper.getMainLooper())
            uiHandler.postDelayed({
                if (isRecording) {
                    videoCapture.stopRecording()
                    timer.cancel()
                    binding.tvRecordView.visibility = View.GONE
                    animateRecord.cancel()
                    //videoCapture.clear()

                }
                deleteVideoFile()

                requireActivity().finish()
            }, 250)
        }
}

    private fun deleteVideoFile() {
        absoulutePath?.let {
            val f0 = File(it)
            if (f0.exists()) {
                val d0 = f0.delete()
                Log.w(TAG, "File deleted: $it/myFile $d0")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        preview.enableTorch(false)
    }


}