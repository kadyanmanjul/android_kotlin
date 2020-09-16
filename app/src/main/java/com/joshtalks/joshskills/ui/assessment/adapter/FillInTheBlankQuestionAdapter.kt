package com.joshtalks.joshskills.ui.assessment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.joshtalks.joshskills.ui.assessment.listener.OnChoiceClickListener

class FillInTheBlankQuestionAdapter(
    private var assessment: Assessment,
    private var question: AssessmentQuestion,
    private val choiceResponse: ArrayList<Choice>,
    private var onChoiceClickListener: OnChoiceClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_EMPTY = 1
    private val TYPE_FILLED = 2

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        if (viewType == TYPE_FILLED) {
            return ChipAnswerViewHolder(
                parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            ).apply {
                setIsRecyclable(false)
            }
        } else {
            return EmptyViewHolder(
                parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            ).apply {
                setIsRecyclable(false)
            }
        }
    }

    override fun getItemCount() = choiceResponse.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_FILLED) {
            (holder as ChipAnswerViewHolder).bindView(
                assessment,
                question,
                choiceResponse.get(position),
                onChoiceClickListener
            )
        } else {
            (holder as EmptyViewHolder).bindPhoto(choiceResponse.get(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (choiceResponse.get(position).isSelectedByUser) {
            return TYPE_FILLED
        } else {
            return TYPE_EMPTY
        }
    }

    class ChipAnswerViewHolder(private var view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var choice: Choice
        private lateinit var assessment: Assessment
        private lateinit var question: AssessmentQuestion
        private lateinit var questionText: TextView
        private lateinit var divider: View
        private var onClickListener: OnChoiceClickListener? = null

        fun bindView(
            assessment: Assessment,
            question: AssessmentQuestion,
            choice: Choice,
            onClickListener: OnChoiceClickListener
        ) {
            this.assessment = assessment
            this.question = question
            this.choice = choice
            this.onClickListener = onClickListener
            questionText = view.findViewById(R.id.item_description)
            questionText.text = choice.text
            divider = view.findViewById(R.id.underline)
            divider.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.button_color
                )
            )
            setColor()
            view.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (question.isAttempted.not() && (assessment.status == AssessmentStatus.STARTED || assessment.status == AssessmentStatus.NOT_STARTED)) {
                onClickListener?.onChoiceClick(choice)
            } else setColor()
        }

        private fun setColor() {

            if ((assessment.type == AssessmentType.QUIZ && question.isAttempted)) {
                if (question.isAttempted) {
                    // For Question Submitted

                    if (choice.userSelectedOrder == choice.correctAnswerOrder) {
                        // For Choice isCorrectAnswer
                        setCorrectlySelectedChoiceView()
                    } else {
                        // For Choice isNotCorrectAnswer
                        setWrongChoiceView()
                    }

                }
            } else if (assessment.type == AssessmentType.TEST && assessment.status == AssessmentStatus.COMPLETED) {

                if (choice.userSelectedOrder == choice.correctAnswerOrder) {
                    // For Choice isCorrectAnswer
                    setCorrectlySelectedChoiceView()
                } else {
                    // For Choice isNotCorrectAnswer
                    setWrongChoiceView()
                }

            }

        }

        private fun setWrongChoiceView() {
            setTextColor(R.color.error_color)
            setBackgroundColor(R.color.error_color)
        }

        private fun setCorrectlySelectedChoiceView() {
            setTextColor(R.color.green_right_answer)
            setBackgroundColor(R.color.green_right_answer)
        }

        private fun setTextColor(colorId: Int) =
            questionText.setTextColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    colorId
                )
            )

        private fun setBackgroundColor(colorId: Int) = divider.setBackgroundColor(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                colorId
            )
        )
    }

    class EmptyViewHolder(private var view: View) : RecyclerView.ViewHolder(view) {

        fun bindPhoto(choice: Choice) {
            this.choice = choice
            val question: TextView = view.findViewById(R.id.item_description)
            val divider: View = view.findViewById(R.id.underline)
            question.text = EMPTY
            divider.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.button_color
                )
            )
        }

        private var choice: Choice? = null
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
    }

}
