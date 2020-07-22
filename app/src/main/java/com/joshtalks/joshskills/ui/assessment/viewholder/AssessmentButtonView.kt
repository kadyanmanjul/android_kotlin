package com.joshtalks.joshskills.ui.assessment.viewholder


import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import timber.log.Timber

class AssessmentButtonView : FrameLayout {

    private var assessmentType: AssessmentType? = null
    private var assessmentStatus: AssessmentStatus? = null
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var totalQuestions: Int? = null
    private var numberOfCorrectAnswers: Int = 0
    private var questionAnswered: Int = 0

    private lateinit var submitBtn: MaterialTextView
    private lateinit var reviseBtn: MaterialTextView
    private lateinit var nextBtn: MaterialTextView
    private lateinit var submitContainer: ConstraintLayout
    private lateinit var reviseConceptContainer: ConstraintLayout
    private lateinit var listener: AssessmentButtonListener

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.assessment_button_view, this)
        submitBtn = findViewById(R.id.submit_btn)
        reviseBtn = findViewById(R.id.revise_btn)
        nextBtn = findViewById(R.id.next_btn)
        submitContainer = findViewById(R.id.submit_btn_container)
        reviseConceptContainer = findViewById(R.id.revise_btn_container)
        addListeners()
    }

    private fun addListeners() {
        submitBtn.setOnClickListener {
            if (numberOfCorrectAnswers == questionAnswered && assessmentType == AssessmentType.QUIZ) {
                showNextButtonContainerAndPublishEvent()
            } else if (assessmentType == AssessmentType.TEST)
                listener.onSubmit(
                    assessmentType!!,
                    assessmentQuestion!!
                )
            else if (assessmentQuestion!!.question.choiceType != ChoiceType.FILL_IN_THE_BLANKS_TEXT &&
                questionAnswered >= 1
            ) {
                showNextButtonContainerAndPublishEvent()
            }
        }
        reviseBtn.setOnClickListener {
            assessmentQuestion!!.reviseConcept?.let { it1 -> listener.onReviseConcept(it1) }
        }
        nextBtn.setOnClickListener {
            listener.onNext(
                assessmentQuestion!!.question.sortOrder == totalQuestions,
                assessmentType!!
            )
        }
    }

    private fun showNextButtonContainerAndPublishEvent() {
        showNextButtonContainer()
        listener.onSubmit(
            assessmentType!!,
            assessmentQuestion!!
        )
    }

    private fun showNextButtonContainer() {
        submitContainer.visibility = View.GONE
        reviseConceptContainer.visibility = View.VISIBLE
        if (assessmentQuestion!!.reviseConcept != null && assessmentType == AssessmentType.QUIZ) {
            reviseBtn.visibility = View.VISIBLE
        } else reviseBtn.visibility = View.GONE
    }

    private fun showSubmitButtonContainer() {
        submitContainer.visibility = View.VISIBLE
        reviseConceptContainer.visibility = View.GONE
    }

    private fun alterViews(choiceList: List<Choice>) {
        questionAnswered = 0
        choiceList.forEach { choice ->
            if (choice.isSelectedByUser) {
                questionAnswered = questionAnswered + 1
            }
            if (assessmentType == AssessmentType.QUIZ) {
                if (assessmentQuestion!!.question.choiceType == ChoiceType.FILL_IN_THE_BLANKS_TEXT
                    &&
                    numberOfCorrectAnswers == questionAnswered
                ) {
                    setSubmitBtnColor(true)
                } else if (assessmentQuestion!!.question.choiceType != ChoiceType.FILL_IN_THE_BLANKS_TEXT
                    && questionAnswered >= 1
                ) {
                    setSubmitBtnColor(true)

                } else {
                    setSubmitBtnColor(false)

                }
            } else {
                setSubmitBtnColor(true)

            }
        }
    }

    private fun setSubmitBtnColor(boolean: Boolean) {
        submitBtn.isClickable = boolean
        if (boolean)
            submitBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.button_primary_color
                )
            )
        else submitBtn.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.light_grey
            )
        )
    }

    fun bind(
        assessmentType: AssessmentType,
        assessmentStatus: AssessmentStatus,
        assessmentQuestion: AssessmentQuestionWithRelations,
        listener: AssessmentButtonListener,
        totalQuestions: Int
    ) {
        this.assessmentType = assessmentType
        this.assessmentStatus = assessmentStatus
        this.assessmentQuestion = assessmentQuestion
        this.listener = listener
        this.totalQuestions = totalQuestions
        setUpUI()
    }

    private fun setUpUI() {
        if (assessmentQuestion!!.question.sortOrder == totalQuestions)
            nextBtn.text = context.getString(R.string.finish)
        else nextBtn.text = context.getString(R.string.next)
        if (assessmentQuestion!!.question.choiceType != ChoiceType.FILL_IN_THE_BLANKS_TEXT)
            assessmentQuestion!!.choiceList.forEach {
                if (it.isCorrect)
                    numberOfCorrectAnswers = numberOfCorrectAnswers + 1
            }
        else numberOfCorrectAnswers = assessmentQuestion!!.choiceList.size

        if (assessmentType == AssessmentType.TEST) {
            showNextButtonContainer()
        } else {
            assessmentQuestion?.let { it ->
                if (it.question.isAttempted) {
                    showNextButtonContainer()
                } else {
                    showSubmitButtonContainer()
                }
            }
        }
        alterViews(assessmentQuestion!!.choiceList)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.tag("onDetachedFromWindow").e("ButtonView")
    }

    interface AssessmentButtonListener {
        fun onSubmit(
            assessmentType: AssessmentType,
            assessmentQuestion: AssessmentQuestionWithRelations
        )

        fun onNext(isLastQuestion: Boolean, assessmentType: AssessmentType)
        fun onReviseConcept(reviseConcept: ReviseConcept)
    }

    fun onChoiceAdded(choice: List<Choice>) {
        alterViews(choice)
    }
}
