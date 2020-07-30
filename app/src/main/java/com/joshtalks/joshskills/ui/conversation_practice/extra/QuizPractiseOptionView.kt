package com.joshtalks.joshskills.ui.conversation_practice.extra

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.conversation_practice.AnswersModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.OnChoiceClickListener
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.quiz_practise_option_view)
class QuizPractiseOptionView(var answersModel: AnswersModel, var listener: OnChoiceClickListener) {

    val context: Context = AppObjectController.joshApplication

    @com.mindorks.placeholderview.annotations.View(R.id.choice_container)
    lateinit var container: MaterialCardView

    @com.mindorks.placeholderview.annotations.View(R.id.choice_textview)
    lateinit var choiceTextView: AppCompatTextView


    @Resolve
    fun onResolved() {
        choiceTextView.text = answersModel.text

        if (answersModel.isSelectedByUser) {
            container.setRippleColorResource(R.color.transparent)
        } else {
            container.setRippleColorResource(R.color.dark_grey)
        }
        if (answersModel.isSelectedByUser) {
            // For Choice Selected
            if (answersModel.isEvaluate) {
                if (answersModel.isCorrect) {
                    // For Choice isCorrectAnswer
                    setCorrectlySelectedChoiceView()
                } else {
                    // For Choice isNotCorrectAnswer
                    setWrongChoiceView()
                }
            } else {
                setSelectedChoiceView()
            }

        } else {
            if (answersModel.isEvaluate) {
                // For Choice Not Selected
                if (answersModel.isCorrect) {
                    // For Choice isCorrectAnswer
                    setCorrectButNotSelectedChoiceView()
                } else {
                    // For Choice isNotCorrectAnswer
                    setUnselectedChoiceView()
                }
            } else {
                setUnselectedChoiceView()
            }
        }


    }

    private fun setBackgroundColor(colorId: Int) = container.setCardBackgroundColor(
        ContextCompat.getColor(
            context,
            colorId
        )
    )

    private fun setBorderColor(colorId: Int) {
        container.strokeColor = ContextCompat.getColor(
            context,
            colorId
        )
    }

    @Click(R.id.choice_container)
    fun onClick() {
        listener.onChoiceClick(answersModel)
    }


    private fun setUnselectedChoiceView() {
        setTextColor(R.color.light_grey)
        setBackgroundColor(R.color.artboard_color)
        setBorderColor(R.color.artboard_color)
        setDrawableStart(R.drawable.ic_radio_button_unchecked, R.color.light_grey)
    }

    private fun setSelectedChoiceView() {
        setTextColor(R.color.button_color)
        setBackgroundColor(R.color.light_blue)
        setBorderColor(R.color.button_color)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.button_color)
    }

    private fun setWrongChoiceView() {
        setTextColor(R.color.error_color)
        setBackgroundColor(R.color.light_red)
        setBorderColor(R.color.error_color)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.error_color)

    }

    private fun setCorrectlySelectedChoiceView() {
        setTextColor(R.color.green_right_answer)
        setBackgroundColor(R.color.lighter_green)
        setBorderColor(R.color.green_right_answer)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.green_right_answer)

    }

    private fun setCorrectButNotSelectedChoiceView() {
        setTextColor(R.color.white)
        setBackgroundColor(R.color.green_right_answer)
        setBorderColor(R.color.green_right_answer)
        setDrawableStart(R.drawable.ic_tick_small, R.color.white)
    }

    private fun setTextColor(colorId: Int) =
        choiceTextView.setTextColor(ContextCompat.getColor(context, colorId))

    private fun setDrawableStart(drawableId: Int?, tintColorId: Int) {
        val drawable = drawableId?.let {
            ContextCompat.getDrawable(
                context,
                it
            )
        }
        drawable?.setTint(
            ContextCompat.getColor(
                context,
                tintColorId
            )
        )
        choiceTextView.setCompoundDrawablesWithIntrinsicBounds(
            drawable, null, null, null
        )
    }

}