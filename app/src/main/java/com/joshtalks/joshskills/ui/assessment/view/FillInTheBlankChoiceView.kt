package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonStateEvent
import com.joshtalks.joshskills.repository.local.eventbus.FillInTheBlankSubmitEvent
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.adapter.FillInTheBlankQuestionAdapter
import com.joshtalks.joshskills.ui.assessment.extra.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.listener.OnChoiceClickListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class FillInTheBlankChoiceView : FrameLayout,
    OnChoiceClickListener {

    private var assessment: Assessment? = null
    private var viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalAnswered: TextView
    private lateinit var seeAnswer: MaterialTextView
    private lateinit var choicesChipGroup: ChipGroup
    private var chipChoiceList = mutableListOf<Choice>()
    private var selectedOrder = mutableMapOf<Int, Int>()
    private var filled = 0
    private var totalOptions = 0
    private var compositeDisposable = CompositeDisposable()
    private var correctAnswerVisible = false


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
        View.inflate(context, R.layout.fill_in_the_blank_choice_view, this)
        recyclerView = findViewById(R.id.recycler_view)
        totalAnswered = findViewById(R.id.total_answered)
        choicesChipGroup = findViewById(R.id.chip_choice)
        seeAnswer = findViewById(R.id.see_answer)
        addObservers()

    }

    private fun addObservers() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(FillInTheBlankSubmitEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    onSubmit()

                })

        seeAnswer.setOnClickListener {
            choicesChipGroup.visibility=View.GONE
            toogleViews()
        }
    }

    private fun toogleViews() {
        if (correctAnswerVisible) {
            sortChoiceViaUserOrder()
        } else {
            sortChoiceViaCorrectOrder()
        }
        correctAnswerVisible = correctAnswerVisible.not()
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun sortChoiceViaCorrectOrder() {
        logSeeYourAnswerEvent()
        chipChoiceList.clear()
        assessmentQuestion?.choiceList?.sortedBy { it.correctAnswerOrder }?.forEach { choice ->
            choice.userSelectedOrder = choice.correctAnswerOrder
            choice.isSelectedByUser=true
            chipChoiceList.add(choice)
        }
        seeAnswer.text = context.getString(R.string.see_your_answer)
    }

    private fun logSeeYourAnswerEvent() {
        AppAnalytics.create(AnalyticsEvent.SEE_YOUR_ANSWER_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessment?.remoteId ?: -1)
            .addParam(AnalyticsEvent.QUESTION_ID.NAME, assessmentQuestion?.question?.remoteId?:-1)
            .push()
    }

    private fun sortChoiceViaUserOrder() {
        chipChoiceList.clear()
        assessmentQuestion?.choiceList?.sortedBy { it.userSelectedOrder }?.forEach { choice ->
            choice.userSelectedOrder =
                selectedOrder.get(choice.remoteId) ?: choice.userSelectedOrder
            chipChoiceList.add(choice)
        }
        chipChoiceList.clear()
        assessmentQuestion?.choiceList?.sortedBy { it.userSelectedOrder }?.forEach { choice ->
            if(choice.userSelectedOrder>50){
                choice.isSelectedByUser=false
            }
            chipChoiceList.add(choice)
        }

        seeAnswer.text = context.getString(R.string.see_answer)
    }

    fun bind(
        assessment: Assessment,
        viewType: AssessmentQuestionViewType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        this.assessment = assessment
        this.viewType = viewType
        this.assessmentQuestion = assessmentQuestion
        setUpUI()
    }

    private fun setUpUI() {

        filled = assessmentQuestion?.choiceList?.filter { it.isSelectedByUser == true }?.size ?: 0
        choicesChipGroup.removeAllViews()
        chipChoiceList.clear()

        assessmentQuestion?.let { addChoicesListItems(it) }

        assessmentQuestion?.choiceList?.sortedBy { it.sortOrder }?.forEach {

            val chip = LayoutInflater.from(context)
                .inflate(R.layout.choice_fib_item, null, false) as Chip
            chip.text = it.text
            chip.tag = it.remoteId
            chip.id = it.remoteId
            chip.setOnClickListener(chipClickListener)
            choicesChipGroup.addView(chip)
            if (it.isSelectedByUser) {
                chip.visibility = View.GONE
            }
        }

        totalOptions = assessmentQuestion?.choiceList?.size ?: 0
        totalAnswered.text = filled.toString().plus("/").plus(totalOptions)

        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.CENTER
        layoutManager.flexWrap = FlexWrap.WRAP

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter =
            FillInTheBlankQuestionAdapter(
                assessment!!,
                assessmentQuestion!!.question,
                chipChoiceList as ArrayList<Choice>,
                this
            )

        if ((assessment!!.status == AssessmentStatus.COMPLETED) || (assessmentQuestion!!.question.isAttempted && assessment!!.type == AssessmentType.QUIZ)) {
            seeAnswer.visibility = View.VISIBLE
            disableAllClicks()
        }
    }

    private val chipClickListener = OnClickListener { view ->

        val anim = AlphaAnimation(1f, 0f)
        anim.duration = 250
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })

        view.startAnimation(anim)
        val choice = chipChoiceList.filter { it.remoteId == view.id }[0]
        if (choice.isSelectedByUser.not())
            filled++
        choice.isSelectedByUser = true
        choice.userSelectedOrder = filled
        assessmentQuestion!!.choiceList.filter { it.remoteId == choice.remoteId }
            .map { qChoice ->
                qChoice.isSelectedByUser = true
                qChoice.userSelectedOrder = filled
            }

        updateView()
        publishUpdateButtonViewEvent(filled == totalOptions)
    }


    private fun addChoicesListItems(assessmentQuestion: AssessmentQuestionWithRelations) {

        if (assessmentQuestion.question.isAttempted && assessment!!.type == AssessmentType.QUIZ) {
            addViaUserSelectedOrder(assessmentQuestion)

        } else if (assessmentQuestion.question.isAttempted.not() && assessment!!.type == AssessmentType.QUIZ) {
            addViaSortOrder(assessmentQuestion)

        } else if (assessment!!.type == AssessmentType.TEST && assessment!!.status == AssessmentStatus.COMPLETED) {
            addViaUserSelectedOrder(assessmentQuestion)
        } else {
            if (filled == 0)
                addViaSortOrder(assessmentQuestion)
            else addViaUserSelectedOrder(assessmentQuestion)
        }
        if (assessmentQuestion.question.isAttempted || assessment!!.status == AssessmentStatus.COMPLETED) {
            filled = chipChoiceList.size
            disableAllClicks()
            assessmentQuestion.choiceList.forEach { choice ->
                selectedOrder.put(choice.remoteId, choice.userSelectedOrder)
            }
        }
    }

    private fun addViaSortOrder(assessmentQuestion: AssessmentQuestionWithRelations) {
        assessmentQuestion.choiceList.sortedBy { it.sortOrder }.forEach { choice ->
            choice.userSelectedOrder = 100
            chipChoiceList.add(choice)
        }
    }

    private fun addViaUserSelectedOrder(assessmentQuestion: AssessmentQuestionWithRelations) {
        assessmentQuestion.choiceList.sortedBy { it.userSelectedOrder }.forEach { choice ->
            chipChoiceList.add(choice)
        }
        filled = chipChoiceList.size
        disableAllClicks()
    }

    private fun updateView(
        isDeleted: Boolean = false,
        fromIndex: Int = 100
    ) {
        totalAnswered.text = filled.toString().plus("/").plus(totalOptions)

        if (isDeleted)
            this.chipChoiceList.forEach {
                it.userSelectedOrder =
                    if (it.userSelectedOrder > fromIndex) it.userSelectedOrder - 1 else it.userSelectedOrder
            }

        this.chipChoiceList.sortBy { it.userSelectedOrder }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun publishUpdateButtonViewEvent(isAnswered: Boolean) {
        RxBus2.publish(
            AssessmentButtonStateEvent(
                assessment!!.type,
                assessmentQuestion?.question?.isAttempted!!,
                isAnswered
            )
        )
    }

    override fun onChoiceClick(choice: Choice) {
        if (assessmentQuestion?.question?.choiceType == ChoiceType.FILL_IN_THE_BLANKS_TEXT) {
            logChoiceSelectedEvent(assessmentQuestion?.question?.choiceType)
            choicesChipGroup.forEach { view ->
                if (choice.remoteId == view.id) {
                    view.visibility = View.VISIBLE
                }
            }
            val choice = chipChoiceList.filter { it.remoteId == choice.remoteId }.get(0)
            if (choice.isSelectedByUser) {
                filled -= 1
            }
            choice.isSelectedByUser = false
            val fromIndex = choice.userSelectedOrder
            assessmentQuestion!!.choiceList.filter { it.remoteId == choice.remoteId }
                .map { qChoice ->
                    qChoice.isSelectedByUser = false
                    qChoice.userSelectedOrder = 100
                }
            choice.userSelectedOrder = 100

            updateView(true, fromIndex)
            publishUpdateButtonViewEvent(false)
        }
    }

    private fun logChoiceSelectedEvent(choiceType: ChoiceType?) {
        AppAnalytics.create(AnalyticsEvent.OPTION_SELECTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.OPTION_TYPE.NAME, choiceType?.type ?: "NA")
            .push()
    }

    private fun onSubmit() {
        updateView()
        disableAllClicks()
    }

    private fun disableAllClicks() {
        choicesChipGroup.forEach { view ->
            view.isClickable = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.tag("onAttachedToWindow").e("FillInTheBlankChoiceView")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
        Timber.tag("onDetachedFromWindow").e("FillInTheBlankChoiceView")
    }
}
