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
import com.joshtalks.joshskills.ui.assessment.viewholder.OnChoiceClickListener

class FillInTheBlankQuestionAdapter(
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
        val view: View
        if (viewType == TYPE_FILLED) {
            view = parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            return ChipAnswerViewHolder(
                view
            )
        } else {
            view = parent.inflate(R.layout.fill_in_the_blank_recyclerview_item_row, false)
            return EmptyViewHolder(
                view
            )
        }
    }

    override fun getItemCount() = choiceResponse.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_FILLED) {
            (holder as ChipAnswerViewHolder).bindPhoto(
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

        fun bindPhoto(choice: Choice, onClickListener: OnChoiceClickListener) {
            this.choice = choice
            this.onClickListener = onClickListener
            val question: TextView = view.findViewById(R.id.item_description)
            question.text = choice.text
            val divider: View = view.findViewById(R.id.underline)
            divider.setBackgroundColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.button_primary_color
                )
            )
            view.setOnClickListener(this)
        }

        private var choice: Choice? = null
        private var onClickListener: OnChoiceClickListener? = null

        override fun onClick(view: View) {
            onClickListener?.onChoiceClick(choice!!)
        }
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
