package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AnimateAtsOtionViewEvent
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
            val fromLocation = IntArray(2)
            this.getLocationOnScreen(fromLocation)
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
            //this.invalidate()

            choice.apply {
                this.userSelectedOrder = answerLayout.childCount
                this.isSelectedByUser = true
            }
            this.visibility = View.INVISIBLE
            RxBus2.publish(AnimateAtsOtionViewEvent(fromLocation, this.height, this.width, this))
        } else {

            val fromLocation = IntArray(2)
            this.getLocationOnScreen(fromLocation)
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
                this.isSelectedByUser = false
            }
            //RxBus2.publish(AnimateAtsOtionViewEvent(fromLocation, this.height, this.width, this))
            /*RxBus2.publish(
                AnimateAtsOtionViewEvent(
                    fromLocation,
                    this.height,
                    this.width,
                    this,
                    optionsLayout
                )
            )*/

        }
    }

    fun updateChoice(choice: Choice) {
        this.choice = choice
    }

    companion object {
        private const val TAG = "CustomWord"
        var mPaddingTop = Utils.sdpToPx(R.dimen._18sdp).toInt()
        var mPaddingBottom = Utils.sdpToPx(R.dimen._18sdp).toInt()
        var mPaddingLeft = Utils.sdpToPx(R.dimen._16sdp).toInt()
        var mPaddingRight = Utils.sdpToPx(R.dimen._16sdp).toInt()
        private var mPadding14F = Utils.sdpToPx(R.dimen._7sdp).toInt()
        private var mPadding22F = Utils.sdpToPx(R.dimen._11sdp).toInt()

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
        minWidth = Utils.sdpToPx(R.dimen._40sdp).toInt()
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
                        mPaddingTop + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                        mPaddingRight,
                        mPaddingBottom - Utils.sdpToPx(R.dimen._1sdp).toInt(),
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
                        mPaddingTop - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                        mPaddingRight,
                        mPaddingBottom + Utils.sdpToPx(R.dimen._1sdp).toInt(),
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
