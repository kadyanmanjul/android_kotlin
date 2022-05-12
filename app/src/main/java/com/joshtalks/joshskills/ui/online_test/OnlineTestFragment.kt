package com.joshtalks.joshskills.ui.online_test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.custom_ui.JoshGrammarVideoPlayer
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.databinding.FragmentOnlineTestBinding
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.*
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.grammar_new.McqChoiceView
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.userexperior.utilities.SecureViewBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.reflect.Type

class OnlineTestFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var binding: FragmentOnlineTestBinding
    private val viewModel: OnlineTestViewModel by lazy {
        ViewModelProvider(requireActivity()).get(OnlineTestViewModel::class.java)
    }
    private var assessmentQuestions: AssessmentQuestionWithRelations? = null
    private var ruleAssessmentQuestionId: String? = null
    private var totalQuestion: Int? = null
    private var totalAnsweredQuestions: Int? = null
    private var lessonNumber: Int = -1
    private var lessonId: Int = -1
    private var headingView: Stub<GrammarHeadingView>? = null
    private var mcqChoiceView: Stub<McqChoiceView>? = null
    private var atsChoiceView: Stub<AtsChoiceView>? = null
    private var subjectiveChoiceView: Stub<SubjectiveChoiceView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var errorView: Stub<ErrorView>? = null
    private var isFirstTime: Boolean = true
    private var isTestCompleted: Boolean = false
    private var scoreText: Int? = 0
    private var pointsList: String? = null
    private var testCallback: OnlineTestInterface? = null
    private var lessonActivityListener: LessonActivityListener? = null
    var reviseVideoObject: VideoModel? = null
    private var previousId: Int = -1
    private var completed = false
    private var questionId = -1
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnlineTestInterface)
            testCallback = context

        if (context is LessonActivityListener)
            lessonActivityListener = context
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

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_online_test,
                container,
                false
            )
        binding.handler = this
        binding.lifecycleOwner = this
        setObservers()
        binding.progressContainer.visibility = View.VISIBLE
        viewModel.fetchAssessmentDetails(lessonId)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        headingView = Stub(binding.choiceContainer.findViewById(R.id.heading_view))
        mcqChoiceView = Stub(binding.choiceContainer.findViewById(R.id.mcq_choice_view))
        atsChoiceView = Stub(binding.choiceContainer.findViewById(R.id.ats_choice_view))
        subjectiveChoiceView =
            Stub(binding.choiceContainer.findViewById(R.id.subjective_choice_view))
        buttonView = Stub(binding.container.findViewById(R.id.button_action_views))
        errorView = Stub(binding.container.findViewById(R.id.error_view))
    }

    private fun setObservers() {
        viewModel.grammarAssessmentLiveData.observe(viewLifecycleOwner) { onlineTestResponse ->
            this.ruleAssessmentQuestionId = onlineTestResponse.ruleAssessmentQuestionId
            this.reviseVideoObject = onlineTestResponse.videoObject
            this.totalQuestion = onlineTestResponse.totalQuestions
            this.totalAnsweredQuestions = onlineTestResponse.totalAnswered ?: 0
            questionId = onlineTestResponse.question?.id?:-1
            animateProgress()
            if (onlineTestResponse.completed) {
                PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, lessonNumber)
                if (onlineTestResponse.ruleAssessmentId != null) {
                    addNewRuleCompleted(onlineTestResponse.ruleAssessmentId)
                }
                isTestCompleted = onlineTestResponse.completed
                scoreText = onlineTestResponse.scoreText
                onlineTestResponse.scoreText?.let {
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
                    this.assessmentQuestions = AssessmentQuestionWithRelations(it, 10)
                    this.assessmentQuestions?.choiceList?.let { list ->
                        viewModel.insertChoicesToDB(
                            list, this.assessmentQuestions
                        )
                    }
                    if (isFirstTime) {
                        setupViews(assessmentQuestions!!)
                    }
                }
            }

            isFirstTime = false
        }

        viewModel.apiStatus.observe(viewLifecycleOwner) {
            when (it) {
                ApiCallStatus.START -> {
                    //binding.progressContainer.visibility = View.VISIBLE
                    binding.progressContainer.visibility = View.GONE
                }
                ApiCallStatus.FAILED -> {
                    binding.progressContainer.visibility = View.GONE
                    errorView?.resolved()?.let {
                        errorView!!.get().onFailure(object : ErrorView.ErrorCallback {
                            override fun onRetryButtonClicked() {
                                if (assessmentQuestions != null) {
                                    viewModel.postAnswerAndGetNewQuestion(
                                        assessmentQuestions!!,
                                        ruleAssessmentQuestionId,
                                        lessonId
                                    )
                                } else {
                                    viewModel.fetchAssessmentDetails(lessonId)
                                }
                            }
                        })
                    }
                }
                ApiCallStatus.SUCCESS -> {
                    binding.progressContainer.visibility = View.GONE
                    errorView?.resolved()?.let {
                        errorView!!.get().onSuccess()
                    }
                }
                else -> {
                    binding.progressContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun addNewRuleCompleted(ruleCompletedId: Int) {
        completed = true
        val mapTypeToken: Type = object : TypeToken<List<Int>?>() {}.type
        val list: List<Int>? = AppObjectController.gsonMapper.fromJson(
            PrefManager.getStringValue(ONLINE_TEST_LIST_OF_COMPLETED_RULES),
            mapTypeToken
        )
        val newupdateList = mutableSetOf<Int>()
        if (list.isNullOrEmpty().not()) {
            newupdateList.addAll(list!!)
        }
        val isRuleAlreadyCompleted = newupdateList.contains(ruleCompletedId)
        if (isRuleAlreadyCompleted.not()) {
            newupdateList.add(ruleCompletedId)
            PrefManager.put(
                ONLINE_TEST_LIST_OF_COMPLETED_RULES,
                newupdateList.toString()
            )
            viewModel.sendCompletedRuleIdsToBAckend(ruleCompletedId)
        }
    }

    private fun setupViews(assessmentQuestions: AssessmentQuestionWithRelations) {
        if (isTestCompleted) {
            showGrammarCompleteFragment()
            return
        }
        if (totalQuestion != null) {
            binding.questionProgressBar.visibility = View.VISIBLE
            binding.questionProgressBar.max = (totalQuestion ?: 0).times(100)
        } else {
            binding.questionProgressBar.visibility = View.VISIBLE
        }
        downloadAudios(assessmentQuestions.choiceList)
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.question.mediaUrl,
                assessmentQuestions.question.mediaUrl2,
                assessmentQuestions.question.text,
                assessmentQuestions.question.subText,
                assessmentQuestions.question.isNewHeader
            )
        }
        binding.videoContainer.setOnClickListener {
            LessonActivity.isVideoVisible.postValue(false)
        }
        when (assessmentQuestions.question.choiceType) {
            ChoiceType.ARRANGE_THE_SENTENCE -> {
                mcqChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.let {
                    SecureViewBucket.removeFromSecureViewBucket(it.get())
                }
                atsChoiceView?.resolved().let {
                    atsChoiceView?.get()?.visibility = View.VISIBLE
                    atsChoiceView!!.get().setup(assessmentQuestions)
                    atsChoiceView!!.get().addImpressionCallback(object : TrackUndoImpression {
                        override fun trackUndoImpression() {
                            viewModel.saveImpression(IMPRESSION_UNDO_ATS_OPTION)
                        }
                    })
                    atsChoiceView!!.get()
                        .addCallback(object : EnableDisableGrammarButtonCallback {
                            override fun disableGrammarButton() {
                                buttonView?.get()?.disableBtn()
                            }

                            override fun enableGrammarButton() {
                                buttonView?.get()?.enableBtn()
                            }

                            override fun alreadyAttempted(isCorrectAnswer: Boolean) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                                }
                            }

                        })
                }
            }
            ChoiceType.SINGLE_SELECTION_TEXT -> {
                atsChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.let {
                    SecureViewBucket.removeFromSecureViewBucket(it.get())
                }
                mcqChoiceView?.resolved().let {
                    mcqChoiceView?.get()?.visibility = View.VISIBLE
                    mcqChoiceView!!.get().setup(assessmentQuestions)
                    mcqChoiceView!!.get()
                        .addCallback(object : EnableDisableGrammarButtonCallback {
                            override fun disableGrammarButton() {
                                buttonView?.get()?.disableBtn()
                            }

                            override fun enableGrammarButton() {
                                buttonView?.get()?.enableBtn()
                            }

                            override fun alreadyAttempted(isCorrectAnswer: Boolean) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                                }
                            }

                        })
                }
            }
            ChoiceType.INPUT_TEXT -> {
                atsChoiceView?.get()?.visibility = View.GONE
                mcqChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.resolved().let {
                    subjectiveChoiceView?.get()?.visibility = View.VISIBLE
                    subjectiveChoiceView!!.get().setup(assessmentQuestions)
                    subjectiveChoiceView!!.get()
                        .addCallback(object : EnableDisableGrammarButtonCallback {
                            override fun disableGrammarButton() {
                                buttonView?.get()?.disableBtn()
                            }

                            override fun enableGrammarButton() {
                                buttonView?.get()?.enableBtn()
                            }

                            override fun alreadyAttempted(isCorrectAnswer: Boolean) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                                }
                            }

                        })
                }
            }
            else -> {
                atsChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
                mcqChoiceView?.get()?.visibility = View.GONE
            }
        }

        buttonView?.resolved().let {
            buttonView!!.get().setup(assessmentQuestions, reviseVideoObject)
            buttonView!!.get().addCallback(object : GrammarButtonView.CheckQuestionCallback {
                override fun checkQuestionCallBack(): Boolean? {
                    return when (assessmentQuestions.question.choiceType) {
                        ChoiceType.ARRANGE_THE_SENTENCE -> atsChoiceView?.get()?.isCorrectAnswer()
                            ?.apply {
                                onCheckQuestion(assessmentQuestions, this)
                            }
                        ChoiceType.SINGLE_SELECTION_TEXT -> {
                            mcqChoiceView?.get()?.isCorrectAnswer()?.apply {
                                onCheckQuestion(assessmentQuestions, this)
                            }
                        }
                        ChoiceType.INPUT_TEXT -> {
                            subjectiveChoiceView?.get()?.isCorrectAnswer()?.apply {
                                onCheckQuestion(assessmentQuestions, this)
                            }
                        }
                        else -> null
                    }
                }

                override fun nextQuestion() {
                    MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_QUIZ_CONTINUE)
                        .addParam(ParamKeys.LESSON_ID,lessonId)
                        .addParam(ParamKeys.QUESTION_ID,questionId)
                        .push()
                    moveToNextGrammarQuestion()
                }

                override fun onVideoButtonAppear(
                    wrongAnswerHeading: String?,
                    wrongAnswerSubHeading: String?,
                    wrongAnswerText: String?,
                    wrongAnswerDescription: String?
                ) {
                    if (PrefManager.hasKey(
                            HAS_SEEN_QUIZ_VIDEO_TOOLTIP,
                        ).not()
                    ) {
                        lessonActivityListener?.showVideoToolTip(
                            shouldShow = true,
                            wrongAnswerHeading = wrongAnswerHeading,
                            wrongAnswerSubHeading = wrongAnswerSubHeading,
                            wrongAnswerText = wrongAnswerText,
                            wrongAnswerDescription = wrongAnswerDescription,
                            videoClickListener = { buttonView!!.get().viewVideo() }
                        )
                    }
                }

                override fun onVideoButtonClicked() {
                    binding.progressContainer.isVisible = true
                    LessonActivity.isVideoVisible.value = true
                    reviseVideoObject?.let {
                        with(binding.videoPlayer) {
                            setUrl(it.video_url)
                            setVideoId(it.id)
                            setVideoThumbnail(it.video_image_url)
                            fitToScreen()
                            if (it.video_height != 0 && it.video_width != 0) {
                                (binding.videoPlayer.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                                    (it.video_width / it.video_height).toString()
                            }
                            downloadStreamPlay()
                            setPlayerEventCallback { event, _ ->
                                if (event == ExoPlayer.STATE_ENDED) {
                                    LessonActivity.isVideoVisible.value = false
                                    PrefManager.appendToSet(
                                        LAST_SEEN_VIDEO_ID,
                                        it.id, false
                                    )
                                }
                            }
                            setPlayListener(object :
                                JoshGrammarVideoPlayer.PlayerFullScreenListener {
                                override fun onFullScreen() {
                                    val currentVideoProgressPosition = binding.videoPlayer.progress
                                    startActivity(
                                        VideoPlayerActivity.getActivityIntent(
                                            requireContext(),
                                            "",
                                            it.id,
                                            it.video_url,
                                            currentVideoProgressPosition,
                                            conversationId = getConversationId()
                                        )
                                    )
                                    LessonActivity.isVideoVisible.value = false
                                }

                                override fun onClose() {
                                    onPause()
                                    LessonActivity.isVideoVisible.value = false
                                }
                            })
                        }
                    } ?: showToast("Error playing video")
                }
            })
        }
    }

    private fun setVideoThumbnail(thumbnailUrl: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val thumbnailDrawable: Drawable? =
                    Utils.getDrawableFromUrl(thumbnailUrl)
                if (thumbnailDrawable != null) {
                    AppObjectController.uiHandler.post {
                        binding.videoPlayer.useArtwork = true
                        binding.videoPlayer.defaultArtwork = thumbnailDrawable
//                    val imgArtwork: ImageView = binding.videoPlayer.findViewById(R.id.exo_artwork) as ImageView
//                    imgArtwork.setImageDrawable(thumbnailDrawable)
//                    imgArtwork.visibility = View.VISIBLE
                    }
                }
            } catch (e: java.lang.Exception) {
            }
        }
    }

    private fun animateProgress() {
        val currentProgress = binding.questionProgressBar.progress
        val finalProgress = (totalAnsweredQuestions ?: 0).times(100)
        ValueAnimator.ofInt(currentProgress, finalProgress).apply {
            duration = 400
            addUpdateListener {
                binding.questionProgressBar.progress = it.animatedValue as Int
            }
            start()
        }
    }

    private fun downloadAudios(choiceList: List<Choice>) {
        if (PermissionUtils.isStoragePermissionEnabled(AppObjectController.joshApplication).not()) {
            askStoragePermission(choiceList)
            return
        }
        downloadAudioFileForNewGrammar(choiceList)
    }

    private fun downloadAudioFileForNewGrammar(choiceList: List<Choice>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
                    return@launch
                }
                for (choice in choiceList) {
                    choice.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                    AppObjectController.appDatabase.assessmentDao()
                        .updateChoiceDownloadStatusForAudio(
                            choice.remoteId,
                            DOWNLOAD_STATUS.DOWNLOADING
                        )
                    val file =
                        AppDirectory.getAudioReceivedFile(choice.audioUrl.toString()).absolutePath
                    if (choice.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                        return@launch
                    }

                    val request = Request(choice.audioUrl.toString(), file)
                    request.priority = Priority.HIGH
                    request.networkType = NetworkType.ALL
                    request.tag = choice.remoteId.toString()
                    AppObjectController.getFetchObject().enqueue(request, {
                        choice.localAudioUrl = it.file
                        choice.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                        CoroutineScope(Dispatchers.IO).launch {
                            it.tag?.toInt()?.let { id ->
                                AppObjectController.appDatabase.assessmentDao()
                                    .updateChoiceDownloadStatusForAudio(
                                        id,
                                        DOWNLOAD_STATUS.DOWNLOADED
                                    )
                                AppObjectController.appDatabase.assessmentDao()
                                    .updateChoiceLocalPathForAudio(id, it.file)
                            }
                        }
                        DownloadUtils.objectFetchListener.remove(it.tag)
                        Timber.e(it.url + "   " + it.file)
                    },
                        {
                            it.throwable?.printStackTrace()
                            choice.downloadStatus = DOWNLOAD_STATUS.FAILED
                            CoroutineScope(Dispatchers.IO).launch {
                                AppObjectController.appDatabase.assessmentDao()
                                    .updateAssessmentChoice(choice)
                            }
                        })
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun askStoragePermission(choiceList: List<Choice>) {
        PermissionUtils.storageReadAndWritePermission(
            requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            downloadAudioFileForNewGrammar(choiceList)
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

    fun showGrammarCompleteFragment() {
        activity?.supportFragmentManager?.beginTransaction()?.replace(
            R.id.parent_Container,
            GrammarOnlineTestFragment.getInstance(lessonNumber, scoreText, pointsList),
            GrammarOnlineTestFragment.TAG
        )?.addToBackStack(TAG)?.commitAllowingStateLoss()
    }

    private fun onCheckQuestion(
        assessmentQuestions: AssessmentQuestionWithRelations,
        status: Boolean
    ) {
        if (completed) {
            completed = false
            totalAnsweredQuestions = 0
            animateProgress()
        }
        assessmentQuestions.question.status =
            if (status) QuestionStatus.CORRECT else QuestionStatus.WRONG
        if (status) {
            playSnackbarSound(requireActivity())
        } else {
            playWrongAnswerSound(requireActivity())
        }
        viewModel.postAnswerAndGetNewQuestion(
            assessmentQuestions,
            ruleAssessmentQuestionId,
            lessonId
        )
        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, lessonNumber)
    }

    private fun moveToNextGrammarQuestion() {
        if (completed) {
            completed = false
            totalAnsweredQuestions = 0
            animateProgress()
        }
        lessonActivityListener?.onLessonUpdate()
        setupViews(assessmentQuestions!!)
    }

    override fun onPause() {
        binding.videoPlayer.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
    }

    private fun addObserver() {
        LessonActivity.isVideoVisible.observe(viewLifecycleOwner) { isVideoVisible ->
            if (!isVideoVisible) {
                binding.videoPlayer.onPause()
                if (binding.videoContainer.isVisible)
                    binding.videoContainer
                        .animate()
                        .alpha(0.0f)
                        .setDuration(200)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                binding.videoContainer.visibility = View.GONE
                            }
                        })
                        .start()
            } else if (binding.videoContainer.isVisible.not()) {
                binding.videoContainer
                    .animate()
                    .alpha(1.0f)
                    .setDuration(500)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            binding.videoContainer.visibility = View.VISIBLE
                            binding.progressContainer.isVisible = false
                        }
                    })
                    .start()
            }
        }
    }

    override fun onScrollChanged() {
    }

    interface OnlineTestInterface {
        fun testCompleted()
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
}
