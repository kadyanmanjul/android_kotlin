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
        setPadding(
            mPaddingLeft,
            mPaddingTop,
            mPaddingRight,
            mPaddingBottom
        )
        invalidate()
    }

    fun changeViewGroup(optionsLayout: CustomLayout, answerLayout: FlowLayout) {
        if (parent is CustomLayout) {
//            val fromLocation = IntArray(2)
//            this.getLocationOnScreen(fromLocation)
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER
            layoutParams.setMargins(
                mPadding14F,
                mPadding14F,
                mPadding14F,
                mPadding14F
            )
            setLayoutParams(layoutParams)
            optionsLayout.removeViewCustomLayout(this, choice)
            answerLayout.addView(this)

//            val toLocation = IntArray(2)
//            this.getLocationOnScreen(toLocation)

            choice.apply {
                this.userSelectedOrder = answerLayout.childCount
            }
//            RxBus2.publish(AnimateAtsOtionViewEvent(fromLocation, toLocation, this.choice.text))
        } else {

//            val fromLocation = IntArray(2)
//            this.getLocationOnScreen(fromLocation)
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER
            layoutParams.setMargins(
                mPadding14F,
                mPadding14F,
                mPadding14F,
                mPadding14F
            )
            setLayoutParams(layoutParams)
            answerLayout.removeView(this)
            optionsLayout.addViewAt(this, choice.sortOrder - 1)
            choice.apply {
                this.userSelectedOrder = 100
            }
        }
    }

    companion object {
        private const val TAG = "CustomWord"
        var mPaddingTop = Utils.convertDpToPixel(35f).roundToInt()
        var mPaddingBottom = Utils.convertDpToPixel(35f).roundToInt()
        var mPaddingLeft = Utils.convertDpToPixel(30f).roundToInt()
        var mPaddingRight = Utils.convertDpToPixel(30f).roundToInt()
        private var mPadding14F = Utils.convertDpToPixel(14f).roundToInt()
        private var mPadding22F = Utils.convertDpToPixel(22f).roundToInt()

    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(
            mPadding14F,
            mPadding14F,
            mPadding14F,
            mPadding14F
        )
        setLayoutParams(layoutParams)
        gravity = Gravity.CENTER
        minWidth = 125
        textAlignment = TEXT_ALIGNMENT_CENTER
        textSize = 16f
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rounded_rectangle_with_grey_border_pressed
                    )
                    v.setPadding(
                        mPaddingLeft,
                        mPaddingTop + Utils.convertDpToPixel(3f).roundToInt(),
                        mPaddingRight,
                        mPaddingBottom - Utils.convertDpToPixel(3f).roundToInt(),
                    )
                    v.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rounded_rectangle_with_grey_border
                    )
                    v.setPadding(
                        mPaddingLeft,
                        mPaddingTop - Utils.convertDpToPixel(3f).roundToInt(),
                        mPaddingRight,
                        mPaddingBottom + Utils.convertDpToPixel(3f).roundToInt(),
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
