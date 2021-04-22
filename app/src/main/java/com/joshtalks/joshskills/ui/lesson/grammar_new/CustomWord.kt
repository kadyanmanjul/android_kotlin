package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.utils.Utils
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout
import kotlin.math.roundToInt

class CustomWord : AppCompatTextView {

    lateinit var choice: Choice

    constructor(context: Context) : super(context)

    constructor(context: Context, choice: Choice) : this(context) {
        this.choice = choice
        text = choice.text
    }

    fun changeViewGroup(optionsLayout: CustomLayout, answerLayout: FlowLayout) {
        if (parent is CustomLayout) {
            optionsLayout.removeViewCustomLayout(this, choice)
            answerLayout.addView(this)
            choice.apply {
                this.userSelectedOrder = answerLayout.childCount
            }
        } else {
            answerLayout.removeView(this)
            optionsLayout.addViewAt(this, choice.sortOrder - 1)
            choice.apply {
                this.userSelectedOrder = 100
            }
        }
    }

    companion object {
        private const val TAG = "CustomWord"
    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(
            Utils.convertDpToPixel(10f).roundToInt(),
            Utils.convertDpToPixel(10f).roundToInt(),
            Utils.convertDpToPixel(10f).roundToInt(),
            Utils.convertDpToPixel(10f).roundToInt()
        )
        setLayoutParams(layoutParams)
        gravity = Gravity.CENTER
        minWidth = 120
        textAlignment = TEXT_ALIGNMENT_CENTER
        textSize = 20f
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rounded_rectangle_with_grey_border_pressed
                    )
                    val currentPaddingTop = v.paddingTop
                    val currentPaddingBottom = v.paddingBottom
                    v.setPadding(
                        v.paddingLeft,
                        currentPaddingTop + Utils.convertDpToPixel(3f).roundToInt(),
                        v.paddingRight,
                        currentPaddingBottom - Utils.convertDpToPixel(3f).roundToInt(),
                    )
                    v.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rounded_rectangle_with_grey_border
                    )
                    val currentPaddingTop = v.paddingTop
                    val currentPaddingBottom = v.paddingBottom
                    v.setPadding(
                        v.paddingLeft,
                        currentPaddingTop - Utils.convertDpToPixel(3f).roundToInt(),
                        v.paddingRight,
                        currentPaddingBottom + Utils.convertDpToPixel(3f).roundToInt(),
                    )
                    v.invalidate()
                }
            }
            false
        }
        background =
            ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_with_grey_border)
        setTextColor(ContextCompat.getColor(context, R.color.grammar_black_text_color))
    }
}
