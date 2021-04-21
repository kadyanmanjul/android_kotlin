package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice

class McqOptionView(context: Context) : AppCompatRadioButton(context) {

    lateinit var choice: Choice
    private var currentState: McqOptionState = McqOptionState.UNSELECTED

    constructor(context: Context, choice: Choice) : this(context) {
        this.choice = choice
        setup(choice)
    }

    fun setup(choice: Choice) {
        text = choice.text
        if (choice.isSelectedByUser) {
            setState(McqOptionState.SELECTED)
        } else {
            setState(McqOptionState.UNSELECTED)
        }
    }

    fun changeState() {
        if (currentState == McqOptionState.UNSELECTED) {
            setState(McqOptionState.SELECTED)
        } else {
            setState(McqOptionState.UNSELECTED)
        }
    }

    fun setState(state: McqOptionState) {
        currentState = state

        if (state == McqOptionState.UNSELECTED) {
            choice.isSelectedByUser = false
            setTextColor(resources.getColor(R.color.black))
            background =
                ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_with_border)
        } else {
            choice.isSelectedByUser = true
            setTextColor(resources.getColor(R.color.button_color))
            background =
                ContextCompat.getDrawable(getContext(), R.drawable.primary_dark_rounded_bg)
        }
    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(20, 20, 20, 0)
        setLayoutParams(layoutParams)
        textAlignment = TEXT_ALIGNMENT_CENTER
        buttonDrawable = null
        textSize = 20f
    }

}

enum class McqOptionState(state: String) {
    SELECTED("SELECTED"),
    UNSELECTED("UNSELECTED")
}
