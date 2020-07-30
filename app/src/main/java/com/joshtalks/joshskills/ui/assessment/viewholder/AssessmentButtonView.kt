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
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClick
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonClickEvent
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonStateEvent
import com.joshtalks.joshskills.repository.local.eventbus.TestItemClickedEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AssessmentButtonView : FrameLayout {

    private var assessmentType: AssessmentType? = null
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var isLastQuestion = false

    private lateinit var submitBtn: MaterialTextView
    private lateinit var reviseBtn: MaterialTextView
    private lateinit var nextBtn: MaterialTextView
    private lateinit var submitContainer: ConstraintLayout
    private lateinit var reviseConceptContainer: ConstraintLayout
    private lateinit var backBtn: MaterialTextView
    private lateinit var backBtnContainer: ConstraintLayout
    private var compositeDisposable = CompositeDisposable()


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
        backBtn = findViewById(R.id.back_button)
        backBtnContainer = findViewById(R.id.back_button_container)
        addListeners()
    }

    private fun addListeners() {
        submitBtn.setOnClickListener {
            assessmentQuestion?.question?.isAttempted = true
            if (assessmentType == AssessmentType.TEST) {
                RxBus2.publish(
                    AssessmentButtonClickEvent(
                        AssessmentType.TEST,
                        assessmentQuestion!!.question.remoteId,
                        assessmentQuestion!!.question.isAttempted,
                        true,
                        AssessmentButtonClick.SUBMIT
                    )
                )
            } else {
                showNextButtonContainer()
                assessmentQuestion?.let { question ->
                    RxBus2.publish(
                        AssessmentButtonClickEvent(
                            AssessmentType.QUIZ,
                            assessmentQuestion!!.question.remoteId,
                            assessmentQuestion!!.question.isAttempted,
                            true,
                            AssessmentButtonClick.SUBMIT
                        )
                    )
                }
            }
        }

        reviseBtn.setOnClickListener {
            assessmentQuestion?.reviseConcept?.let { reviseConcept ->
                RxBus2.publish(
                    AssessmentButtonClickEvent(
                        AssessmentType.QUIZ,
                        assessmentQuestion!!.question.remoteId,
                        assessmentQuestion!!.question.isAttempted,
                        true,
                        AssessmentButtonClick.REVISE
                    )
                )
            }
        }

        nextBtn.setOnClickListener {
            RxBus2.publish(
                AssessmentButtonClickEvent(
                    assessmentType!!,
                    assessmentQuestion!!.question.remoteId,
                    assessmentQuestion!!.question.isAttempted,
                    true,
                    AssessmentButtonClick.NEXT
                )
            )
        }

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(AssessmentButtonStateEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    setColor(
                        it.assessmentType, it.isAnswered
                    )
                })

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(TestItemClickedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    backBtnContainer.visibility = View.VISIBLE
                    submitContainer.visibility = View.GONE
                    reviseConceptContainer.visibility = View.GONE
                })

        backBtn.setOnClickListener {
            RxBus2.publish(
                AssessmentButtonClickEvent(
                    assessmentType!!,
                    assessmentQuestion!!.question.remoteId,
                    assessmentQuestion!!.question.isAttempted,
                    true,
                    AssessmentButtonClick.BACK_TO_SUMMARY
                )
            )
        }

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

    private fun setColor(
        assessmentType: AssessmentType,
        isAnswered: Boolean
    ) {
        if (assessmentType == AssessmentType.TEST) {
            setSubmitBtnActive(true)
        } else {
            setSubmitBtnActive(isAnswered)
        }
    }

    private fun setSubmitBtnActive(isActive: Boolean) {
        submitBtn.isClickable = isActive
        val btnColor = if (isActive) R.color.button_color else R.color.light_grey
        submitBtn.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                btnColor
            )
        )
    }

    fun bind(
        assessmentType: AssessmentType,
        assessmentQuestion: AssessmentQuestionWithRelations,
        isLastQuestion: Boolean
    ) {
        this.assessmentType = assessmentType
        this.assessmentQuestion = assessmentQuestion
        this.isLastQuestion = isLastQuestion
        setUpUI(isLastQuestion)
    }

    private fun setUpUI(isLastQuestion: Boolean) {
        nextBtn.text =
            if (isLastQuestion) {
                context.getString(R.string.finish)
            } else {
                context.getString(R.string.next)
            }

        if (assessmentType == AssessmentType.TEST) {
            showNextButtonContainer()
        } else {
            if (assessmentQuestion?.question?.isAttempted != false) {
                showNextButtonContainer()
            } else {
                showSubmitButtonContainer()
            }
        }
        setColor(assessmentType!!, isAnswereCorrect())
    }

    private fun isAnswereCorrect(): Boolean {
        when (assessmentQuestion?.question?.choiceType) {

            ChoiceType.SINGLE_SELECTION_TEXT,
            ChoiceType.SINGLE_SELECTION_IMAGE,
            ChoiceType.MULTI_SELECTION_TEXT,
            ChoiceType.MULTI_SELECTION_IMAGE -> {
                assessmentQuestion?.choiceList?.forEach {
                    if (it.isSelectedByUser) {
                        return true
                    }
                }
            }

            ChoiceType.FILL_IN_THE_BLANKS_TEXT -> {
                var numberOfChoicesSelected = 0
                assessmentQuestion?.choiceList?.forEach {
                    if (it.isSelectedByUser) {
                        numberOfChoicesSelected++
                    }
                }
                if (numberOfChoicesSelected == assessmentQuestion?.choiceList?.size) {
                    return true
                }
            }

            else ->
                return false
        }
        return false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.tag("onDetachedFromWindow").e("ButtonView")
    }

}
