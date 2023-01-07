package com.joshtalks.joshskills.common.repository.local.model

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout

class AtsOptionView : AppCompatTextView {

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

    fun changeViewGroup(
        optionsLayout: FlowLayout,
        answerLayout: FlowLayout,
        selectedOption: Boolean = true
    ) {
        if (selectedOption) {
            /**
             * Answer Selected
             */
            val fromLocation = IntArray(2)
            this.getLocationInWindow(fromLocation)
            updateView(isSelected = true)
            optionsLayout.removeView(this, choice)
            answerLayout.addView(this)
            //this.invalidate()
            post {
                choice.apply {
                    this.userSelectedOrder = answerLayout.childCount
                    this.isSelectedByUser = true
                }
            }
            this.visibility = View.INVISIBLE
            RxBus2.publish(AnimateAtsOptionViewEvent(fromLocation, this.height, this.width, this))
        } else {
            /**
             * Answer Unselected
             */
            val fromLocation = IntArray(2)
            this.getLocationInWindow(fromLocation)
            updateView(isSelected = false)
            answerLayout.removeView(this)
            optionsLayout.addViewAt(this, choice.sortOrder - 1)
            choice.apply {
                this.userSelectedOrder = 100
                this.isSelectedByUser = false
            }
            this.visibility = View.VISIBLE
            RxBus2.publish(
                AnimateAtsOptionViewEvent(
                    fromLocation,
                    this.height,
                    this.width,
                    this,
                    optionsLayout
                )
            )
        }
    }

    fun updateView(isSelected: Boolean) {
        this.background = ContextCompat.getDrawable(
            context,
            if (isSelected) R.drawable.rounded_rectangle_with_grey_border else R.drawable.rounded_rectangle_grey
        )
        this.setTextColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.text_default else R.color.disabled
            )
        )
        this.isEnabled = isSelected
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(
            mPadding4F,
            mPadding4F,
            mPadding4F,
            mPadding4F
        )
        setLayoutParams(layoutParams)
        setPadding(
            mPaddingLeft,
            mPaddingTop,
            mPaddingRight,
            mPaddingBottom
        )
    }

    fun updateChoice(choice: Choice) {
        this.choice = choice
    }

    companion object {
        var mPaddingTop = Utils.sdpToPx(R.dimen._10sdp).toInt()
        var mPaddingBottom = Utils.sdpToPx(R.dimen._10sdp).toInt()
        var mPaddingLeft = Utils.sdpToPx(R.dimen._14sdp).toInt()
        var mPaddingRight = Utils.sdpToPx(R.dimen._14sdp).toInt()
        var mPadding10F = Utils.sdpToPx(R.dimen._16sdp).toInt()
        var mPadding6F = Utils.sdpToPx(R.dimen._8sdp).toInt()
        var mPadding4F = Utils.sdpToPx(R.dimen._3sdp).toInt()

    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(
            mPadding4F,
            mPadding4F,
            mPadding4F,
            mPadding4F
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
        background = ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_with_grey_border)
        setTextColor(ContextCompat.getColor(context, R.color.text_default))
    }
}

class AnimateAtsOptionViewEvent(
    val fromLocation: IntArray,
    val height: Int,
    val width: Int,
    val atsOptionView: AtsOptionView,
    val optionLayout: FlowLayout? = null,
)

@SuppressLint("ClickableViewAccessibility")
fun FlowLayout.removeView(view: View, choice: Choice) {
    val atsOptionView = AtsOptionView(context, choice)
    atsOptionView.setPadding(
        AtsOptionView.mPaddingLeft,
        AtsOptionView.mPaddingTop,
        AtsOptionView.mPaddingRight,
        AtsOptionView.mPaddingBottom
    )
    invalidate()
    atsOptionView.background =
        ContextCompat.getDrawable(context, R.drawable.rounded_rectangle_grey)
    atsOptionView.setTextColor(ContextCompat.getColor(context, R.color.disabled))
    atsOptionView.setOnTouchListener(null)
    atsOptionView.setOnClickListener(null)
    removeView(view)
    addView(atsOptionView, choice.sortOrder - 1)
}

fun FlowLayout.addViewAt(view: View, index: Int) {
    if (getChildAt(index) != null) {
        removeViewAt(index)
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }
        addView(view, index)
    } else {
        addView(view, childCount)
    }
}