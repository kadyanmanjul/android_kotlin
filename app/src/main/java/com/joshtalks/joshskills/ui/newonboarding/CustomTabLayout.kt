package com.joshtalks.joshskills.ui.newonboarding

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout

class CustomTabLayout : TabLayout {
    private var tabStrip: ViewGroup

    constructor(context: Context?) : super(context!!) {
        tabStrip = getChildAt(0) as ViewGroup
        tabStrip.layoutParams.height = LayoutParams.WRAP_CONTENT
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        tabStrip = getChildAt(0) as ViewGroup
        tabStrip.layoutParams.height = LayoutParams.WRAP_CONTENT
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        tabStrip = getChildAt(0) as ViewGroup
        tabStrip.layoutParams.height = LayoutParams.WRAP_CONTENT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Force the height of this ViewGroup to be the same height of the tabStrip
        setMeasuredDimension(measuredWidth, tabStrip.measuredHeight)
    }
}