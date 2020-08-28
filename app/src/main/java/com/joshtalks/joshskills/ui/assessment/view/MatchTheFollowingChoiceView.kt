package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentButtonStateEvent
import com.joshtalks.joshskills.repository.local.eventbus.MatchTheFollowingSubmitEvent
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn
import com.joshtalks.joshskills.ui.assessment.adapter.MatchTheFollowingChoiceAdapter
import com.joshtalks.joshskills.ui.assessment.extra.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.listener.EmptyListListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MatchTheFollowingChoiceView : FrameLayout,
    EmptyListListener {

    private var assessment: Assessment? = null
    private var viewType = AssessmentQuestionViewType.MY_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null

    private lateinit var recyclerTableA: RecyclerView
    private lateinit var recyclerTableB: RecyclerView
    private var listOfTableA: List<Choice>? = null
    private var listOfTableB: List<Choice>? = null
    private lateinit var seeAnswer: MaterialTextView
    private lateinit var resetAnswer: MaterialTextView
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
        View.inflate(context, R.layout.match_the_following_choice_view, this)
        recyclerTableA = findViewById(R.id.table_a_choices)
        recyclerTableB = findViewById(R.id.table_b_choices)
        seeAnswer = findViewById(R.id.see_answer)
        resetAnswer = findViewById(R.id.reset_answer)
        addObservers()
    }

    private fun addObservers() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(MatchTheFollowingSubmitEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    onSubmit()
                })

        seeAnswer.setOnClickListener {
            toogleViews()
        }
        resetAnswer.setOnClickListener {
            resetQuestion()
            publishUpdateButtonViewEvent(false)
        }
    }

    private fun resetQuestion() {
        assessmentQuestion?.choiceList?.forEach {
            it.userSelectedOrder = 100
            it.isSelectedByUser = false
        }

        recyclerTableA.adapter?.notifyDataSetChanged()
        recyclerTableB.adapter?.notifyDataSetChanged()
    }

    private fun toogleViews() {
        if (correctAnswerVisible) {
            viewType = AssessmentQuestionViewType.MY_ANSWER_VIEW
            seeAnswer.text = context.getString(R.string.see_answer)
        } else {
            viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
            seeAnswer.text = context.getString(R.string.see_your_answer)
        }
        correctAnswerVisible = correctAnswerVisible.not()
        (recyclerTableA.adapter as MatchTheFollowingChoiceAdapter).updateViewType(viewType)
        (recyclerTableB.adapter as MatchTheFollowingChoiceAdapter).updateViewType(viewType)
    }

    private fun onSubmit() {
        resetAnswer.visibility = View.GONE
        recyclerTableA.adapter?.notifyDataSetChanged()
        recyclerTableB.adapter?.notifyDataSetChanged()
        //disable all listeners
        //TODO("Not yet implemented")
    }

    fun bind(
        assessment: Assessment,
        viewType: AssessmentQuestionViewType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        this.assessment = assessment
        this.viewType = AssessmentQuestionViewType.MY_ANSWER_VIEW
        this.assessmentQuestion = assessmentQuestion
        setUpUI()
    }

    private fun initRecyclerView(
        recyclerTable: RecyclerView,
        sourceList: List<Choice>,
        targetList: List<Choice>
    ) {

        assessmentQuestion?.let {
            val topListAdapter =
                MatchTheFollowingChoiceAdapter(
                    assessment!!,
                    assessmentQuestion!!.question,
                    sourceList,
                    targetList,
                    this,
                    viewType
                )
            recyclerTable.adapter = topListAdapter
            recyclerTable.setOnDragListener(topListAdapter.getDragInstance())
        }
    }

    private fun setUpUI() {

        if ((assessment!!.status == AssessmentStatus.COMPLETED) || (assessmentQuestion!!.question.isAttempted && assessment!!.type == AssessmentType.QUIZ)) {
            seeAnswer.visibility = View.VISIBLE
            resetAnswer.visibility = View.GONE
            //disableAllClicks()
        }
        if (assessmentQuestion?.choiceList?.filter { it.isSelectedByUser == true }?.size == 0) {
            assessmentQuestion?.choiceList?.forEach {
                it.userSelectedOrder = 100
                it.isSelectedByUser = false
            }

        }

        updateList()

        val layoutManagerA = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        layoutManagerA.isSmoothScrollbarEnabled = true

        val layoutManagerB = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        layoutManagerB.isSmoothScrollbarEnabled = true

        recyclerTableB.itemAnimator = null
        recyclerTableB.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    context,
                    12f
                )
            )
        )
        recyclerTableB.layoutManager = layoutManagerA

        recyclerTableA.itemAnimator = null
        recyclerTableA.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    context,
                    12f
                )
            )
        )
        recyclerTableA.layoutManager = layoutManagerB

        initRecyclerView(recyclerTableA, listOfTableA!!, listOfTableB!!)
        initRecyclerView(recyclerTableB, listOfTableB!!, listOfTableA!!)

        if (assessmentQuestion?.question?.isAttempted!!) {
            recyclerTableA.visibility = View.GONE
        }
    }

    private fun updateList() {
        listOfTableA = assessmentQuestion?.choiceList?.filter { it.column == ChoiceColumn.LEFT }
            ?.sortedBy { it.sortOrder }

        listOfTableB = assessmentQuestion?.choiceList?.filter { it.column == ChoiceColumn.RIGHT }
            ?.sortedBy { it.sortOrder }
    }


    override fun setEmptyLeftList(visibility: Boolean) {
        publishUpdateButtonViewEvent(visibility)
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateList()
        Timber.tag("onAttachedToWindow").e("FillInTheBlankChoiceView")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
        saveList(listOfTableA)
        saveList(listOfTableB)
        Timber.tag("onDetachedFromWindow").e("FillInTheBlankChoiceView")
    }

    private fun saveList(list: List<Choice>?) {
        if (list.isNullOrEmpty().not()) {
            list!!.forEach { listItem ->
                assessmentQuestion?.choiceList?.filter { it.remoteId == listItem.remoteId }
                    ?.get(0)?.userSelectedOrder = listItem.userSelectedOrder
                assessmentQuestion?.choiceList?.filter { it.remoteId == listItem.remoteId }
                    ?.get(0)?.isSelectedByUser = listItem.isSelectedByUser
            }

        }
    }
}
