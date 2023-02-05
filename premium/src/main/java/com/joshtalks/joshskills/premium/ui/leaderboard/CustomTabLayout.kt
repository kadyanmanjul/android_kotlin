package com.joshtalks.joshskills.premium.ui.leaderboard

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.tabs.TabLayout


class CustomTabLayout : TabLayout {
    private val tabWidthList = mutableListOf<Int>()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {

        val equalTabWidth= (MeasureSpec.getSize(widthMeasureSpec) / DIVIDER_FACTOR).toInt()
        for (index in 0..tabCount) {
            val tab = getTabAt(index)
            val tabMeasuredWidth = tab?.view?.measuredWidth ?: equalTabWidth
            //tabWidthList[index] = tabMeasuredWidth
            if (tabMeasuredWidth < equalTabWidth) {
                tab?.view?.minimumWidth = equalTabWidth
                //tabWidthList[index] = equalTabWidth
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    companion object {
        private const val DIVIDER_FACTOR = 3.7
    }

    //fun getTabWidth(index : Int) = tabWidthList[index]
}