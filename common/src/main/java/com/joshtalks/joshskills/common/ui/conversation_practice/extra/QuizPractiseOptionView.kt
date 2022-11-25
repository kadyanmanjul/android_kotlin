package com.joshtalks.joshskills.common.ui.conversation_practice.extra

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.server.conversation_practice.AnswersModel
import com.joshtalks.joshskills.common.ui.conversation_practice.adapter.OnChoiceClickListener
import com.mindorks.placeholderview.annotations.Animate
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Animate(Animate.CARD_BOTTOM_IN_ASC, duration = 1000)
@Layout(R.layout.quiz_practise_option_view)
class QuizPractiseOptionView(
    var postion: Int,
    var answerModel: AnswersModel,
    var listener: OnChoiceClickListener
) {

    val context: Context = AppObjectController.joshApplication

    @com.mindorks.placeholderview.annotations.View(R.id.choice_container)
    lateinit var container: MaterialCardView

    @com.mindorks.placeholderview.annotations.View(R.id.choice_textview)
    lateinit var choiceTextView: AppCompatTextView


    @Resolve
    fun onResolved() {
        choiceTextView.text = answerModel.text

        if (answerModel.isSelectedByUser) {
            // For Choice Selected
            if (answerModel.isEvaluate) {
                if (answerModel.isCorrect) {
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
            if (answerModel.isEvaluate) {
                // For Choice Not Selected
                if (answerModel.isCorrect) {
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
        if (answerModel.isSelectedByUser) {
            container.setRippleColorResource(R.color.transparent)
        } else {
            container.setRippleColorResource(R.color.dark_grey)
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
        if (answerModel.isEvaluate) {
            return
        }
        listener.onChoiceClick(postion, answerModel)
    }


    private fun setUnselectedChoiceView() {
        setTextColor(R.color.dark_grey)
        setBackgroundColor(R.color.pure_grey)
        setBorderColor(R.color.pure_grey)
        setDrawableStart(R.drawable.ic_radio_button_unchecked, R.color.dark_grey)
    }

    private fun setSelectedChoiceView() {
        setTextColor(R.color.primary_500)
        setBackgroundColor(R.color.primary_400)
        setBorderColor(R.color.primary_500)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.primary_500)
    }

    private fun setWrongChoiceView() {
        setTextColor(R.color.critical)
        setBackgroundColor(R.color.surface_critical)
        setBorderColor(R.color.critical)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.critical)

    }

    private fun setCorrectlySelectedChoiceView() {
        setTextColor(R.color.success)
        setBackgroundColor(R.color.surface_success)
        setBorderColor(R.color.success)
        setDrawableStart(R.drawable.ic_radio_button_checked, R.color.success)
    }

    private fun setCorrectButNotSelectedChoiceView() {
        setTextColor(R.color.pure_white)
        setBackgroundColor(R.color.success)
        setBorderColor(R.color.success)
        setDrawableStart(R.drawable.ic_tick_smallest, R.color.pure_white)
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