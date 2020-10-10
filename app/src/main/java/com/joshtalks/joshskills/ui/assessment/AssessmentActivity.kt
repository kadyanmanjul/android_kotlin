package com.joshtalks.joshskills.ui.assessment

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshcamerax.utils.onPageSelected
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityAssessmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClick
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClickEvent
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentLastQuestionSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.FillInTheBlankSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.MatchTheFollowingSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.McqSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.TestItemClickedEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.ui.assessment.adapter.AssessmentQuestionAdapter
import com.joshtalks.joshskills.ui.assessment.extra.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.fragment.QuizSuccessFragment
import com.joshtalks.joshskills.ui.assessment.fragment.ReviseConceptFragment
import com.joshtalks.joshskills.ui.assessment.fragment.TestSummaryFragment
import com.joshtalks.joshskills.ui.assessment.viewmodel.AssessmentViewModel
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AssessmentActivity : CoreJoshActivity() {

    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AssessmentViewModel::class.java) }
    private var assessmentId: Int = 0
    private var flowFrom: String? = null
    private var compositeDisposable = CompositeDisposable()
    private var isTestFinished = false
    private var isTestFragmentVisible = false
    private var isViewBinded = false
    //private val hintOptionsSet = mutableSetOf<ChoiceType>()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.black)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_assessment)
        binding.lifecycleOwner = this
        binding.handler = this
        assessmentId = intent.getIntExtra(KEY_ASSESSMENT_ID, 0)
        if (intent.hasExtra(STARTED_FROM)) {
            flowFrom = intent.getStringExtra(STARTED_FROM)
        }
        addObservers()
        if (assessmentId != 0) {
            getAssessmentDetails(assessmentId)
        } else {
            finish()
        }
    }

    private fun showTestSummaryFragment(
        assessmentId: Int,
        isTestAlreadyAttempted: Boolean = false
    ) {
        isTestFinished = true
        isTestFragmentVisible = true
        binding.buttonView.visibility = GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                TestSummaryFragment.newInstance(assessmentId, isTestAlreadyAttempted),
                "Test Summary"
            )
            .commitAllowingStateLoss()
    }

    private fun showQuizSuccessFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                QuizSuccessFragment.newInstance(),
                "Quiz Success"
            )
            .commitAllowingStateLoss()
    }

    private fun showReviseConceptFragment(reviseConcept: ReviseConcept) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                ReviseConceptFragment.newInstance(reviseConcept),
                "Revise Concept"
            )
            .commitAllowingStateLoss()
    }

    private fun getAssessmentDetails(assessmentId: Int) {
        viewModel.fetchAssessmentDetails(assessmentId)
    }

    private fun addObservers() {

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
        })

        viewModel.assessmentLiveData.observe(this, Observer { assessmentWithRelations ->
            if (isViewBinded.not()) {
                isViewBinded = true
                assessmentWithRelations.questionList.sortedBy { it.question.sortOrder }
                bindView(assessmentWithRelations)

            }
        })

        viewModel.assessmentStatus.observe(this, Observer { status ->
            if ((status == AssessmentStatus.COMPLETED) && (viewModel.getAssessmentType() == null || viewModel.getAssessmentType() == AssessmentType.TEST)) {
                viewModel.assessmentLiveData.value?.let {
                    if (it.questionList.filter { it.question.choiceType == ChoiceType.MATCH_TEXT }.size > 0) {
                        RxBus2.publish(MatchTheFollowingSubmitEvent(it.assessment.remoteId))
                    }
                }
                val fragment = supportFragmentManager.findFragmentByTag("Test Summary")
                if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment)
                    .commit()
                showTestSummaryFragment(assessmentId, true)
            }

        })
    }

    private fun handleButtonClicks(
        assessmentButtonClick: AssessmentButtonClick,
        assessmentType: AssessmentType,
        questionId: Int
    ) {
        viewModel.assessmentLiveData.value?.let { assessmentWithRelations ->
            when (assessmentButtonClick) {
                AssessmentButtonClick.SUBMIT -> {
                    onSubmit(assessmentWithRelations.questionList.filter { it.question.remoteId == questionId }[0])
                }

                AssessmentButtonClick.NEXT -> {
                    onNext(
                        assessmentWithRelations.questionList.size - 1 == binding.questionViewPager.currentItem,
                        assessmentType,
                        assessmentWithRelations.questionList.filter { it.question.remoteId == questionId }[0],
                        assessmentWithRelations.assessment
                    )
                }
                AssessmentButtonClick.REVISE -> {
                    onReviseConcept(assessmentWithRelations.questionList.filter { it.question.remoteId == questionId }[0].reviseConcept)
                }
                AssessmentButtonClick.NONE -> {

                }
                AssessmentButtonClick.BACK_TO_SUMMARY -> {
                    RxBus2.publish(AssessmentLastQuestionSubmitEvent(questionId))
                    logBackToSummaryAnalyticEvent()
                    binding.buttonView.visibility = GONE
                    onBackPressed()
                }
            }
        }
    }

    private fun onSubmit(assessmentQuestion: AssessmentQuestionWithRelations) {
        logSubmitButtonAnalyticEvent(assessmentQuestion.question.choiceType)
        if (assessmentQuestion.question.choiceType == ChoiceType.FILL_IN_THE_BLANKS_TEXT) {
            RxBus2.publish(FillInTheBlankSubmitEvent(assessmentQuestion.question.remoteId))
        } else if (assessmentQuestion.question.choiceType == ChoiceType.MATCH_TEXT) {
            RxBus2.publish(MatchTheFollowingSubmitEvent(assessmentQuestion.question.remoteId))
        } else RxBus2.publish(McqSubmitEvent(assessmentQuestion.question.remoteId))

        assessmentQuestion.question.isAttempted = true
        assessmentQuestion.question.status = evaluateQuestionStatus(assessmentQuestion)
        showToastForQuestion(assessmentQuestion)
        viewModel.saveAssessmentQuestion(assessmentQuestion)
    }

    private fun logReviseConceptEvent(questionId: Int, title: String) {
        AppAnalytics.create(AnalyticsEvent.REVISE_CONCEPT_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId)
            .addParam(AnalyticsEvent.QUESTION_ID.name, questionId)
            .addParam(AnalyticsEvent.TITLE.name, title)
            .push()
    }

    private fun logNextButtonAnalyticEvent(choiceType: ChoiceType) {
        AppAnalytics.create(AnalyticsEvent.ASSESSMENT_NEXT_BUTTON_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId)
            .addParam(AnalyticsEvent.OPTION_TYPE.NAME, choiceType.type)
            .push()
    }

    private fun logSubmitButtonAnalyticEvent(choiceType: ChoiceType) {
        AppAnalytics.create(AnalyticsEvent.ASSESSMENT_SUBMIT_BUTTON_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId)
            .addParam(AnalyticsEvent.OPTION_TYPE.NAME, choiceType.type)
            .push()
    }

    private fun logBackToSummaryAnalyticEvent() {
        AppAnalytics.create(AnalyticsEvent.BACK_TO_SUMMARY_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId)
            .push()
    }

    private fun evaluateQuestionStatus(assessmentQuestion: AssessmentQuestionWithRelations): QuestionStatus {
        var status = QuestionStatus.NONE
        var isAttempted = false
        assessmentQuestion.choiceList.forEach {
            when (assessmentQuestion.question.choiceType) {
                ChoiceType.SINGLE_SELECTION_TEXT,
                ChoiceType.MULTI_SELECTION_TEXT,
                ChoiceType.SINGLE_SELECTION_IMAGE,
                ChoiceType.MULTI_SELECTION_IMAGE -> {
                    if (it.isSelectedByUser)
                        isAttempted = true
                    if (it.isCorrect != it.isSelectedByUser) {
                        status = QuestionStatus.WRONG
                    }
                }
                ChoiceType.FILL_IN_THE_BLANKS_TEXT -> {
                    if (it.userSelectedOrder < 50)
                        isAttempted = true
                    if (it.userSelectedOrder != it.correctAnswerOrder)
                        status = QuestionStatus.WRONG
                }
                ChoiceType.MATCH_TEXT -> {
                    if (it.userSelectedOrder < 50)
                        isAttempted = true
                    if (it.userSelectedOrder != it.correctAnswerOrder.minus(1))
                        status = QuestionStatus.WRONG
                }
            }
        }
        if (isAttempted.not())
            return QuestionStatus.SKIPPED
        return status
    }

    private fun onNext(
        isLastQuestion: Boolean,
        assessmentType: AssessmentType,
        assessmentQuestionWithRelations: AssessmentQuestionWithRelations,
        assessment: Assessment
    ) {
        logNextButtonAnalyticEvent(assessmentQuestionWithRelations.question.choiceType)
        if (isLastQuestion) {
            RxBus2.publish(AssessmentLastQuestionSubmitEvent(assessmentQuestionWithRelations.question.remoteId))
            if (assessmentType == AssessmentType.QUIZ) {
                when (assessment.status) {
                    AssessmentStatus.NOT_STARTED,
                    AssessmentStatus.STARTED -> {
                        showQuizSuccessFragment()
                        assessment.status = AssessmentStatus.COMPLETED
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.updateAssessmentStatus(assessmentId)
                        }
                    }

                    AssessmentStatus.COMPLETED -> {
                        val resultIntent = Intent().apply {
                            putExtra(CHAT_ROOM_ID, intent.getStringExtra(CHAT_ROOM_ID))
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            } else {
                // Disable ViewPager Scrolling
                binding.questionViewPager.isUserInputEnabled = false
                showTestSummaryFragment(assessmentId)
                binding.buttonView.visibility = GONE
            }
        } else {
            binding.questionViewPager.currentItem = binding.questionViewPager.currentItem + 1
        }
    }

    private fun onReviseConcept(reviseConcept: ReviseConcept?) {
        reviseConcept?.let {
            logReviseConceptEvent(reviseConcept.questionId, reviseConcept.title)
            showReviseConceptFragment(it)
        }
    }

    private fun moveToQuestion(questionId: Int) {
        isTestFragmentVisible = false
        val fragment = supportFragmentManager.findFragmentByTag("Test Summary")
        if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment).commit()
        var position = 0
        viewModel.assessmentLiveData.value?.questionList?.sortedBy { it.question.sortOrder }
            ?.forEachIndexed { index, assessmentQuestionWithRelations ->
                if (assessmentQuestionWithRelations.question.remoteId == questionId) {
                    position = index
                    return@forEachIndexed
                }
            }
        binding.questionViewPager.setCurrentItem(position, false)
    }

    fun submitTest() {
        if (viewModel.assessmentLiveData.value?.assessment?.status == AssessmentStatus.COMPLETED) {
            finish()
        } else {
            viewModel.postTestData(assessmentId)
        }
    }

    private fun bindView(assessmentWithRelations: AssessmentWithRelations) {
        binding.tvTitle.text = assessmentWithRelations.assessment.title
        setupViewPager(assessmentWithRelations)
    }

    private fun setupViewPager(assessmentWithRelations: AssessmentWithRelations) {
        val adapter =
            AssessmentQuestionAdapter(
                assessmentWithRelations.assessment,
                AssessmentQuestionViewType.CORRECT_ANSWER_VIEW,
                assessmentWithRelations.questionList
            )
        binding.questionViewPager.adapter = adapter
        TabLayoutMediator(
            binding.tabLayout,
            binding.questionViewPager
        ) { tab, position -> /*Do Nothing*/ }.attach()
        binding.questionViewPager.setPageTransformer(
            MarginPageTransformer(
                Utils.dpToPx(
                    applicationContext,
                    16f
                )
            )
        )
        val tabStrip: LinearLayout = binding.tabLayout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
        }

        // Enable/Disable Scrolling
        binding.questionViewPager.isUserInputEnabled =
            (assessmentWithRelations.assessment.type == AssessmentType.QUIZ || assessmentWithRelations.assessment.status == AssessmentStatus.COMPLETED).not()

        binding.questionViewPager.onPageSelected { position ->

            setButtonView(
                assessmentWithRelations.assessment.type,
                assessmentWithRelations.questionList.sortedBy { it.question.sortOrder }[position],
                assessmentWithRelations.questionList.size - 1 == position
            )
        }

    }

    private fun showToastForQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        if (evaluateAnswer(assessmentQuestion))
            showToast(getString(R.string.correct_answer_label))
        else
            showToast(getString(R.string.wrong_answer_label))
    }

    private fun evaluateAnswer(assessmentQuestion: AssessmentQuestionWithRelations?): Boolean {
        assessmentQuestion?.choiceList?.forEach {
            when (assessmentQuestion.question.choiceType) {
                ChoiceType.SINGLE_SELECTION_TEXT,
                ChoiceType.MULTI_SELECTION_TEXT,
                ChoiceType.SINGLE_SELECTION_IMAGE,
                ChoiceType.MULTI_SELECTION_IMAGE -> if (it.isCorrect != it.isSelectedByUser) {
                    return false
                }
                ChoiceType.FILL_IN_THE_BLANKS_TEXT -> if (it.userSelectedOrder != it.correctAnswerOrder)
                    return false
                ChoiceType.MATCH_TEXT -> if (it.column == ChoiceColumn.LEFT && it.userSelectedOrder != it.correctAnswerOrder.minus(
                        1
                    )
                )
                    return false
            }
        }
        return true
    }

    private fun setButtonView(
        assessmentType: AssessmentType,
        assessmentQuestion: AssessmentQuestionWithRelations,
        isLastQuestion: Boolean
    ) {
        binding.buttonView.bind(assessmentType, assessmentQuestion, isLastQuestion)
    }

    override fun onBackPressed() {
        if (exitActivity())
            super.onBackPressed()
    }

    private fun subscribeRxBus() {

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(TestItemClickedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    moveToQuestion(it.questionId)
                    binding.buttonView.visibility = VISIBLE
                })

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(AssessmentButtonClickEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleButtonClicks(
                        it.assessmentButtonClick,
                        it.assessmentType,
                        it.questionId
                    )
                })
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun exitActivity(): Boolean {
        if (isTestFinished) {
            if (isTestFragmentVisible) {
                return true
            } else {
                showTestSummaryFragment(assessmentId)
                return false
            }
        }
        return true
    }

    companion object {
        const val KEY_ASSESSMENT_ID = "assessment-id"

        fun startAssessmentActivity(
            activity: Activity,
            requestCode: Int,
            assessmentId: Int,
            chatRoomId: String? = null,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, AssessmentActivity::class.java).apply {
                putExtra(KEY_ASSESSMENT_ID, assessmentId)
                putExtra(CHAT_ROOM_ID, chatRoomId)
                if (startedFrom.isNotBlank())
                    putExtra(STARTED_FROM, startedFrom)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivityForResult(this, requestCode)
            }
        }
    }
}
