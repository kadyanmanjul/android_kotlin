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
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionFeedbackResponse
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.chat.vh.GrammarChoiceView
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
    private var choiceView: Stub<GrammarChoiceView>? = null
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
        headingView?.resolved().let {
            headingView!!.get().setup(
                "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/Vidushi_audio_recordings/RP+ogg+format/L27.ogg",
                "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/Vidushi_audio_recordings/RP+ogg+format/L28.ogg",
                "HEading",
                "Description"
            )
        }

        choiceView = Stub(binding.grammarScrollView.findViewById(R.id.choice_view))
        choiceView?.resolved().let {
            choiceView!!.get().setup()
        }

        buttonView = Stub(binding.grammarScrollView.findViewById(R.id.button_action_views))
        buttonView?.resolved().let {
            buttonView!!.get().setup(
                AssessmentQuestionFeedbackResponse(
                    10, "Correct Answer", "Correct Answer Text",
                    "Wrong Answer H1", "Wrong Answer T1", "Wrong Answer H2", "Wrong Answer T2"
                )
            )
        }
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

                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                   // setQuizScore(assessmentQuestions)
                   // showQuizCompleteLayout()
                }
            }
        }
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
