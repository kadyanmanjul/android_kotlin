package com.joshtalks.joshskills.ui.online_test

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.databinding.FragmentOnlineTestBinding
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.GrammarHeadingView
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.online_test.util.*
import com.joshtalks.joshskills.ui.online_test.vh.AtsChoiceView
import com.joshtalks.joshskills.ui.online_test.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.online_test.vh.McqChoiceView
import com.joshtalks.joshskills.ui.online_test.vh.SubjectiveChoiceView
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView
import com.joshtalks.joshskills.util.ExoAudioPlayer2
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import com.userexperior.utilities.SecureViewBucket
import java.lang.reflect.Type
import kotlin.random.Random

class OnlineTestFragment :
    CoreJoshFragment(),
    GrammarSubmitButtonListener,
    GrammarButtonViewCallback,
    ViewTreeObserver.OnScrollChangedListener, AudioPlayerEventListener {

    private lateinit var binding: FragmentOnlineTestBinding

    private val viewModel: OnlineTestViewModel by lazy {
        ViewModelProvider(requireActivity()).get(OnlineTestViewModel::class.java)
    }
    private var lessonNumber: Int = -1
    private var lessonId: Int = -1
    private var headingView: Stub<GrammarHeadingView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var errorView: Stub<ErrorView>? = null
    private var lessonActivityListener: LessonActivityListener? = null
    private var testCompletedListener: TestCompletedListener? = null
    private var ruleAssessmentQuestionId: String? = null
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var audioManager: ExoAudioPlayer2? = null
    private var atsChoiceView: Stub<AtsChoiceView>? = null
    private var mcqChoiceView: Stub<McqChoiceView>? = null
    private var subjectiveChoiceView: Stub<SubjectiveChoiceView>? = null
    private var reviseVideoObject: VideoModel? = null
    private var isTestCompleted: Boolean = false
    private var scoreText: Int? = 0
    private var pointsList: String? = null
    private var previousId: Int = -1
    private var isAttempted: Boolean = false
    private var questionStartTime: Long = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
        }
        if (context is TestCompletedListener) {
            testCompletedListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            lessonNumber = it.getInt(GrammarOnlineTestFragment.CURRENT_LESSON_NUMBER, -1)
        }

        lessonId = if (requireActivity().intent.hasExtra(LessonActivity.LESSON_ID)) {
            requireActivity().intent.getIntExtra(LessonActivity.LESSON_ID, 0)
        } else 0
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_online_test,
            container,
            false
        )
        binding.handler = this
        binding.lifecycleOwner = this
        setObservers()
        binding.progressContainer.visibility = View.VISIBLE
        viewModel.fetchQuestionsOrPostAnswer(lessonId)
        audioManager = ExoAudioPlayer2.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    fun initViews() {
        headingView = Stub(binding.choiceContainer.findViewById(R.id.heading_view))
        buttonView = Stub(binding.container.findViewById(R.id.button_action_views))
        errorView = Stub(binding.container.findViewById(R.id.error_view))
        atsChoiceView = Stub(binding.container.findViewById(R.id.ats_choice_view))
        mcqChoiceView = Stub(binding.container.findViewById(R.id.mcq_choice_view))
        subjectiveChoiceView = Stub(binding.container.findViewById(R.id.subjective_choice_view))
    }

    private fun setObservers() {
        viewModel.grammarAssessmentLiveData.observe(viewLifecycleOwner) { onlineTestResponse ->
            this.ruleAssessmentQuestionId = onlineTestResponse.ruleAssessmentQuestionId
            this.reviseVideoObject = onlineTestResponse.videoObject
            binding.questionProgressBar.apply {
                max =
                    (onlineTestResponse.totalQuestions ?: 0).times(100)
                isVisible = onlineTestResponse.totalQuestions != null
                animateProgress(onlineTestResponse.totalAnswered ?: 0)
            }
            binding.markAsCorrect.isVisible = BuildConfig.DEBUG
            binding.markAsCorrect.setOnClickListener {
//                 TODO: STOPSHIP this is a temporary fix for marking as correct
                buttonView?.get()?.showAnswerFeedbackView(true)
                buttonView?.get()?.toggleSubmitButton(true)
                assessmentQuestion!!.question.isCorrect = true
                assessmentQuestion!!.question.status = QuestionStatus.CORRECT
                playSnackbarSound(requireActivity())
                getViewStub()?.lockViews()
                binding.markAsCorrect.isEnabled = false
                val answerText: String? = when (assessmentQuestion!!.question.choiceType) {
                    ChoiceType.SINGLE_SELECTION_TEXT -> assessmentQuestion!!.choiceList.first { it.isCorrect }.text
                    ChoiceType.ARRANGE_THE_SENTENCE -> assessmentQuestion!!.question.listOfAnswers?.first()
                    ChoiceType.INPUT_TEXT -> assessmentQuestion!!.choiceList.first().text
                    else -> ""
                }
                viewModel.storeAnswerToDb(
                    assessmentQuestion = assessmentQuestion!!,
                    lessonId = lessonId,
                    lessonNumber = lessonNumber,
                    answerText = answerText ?: "",
                    ruleAssessmentQuestionId = ruleAssessmentQuestionId,
                    timeTaken = System.currentTimeMillis() - questionStartTime
                )
                isAttempted = true
            }
            if (onlineTestResponse.completed) {
                PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, lessonNumber)
                if (onlineTestResponse.ruleAssessmentId != null) {
                    addNewRuleCompleted(onlineTestResponse.ruleAssessmentId)
                }
                isTestCompleted = onlineTestResponse.completed
                onlineTestResponse.scoreText?.let {
                    scoreText = it
                    PrefManager.put(
                        FREE_TRIAL_TEST_SCORE, it, false
                    )
                }
                onlineTestResponse.pointsList?.let { pointsListRes ->
                    pointsList = pointsListRes.get(0)
                    PrefManager.put(
                        LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                        pointsListRes.last(),
                        false
                    )
                }
                showGrammarCompleteFragment()
            } else {
                if (onlineTestResponse.ruleAssessmentId != null) {
                    if (previousId != onlineTestResponse.ruleAssessmentId &&
                        onlineTestResponse.questiontype == OnlineTestType.TEST && previousId != -1
                    ) {
                        addNewRuleCompleted(previousId)
                    }
                    previousId = onlineTestResponse.ruleAssessmentId
                }
                onlineTestResponse.question?.let {
                    this.assessmentQuestion = AssessmentQuestionWithRelations(it, 10)
                    this.assessmentQuestion?.choiceList?.let { list ->
                        viewModel.insertChoicesToDB(
                            list, this.assessmentQuestion
                        )
                    }
                    moveToNextGrammarQuestion()
                }
            }
        }
        viewModel.apiStatus.observe(viewLifecycleOwner)
        {
            when (it) {
                ApiCallStatus.START -> {
                    toggleLoading(true)
                }
                ApiCallStatus.FAILED -> {
                    binding.progressContainer.visibility = View.GONE
                    toggleLoading(false)
                    errorView?.resolved()?.let {
                        errorView!!.get().onFailure(object : ErrorView.ErrorCallback {
                            override fun onRetryButtonClicked() {
                                viewModel.fetchQuestionsOrPostAnswer(lessonId)
                            }
                        })
                    }
                }
                ApiCallStatus.SUCCESS -> {
                    toggleLoading(false)
                    binding.progressContainer.visibility = View.GONE
                    errorView?.resolved()?.let {
                        errorView!!.get().onSuccess()
                    }
                }
                else -> {
                    toggleLoading(false)
                    binding.progressContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViews(assessmentQuestions: AssessmentQuestionWithRelations) {
        if (isTestCompleted) {
            showGrammarCompleteFragment()
            return
        }
        downloadAudios(assessmentQuestions.choiceList)
        questionStartTime = System.currentTimeMillis()
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.question.mediaUrl,
                assessmentQuestions.question.mediaUrl2,
                assessmentQuestions.question.text,
                assessmentQuestions.question.subText,
                assessmentQuestions.question.isNewHeader
            )
        }
        getViewStub()?.apply {
            setup(assessmentQuestions)
            addGrammarTestCallback(this@OnlineTestFragment)
        }
        binding.markAsCorrect.isEnabled = true
        buttonView?.resolved()?.let {
            buttonView!!.get().setup(assessmentQuestions, reviseVideoObject?.id)
            buttonView!!.get().addCallback(this)
        }
    }

    fun showGrammarCompleteFragment() {
        activity?.supportFragmentManager?.beginTransaction()?.replace(
            R.id.parent_Container,
            GrammarOnlineTestFragment.getInstance(lessonNumber, scoreText, pointsList),
            GrammarOnlineTestFragment.TAG
        )?.addToBackStack(GrammarOnlineTestFragment.TAG)?.commitAllowingStateLoss()
        testCompletedListener?.onTestCompleted()
    }

    private fun addNewRuleCompleted(ruleCompletedId: Int) {
        animateProgress(0)
        val mapTypeToken: Type = object : TypeToken<List<Int>?>() {}.type
        val list: List<Int>? = AppObjectController.gsonMapper.fromJson(
            PrefManager.getStringValue(ONLINE_TEST_LIST_OF_COMPLETED_RULES),
            mapTypeToken
        )
        val newUpdateList = mutableSetOf<Int>()
        if (list.isNullOrEmpty().not()) {
            newUpdateList.addAll(list!!)
        }
        if (newUpdateList.contains(ruleCompletedId).not()) {
            newUpdateList.add(ruleCompletedId)
            PrefManager.put(
                ONLINE_TEST_LIST_OF_COMPLETED_RULES,
                newUpdateList.toString()
            )
            viewModel.sendCompletedRuleIdsToBAckend(ruleCompletedId)
        }
        if (PrefManager.hasKey(IS_A2_C1_RETENTION_ENABLED) && PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID) {
            viewModel.postGoal("RULE_${lessonNumber}_COMPLETED", CampaignKeys.A2_C1.name)
        }
        showGrammarCompleteFragment()
    }

    @SuppressLint("Recycle")
    private fun animateProgress(answeredCount: Int) {
        val currentProgress = binding.questionProgressBar.progress
        val finalProgress = answeredCount.times(100)
        ValueAnimator.ofInt(currentProgress, finalProgress).apply {
            duration = 400
            addUpdateListener {
                binding.questionProgressBar.progress = it.animatedValue as Int
            }
            start()
        }
    }

    private fun isAudioPlaying(): Boolean =
        this.checkIsPlayer() && this.audioManager!!.isPlaying()

    private fun checkIsPlayer(): Boolean =
        audioManager != null

    private fun onPlayAudio(
        audioObject: AudioType
    ) {
        try {
            audioManager?.playerListener = this
            audioManager?.play(
                audioObject.audio_url,
                playbackSpeed = AppObjectController.getFirebaseRemoteConfig()
                    .getDouble(FirebaseRemoteConfigKey.GRAMMAR_CHOICE_PLAYBACK_SPEED).toFloat()
            )
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    override fun toggleSubmitButton(isEnabled: Boolean) {
        buttonView?.get()?.toggleSubmitButton(isEnabled)
    }

    override fun toggleLoading(isLoading: Boolean) {
        buttonView?.get()?.toggleLoading(isLoading)
    }

    fun isCorrectAnswer(): Boolean = getViewStub()?.isAnswerCorrect() ?: false

    fun getAnswerText(): String = getViewStub()?.getAnswerText() ?: ""

    override fun playAudio(audioUrl: String?, localAudioPath: String?) {
        val audioUrl2 = localAudioPath ?: audioUrl?.replace(" ".toRegex(), "%20")
        audioUrl2?.let { url ->
            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication)
                    .gravity(Gravity.BOTTOM)
                    .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                    .cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }
            if (isAudioPlaying())
                audioManager?.onPause()
            val audioType = AudioType()
            audioType.audio_url = url
            audioType.downloadedLocalPath = url
            audioType.duration = 1_00
            audioType.id = Random.nextInt().toString()
            onPlayAudio(audioType)
        }
    }

    companion object {
        const val TAG = "OnlineTestFragment"

        @JvmStatic
        fun getInstance(lessonNumber: Int): OnlineTestFragment {
            val args = Bundle()
            args.putInt(GrammarOnlineTestFragment.CURRENT_LESSON_NUMBER, lessonNumber)
            val fragment = OnlineTestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onGrammarButtonClick() {
        if (isAttempted) {
            viewModel.fetchQuestionsOrPostAnswer(lessonId)
        } else {
            isCorrectAnswer().run {
                assessmentQuestion!!.question.status =
                    if (this) QuestionStatus.CORRECT else QuestionStatus.WRONG
                if (this)
                    playSnackbarSound(requireActivity())
                else
                    playWrongAnswerSound(requireActivity())
                buttonView?.get()?.showAnswerFeedbackView(this)
                viewModel.storeAnswerToDb(
                    assessmentQuestion = assessmentQuestion!!,
                    answerText = getAnswerText(),
                    lessonId = lessonId,
                    lessonNumber = lessonNumber,
                    ruleAssessmentQuestionId = ruleAssessmentQuestionId,
                    timeTaken = System.currentTimeMillis() - questionStartTime
                )
            }
            isAttempted = true
        }
    }

    private fun moveToNextGrammarQuestion() {
        isAttempted = false
        lessonActivityListener?.onLessonUpdate()
        assessmentQuestion?.let { setupViews(it) } ?: showGrammarCompleteFragment()
    }

    override fun showTooltip(
        wrongAnswerTitle: String?,
        wrongAnswerDescription: String?,
        explanationTitle: String?,
        explanationText: String?,
    ) {
        if (PrefManager.hasKey(
                HAS_SEEN_QUIZ_VIDEO_TOOLTIP,
            ).not()
        ) {
            lessonActivityListener?.showVideoToolTip(
                shouldShow = true,
                wrongAnswerHeading = wrongAnswerTitle,
                wrongAnswerSubHeading = wrongAnswerDescription,
                wrongAnswerText = explanationText,
                wrongAnswerDescription = wrongAnswerDescription,
                videoClickListener = { onVideoButtonClicked() }
            )
        }
    }

    fun getViewStub(): AssessmentQuestionViewCallback? {
        return when (assessmentQuestion?.question?.choiceType) {
            ChoiceType.ARRANGE_THE_SENTENCE -> atsChoiceView?.get()?.also {
                it.visibility = View.VISIBLE
                mcqChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
            }
            ChoiceType.SINGLE_SELECTION_TEXT -> mcqChoiceView?.get()?.also {
                it.visibility = View.VISIBLE
                atsChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
            }
            ChoiceType.INPUT_TEXT -> subjectiveChoiceView?.get()?.also {
                it.visibility = View.VISIBLE
                SecureViewBucket.removeFromSecureViewBucket(it)
                atsChoiceView?.get()?.visibility = View.GONE
                mcqChoiceView?.get()?.visibility = View.GONE
            }
            else -> null
        }
    }

    override fun onVideoButtonClicked() {
        A2C1Impressions.saveImpression(A2C1Impressions.Impressions.RULE_VIDEO_PLAYED)
        reviseVideoObject?.let { LessonActivity.videoEvent.postValue(Event(it)) }
    }

    private fun downloadAudios(choiceList: List<Choice>) {
        if (PermissionUtils.isStoragePermissionEnabled(AppObjectController.joshApplication).not()) {
            askStoragePermission(choiceList)
            return
        }
        viewModel.downloadAudioFileForNewGrammar(choiceList)
    }

    private fun askStoragePermission(choiceList: List<Choice>) {
        PermissionUtils.storageReadAndWritePermission(
            requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            viewModel.downloadAudioFileForNewGrammar(choiceList)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                            //errorDismiss()
                            return
                        }
                        return
                    }
                    report?.isAnyPermissionPermanentlyDenied?.let {
                        PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                        //errorDismiss()
                        return
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    override fun onScrollChanged() {
    }

    override fun onPause() {
        audioManager?.onPause()
        super.onPause()
    }

    override fun onStop() {
        audioManager?.onPause()
        super.onStop()
    }
}