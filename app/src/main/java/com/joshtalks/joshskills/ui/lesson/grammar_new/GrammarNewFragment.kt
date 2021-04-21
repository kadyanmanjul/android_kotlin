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
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import com.joshtalks.joshskills.ui.chat.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.chat.vh.GrammarHeadingView
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import io.reactivex.disposables.CompositeDisposable
import java.util.ArrayList

class GrammarNewFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var binding: com.joshtalks.joshskills.databinding.FragmentGrammarNewLayoutBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    private val compositeDisposable = CompositeDisposable()
    private var assessmentQuestions: ArrayList<AssessmentQuestionWithRelations> = ArrayList()

    private var headingView: Stub<GrammarHeadingView>? = null
    private var choiceView: Stub<McqChoiceView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var quizQuestion: LessonQuestion? = null

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
        choiceView = Stub(binding.grammarScrollView.findViewById(R.id.choice_view))
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
                // binding.quizRadioGroup.setOnCheckedChangeListener(quizCheckedChangeListener)
                //showQuizUi()
                // updateQuiz(assessmentQuestions[0])
                binding.progressBar.max = assessmentQuestions.size
                binding.progressBar.progress = 0
                setupViews()

                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                    // setQuizScore(assessmentQuestions)
                    // showQuizCompleteLayout()
                }
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
        choiceView?.resolved().let {
            //TODO reset the view in setup as well for next question
            choiceView!!.get().setup(assessmentQuestions.get(position))
            choiceView!!.get()
                .addCallback(object : EnableDisableGrammarButtonCallback {
                    override fun disableGrammarButton() {
                        buttonView?.get()?.disableBtn()
                    }

                    override fun enableGrammarButton() {
                        buttonView?.get()?.enableBtn()
                    }

                })
        }
        buttonView?.resolved().let {
            buttonView!!.get().setup(assessmentQuestions.get(position))
            buttonView!!.get().addCallback(object : GrammarButtonView.CheckQuestionCallback {
                override fun checkQuestionCallBack(): Boolean? {
                    binding.progressBar.progress = position.plus(1)
                    return choiceView?.get()?.isCorrectAnswer()
                }

                override fun nextQuestion() {
                    moveToNextGrammarQuestion(position)
                }
            })
        }
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
        binding.marksTv.text = getString(R.string.marks_text, 0, assessmentQuestions.size)
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
