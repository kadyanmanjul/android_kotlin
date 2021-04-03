package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.tabs.TabLayout
import com.joshtalks.joshskills.core.getScreenSize
import java.lang.reflect.Field

class CustomTabLayout : TabLayout {
    constructor(context: Context) : super(context) {
        initTabMinWidth()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initTabMinWidth()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initTabMinWidth()
    }

    private fun initTabMinWidth() {
        val wh: IntArray = getScreenSize(getContext())
        val tabMinWidth = wh[WIDTH_INDEX] / DIVIDER_FACTOR
        val field: Field
        try {
            field = TabLayout::class.java.getDeclaredField(SCROLLABLE_TAB_MIN_WIDTH)
            field.setAccessible(true)
            field.set(this, tabMinWidth)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val WIDTH_INDEX = 0
        private const val DIVIDER_FACTOR = 3.2
        private const val SCROLLABLE_TAB_MIN_WIDTH = "mScrollableTabMinWidth"
    }
}