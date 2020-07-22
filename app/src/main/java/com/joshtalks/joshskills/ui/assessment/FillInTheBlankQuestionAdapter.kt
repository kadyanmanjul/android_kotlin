package com.joshtalks.joshskills.ui.assessment

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
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.ui.assessment.viewholder.OnChoiceClickListener

class FillInTheBlankQuestionAdapter(
    private var assessmentType: AssessmentType,
    private var assessmentStatus: AssessmentStatus,
    private var isQuestionAttempted: Boolean,
    private val choiceResponse: ArrayList<Choice>,
    private var onClickListener: OnChoiceClickListener

) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_EMPTY = 1
    private val TYPE_FILLED = 2

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        if (viewType == TYPE_FILLED) {
            return ChipAnswerViewHolder(
                parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            )
        } else {
            return EmptyViewHolder(
                parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            )
        }
    }

    fun setIsAttempted() {
        isQuestionAttempted = true
    }

    override fun getItemCount() = choiceResponse.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_FILLED) {
            (holder as ChipAnswerViewHolder).bindPhoto(
                assessmentType,
                assessmentStatus,
                isQuestionAttempted,
                choiceResponse.get(position),
                onClickListener
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

        fun bindPhoto(
            assessmentType: AssessmentType,
            assessmentStatus: AssessmentStatus,
            isQuestionAttempted: Boolean,
            choice: Choice,
            onClickListener: OnChoiceClickListener
        ) {
            this.assessmentType = assessmentType
            this.assessmentStatus = assessmentStatus
            this.isQuestionAttempted = isQuestionAttempted
            this.choice = choice
            this.onClickListener = onClickListener
            question = view.findViewById(R.id.item_description)
            question.text = choice.text
            divider = view.findViewById(R.id.underline)
            divider.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.button_primary_color
                )
            )
            setColor()
            view.setOnClickListener(this)
        }

        private lateinit var choice: Choice
        private lateinit var assessmentType: AssessmentType
        private lateinit var assessmentStatus: AssessmentStatus
        private var isQuestionAttempted: Boolean = false
        private lateinit var question: TextView
        private lateinit var divider: View

        private var onClickListener: OnChoiceClickListener? = null

        override fun onClick(view: View) {
            if (!isQuestionAttempted)
                onClickListener?.onChoiceClick(choice)
            setColor()
        }

        private fun setColor() {

            if (((assessmentType == AssessmentType.QUIZ && isQuestionAttempted) || (assessmentType == AssessmentType.TEST && assessmentStatus == AssessmentStatus.COMPLETED))) {
                if (isQuestionAttempted) {
                    // For Question Submitted

                    if (choice.isSelectedByUser) {
                        // For Choice Selected

                        if (choice.userSelectedOrder == choice.correctAnswerOrder) {
                            // For Choice isCorrectAnswer
                            setCorrectlySelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setWrongChoiceView()
                        }

                    }
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
            question.setTextColor(
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
                    R.color.button_primary_color
                )
            )
        }

        private var choice: Choice? = null
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
    }

}
