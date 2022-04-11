package com.joshtalks.joshskills.ui.special_practice

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentRecordVideoBinding
import com.joshtalks.joshskills.ui.special_practice.base.BaseKFactorFragment
import com.joshtalks.joshskills.ui.special_practice.utils.KFactorUtils
import com.joshtalks.joshskills.ui.special_practice.utils.KFactorUtils.changeUIAccordingToState
import com.joshtalks.joshskills.ui.special_practice.utils.KFactorUtils.getTime
import com.joshtalks.joshskills.ui.special_practice.utils.KFactorUtils.setBtnBackgroundResources
import com.joshtalks.joshskills.ui.special_practice.utils.KFactorUtils.updateUI
import com.joshtalks.joshskills.ui.special_practice.utils.getRecordingFileName
import com.joshtalks.joshskills.ui.special_practice.viewmodel.SpecialPracticeViewModel
import com.joshtalks.joshskills.util.getBitMapFromView
import com.joshtalks.joshskills.util.toFile
import com.joshtalks.joshskills.util.uriToFile
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

class RecordVideoFragment : BaseKFactorFragment() {
    private lateinit var binding: FragmentRecordVideoBinding
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent
    private var timer: CountDownTimer? = null
    var name: String? = EMPTY

    val spviewModel by lazy {
        ViewModelProvider(requireActivity())[SpecialPracticeViewModel::class.java]
    }

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        timer = getTime(binding.recordVideoBtn)
        binding.vm = spviewModel
        binding.executePendingBindings()
        initCameraFragment()
    }

    override fun initViewState() {}
    override fun setArguments() {}
    override fun onBackPressed() {
        spviewModel.moveToActivity()
    }

    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            videoCapture = KFactorUtils.bindCaptureUseCase(
                requireContext(),
                viewLifecycleOwner,
                binding.previewView.surfaceProvider
            )
        }
    }

    private fun initializeUI() {
        try {
            binding.recordVideoBtn.apply {
                setOnClickListener {
                    if (!this@RecordVideoFragment::recordingState.isInitialized || recordingState is VideoRecordEvent.Finalize) {
                        startRecordingAndTimer()
                        setBtnBackgroundResources(binding.recordVideoBtn)
                    } else {
                        changeUIAccordingToState(binding.recordVideoBtn, recordingState)
                    }
                }
            }
        } catch (ex: Exception) {
        }
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        try {
            if (event !is VideoRecordEvent.Status)
                recordingState = event

            updateUI(event, binding.chronometer)

            if (event is VideoRecordEvent.Finalize) {
                spviewModel.imageNameForDelete.set(name)
                spviewModel.videoUri.set(event.outputResults.outputUri.toString())
                spviewModel.cameraVideoPath.set(
                    uriToFile(
                        requireContext(),
                        event.outputResults.outputUri
                    ).absolutePath
                )
                spviewModel.imagePathForSetOnVideo.set(
                    getBitMapFromView(binding.imageOverlay).toFile(
                        requireContext()
                    ).absolutePath
                )

                spviewModel.openViewShareFrag()
            }
        } catch (ex: Exception) {
        }
    }

    fun startRecordingAndTimer() {
        name = getRecordingFileName()
        currentRecording = KFactorUtils.startRecording(
            requireContext(),
            name ?: EMPTY,
            mainThreadExecutor,
            captureListener
        )
        timer?.start()
    }
}