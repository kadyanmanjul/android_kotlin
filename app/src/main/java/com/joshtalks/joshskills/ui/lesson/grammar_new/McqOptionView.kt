package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
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
            setTextColor(ContextCompat.getColor(context, R.color.grammar_black_text_color))
            background =
                ContextCompat.getDrawable(
                    getContext(),
                    R.drawable.rounded_rectangle_with_grey_border_mcq_grammar
                )
        } else {
            choice.isSelectedByUser = true
            setTextColor(ContextCompat.getColor(context, R.color.grammar_button_color_blue))
            background =
                ContextCompat.getDrawable(
                    getContext(),
                    R.drawable.rounded_rectangle_with_blue_border
                )
        }
    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(20, 20, 20, 0)
        setLayoutParams(layoutParams)
        gravity = Gravity.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER
        buttonDrawable = null
        textSize = 18f
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentState == McqOptionState.UNSELECTED) {
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grammar_black_text_color
                            )
                        )
                        v.background = ContextCompat.getDrawable(
                            context,
                            R.drawable.rounded_rectangle_with_grey_border_pressed_mcq_grammar
                        )
                    } else {
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grammar_button_color_blue
                            )
                        )
                        v.background =
                            ContextCompat.getDrawable(
                                getContext(),
                                R.drawable.rounded_rectangle_with_blue_border_pressed
                            )
                    }
                    val currentPaddingTop = v.paddingTop
                    val currentPaddingBottom = v.paddingBottom
                    v.setPadding(
                        v.paddingLeft,
                        currentPaddingTop + Utils.dpToPx(1),
                        v.paddingRight,
                        currentPaddingBottom - Utils.dpToPx(1),
                    )
                    v.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (currentState == McqOptionState.UNSELECTED) {
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grammar_black_text_color
                            )
                        )
                        v.background = ContextCompat.getDrawable(
                            context,
                            R.drawable.rounded_rectangle_with_grey_border_mcq_grammar
                        )
                    } else {
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grammar_button_color_blue
                            )
                        )
                        v.background =
                            ContextCompat.getDrawable(
                                getContext(),
                                R.drawable.rounded_rectangle_with_blue_border
                            )
                    }
                    val currentPaddingTop = v.paddingTop
                    val currentPaddingBottom = v.paddingBottom
                    v.setPadding(
                        v.paddingLeft,
                        currentPaddingTop - Utils.dpToPx(1),
                        v.paddingRight,
                        currentPaddingBottom + Utils.dpToPx(1),
                    )
                    v.invalidate()
                }
            }
            false
        }
    }
}

enum class McqOptionState(state: String) {
    SELECTED("SELECTED"),
    UNSELECTED("UNSELECTED")
}
