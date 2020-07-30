package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import com.esafirm.imagepicker.view.GridSpacingItemDecoration
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonStateEvent
import com.joshtalks.joshskills.repository.local.eventbus.McqSubmitEvent
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.viewholder.MCQChoiceViewHolder
import com.joshtalks.joshskills.ui.assessment.viewholder.OnChoiceClickListener
import com.mindorks.placeholderview.PlaceHolderView
import com.vanniktech.emoji.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.InvalidClassException


class MCQChoicesView : FrameLayout, OnChoiceClickListener {

    private var assessment: Assessment? = null
    private var viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private val compositeDisposable = CompositeDisposable()

    private lateinit var choicesPlaceHolderView: PlaceHolderView

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
        View.inflate(context, R.layout.mcq_choices_view, this)
        choicesPlaceHolderView = findViewById(R.id.choices_recycler_view)
        addObservers()
    }

    private fun addObservers() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(McqSubmitEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    choicesPlaceHolderView.adapter?.notifyDataSetChanged()
                })

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
        setupPlaceHolderView()
        assessmentQuestion?.let { addChoicesListItems(it) }
    }

    private fun setupPlaceHolderView() {
        lateinit var layoutManager: RecyclerView.LayoutManager
        when (assessmentQuestion?.question?.choiceType) {

            ChoiceType.SINGLE_SELECTION_TEXT, ChoiceType.MULTI_SELECTION_TEXT -> {
                layoutManager = SmoothLinearLayoutManager(context)
                layoutManager.isSmoothScrollbarEnabled = false
            }

            ChoiceType.SINGLE_SELECTION_IMAGE, ChoiceType.MULTI_SELECTION_IMAGE -> {
                layoutManager = GridLayoutManager(context, 2)
                layoutManager.isSmoothScrollbarEnabled = false
                addGridItemDecoration()
            }

            else -> {
                Timber.tag("Wrong Choice Type").e("Wrong Choice Type in  MCQViewHolder")
                Crashlytics.logException(InvalidClassException("Wrong Choice Type"))
            }

        }

        choicesPlaceHolderView.builder
            .setHasFixedSize(true)
            .setLayoutManager(layoutManager)

    }

    private fun addChoicesListItems(assessmentQuestion: AssessmentQuestionWithRelations) {
        if (choicesPlaceHolderView.viewAdapter == null || choicesPlaceHolderView.viewAdapter.itemCount == 0) {
            assessmentQuestion.choiceList.sortedBy { it.sortOrder }.forEach { choice ->
                choicesPlaceHolderView.addView(
                    MCQChoiceViewHolder(
                        assessmentQuestion.question.choiceType,
                        choice.sortOrder,
                        choice,
                        assessment!!,
                        assessmentQuestion.question,
                        this,
                        context
                    )
                )
            }
        }
    }

    private fun addGridItemDecoration() {
        if (choicesPlaceHolderView.itemDecorationCount < 1) {
            choicesPlaceHolderView.addItemDecoration(
                GridSpacingItemDecoration(
                    2, Utils.dpToPx(context, 12f),
                    true
                )
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }

    override fun onChoiceClick(choice: Choice) {
        if (assessmentQuestion?.question?.isAttempted == false) {
            when (assessmentQuestion?.question?.choiceType) {

                ChoiceType.SINGLE_SELECTION_TEXT, ChoiceType.SINGLE_SELECTION_IMAGE -> {
                    assessmentQuestion?.choiceList?.forEach { it.isSelectedByUser = false }
                    choice.isSelectedByUser = true
                    choicesPlaceHolderView.adapter?.notifyDataSetChanged()

                    RxBus2.publish(
                        AssessmentButtonStateEvent(
                            assessment!!.type,
                            assessmentQuestion?.question?.isAttempted!!,
                            true
                        )
                    )

                }

                ChoiceType.MULTI_SELECTION_TEXT, ChoiceType.MULTI_SELECTION_IMAGE -> {
                    choice.isSelectedByUser = choice.isSelectedByUser.not()
                    choicesPlaceHolderView.adapter?.notifyDataSetChanged()

                    var isAnswered = false
                    assessmentQuestion?.choiceList?.forEach {
                        if (it.isSelectedByUser) {
                            isAnswered = true
                            return@forEach
                        }
                    }

                    RxBus2.publish(
                        AssessmentButtonStateEvent(
                            assessment!!.type,
                            assessmentQuestion?.question?.isAttempted!!,
                            isAnswered
                        )
                    )

                }

                else -> {
                    Timber.tag("Wrong Choice Type").e("Wrong Choice Type in  MCQViewHolder")
                    Crashlytics.logException(InvalidClassException("Wrong Choice Type"))
                }
            }
        }
    }


}
