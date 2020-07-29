package com.joshtalks.joshskills.ui.assessment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityAssessmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClick
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClickEvent
import com.joshtalks.joshskills.repository.local.eventbus.FillInTheBlankSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.McqSubmitEvent
import com.joshtalks.joshskills.repository.local.eventbus.TestItemClickedEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.ui.assessment.viewholder.AssessmentQuestionAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class AssessmentActivity : CoreJoshActivity() {

    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AssessmentViewModel::class.java) }
    private var assessmentId: Int = 0
    private var flowFrom: String? = null
    private var compositeDisposable = CompositeDisposable()
    private var isTestFinished = false
    private var isTestFragmentVisible = false
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
        assessmentId = intent.getIntExtra(AssessmentActivity.KEY_ASSESSMENT_ID, 0)
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
            assessmentWithRelations.questionList.sortedBy { it.question.sortOrder }
            bindView(assessmentWithRelations)
        })

        viewModel.assessmentStatus.observe(this, Observer { status ->
            if (status == AssessmentStatus.COMPLETED && viewModel.getAssessmentType() == AssessmentType.TEST) {
                showTestSummaryFragment(assessmentId, true)
            }

        })
    }

    private fun handleButtonClicks(
        assessmentButtonClick: AssessmentButtonClick,
        assessmentType: AssessmentType
    ) {
        val assessmentWithRelations = viewModel.assessmentLiveData.value
        when (assessmentButtonClick) {
            AssessmentButtonClick.SUBMIT -> {
                onSubmit(assessmentWithRelations?.questionList?.get(binding.questionViewPager.currentItem)!!)
            }

            AssessmentButtonClick.NEXT -> {
                onNext(
                    assessmentWithRelations!!.questionList.size - 1 == binding.questionViewPager.currentItem,
                    assessmentType,
                    assessmentWithRelations.questionList.get(binding.questionViewPager.currentItem),
                    assessmentWithRelations.assessment
                )
            }
            AssessmentButtonClick.REVISE -> {
                onReviseConcept(assessmentWithRelations?.questionList?.get(binding.questionViewPager.currentItem)!!.reviseConcept)
            }
            AssessmentButtonClick.NONE -> {

            }
        }
    }

    private fun onSubmit(assessmentQuestion: AssessmentQuestionWithRelations) {
        if (assessmentQuestion.question.choiceType == ChoiceType.FILL_IN_THE_BLANKS_TEXT)
            RxBus2.publish(FillInTheBlankSubmitEvent(assessmentQuestion.question.remoteId))
        else
            RxBus2.publish(McqSubmitEvent(assessmentQuestion.question.remoteId))
        assessmentQuestion.question.isAttempted = true
        assessmentQuestion.question.status = evaluateQuestionStatus(assessmentQuestion)
        showToastForQuestion(assessmentQuestion)
        viewModel.saveAssessmentQuestion(assessmentQuestion)
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
                else -> status = QuestionStatus.SKIPPED
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
        if (assessmentType == AssessmentType.QUIZ) {
            viewModel.saveAssessmentQuestion(assessmentQuestionWithRelations)
        }
        if (isLastQuestion) {
            if (assessmentType == AssessmentType.QUIZ) {
                when (assessment.status) {
                    AssessmentStatus.NOT_STARTED,
                    AssessmentStatus.STARTED -> {
                        showQuizSuccessFragment()
                        assessment.status = AssessmentStatus.COMPLETED
                        viewModel.updateAssessmentStatus(assessmentId)
                    }

                    AssessmentStatus.COMPLETED -> {
                        finish()
                    }
                }
            } else {
                viewModel.assessmentLiveData
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
            showReviseConceptFragment(it)
        }
    }

    private fun moveToQuestion(questionId: Int) {
        isTestFragmentVisible = false
        val fragment = supportFragmentManager.findFragmentByTag("Test Summary")
        if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment).commit()
        binding.questionViewPager.setCurrentItem(questionId - 1, false)
    }

    fun submitTest() {
        if (viewModel.assessmentLiveData.value?.assessment?.status == AssessmentStatus.COMPLETED) {
            finish()
        } else {
            viewModel.postTestData(assessmentId)
            AppObjectController.uiHandler.postDelayed({
                finish()
            }, 200)
        }
    }

    private fun bindView(assessmentWithRelations: AssessmentWithRelations) {
        setupViewPager(assessmentWithRelations)
    }

    private fun setupViewPager(assessmentWithRelations: AssessmentWithRelations) {
        val adapter = AssessmentQuestionAdapter(
            assessmentWithRelations.assessment.type,
            assessmentWithRelations.assessment.status,
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
        binding.questionViewPager.offscreenPageLimit = 1

        // Enable/Disable Scrolling
        binding.questionViewPager.isUserInputEnabled =
            (assessmentWithRelations.assessment.type == AssessmentType.QUIZ || assessmentWithRelations.assessment.status == AssessmentStatus.COMPLETED).not()

        binding.questionViewPager.onPageSelected { position ->

            setButtonView(
                assessmentWithRelations.assessment.type,
                assessmentWithRelations.questionList[position],
                assessmentWithRelations.questionList.size - 1 == position
            )
        }

    }

    private fun showToastForQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        if (evaluateAnswer(assessmentQuestion))
            showToast("Your answer is Correct")
        else
            showToast("Your answer is Wrong")
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
                else -> return true
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

    private fun subsrcibeRxBus() {

        compositeDisposable.add(
            RxBus2.listen(TestItemClickedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    moveToQuestion(it.questionId)
                })

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(AssessmentButtonClickEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleButtonClicks(
                        it.assessmentButtonClick,
                        it.assessmentType
                    )
                })
    }

    override fun onResume() {
        super.onResume()
        subsrcibeRxBus()
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
                showTestSummaryFragment(
                    assessmentId, viewModel.assessmentStatus.value == AssessmentStatus.COMPLETED
                )
                return false
            }
        }
        return true
    }

    companion object {
        const val KEY_ASSESSMENT_ID = "assessment-id"

        fun startAssessmentActivity(
            activity: Activity,
            assessmentId: Int,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, AssessmentActivity::class.java).apply {
                putExtra(KEY_ASSESSMENT_ID, assessmentId)
                if (startedFrom.isNotBlank())
                    putExtra(STARTED_FROM, startedFrom)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }

        /**
         *  Use this method for
         *  Directly Opening Assessment Screen
         *  from Notification
         */
        fun getIntent(
            context: Context,
            assessmentId: Int,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, AssessmentActivity::class.java).apply {
            putExtra(KEY_ASSESSMENT_ID, assessmentId)
            if (startedFrom.isNotBlank())
                putExtra(STARTED_FROM, startedFrom)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }

}
