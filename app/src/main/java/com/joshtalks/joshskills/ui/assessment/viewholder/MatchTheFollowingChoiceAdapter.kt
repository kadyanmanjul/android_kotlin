package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.ClipData
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.DragListener
import com.joshtalks.joshskills.ui.assessment.view.EmptyListListener


class MatchTheFollowingChoiceAdapter(
    private var assessment: Assessment,
    private var question: AssessmentQuestion,
    private var sourceList: List<Choice>,
    private var tragetList: List<Choice>,
    private val listener: EmptyListListener,
    private var viewType: AssessmentQuestionViewType
) : RecyclerView.Adapter<MatchTheFollowingChoiceAdapter.MatchTheFollowingViewHolder>() {

    private val TYPE_EMPTY = 1
    private val TYPE_FILLED = 2
    private lateinit var holder: MatchTheFollowingViewHolder

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MatchTheFollowingViewHolder {
        return MatchTheFollowingViewHolder(
            parent.inflate(R.layout.match_the_following_recyclerview_item_row, false), viewType
        ).apply {
            setIsRecyclable(false)
        }
    }

    override fun getItemCount() = sourceList.size

    override fun onBindViewHolder(holder: MatchTheFollowingViewHolder, position: Int) {
        this.holder = holder
        holder.bindView(
            assessment,
            question,
            sourceList.get(position),
            tragetList,
            listener,
            position,
            viewType
        )
    }

    override fun getItemViewType(position: Int): Int {
        if (sourceList[position].isSelectedByUser) {
            return TYPE_FILLED
        } else {
            return TYPE_EMPTY
        }
    }

    fun getList(): List<Choice> {
        return sourceList
    }

    fun updateList(list: List<Choice>) {
        sourceList = list
    }

    fun updateViewType(viewType: AssessmentQuestionViewType) {
        this.viewType = viewType
        notifyDataSetChanged()
    }


    class MatchTheFollowingViewHolder(private var view: View, viewType: Int) :
        RecyclerView.ViewHolder(view), View.OnTouchListener {

        private lateinit var choice: Choice
        private lateinit var targetList: List<Choice>
        private lateinit var assessment: Assessment
        lateinit var viewType: AssessmentQuestionViewType
        private lateinit var question: AssessmentQuestion
        private lateinit var questionText: TextView
        private lateinit var frameLayout: FrameLayout
        private var listner: EmptyListListener? = null

        fun bindView(
            assessment: Assessment,
            question: AssessmentQuestion,
            choice: Choice,
            targetList: List<Choice>,
            listner: EmptyListListener,
            position: Int,
            viewType: AssessmentQuestionViewType
        ) {
            this.assessment = assessment
            this.question = question
            this.choice = choice
            this.targetList = targetList
            this.listner = listner
            this.viewType = viewType
            questionText = view.findViewById(R.id.item_description)
            frameLayout = view.findViewById(R.id.root_view)
            frameLayout.tag = position
            setColorAndText()
            frameLayout.setOnTouchListener(
                if (question.isAttempted.not()) {
                    this
                } else null
            )
            frameLayout.setOnDragListener(
                if (question.isAttempted.not()) {
                    DragListener(
                        listner
                    )
                } else null
            )

        }

        private fun setColorAndText() {
            if ((assessment.status == AssessmentStatus.COMPLETED) || (question.isAttempted && assessment.type == AssessmentType.QUIZ)) {
                if (viewType == AssessmentQuestionViewType.CORRECT_ANSWER_VIEW) {
                    if (choice.column == ChoiceColumn.RIGHT) {
                        questionText.text =
                            choice.text?.plus("\n-\n").plus(targetList.sortedBy { it.sortOrder }
                                .get(choice.correctAnswerOrder.minus(1)).text)
                        setCorrectOrWrongColor(true)
                    } else {
                        questionText.text = EMPTY
                        setAttemptedColor()
                    }
                } else {
                    if (question.isAttempted && choice.isSelectedByUser) {
                        if (choice.column == ChoiceColumn.RIGHT) {
                            questionText.text =
                                choice.text?.plus("\n-\n")
                                    .plus(targetList.sortedBy { it.sortOrder }
                                        .get(choice.userSelectedOrder).text)
                            setCorrectOrWrongColor(
                                choice.userSelectedOrder == choice.correctAnswerOrder.minus(
                                    1
                                )
                            )
                        } else {
                            questionText.text = EMPTY
                            setAttemptedColor()
                        }
                    } else {
                        questionText.text = choice.text
                        setAttemptedColor()
                    }
                }
            } else {
                if (question.isAttempted) {
                    if (choice.column == ChoiceColumn.RIGHT) {
                        if (choice.isSelectedByUser) {
                            questionText.text =
                                choice.text?.plus("\n-\n").plus(targetList.sortedBy { it.sortOrder }
                                    .get(choice.userSelectedOrder).text)
                        } else {
                            questionText.text = choice.text
                        }
                    }
                } else {
                    if (choice.column == ChoiceColumn.RIGHT) {
                        if (choice.isSelectedByUser) {
                            questionText.text =
                                choice.text?.plus("\n-\n").plus(targetList.sortedBy { it.sortOrder }
                                    .get(choice.userSelectedOrder).text)
                        } else {
                            questionText.text = choice.text
                        }
                    } else {
                        if (choice.isSelectedByUser) {
                            questionText.text = EMPTY
                            setAttemptedColor()
                        } else {
                            questionText.text = choice.text
                            setUnAttemptedColor()
                        }
                    }

                }
            }
        }

        private fun setCorrectOrWrongColor(correctAttempted: Boolean) {

            if (correctAttempted) {
                frameLayout.background = ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.rect_with_green_bound
                )
                questionText.setTextColor(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.green_right_answer
                    )
                )
            } else {
                frameLayout.background = ContextCompat.getDrawable(
                    AppObjectController.joshApplication,
                    R.drawable.rect_with_dotted_red_bound
                )
                questionText.setTextColor(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.error_color
                    )
                )

            }
        }

        private fun setAttemptedColor() {
            frameLayout.background = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rectangle_without_dotted_border
            )
        }

        private fun setUnAttemptedColor() {

            val drawable = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rectangle_without_dotted_border
            )
            if (drawable != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    drawable.setTint(
                        AppObjectController.joshApplication.resources.getColor(
                            R.color.lighter_grey,
                            null
                        )
                    )
                }
            }

            frameLayout.background = drawable
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    val data =
                        ClipData.newPlainText("", "")
                    val shadowBuilder =
                        View.DragShadowBuilder(v)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        v!!.startDragAndDrop(data, shadowBuilder, v, 0)
                    } else {
                        v!!.startDrag(data, shadowBuilder, v, 0)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        v!!.cancelDragAndDrop()
                    }
                }
            }
            return false
        }
    }

    fun getDragInstance(): DragListener? {
        return if (listener != null) {
            DragListener(listener)
        } else {
            Log.e("ListAdapter", "Listener wasn't initialized!")
            null
        }
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
    }

}
