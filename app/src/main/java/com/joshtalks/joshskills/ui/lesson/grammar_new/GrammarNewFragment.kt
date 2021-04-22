package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
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
import io.reactivex.disposables.CompositeDisposable
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GrammarNewFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener {

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
        subscribeRxBus()
        setObservers()

        return binding.root
    }

    private fun initViews() {
        headingView = Stub(binding.grammarScrollView.findViewById(R.id.heading_view))
        mcqChoiceView = Stub(binding.grammarScrollView.findViewById(R.id.mcq_choice_view))
        atsChoiceView = Stub(binding.grammarScrollView.findViewById(R.id.ats_choice_view))
        buttonView = Stub(binding.grammarScrollView.findViewById(R.id.button_action_views))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()

    }

    private fun subscribeRxBus() {

    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
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
                setupViews()

                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                    //binding.progressBar.progress = binding.progressBar.max
                    //setQuizScore(assessmentQuestions)
                    //showQuizCompleteLayout()
                }
            }
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

    private fun setupViews(position: Int = 0) {
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.get(position).question.mediaUrl,
                assessmentQuestions.get(position).question.mediaUrl2,
                assessmentQuestions.get(position).question.text,
                assessmentQuestions.get(position).question.subText
            )
        }

        if (assessmentQuestions.get(position).question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
            mcqChoiceView?.get()?.visibility = View.GONE
            atsChoiceView?.resolved().let {
                atsChoiceView?.get()?.visibility = View.VISIBLE
                //TODO reset the view in setup as well for next question
                atsChoiceView!!.get().setup(assessmentQuestions.get(position))
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
        } else if (assessmentQuestions.get(position).question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
            atsChoiceView?.get()?.visibility = View.GONE
            mcqChoiceView?.resolved().let {
                //TODO reset the view in setup as well for next question
                mcqChoiceView?.get()?.visibility = View.VISIBLE
                mcqChoiceView!!.get().setup(assessmentQuestions.get(position))
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
            buttonView!!.get().setup(assessmentQuestions.get(position))
            buttonView!!.get().addCallback(object : GrammarButtonView.CheckQuestionCallback {
                override fun checkQuestionCallBack(): Boolean? {
                    binding.progressBar.progress = position.plus(1)
                    if (assessmentQuestions.get(position).question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
                        return atsChoiceView?.get()?.isCorrectAnswer()?.apply {
                            //saveAssessmentQuestionToDb(position, this)
                            if (this) {
                                correctAnswers = correctAnswers.plus(1)
                            }
                        }
                    } else if (assessmentQuestions.get(position).question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
                        return mcqChoiceView?.get()?.isCorrectAnswer()?.apply {
                            //saveAssessmentQuestionToDb(position, this)
                            if (this) {
                                correctAnswers = correctAnswers.plus(1)
                            }
                        }
                    } else return null
                }

                override fun nextQuestion() {
                    moveToNextGrammarQuestion(position)
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
        if (currentQuizQuestion == assessmentQuestions.size - 1)
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                quizQuestion?.id,
                quizCorrectQuestionIds = correctQuestionList
            )
        viewModel.saveQuizToServer(assessmentQuestions[currentQuizQuestion].question.assessmentId)
    }

    private fun moveToNextGrammarQuestion(position: Int) {
        if (position >= assessmentQuestions.size.minus(1)) {
            showQuizCompleteLayout()
        } else {
            setupViews(position.plus(1))
        }
    }


    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    fun onRedoQuizClick() {
        showToast("Redo quiz clicked")
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

    companion object {

        @JvmStatic
        fun getInstance() = GrammarNewFragment()
    }

    override fun onScrollChanged() {

    }
}
