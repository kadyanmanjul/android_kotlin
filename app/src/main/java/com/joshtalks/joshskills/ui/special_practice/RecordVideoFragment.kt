package com.joshtalks.joshskills.ui.special_practice

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.databinding.FragmentRecordVideoBinding
import com.joshtalks.joshskills.util.VideoEditor
import com.joshtalks.joshskills.util.getNameString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class RecordVideoFragment : CoreJoshFragment(), VideoEditor.VideoFFMpegCallback {
    private lateinit var binding: FragmentRecordVideoBinding
    private val captureLiveStatus = MutableLiveData<String>()
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    enum class UiState {
        IDLE,
        RECORDING,
        FINALIZED,
        RECOVERY
    }

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    companion object {
        fun newInstance(): RecordVideoFragment {
            return RecordVideoFragment()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCameraFragment()
    }

    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            bindCaptureUsecase()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun initializeUI() {

        binding.recordVideoBtn.apply {
            setOnClickListener {
                if (!this@RecordVideoFragment::recordingState.isInitialized ||
                    recordingState is VideoRecordEvent.Finalize
                ) {
                    enableUI(false)
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
//                            binding.stopButton.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Pause -> currentRecording?.resume()
                        is VideoRecordEvent.Resume -> currentRecording?.pause()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
//            isEnabled = false
        }
    }

    private suspend fun bindCaptureUsecase() {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.LOWEST))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                videoCapture,
                preview
            )
        } catch (exc: Exception) {
            Log.e("TAG", "Use case binding failed", exc)
            resetUIandState("bindToLifecycle failed: $exc")
        }
        enableUI(true)
    }

    private fun resetUIandState(reason: String) {
        enableUI(true)
        showUI(UiState.IDLE, reason)
    }

    private fun enableUI(enable: Boolean) {
        binding.recordVideoBtn.isEnabled = enable
    }

    private fun showUI(state: UiState, status: String = "idle") {
        binding.let {
            when (state) {
                UiState.IDLE -> {
                    it.recordVideoBtn.setImageResource(R.drawable.bg_white_round_36)
                }
                UiState.RECORDING -> {
                    it.recordVideoBtn.setImageResource(R.drawable.ic_stop_white_48)
                }
                UiState.FINALIZED -> {
                    it.recordVideoBtn.setImageResource(R.drawable.bg_white_round_36)
                }
                else -> {
                    val errorMsg = "Error: showUI($state) is not supported"
                    Log.e("TAG", errorMsg)
                    return
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" +
                SimpleDateFormat("ddMMyyyy", Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireActivity().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutput)
            .apply { withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)

        Log.i("Yash", "Recording started")
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
            /*lifecycleScope.launch {
                navController.navigate(
                    CaptureFragmentDirections.actionCaptureToVideoViewer(
                        event.outputResults.outputUri
                    )
                )
            }*/
            Log.d("Yash", "video saved to uri: ${event.outputResults.outputUri}")
        }
    }

    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Status -> {
                // placeholder: we update the UI with new status after this when() block,
                // nothing needs to do here.
            }
            is VideoRecordEvent.Start -> {
                binding.chronometer.start()
                showUI(UiState.RECORDING, event.getNameString())
            }
            is VideoRecordEvent.Finalize -> {
                binding.chronometer.stop()
                showUI(UiState.FINALIZED, event.getNameString())
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if (event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        captureLiveStatus.value = text
        Log.i("Yash", "recording event: $text")
    }
    /*
    captureViewBinding.stopButton.apply {
        setOnClickListener {
            // stopping: hide it after getting a click before we go to viewing fragment
            captureViewBinding.stopButton.visibility = View.INVISIBLE
            if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
                return@setOnClickListener
            }

            val recording = currentRecording
            if (recording != null) {
                recording.stop()
                currentRecording = null
            }
            captureViewBinding.captureButton.setImageResource(R.drawable.ic_start)
        }
        // ensure the stop button is initialized disabled & invisible
        visibility = View.INVISIBLE
        isEnabled = false
    }*/


    private fun addOverlayToVideo(videoPath: String, imagePath: String) {
        VideoEditor.with(requireContext())
            .setVideoPath(videoPath)
            .setImagePath(imagePath)
            .setPosition(VideoEditor.OverlayPosition.BOTTOM_CENTER_ALIGN)
            .setCallback(this)
            .execute()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onProgress(progress: String) {
        Log.i("Yash", "onProgress: $progress")
    }

    override fun onSuccess(convertedFile: File, type:
    String) {
        Log.d("Yash", "onSuccess: $convertedFile")
    }

    override fun onFailure(error: Exception) {
        error.printStackTrace()
    }

    override fun onNotAvailable(error: Exception) {
        error.printStackTrace()
    }

    override fun onFinish() {
        Log.d("Yash", "onFinish: ")
    }

}