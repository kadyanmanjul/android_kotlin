package com.joshtalks.joshskills.ui.special_practice

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import com.joshtalks.joshskills.core.EMPTY
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
    private var specialId: String? = null
    private var wordInEnglish: String? = null
    private var wordInHindi: String? = null
    private var sentenceInEnglish: String? = null
    private var sentenceInHindi: String? = null
    private var timer: CountDownTimer? = null


    enum class UiState {
        IDLE,
        RECORDING,
        FINALIZED,
        RECOVERY
    }

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            wordInEnglish = it.getString(WORD_IN_ENGLISH)
            sentenceInEnglish = it.getString(SENTENCE_IN_ENGLISH)
            wordInHindi = it.getString(WORD_IN_HINDI)
            sentenceInHindi = it.getString(SENTENCE_IN_HINDI)
            specialId = it.getString(SPECIAL_ID)
        }
    }

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
            sentenceInHindi: String,
            specialId11: String
        ) =
            RecordVideoFragment().apply {
                arguments = Bundle().apply {
                    putString(WORD_IN_ENGLISH, wordInEnglish)
                    putString(SENTENCE_IN_ENGLISH, sentenceInEnglish)
                    putString(WORD_IN_HINDI, wordInHindi)
                    putString(SENTENCE_IN_HINDI, sentenceInHindi)
                    putString(SPECIAL_ID, specialId11)
                }
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
        getTime()
        initCameraFragment()
        setImageData()
        onBackPress()
    }

    private fun setImageData() {
        if (wordInEnglish != EMPTY && sentenceInEnglish != EMPTY && wordInHindi != EMPTY && sentenceInHindi != EMPTY) {
            binding.wordInEnglish.text = wordInEnglish
            binding.sentenceInEnglish.text = sentenceInEnglish
            binding.wordInHindi.text = wordInHindi
            binding.sentenceInHindi.text = sentenceInHindi
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
        try {
            binding.recordVideoBtn.apply {
                setOnClickListener {
                    if (!this@RecordVideoFragment::recordingState.isInitialized || recordingState is VideoRecordEvent.Finalize) {
                        startRecording()
                        timer?.start()
                        setBackgroundResource(R.drawable.ic_ellipse_50__2_)
                        setImageResource(R.drawable.ic_rectangle_stop)
                    } else {
                        when (recordingState) {
                            is VideoRecordEvent.Start -> {
                                timer?.onFinish()
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
        } catch (ex: Exception) {
        }
    }

    private suspend fun bindCaptureUseCase() {
        try {
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
        } catch (ex: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        try {
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

        } catch (ex: Exception) {
        }
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        try {
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
                            imagePath = getBitMapFromView(binding.imageOverlay).toFile(
                                requireContext()
                            ).absolutePath,
                            imageBitmap = getBitMapFromView(binding.imageOverlay),
                            specialId ?: EMPTY
                        )
                    ).commit()
            }
        } catch (ex: Exception) {

        }
    }

    fun getTime() {
        try {
            timer = object : CountDownTimer(30000, 1000) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {
                    currentRecording?.stop()
                    binding.recordVideoBtn.setBackgroundResource(R.drawable.ic_ellipse_50__2_)
                    binding.recordVideoBtn.setImageResource(R.drawable.ic_rectangle_stop)
                }
            }
        } catch (ex: Exception) {
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
        try {
            val i = Intent(activity, SpecialPracticeActivity::class.java)
            i.putExtra(SPECIAL_ID, specialId)
            startActivity(i)
            requireActivity().overridePendingTransition(0, 0)
            requireActivity().finish()
        } catch (ex: Exception) {
        }
    }
}