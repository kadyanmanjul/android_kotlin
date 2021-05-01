package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout

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
        var mPaddingTop = Utils.dpToPx(18)
        var mPaddingBottom = Utils.dpToPx(16)
        var mPaddingLeft = Utils.dpToPx(16)
        var mPaddingRight = Utils.dpToPx(15)
        private var mPadding14F = Utils.dpToPx(7)
        private var mPadding22F = Utils.dpToPx(11)

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
        minWidth = Utils.dpToPx(40)
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
                        mPaddingTop + Utils.dpToPx(3),
                        mPaddingRight,
                        mPaddingBottom - Utils.dpToPx(3),
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
                        mPaddingTop - Utils.dpToPx(3),
                        mPaddingRight,
                        mPaddingBottom + Utils.dpToPx(3),
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
