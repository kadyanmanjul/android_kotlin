package com.joshtalks.joshskills.quizgame.ui.main.view.customview

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.joshtalks.joshskills.R

class InnerProgressViewFirstClick(
    context: Context?,
    attrs: AttributeSet?
) : View(context, attrs) {

    companion object {
        const val ARC_FULL_ROTATION_DEGREE = 360
        const val PERCENTAGE_DIVIDER = 100.0
        const val PERCENTAGE_VALUE_HOLDER = "percentage"
    }

    var valuesHolder: PropertyValuesHolder? = null
    var animator: Animator? = null

    private var currentPercentage = 0

    private val ovalSpace = RectF()

    private val parentArcColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context?.resources?.getColor(R.color.blue, null) ?: Color.GRAY
    } else {
        TODO("VERSION.SDK_INT < M")
    }

    private val fillArcColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //   context?.resources?.getColor(R.color.gray_light_copy, null) ?: Color.GREEN
        context?.resources?.getColor(R.color.blue2, null) ?: Color.GREEN
    } else {
        TODO("VERSION.SDK_INT < M")
    }

    private val parentArcPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = parentArcColor
        strokeWidth = 28f
    }

    private val fillArcPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = fillArcColor
        strokeWidth = 28f
        strokeCap = Paint.Cap.BUTT
    }

    override fun onDraw(canvas: Canvas?) {
        setSpace()
        canvas?.let {
            drawBackgroundArc(it)
            drawInnerArc(it)
        }
    }

    private fun setSpace() {
        val horizontalCenter = (width.div(2)).toFloat()
        val verticalCenter = (height.div(2)).toFloat()
        val ovalSize = 50
        ovalSpace.set(
            horizontalCenter - ovalSize,
            verticalCenter - ovalSize,
            horizontalCenter + ovalSize,
            verticalCenter + ovalSize
        )
    }

    private fun drawBackgroundArc(it: Canvas) {
//        val percentageToFill = getCurrentPercentageToFill()
//        canvas.drawArc(ovalSpace, 90f, percentageToFill, false, parentArcPaint)
        it.drawArc(ovalSpace, 0f, 360f, false, parentArcPaint)
    }

    private fun drawInnerArc(canvas: Canvas) {
        val percentageToFill = getCurrentPercentageToFill()
        canvas.drawArc(ovalSpace, 270f, percentageToFill, false, fillArcPaint)
    }

    fun getCurrentPercentageToFill() =
        (-ARC_FULL_ROTATION_DEGREE * (currentPercentage / PERCENTAGE_DIVIDER)).toFloat()

    fun animateProgress() {
        valuesHolder = PropertyValuesHolder.ofFloat("percentage", 100f, 0f)
        animator = ValueAnimator().apply {
            setValues(valuesHolder)
            duration = 16000

            addUpdateListener {
                val percentage = it.getAnimatedValue(PERCENTAGE_VALUE_HOLDER) as Float
                currentPercentage = percentage.toInt()
                invalidate()
            }
        }
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            animator?.start()
        }, 2000)
    }

    fun pauseProgress() {
        animator?.pause()
    }

    fun setAnimZero() {
        animator?.end()
    }
}