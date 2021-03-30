package com.joshtalks.joshskills.ui.lesson.reading.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieCompositionFactory
import com.github.razir.progressbutton.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.FragmentRecordFeedbackBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.entity.practise.PractiseType
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class RecordAndFeedbackFragment : Fragment(), OnAudioRecordListener {

    private lateinit var binding: FragmentRecordFeedbackBinding
    private var filePath: String? = null
    private var practiceEngagement: PracticeEngagementV2? = null
    private var startTime: Long = 0
    private var isImproveEnable: Boolean = false
    var callback: ReadingPractiseCallback? = null
    var submit: DOWNLOAD_STATUS = DOWNLOAD_STATUS.NOT_START


    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PracticeViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is ReadingPractiseCallback) {
            callback = parentFragment as ReadingPractiseCallback
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            practiceEngagement = it.getParcelable(ARG_FEEDBACK)
            isImproveEnable = it.getBoolean(ARG_IMPROVE, false)
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

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProgressButton(binding.btnSubmitButton)
        binding.btnSubmitButton.attachTextChangeAnimator()
        initView()
    }

    private fun initView() {
        CoroutineScope(Dispatchers.Main).launch {
            practiceEngagement?.run {
                if (uploadStatus == DOWNLOAD_STATUS.UPLOADED) {
                    if (PractiseType.SUBMITTED == practiseType) {
                        binding.groupRecordView.visibility = View.GONE
                        binding.cardViewFeedback.visibility = View.VISIBLE

                        if (isImproveEnable) {
                            binding.txtImproveButton.visibility = View.VISIBLE
                            binding.txtContinueButton.visibility = View.VISIBLE
                        }
                        practiceEngagement?.answerUrl?.let {
                            binding.imgCancel.visibility = View.INVISIBLE
                            binding.cardViewAnswerVoiceNote.visibility = View.VISIBLE
                            binding.submitAudioNote.initAudioPlayer(
                                it,
                                practiceEngagement?.duration ?: 0
                            )
                        }

                        practiseFeedback?.let { feedback ->
                            binding.txtLabelFeedback.text = feedback.feedbackTitle
                            binding.txtFeedback.text = feedback.feedbackText

                            if (feedback.error) {
                                return@launch
                            }
                            feedback.pronunciation?.let { pronunciation ->
                                binding.txtWordsPronounced.text = pronunciation.text
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
                                                feedback.pointsList?.find {
                                                    it.word.equals(
                                                        word,
                                                        ignoreCase = true
                                                    )
                                                }?.let {
                                                    ReadingResultFragment.showLanguageDialog(
                                                        childFragmentManager,
                                                        it,
                                                        feedback.teacherAudioUrl,
                                                        feedback.studentAudioUrl
                                                    )
                                                }
                                            }
                                        })
                                binding.pronunciationFeedbackView.visibility = View.VISIBLE
                            }
                            feedback.speed?.let { speed ->
                                binding.txtReadingSpeed.text = speed.text
                                binding.txtReadingSpeedFeedback.text = speed.description
                                binding.readingSpeedFeedbackView.visibility = View.VISIBLE
                            }
                            feedback.recommendation?.let { recommendation ->
                                val temp = "Recommendation:  "
                                val sBuilder = SpannableStringBuilder(temp)
                                sBuilder.setSpan(
                                    StyleSpan(Typeface.BOLD),
                                    0,
                                    temp.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
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

                                val params: ConstraintLayout.LayoutParams =
                                    binding.txtRecommendation.layoutParams as ConstraintLayout.LayoutParams
                                params.setMargins(0, Utils.dpToPx(16), 0, Utils.dpToPx(16))
                                binding.txtRecommendation.layoutParams = params
                                binding.txtRecommendation.visibility = View.VISIBLE
                            }

                        }
                    } else {
                        setUpAudioRecordTouchListener()
                    }
                } else {
                    submit = DOWNLOAD_STATUS.STARTED
                    binding.btnSubmitButton.visibility = View.VISIBLE
                    startSubmitProgress()
                }
            }
            LottieCompositionFactory.fromAsset(requireContext(), "lottie/audio_record.json")
                .addListener {
                    binding.imgRecordAnimationView.setComposition(it)
                    binding.imgRecordAnimationView.playAnimation()
                }
            binding.imgRecordAnimationView.setSafeMode(true)
        }
    }

    private fun setUpAudioRecordTouchListener() {
        binding.imgRecordButton.setOnClickListener {
            if (isCallOngoing()) {
                return@setOnClickListener
            }
            if (practiceViewModel.isRecordingStarted()) {
                stopAudioRecording()
            } else {
                if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                    RxBus2.publish(ViewPagerDisableEventBus(true))
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
        binding.imgRecordButton.setImageResource(0)
        binding.imgRecordAnimationView.visibility = View.VISIBLE
        RxBus2.publish(ViewPagerDisableEventBus(false))
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        practiceViewModel.startRecordAudio(this)
    }

    private fun stopAudioRecording() {
        practiceViewModel.stopRecordingAudio(false)
    }


    override fun onRecordingStarted() {
        AppObjectController.uiHandler.post {
            binding.rootView.requestDisallowInterceptTouchEvent(true)
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
                filePath = AppDirectory.getAudioSentFile(null, audioExtension = ".m4a").absolutePath
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
        RxBus2.publish(ViewPagerDisableEventBus(true))
        CoroutineScope(Dispatchers.Main).launch {
            binding.rootView.requestDisallowInterceptTouchEvent(false)
            binding.txtCaptionRecord.text = getString(R.string.recording_start)
            binding.counterTv.stop()
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun afterRecordCompleteUI() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.imgRecordAnimationView.visibility = View.INVISIBLE
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

    override fun onStop() {
        super.onStop()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    /** Start  View onclicks function **/
    fun cancelAudio() {
        try {
            binding.imgRecordButton.setImageResource(R.drawable.ic_mic_white_24dp)
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
        if (DOWNLOAD_STATUS.NOT_START == submit) {
            submit = DOWNLOAD_STATUS.STARTED
            callback?.onPracticeSubmitted()
            practiceEngagement?.questionForId?.let {
                practiceViewModel.submitReadingPractise(it, filePath!!)
            }
            startSubmitProgress()
        }
    }

    private fun startSubmitProgress() {
        AppObjectController.uiHandler.postDelayed(Runnable {
            binding.btnSubmitButton.showProgress {
                //buttonTextRes = R.string.plz_wait
                progressColors =
                    intArrayOf(
                        ContextCompat.getColor(requireContext(), R.color.white),
                        ContextCompat.getColor(requireContext(), R.color.practise_complete_tint),
                        ContextCompat.getColor(requireContext(), R.color.green_3d)
                    )

                gravity = DrawableButton.GRAVITY_CENTER
                progressRadiusRes = R.dimen.dp8
                progressStrokeRes = R.dimen.dp2
                textMarginRes = R.dimen.dp8
            }
        }, 250)
    }


    private fun hideProgress() {
        binding.btnSubmitButton.isEnabled = true
        binding.btnSubmitButton.hideProgress(getString(R.string.submit_answer))
    }

    fun improveAnswer() {
        if (isImproveEnable) {
            callback?.onImproveAnswer()
        }
    }

    fun continueAnswer() {
        callback?.onContinue()

    }

    /**   end **/


    companion object {
        const val ARG_FEEDBACK = "feedback"
        const val ARG_IMPROVE = "improve"

        const val separatorRegex = "<a>([\\s\\S]*?)<\\/a>"

        @JvmStatic
        fun newInstance(obj: PracticeEngagementV2, isImprove: Boolean) =
            RecordAndFeedbackFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FEEDBACK, obj)
                    putBoolean(ARG_IMPROVE, isImprove)
                }
            }
    }
}


interface ReadingPractiseCallback {
    fun onImproveAnswer()
    fun onContinue()
    fun onPracticeSubmitted()
}