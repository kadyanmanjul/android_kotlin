package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayerEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.AtsChoiceView
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import com.joshtalks.joshskills.ui.chat.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.chat.vh.GrammarHeadingView
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class GrammarNewFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener,
    AudioPlayerEventListener {

    lateinit var binding: com.joshtalks.joshskills.databinding.FragmentGrammarNewLayoutBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    private val compositeDisposable = CompositeDisposable()
    private var assessmentQuestions: ArrayList<AssessmentQuestionWithRelations> = ArrayList()

    private var headingView: Stub<GrammarHeadingView>? = null
    private var mcqChoiceView: Stub<McqChoiceView>? = null
    private var atsChoiceView: Stub<AtsChoiceView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var quizQuestion: LessonQuestion? = null
    private var correctAnswers: Int = 0
    private var currentQuizQuestion: Int = 0
    private var currentPlayingAudioObjectUrl: String? =null
    private var audioPlayerManager: ExoAudioPlayer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener)
            lessonActivityListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_grammar_new_layout,
                container,
                false
            )
        binding.handler = this
        initAudioManager()
        setObservers()

        return binding.root
    }

    private fun initAudioManager() {
        audioPlayerManager = ExoAudioPlayer.getInstance()
        audioPlayerManager?.playerListener = this
    }

    private fun initViews() {
        headingView = Stub(binding.container.findViewById(R.id.heading_view))
        mcqChoiceView = Stub(binding.container.findViewById(R.id.mcq_choice_view))
        atsChoiceView = Stub(binding.container.findViewById(R.id.ats_choice_view))
        buttonView = Stub(binding.container.findViewById(R.id.button_action_views))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun subscribeRxBus() {
        compositeDisposable.add(
            RxBus2.listen(AudioPlayerEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (Utils.getCurrentMediaVolume(requireActivity().applicationContext) <= 0) {
                            StyleableToast.Builder(requireActivity().applicationContext)
                                .gravity(Gravity.BOTTOM)
                                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                                .length(Toast.LENGTH_LONG)
                                .solidBackground().show()
                        }
                        if (it.state == PlaybackInfoListener.State.PAUSED) {
                            audioPlayerManager?.onPause()
                            return@subscribe
                        }
                        /*if (currentPlayingAudioObjectUrl != null && ExoAudioPlayer.LAST_ID == it?.id) {
                            audioPlayerManager?.resumeOrPause()
                        } else {*/
                        currentPlayingAudioObjectUrl = it.audioUrl
                        audioPlayerManager?.onPause()
                        audioPlayerManager?.play(
                            it.audioUrl,
                            it.id
                        )
                        /* }*/
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun setObservers() {
        viewModel.lessonQuestionsLiveData.observe(
            viewLifecycleOwner,
            Observer { lessonQuestions ->
                val grammarQuestions = lessonQuestions.filter { it.chatType == CHAT_TYPE.GR }
                grammarQuestions.forEach {
                    setupUi(it)
                }
            }
        )

        viewModel.grammarAssessmentLiveData.observe(viewLifecycleOwner) { assessmentRelations ->
            assessmentQuestions.clear()
            assessmentRelations.questionList.sortedBy { it.question.sortOrder }.let {
                assessmentQuestions.addAll(it)
            }

            if (assessmentQuestions.size > 0) {
                binding.progressBar.max = assessmentQuestions.size
                binding.progressBar.progress = 0
                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                    binding.progressBar.progress = binding.progressBar.max
                    setQuizScore(assessmentQuestions)
                    showQuizCompleteLayout()
                }
                setCurrentQuestion(assessmentQuestions)
                if (currentQuizQuestion == assessmentQuestions.size) {
                    currentQuizQuestion = currentQuizQuestion.minus(1)
                    setupViews(currentQuizQuestion)
                } else {
                    setupViews(currentQuizQuestion)
                }

            }
        }
    }

    private fun setCurrentQuestion(assessmentQuestions: ArrayList<AssessmentQuestionWithRelations>) {
        var isQuizAttempted = false
        if (quizQuestion?.status == QUESTION_STATUS.AT) {
            currentQuizQuestion = assessmentQuestions.size
            return
        }
        assessmentQuestions.forEachIndexed { index, assessmentQuestionWithRelations ->
            if (assessmentQuestionWithRelations.question.isAttempted) {
                currentQuizQuestion = index.plus(1)
                isQuizAttempted = true
            }
        }
        if (isQuizAttempted) {
            binding.progressBar.progress = currentQuizQuestion
        } else {
            binding.progressBar.progress = 0
        }
    }

    private fun setQuizScore(assessmentQuestions: ArrayList<AssessmentQuestionWithRelations>) {
        correctAnswers = 0
        assessmentQuestions.forEach {
            it.choiceList.filter { choice -> choice.isCorrect && choice.isSelectedByUser }
                .forEach {
                    correctAnswers = correctAnswers.plus(1)
                }
        }
    }

    private fun setupViews(position: Int) {
        currentQuizQuestion = position
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.get(currentQuizQuestion).question.mediaUrl,
                assessmentQuestions.get(currentQuizQuestion).question.mediaUrl2,
                assessmentQuestions.get(currentQuizQuestion).question.text,
                assessmentQuestions.get(currentQuizQuestion).question.subText,
                assessmentQuestions.get(currentQuizQuestion).question.isNewHeader
            )
        }

        if (assessmentQuestions.get(currentQuizQuestion).question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
            mcqChoiceView?.get()?.visibility = View.GONE
            atsChoiceView?.resolved().let {
                atsChoiceView?.get()?.visibility = View.VISIBLE
                //TODO reset the view in setup as well for next question
                atsChoiceView!!.get().setup(assessmentQuestions.get(currentQuizQuestion))
                atsChoiceView!!.get()
                    .addCallback(object : EnableDisableGrammarButtonCallback {
                        override fun disableGrammarButton() {
                            buttonView?.get()?.disableBtn()
                        }

                        override fun enableGrammarButton() {
                            buttonView?.get()?.enableBtn()
                        }

                        override fun alreadyAttempted(isCorrectAnswer: Boolean) {
                            CoroutineScope(Dispatchers.Main).launch() {
                                buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                            }
                        }

                    })
            }
        } else if (assessmentQuestions.get(currentQuizQuestion).question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
            atsChoiceView?.get()?.visibility = View.GONE
            mcqChoiceView?.resolved().let {
                //TODO reset the view in setup as well for next question
                mcqChoiceView?.get()?.visibility = View.VISIBLE
                mcqChoiceView!!.get().setup(assessmentQuestions.get(currentQuizQuestion))
                mcqChoiceView!!.get()
                    .addCallback(object : EnableDisableGrammarButtonCallback {
                        override fun disableGrammarButton() {
                            buttonView?.get()?.disableBtn()
                        }

                        override fun enableGrammarButton() {
                            buttonView?.get()?.enableBtn()
                        }

                        override fun alreadyAttempted(isCorrectAnswer: Boolean) {
                            CoroutineScope(Dispatchers.Main).launch() {
                                buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                            }
                        }

                    })
            }
        } else {
            atsChoiceView?.get()?.visibility = View.GONE
            mcqChoiceView?.get()?.visibility = View.GONE
        }

        buttonView?.resolved().let {
            buttonView!!.get().setup(assessmentQuestions.get(currentQuizQuestion))
            buttonView!!.get().addCallback(object : GrammarButtonView.CheckQuestionCallback {
                override fun checkQuestionCallBack(): Boolean? {
                    binding.progressBar.progress = currentQuizQuestion.plus(1)
                    if (assessmentQuestions.get(currentQuizQuestion).question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
                        return atsChoiceView?.get()?.isCorrectAnswer()?.apply {
                            saveAssessmentQuestionToDb(currentQuizQuestion, this)
                            if (this) {
                                correctAnswers = correctAnswers.plus(1)
                            }
                        }
                    } else if (assessmentQuestions.get(currentQuizQuestion).question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
                        return mcqChoiceView?.get()?.isCorrectAnswer()?.apply {
                            saveAssessmentQuestionToDb(currentQuizQuestion, this)
                            if (this) {
                                correctAnswers = correctAnswers.plus(1)
                            }
                        }
                    } else return null
                }

                override fun nextQuestion() {
                    moveToNextGrammarQuestion()
                }
            })
        }
    }

    private fun saveAssessmentQuestionToDb(currentQuizQuestion: Int, isCorrect: Boolean) {
        val question = assessmentQuestions[currentQuizQuestion]
        question.question.isAttempted = true
        question.question.status = if (isCorrect) QuestionStatus.CORRECT else QuestionStatus.WRONG

        var selectedChoiceList = emptyList<Choice>()

        if (question.question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {

            selectedChoiceList =
                question.choiceList.filter { (it.userSelectedOrder != 0 || it.userSelectedOrder != 100) }

            selectedChoiceList.stream().forEach { selectedChoice ->
                selectedChoice.isSelectedByUser = true
            }

        } else if (question.question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {

            selectedChoiceList =
                question.choiceList.filter { (it.isSelectedByUser) }

            selectedChoiceList.stream().forEach { selectedChoice ->
                selectedChoice.isSelectedByUser = true
                selectedChoice.userSelectedOrder = selectedChoice.sortOrder
            }
        }

        viewModel.saveAssessmentQuestion(question)
        val correctQuestionList = ArrayList<Int>()
        assessmentQuestions.forEach { questionWithRelation ->
            if (questionWithRelation.question.isAttempted && questionWithRelation.question.status == QuestionStatus.CORRECT) {
                correctQuestionList.add(questionWithRelation.question.remoteId)
            }
        }
        viewModel.saveQuizToServer(assessmentQuestions[currentQuizQuestion].question.assessmentId)
        if (currentQuizQuestion == assessmentQuestions.size - 1)
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                quizQuestion?.id,
                quizCorrectQuestionIds = correctQuestionList
            )
    }

    private fun moveToNextGrammarQuestion() {
        if (currentQuizQuestion >= assessmentQuestions.size.minus(1)) {
            showQuizCompleteLayout()
        } else {
            setupViews(currentQuizQuestion.plus(1))
        }
    }


    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    fun onRedoQuizClick() {
        correctAnswers = 0
        assessmentQuestions.forEach { question ->
            question.question.isAttempted = false
            question.question.status = QuestionStatus.NONE
            question.choiceList.forEach { choice ->
                choice.isSelectedByUser = false
                choice.userSelectedOrder = 0
            }
            viewModel.saveAssessmentQuestion(question)
        }
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.NA,
            quizQuestion?.id,
            quizCorrectQuestionIds = ArrayList()
        )
        currentQuizQuestion = 0
        updateQuiz(assessmentQuestions[0])
        binding.grammarCompleteLayout.visibility = View.GONE
        binding.progressBar.progress = currentQuizQuestion
        setupViews(currentQuizQuestion)
    }

    private fun updateQuiz(question: AssessmentQuestionWithRelations) {
        /*binding.quizQuestionTv.text = getString(
            R.string.ques_short_form,
            currentQuizQuestion + 1,
            assessmentQuestions.size, question.question.text
        )

        if (currentQuizQuestion == 0)
            binding.previousQuestionIv.visibility = View.GONE
        else
            binding.previousQuestionIv.visibility = View.VISIBLE

        if (assessmentQuestions.size - 1 == currentQuizQuestion)
            binding.nextQuestionIv.visibility = View.GONE
        else
            binding.nextQuestionIv.visibility = View.VISIBLE

        hideExplanation()
        binding.explanationTv.text = question.reviseConcept?.description
        binding.quizRadioGroup.check(-1)
        question.choiceList.forEachIndexed { index, choice ->
            when (index) {
                0 -> {
                    setupOption(binding.option1, choice, question)
                }
                1 -> {
                    setupOption(binding.option2, choice, question)
                }
                2 -> {
                    setupOption(binding.option3, choice, question)
                }
                3 -> {
                    setupOption(binding.option4, choice, question)
                }
            }
        }

        binding.submitAnswerBtn.isEnabled = false
        binding.continueBtn.visibility = View.GONE
        if (question.question.isAttempted) {
            binding.submitAnswerBtn.visibility = View.GONE
            binding.showExplanationBtn.visibility = View.VISIBLE
            binding.continueBtn.visibility = View.VISIBLE
        } else {
            binding.submitAnswerBtn.visibility = View.VISIBLE
            binding.showExplanationBtn.visibility = View.GONE
            binding.continueBtn.visibility = View.GONE
        }*/
    }

    private fun showQuizCompleteLayout() {
        binding.grammarCompleteLayout.visibility = View.VISIBLE
        binding.marksTv.text =
            getString(R.string.marks_text, correctAnswers, assessmentQuestions.size)
    }

    private fun setupUi(lessonQuestion: LessonQuestion) {
        if (lessonQuestion.type == LessonQuestionType.QUIZ) {
            lessonQuestion.assessmentId?.let {
                quizQuestion = lessonQuestion
                viewModel.fetchAssessmentDetails(it)
            }
        }
    }

    private fun requestFocus(view: View) {
        view.parent.requestChildFocus(
            view,
            view
        )
    }


    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    override fun onPlayerPause() {

    }

    override fun onPlayerResume() {
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        audioPlayerManager?.seekTo(0)
        audioPlayerManager?.onPause()
    }

    override fun onPause() {
        super.onPause()
        audioPlayerManager?.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        audioPlayerManager?.release()
        super.onDestroy()
    }

    override fun onScrollChanged() {

    }

    companion object {
        const val TAG = "GrammarNewFragment"
        @JvmStatic
        fun getInstance() = GrammarNewFragment()
    }
}
