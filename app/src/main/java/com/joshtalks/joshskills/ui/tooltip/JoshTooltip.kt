package com.joshtalks.joshskills.ui.tooltip

import android.content.Context
import android.util.AttributeSet
import android.view.View
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
                } finally {
                    attrsValue.recycle()
                }
            }
        }
}