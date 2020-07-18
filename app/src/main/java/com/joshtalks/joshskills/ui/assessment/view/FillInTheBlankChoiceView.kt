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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.FillInTheBlankQuestionAdapter
import com.joshtalks.joshskills.ui.assessment.viewholder.OnChoiceClickListener
import org.jetbrains.anko.collections.forEachWithIndex

class FillInTheBlankChoiceView : FrameLayout, OnChoiceClickListener {

    private var assessmentType: AssessmentType? = null
    private var assessmentStatus: AssessmentStatus? = null
    private var viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalAnswered: TextView
    private lateinit var chipChoice: ChipGroup
    private var chipChoiceList = mutableListOf<Choice>()
    private var filled = 0
    private var totalOptions = 0


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
        chipChoice = findViewById(R.id.chip_choice)
    }

    fun bind(
        assessmentType: AssessmentType,
        assessmentStatus: AssessmentStatus,
        viewType: AssessmentQuestionViewType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        this.assessmentType = assessmentType
        this.assessmentStatus = assessmentStatus
        this.viewType = viewType
        this.assessmentQuestion = assessmentQuestion
        setUpUI()
    }

    private fun setUpUI() {
        renderView()
        setupPlaceHolderView()
    }


    private fun renderView() {
        chipChoice.removeAllViews()
        val choice: List<Choice>? = assessmentQuestion?.choiceList
        choice?.sortedBy { it.sortOrder }?.forEach {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.choice_fib_item, null, false) as Chip
            chip.text = it.text
            chip.tag = it.remoteId
            chip.id = it.remoteId
            chip.setOnClickListener(chipClickListener)
            chipChoice.addView(chip)
        }
        chipChoice.id.run {
            chipChoice.check(this)
        }
        totalOptions = assessmentQuestion?.choiceList?.size ?: 0
    }

    private val chipClickListener = OnClickListener { view ->

        val anim = AlphaAnimation(1f, 0f)
        anim.duration = 250
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
                chipChoiceList.forEachWithIndex { index, choice ->
                    if (view.id == choice.remoteId) {
                        if (!choice.isSelectedByUser)
                            filled = filled + 1
                        choice.isSelectedByUser = true
                        choice.userSelectedOrder = index
                    }

                }
                sortList()
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })

        view.startAnimation(anim)
    }

    private fun setupPlaceHolderView() {
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.CENTER
        layoutManager.flexWrap = FlexWrap.WRAP

        recyclerView.layoutManager = layoutManager
        assessmentQuestion?.let { addChoicesListItems(it) }

        recyclerView.adapter = FillInTheBlankQuestionAdapter(chipChoiceList as ArrayList<Choice>, this)

    }

    private fun addChoicesListItems(assessmentQuestion: AssessmentQuestionWithRelations) {
        assessmentQuestion.choiceList.sortedBy { it.sortOrder }.forEach { choice ->
            choice.userSelectedOrder = 100
            chipChoiceList.add(choice)
        }
    }

    private fun sortList() {
        totalAnswered.text = filled.toString().plus("/").plus(totalOptions)
        chipChoiceList.sortBy { it.userSelectedOrder }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onChoiceClick(choice: Choice) {
        chipChoice.forEach { view ->
            chipChoiceList.forEachWithIndex { index, choiceItem ->
                if (choice.remoteId == view.id && view.id == choiceItem.remoteId) {
                    if (choiceItem.isSelectedByUser)
                        filled = filled - 1
                    choiceItem.isSelectedByUser = false
                    choiceItem.userSelectedOrder = 100
                    view.visibility = View.VISIBLE
                    sortList()
                }
            }
        }
    }
}
