package com.joshtalks.joshskills.ui.online_test

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.custom_ui.JoshGrammarVideoPlayer
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.core.playWrongAnswerSound
import com.joshtalks.joshskills.databinding.FragmentOnlineTestBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.AtsChoiceView
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import com.joshtalks.joshskills.ui.chat.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.chat.vh.GrammarHeadingView
import com.joshtalks.joshskills.ui.chat.vh.SubjectiveChoiceView
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.grammar_new.McqChoiceView
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OnlineTestFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var binding: FragmentOnlineTestBinding
    private val viewModel: OnlineTestViewModel by lazy {
        ViewModelProvider(requireActivity()).get(OnlineTestViewModel::class.java)
    }
    private var assessmentQuestions: AssessmentQuestionWithRelations? = null
    private var ruleAssessmentQuestionId: String? = null
    private var lessonNumber: Int = -1
    private var headingView: Stub<GrammarHeadingView>? = null
    private var mcqChoiceView: Stub<McqChoiceView>? = null
    private var atsChoiceView: Stub<AtsChoiceView>? = null
    private var subjectiveChoiceView: Stub<SubjectiveChoiceView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var isFirstTime: Boolean = true
    private var isTestCompleted: Boolean = false
    private var testCallback: OnlineTestInterface? = null
    private var lessonActivityListener: LessonActivityListener? = null
    var reviseVideoObject: VideoModel? = null
    private var compositeDisposable = CompositeDisposable()

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
        viewModel.fetchAssessmentDetails()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        headingView = Stub(binding.container.findViewById(R.id.heading_view))
        mcqChoiceView = Stub(binding.container.findViewById(R.id.mcq_choice_view))
        atsChoiceView = Stub(binding.container.findViewById(R.id.ats_choice_view))
        subjectiveChoiceView = Stub(binding.container.findViewById(R.id.subjective_choice_view))
        buttonView = Stub(binding.container.findViewById(R.id.button_action_views))
    }

    private fun setObservers() {

        viewModel.grammarAssessmentLiveData.observe(viewLifecycleOwner) { onlineTestResponse ->
            this.ruleAssessmentQuestionId=onlineTestResponse.ruleAssessmentQuestionId
            this.reviseVideoObject=onlineTestResponse.videoObject
            if (onlineTestResponse.completed && onlineTestResponse.question == null) {
                PrefManager.put(ONLINE_TEST_LAST_LESSON_COMPLETED, lessonNumber)
                isTestCompleted = onlineTestResponse.completed
            } else {
                onlineTestResponse.question?.let {
                    this.assessmentQuestions = AssessmentQuestionWithRelations(it, 10)
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
                }
                ApiCallStatus.FAILED, ApiCallStatus.SUCCESS -> {
                    binding.progressContainer.visibility = View.GONE
                }
                else -> {
                    binding.progressContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViews(assessmentQuestions: AssessmentQuestionWithRelations) {
        if (isTestCompleted){
            showGrammarCompleteFragment()
            return
        }
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.question.mediaUrl,
                assessmentQuestions.question.mediaUrl2,
                assessmentQuestions.question.text,
                assessmentQuestions.question.subText,
                assessmentQuestions.question.isNewHeader
            )
        }
        when (assessmentQuestions.question.choiceType) {
            ChoiceType.ARRANGE_THE_SENTENCE -> {
                mcqChoiceView?.get()?.visibility = View.GONE
                subjectiveChoiceView?.get()?.visibility = View.GONE
                atsChoiceView?.resolved().let {
                    atsChoiceView?.get()?.visibility = View.VISIBLE
                    atsChoiceView!!.get().setup(assessmentQuestions)
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
            buttonView!!.get().setup(assessmentQuestions,reviseVideoObject)
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
                    moveToNextGrammarQuestion()
                }
            })
        }
    }

    fun showGrammarCompleteFragment() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            fragmentManager
                .beginTransaction()
                .replace(
                    R.id.parent_Container,
                    GrammarOnlineTestFragment.getInstance(lessonNumber),
                    GrammarOnlineTestFragment.TAG
                )
                .addToBackStack(TAG)
                .commitAllowingStateLoss()
        }
    }

    private fun onCheckQuestion(
        assessmentQuestions: AssessmentQuestionWithRelations,
        status: Boolean
    ) {
        assessmentQuestions.question.status =
            if (status) QuestionStatus.CORRECT else QuestionStatus.WRONG
        if (status) {
            playSnackbarSound(requireActivity())
        } else {
            playWrongAnswerSound(requireActivity())
        }
        viewModel.postAnswerAndGetNewQuestion(assessmentQuestions,ruleAssessmentQuestionId)
        PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, lessonNumber)
    }

    private fun moveToNextGrammarQuestion() {
        lessonActivityListener?.onLessonUpdate()
        setupViews(assessmentQuestions!!)
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoContainer.isVisible){
            binding.videoPlayer.onPause()
        }
        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(VideoShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.videoContainer.visibility=View.VISIBLE
                    binding.videoPlayer.apply {
                        visibility=View.VISIBLE
                        setUrl(it.videoUrl)
                        setVideoId(it.videoId)
                        fitToScreen()
                        downloadStreamPlay()
                        setPlayListener(object :JoshGrammarVideoPlayer.PlayerFullScreenListener{

                            override fun onFullScreen() {
                                val currentVideoProgressPosition = binding.videoPlayer.getProgress()
                                startActivity(
                                    VideoPlayerActivity.getActivityIntent(
                                        requireContext(),
                                        "",
                                        it.videoId,
                                        it.videoUrl,
                                        currentVideoProgressPosition,
                                        conversationId = getConversationId()
                                    )
                                )
                                visibility=View.GONE
                            }

                            override fun onClose() {
                                onPause()
                                visibility=View.GONE
                            }
                        })
                    }

                }, {
                    it.printStackTrace()
                })
        )
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
