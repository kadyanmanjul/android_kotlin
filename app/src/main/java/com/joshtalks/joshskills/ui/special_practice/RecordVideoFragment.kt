package com.joshtalks.joshskills.ui.special_practice

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import com.joshtalks.joshskills.util.getBitMapFromView
import com.joshtalks.joshskills.util.getNameString
import com.joshtalks.joshskills.util.toFile
import com.joshtalks.joshskills.util.uriToFile
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class RecordVideoFragment : CoreJoshFragment() {
    private lateinit var binding: FragmentRecordVideoBinding
    private val captureLiveStatus = MutableLiveData<String>()
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent
    private var specialId = 0

    enum class UiState {
        IDLE,
        RECORDING,
        FINALIZED,
        RECOVERY
    }

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    companion object {
        private const val WORD_IN_ENGLISH = "WORD_IN_ENGLISH"
        private const val SENTENCE_IN_ENGLISH = "SENTENCE_IN_ENGLISH"
        private const val WORD_IN_HINDI = "WORD_IN_HINDI"
        private const val SENTENCE_IN_HINDI = "SENTENCE_IN_HINDI"
        private const val SPECIAL_ID = "SPECIAL_ID"
        fun newInstance(
            wordInEnglish: String,
            sentenceInEnglish: String,
            wordInHindi: String,
            sentenceInHindi: String
        ): RecordVideoFragment {
            val args = Bundle()
            args.putString(WORD_IN_ENGLISH, wordInEnglish)
            args.putString(SENTENCE_IN_ENGLISH, sentenceInEnglish)
            args.putString(WORD_IN_HINDI, wordInHindi)
            args.putString(SENTENCE_IN_HINDI, sentenceInHindi)
            val fragment = RecordVideoFragment()
            fragment.arguments = args
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

        arguments?.getString(WORD_IN_ENGLISH)?.let {
            binding.wordInEnglish.text = it
        }
        arguments?.getString(SENTENCE_IN_ENGLISH)?.let {
            binding.sentenceInEnglish.text = it
        }
        arguments?.getString(WORD_IN_HINDI)?.let {
            binding.wordInHindi.text = it
        }
        arguments?.getString(SENTENCE_IN_HINDI)?.let {
            binding.sentenceInHindi.text = it
        }
        onBackPress()
    }

    private fun setImageData() {
        arguments?.getString(WORD_IN_ENGLISH)?.let {
            binding.wordInEnglish.text = it
        }
        arguments?.getString(SENTENCE_IN_ENGLISH)?.let {
            binding.sentenceInEnglish.text = it
        }
        arguments?.getString(WORD_IN_HINDI)?.let {
            binding.wordInHindi.text = it
        }
        arguments?.getString(SENTENCE_IN_HINDI)?.let {
            binding.sentenceInHindi.text = it
        }
    }

    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            bindCaptureUseCase()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun initializeUI() {

        binding.recordVideoBtn.apply {
            setOnClickListener {
                if (!this@RecordVideoFragment::recordingState.isInitialized || recordingState is VideoRecordEvent.Finalize) {
                    startRecording()
                    setBackgroundResource(R.drawable.ic_ellipse_50__2_)
                    setImageResource(R.drawable.ic_rectangle_stop)
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.stop()
                            setBackgroundResource(R.drawable.ic_ellipse_50__2_)
                            setImageResource(R.drawable.ic_rectangle_stop)
                        }
                        is VideoRecordEvent.Pause -> {
                            currentRecording?.resume()
                            setImageDrawable(null)
                        }
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
        }
    }

    private suspend fun bindCaptureUseCase() {
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
            Log.e("Sagar", "Use case binding failed", exc)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "JoshSkill-recording-" +
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

        Log.i("Sagar", "Recording started")
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_record_container,
                    ViewAndShareVideoFragment.newInstance(
                        videoPath = uriToFile(
                            requireContext(),
                            event.outputResults.outputUri
                        ).absolutePath,
                        imagePath = getBitMapFromView(binding.imageOverlay).toFile(requireContext()).absolutePath,
                        imageBitmap = getBitMapFromView(binding.imageOverlay),
                    )
                ).commit()
            Log.d("Sagar", "video saved to uri: ${event.outputResults.outputUri}")
        }
    }

    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Start -> {
                binding.chronometer.visibility = View.VISIBLE
                binding.chronometer.base = SystemClock.elapsedRealtime()
                binding.chronometer.start()
            }
            is VideoRecordEvent.Finalize -> {
                binding.chronometer.stop()
                binding.chronometer.visibility = View.INVISIBLE
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if (event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        captureLiveStatus.value = text
        Log.i("Sagar", "recording event: $text")
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveToNewActivity()
                }
            })
    }

    private fun moveToNewActivity() {
        val i = Intent(activity, SpecialPracticeActivity::class.java)
        startActivity(i)
        (activity as Activity?)?.overridePendingTransition(0, 0)
        requireActivity().finish()
    }
}