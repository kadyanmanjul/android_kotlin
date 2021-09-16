package com.joshtalks.joshskills.ui.tooltip

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.joshtalks.joshskills.R

class JoshTooltip @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val tooltipTextView: TextView

    init {
        try {
            View.inflate(getContext(), R.layout.josh_tooltip, this)
        } catch (e : Exception) {
            e.printStackTrace()
        }
        tooltipTextView = this.findViewById(R.id.tooltip_tv)
            attrs?.let {
                val attrsValue = context.obtainStyledAttributes(it, R.styleable.JoshTooltip)
                try {
                    tooltipTextView.text = (attrsValue.getString(R.styleable.JoshTooltip_tooltipText) ?: "No value given")
                    tooltipTextView.layoutParams.height = (getScreenHeightAndWidth().first * 0.0815).toInt()
                    //tooltipTextView.text = (attrsValue.getString(R.styleable.JoshTooltip_tooltipText) ?: "No value given")
                } finally {
                    attrsValue.recycle()
                }
            }
        }

    fun setTooltipText(tooltipText : String) {
        tooltipTextView.text = tooltipText
    }

    fun getScreenHeightAndWidth(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels to metrics.widthPixels
    }
}