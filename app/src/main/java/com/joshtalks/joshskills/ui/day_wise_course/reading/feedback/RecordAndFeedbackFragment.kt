package com.joshtalks.joshskills.ui.day_wise_course.reading.feedback

import android.os.Bundle
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.FragmentRecordFeedbackBinding
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.entity.practise.PractiseType
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.translation.LanguageTranslationDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RecordAndFeedbackFragment : Fragment(), OnAudioRecordListener {

    private lateinit var binding: FragmentRecordFeedbackBinding
    private var filePath: String? = null
    private var practiceEngagement: PracticeEngagementV2? = null
    private var startTime: Long = 0

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PracticeViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            practiceEngagement = it.getParcelable(ARG_FEEDBACK)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_record_feedback,
            container,
            false
        )

        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProgressButton(binding.btnSubmitButton)
        binding.btnSubmitButton.attachTextChangeAnimator()
        initView()
    }

    private fun initView() {
        practiceEngagement?.run {
            if (PractiseType.SUBMITTED == practiseType) {
                binding.groupRecordView.visibility = View.GONE
                binding.cardViewFeedback.visibility = View.VISIBLE

                practiseFeedback?.let { feedback ->
                    binding.txtLabelFeedback.text = feedback.feedbackTitle
                    binding.txtFeedback.text = feedback.feedbackText

                    feedback.pronunciation?.let { pronunciation ->
                        binding.txtWordsPronounced.text = pronunciation.text
                        CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.LAZY) {
                            binding.txtPronunciationFeedback.text =
                                pronunciation.description.getSpannableString(
                                    separatorRegex,
                                    "<a>",
                                    "</a>",
                                    selectedColor = ContextCompat.getColor(
                                        requireContext(),
                                        R.color.e1_red
                                    ),
                                    defaultSelectedColor = ContextCompat.getColor(
                                        requireContext(),
                                        R.color.e1_red
                                    ),
                                    clickListener = object : OnWordClick {
                                        override fun clickedWord(word: String) {
                                            LanguageTranslationDialog.showLanguageDialog(
                                                childFragmentManager,
                                                word
                                            )
                                        }
                                    })
                        }
                        binding.pronunciationFeedbackView.visibility = View.VISIBLE
                    }
                    feedback.speed?.let { speed ->
                        binding.txtReadingSpeed.text = speed.text
                        binding.txtReadingSpeedFeedback.text = speed.description
                        binding.readingSpeedFeedbackView.visibility = View.VISIBLE
                    }
                    feedback.recommendation?.let { recommendation ->
                        val temp = "Recommendation:  "
                        val sBuilder = SpannableStringBuilder().append(temp)
                        sBuilder.append(recommendation.text)
                        sBuilder.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.grey_68
                                )
                            ), temp.length, sBuilder.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.txtRecommendation.setText(
                            sBuilder,
                            TextView.BufferType.SPANNABLE
                        )
                    }
                }
            } else {
                setUpAudioRecordTouchListener()
            }
        }
    }

    private fun setUpAudioRecordTouchListener() {
        binding.imgRecordButton.setOnClickListener {
            if (practiceViewModel.isRecordingStarted()) {
                stopAudioRecording()
            } else {
                if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                    requestAudioRecord()
                    return@setOnClickListener
                }
                startAudioRecording()
            }
        }
    }

    private fun requestAudioRecord() {
        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            startAudioRecording()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.record_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    private fun startAudioRecording() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        practiceViewModel.startRecordAudio(this)
    }

    private fun stopAudioRecording() {
        practiceViewModel.stopRecordingAudio(false)
    }


    override fun onRecordingStarted() {
        AppObjectController.uiHandler.post {
            binding.counterTv.visibility = View.VISIBLE
            binding.counterTv.base = SystemClock.elapsedRealtime()
            startTime = System.currentTimeMillis()
            binding.counterTv.start()
            binding.txtCaptionRecord.text = getString(R.string.recording_stop)
        }
    }

    override fun onRecordFinished(recordingItem: RecordingItem?) {
        showUIUserRecordingFailed()
        val timeDifference =
            TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                startTime
            )
        if (timeDifference > 1) {
            practiceViewModel.recordFile?.let {
                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                AppDirectory.copy(it.absolutePath, filePath!!)
                afterRecordCompleteUI()
            }
        } else {
            practiceViewModel.recordFile?.let {
                AppDirectory.deleteFile(it.absolutePath)
            }
        }
    }

    override fun onError(errorCode: Int) {
        showUIUserRecordingFailed()
    }

    private fun showUIUserRecordingFailed() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.txtCaptionRecord.text = getString(R.string.recording_start)
            binding.counterTv.stop()
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun afterRecordCompleteUI() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.counterTv.visibility = View.GONE
            binding.groupRecordView.visibility = View.GONE
            binding.btnSubmitButton.visibility = View.VISIBLE
            binding.cardViewAnswerVoiceNote.visibility = View.VISIBLE
            filePath?.let {
                binding.submitAudioNote.initAudioPlayer(
                    it,
                    Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt() ?: 0
                )
            }

        }
    }


    override fun onPause() {
        stopAudioRecording()
        super.onPause()
    }

    /** Start  View onclicks function **/
    fun cancelAudio() {
        try {
            binding.btnSubmitButton.visibility = View.GONE
            binding.counterTv.visibility = View.GONE
            binding.groupRecordView.visibility = View.VISIBLE
            binding.cardViewAnswerVoiceNote.visibility = View.GONE
            AppDirectory.deleteFile(filePath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun submitAnswer() {

    }

    private fun startSubmitProgress() {
        binding.btnSubmitButton.showProgress {
            progressColors =
                intArrayOf(ContextCompat.getColor(requireContext(), R.color.text_color_10))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnSubmitButton.isEnabled = false

    }


    private fun hideProgress() {
        binding.btnSubmitButton.isEnabled = true
        binding.btnSubmitButton.hideProgress(getString(R.string.submit_answer))
    }

    /**   end **/


    companion object {
        const val ARG_FEEDBACK = "feedback"
        const val separatorRegex = "<a>([\\s\\S]*?)<\\/a>"

        @JvmStatic
        fun newInstance(obj: PracticeEngagementV2) =
            RecordAndFeedbackFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FEEDBACK, obj)
                }
            }
    }
}