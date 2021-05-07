package com.joshtalks.joshskills.ui.online_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentOnlineTestBinding
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.chat.vh.AtsChoiceView
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import com.joshtalks.joshskills.ui.chat.vh.GrammarButtonView
import com.joshtalks.joshskills.ui.chat.vh.GrammarHeadingView
import com.joshtalks.joshskills.ui.lesson.grammar_new.McqChoiceView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OnlineTestFragment : CoreJoshFragment(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var binding: FragmentOnlineTestBinding
    private val viewModel: OnlineTestViewModel by lazy {
        ViewModelProvider(requireActivity()).get(OnlineTestViewModel::class.java)
    }
    private val compositeDisposable = CompositeDisposable()
    private var assessmentQuestions: AssessmentQuestionWithRelations? = null

    private var headingView: Stub<GrammarHeadingView>? = null
    private var mcqChoiceView: Stub<McqChoiceView>? = null
    private var atsChoiceView: Stub<AtsChoiceView>? = null
    private var buttonView: Stub<GrammarButtonView>? = null
    private var quizQuestion: LessonQuestion? = null
    private var currentQuizQuestion: Int = 0
    private var currentPlayingAudioObjectUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_online_test,
                container,
                false
            )
        binding.handler = this
        setObservers()
        viewModel.fetchAssessmentDetails()
        return binding.root
    }

    private fun initViews() {
        headingView = Stub(binding.container.findViewById(R.id.heading_view))
        mcqChoiceView = Stub(binding.container.findViewById(R.id.mcq_choice_view))
        atsChoiceView = Stub(binding.container.findViewById(R.id.ats_choice_view))
        buttonView = Stub(binding.frame.findViewById(R.id.button_action_views))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun subscribeRxBus() {

    }

    private fun setObservers() {

        viewModel.grammarAssessmentLiveData.observe(viewLifecycleOwner) { assessmentQuestion ->
            assessmentQuestion?.let {
                this.assessmentQuestions = it
            }
            if (assessmentQuestions == null) {
                showToast("Empty Or Done Quiz")
            } else {
                setupViews(assessmentQuestions!!)
            }
            /*assessmentRelations.questionList.sortedBy { it.question.sortOrder }.let {
                assessmentQuestions.addAll(it)
            }*/

            /*if (assessmentQuestions.size > 0) {
                if (quizQuestion?.status == QUESTION_STATUS.AT) {

                }
                setCurrentQuestion(assessmentQuestions)
                if (currentQuizQuestion == assessmentQuestions.size) {
                    currentQuizQuestion = currentQuizQuestion.minus(1)
                    setupViews(currentQuizQuestion)
                } else {
                    setupViews(currentQuizQuestion)
                }

            }*/
        }
    }

    private fun setupViews(assessmentQuestions: AssessmentQuestionWithRelations) {
        headingView?.resolved().let {
            headingView!!.get().setup(
                assessmentQuestions.question.mediaUrl,
                assessmentQuestions.question.mediaUrl2,
                assessmentQuestions.question.text,
                assessmentQuestions.question.subText,
                assessmentQuestions.question.isNewHeader
            )
        }

        if (assessmentQuestions.question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
            mcqChoiceView?.get()?.visibility = View.GONE
            atsChoiceView?.resolved().let {
                atsChoiceView?.get()?.visibility = View.VISIBLE
                //TODO reset the view in setup as well for next question
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
                            CoroutineScope(Dispatchers.Main).launch() {
                                buttonView?.get()?.setAlreadyAttemptedView(isCorrectAnswer)
                            }
                        }

                    })
            }
        } else if (assessmentQuestions.question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
            atsChoiceView?.get()?.visibility = View.GONE
            mcqChoiceView?.resolved().let {
                //TODO reset the view in setup as well for next question
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
            buttonView!!.get().setup(assessmentQuestions)
            buttonView!!.get().addCallback(object : GrammarButtonView.CheckQuestionCallback {
                override fun checkQuestionCallBack(): Boolean? {
                    if (assessmentQuestions.question.choiceType == ChoiceType.ARRANGE_THE_SENTENCE) {
                        return atsChoiceView?.get()?.isCorrectAnswer()?.apply {
                            assessmentQuestions.question.status =
                                if (this) QuestionStatus.CORRECT else QuestionStatus.WRONG

                        }
                    } else if (assessmentQuestions.question.choiceType == ChoiceType.SINGLE_SELECTION_TEXT) {
                        return mcqChoiceView?.get()?.isCorrectAnswer()?.apply {
                            assessmentQuestions.question.status =
                                if (this) QuestionStatus.CORRECT else QuestionStatus.WRONG

                        }
                    } else return null
                }

                override fun nextQuestion() {
                    moveToNextGrammarQuestion(assessmentQuestions)
                }
            })
        }
    }

    private fun moveToNextGrammarQuestion(assessmentQuestions: AssessmentQuestionWithRelations) {
        viewModel.postAnswerAndGetNewQuestion(assessmentQuestions)
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    override fun onScrollChanged() {

    }

    companion object {
        const val TAG = "OnlineTestFragment"

        @JvmStatic
        fun getInstance() = OnlineTestFragment()
    }
}
