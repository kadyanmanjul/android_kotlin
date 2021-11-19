package com.joshtalks.joshskills.quizgame.utils

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.joshtalks.joshskills.R

class CircularProgressView(
    context: Context?,
    attrs: AttributeSet?
) : View(context, attrs) {

    companion object {
        const val ARC_FULL_ROTATION_DEGREE = 360
        const val PERCENTAGE_DIVIDER = 100.0
        const val PERCENTAGE_VALUE_HOLDER = "percentage"
    }

    private var currentPercentage = 0


    private val ovalSpace = RectF()

    @RequiresApi(Build.VERSION_CODES.M)
    private val fillArcColor = context?.resources?.getColor(R.color.transparent_color_notification, null) ?: Color.GREEN

    @RequiresApi(Build.VERSION_CODES.M)
    private val fillArcPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = fillArcColor
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDraw(canvas: Canvas?) {
        setSpace()
        canvas?.let {
            drawInnerArc(it)
        }
    }

    private fun setSpace() {
        val horizontalCenter = (width.div(2)).toFloat()
        val verticalCenter = (height.div(2)).toFloat()
        val ovalSize = 200
        ovalSpace.set(
            horizontalCenter - ovalSize,
            verticalCenter - ovalSize,
            horizontalCenter + ovalSize,
            verticalCenter + ovalSize
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun drawInnerArc(canvas: Canvas) {
        val percentageToFill = getCurrentPercentageToFill()
        canvas.drawArc(ovalSpace, 270f, percentageToFill, true, fillArcPaint)
    }

    private fun getCurrentPercentageToFill() =
        (0-(ARC_FULL_ROTATION_DEGREE * (currentPercentage / PERCENTAGE_DIVIDER)).toFloat())

    fun animateProgress() {
        val valuesHolder = PropertyValuesHolder.ofFloat("percentage", 100f, 0f)
        val animator = ValueAnimator().apply {
            setValues(valuesHolder)
            duration = 10000
            addUpdateListener {
                val percentage = it.getAnimatedValue(PERCENTAGE_VALUE_HOLDER) as Float
                currentPercentage = percentage.toInt()
                invalidate()
            }
        }
        animator.start()
    }
}