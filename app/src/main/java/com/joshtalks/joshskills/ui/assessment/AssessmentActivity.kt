package com.joshtalks.joshskills.ui.assessment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
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
import com.joshtalks.joshskills.core.loadJSONFromAsset
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityAssessmentBinding
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.ui.assessment.view.FillInTheBlankChoiceView
import com.joshtalks.joshskills.ui.assessment.viewholder.AssessmentButtonView
import com.joshtalks.joshskills.ui.assessment.viewholder.AssessmentQuestionAdapter

class AssessmentActivity : CoreJoshActivity(), AssessmentButtonView.AssessmentButtonListener,
    FillInTheBlankChoiceView.FillInTheBlankChoiceClickListener {

    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AssessmentViewModel::class.java) }
    private var assessmentId: Int = 0
    private var flowFrom: String? = null
    private val hintOptionsSet = mutableSetOf<ChoiceType>()


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
//        test()
        if (assessmentId != 0) {
            getAssessmentDetails(assessmentId)
        } else {
            finish()
        }
        subscribeLiveData()
    }

    private fun showTestSummaryFragment(questionId: Int) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                TestSummaryFragment.newInstance(questionId),
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

    private fun subscribeLiveData() {
        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
        })

        viewModel.assessmentLiveData.observe(this, Observer { assessmentWithRelations ->
            bindView(assessmentWithRelations)
        })
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

    fun test() {
        val assessmentResponse = AppObjectController.gsonMapperForLocal.fromJson(
            loadJSONFromAsset("assessmentJson.json"),
            AssessmentResponse::class.java
        )

        val data = AssessmentWithRelations(assessmentResponse)
        bindView(data)
    }

    private fun bindView(assessmentWithRelations: AssessmentWithRelations) {
        setupViewPager(assessmentWithRelations)
    }

    private fun setupViewPager(assessmentWithRelations: AssessmentWithRelations) {
        val adapter = AssessmentQuestionAdapter(
            assessmentWithRelations.assessment.type,
            assessmentWithRelations.assessment.status,
            AssessmentQuestionViewType.CORRECT_ANSWER_VIEW,
            assessmentWithRelations.questionList, this
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
        binding.questionViewPager.onPageSelected { position ->
            val type = assessmentWithRelations.questionList[position].question.choiceType
            if (hintOptionsSet.contains(type).not()) {
                assessmentWithRelations.assessmentIntroList.find { it.type == type }?.run {
                    IntroQuestionFragment.newInstance(this)
                        .show(supportFragmentManager, "Question Tip")
                    hintOptionsSet.add(type)
                }
            }
            binding.buttonView.bind(
                assessmentWithRelations.assessment.type,
                assessmentWithRelations.assessment.status,
                assessmentWithRelations.questionList[position],
                this,
                assessmentWithRelations.questionList.size
            )
        }

    }

    override fun onChoiceAdded(choice: List<Choice>) {
        binding.buttonView.onChoiceAdded(choice)
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

    override fun onSubmit(
        assessmentType: AssessmentType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        if (assessmentType == AssessmentType.QUIZ) {
            invalidateQuestionView()
            showToastForQuestion(assessmentQuestion)
            assessmentQuestion.question.isAttempted = true
            viewModel.saveAssessmentQuestion(assessmentQuestion)
        }
    }

    private fun invalidateQuestionView() {
        (binding.questionViewPager.adapter as AssessmentQuestionAdapter).registerSubmitCallback()
    }

    override fun onNext(isLastQuestion: Boolean, assessmentType: AssessmentType) {
        moveToNextQuestion(isLastQuestion, assessmentType)
    }

    private fun moveToNextQuestion(
        lastQuestion: Boolean,
        assessmentType: AssessmentType
    ) {
        if (!lastQuestion)
            binding.questionViewPager.currentItem = binding.questionViewPager.currentItem + 1
        else {
            if (assessmentType == AssessmentType.QUIZ)
                showQuizSuccessFragment()
            else showTestSummaryFragment(assessmentId)
        }
    }

    override fun onReviseConcept(reviseConcept: ReviseConcept) {
        showReviseConceptFragment(reviseConcept)
    }
}
