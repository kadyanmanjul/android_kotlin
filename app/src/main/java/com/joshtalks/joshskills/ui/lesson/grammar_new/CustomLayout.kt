package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout

class CustomLayout(context: Context?) : FlowLayout(context) {
    var customWord: CustomWord? = null

    fun push(word: View) {
        addView(word)
    }

    fun removeViewCustomLayout(view: View, choice: Choice) {
        customWord = CustomWord(context, choice)
        customWord?.setPadding(
            CustomWord.mPaddingLeft,
            CustomWord.mPaddingTop,
            CustomWord.mPaddingRight,
            CustomWord.mPaddingBottom
        )
        invalidate()
        customWord?.background =
            ContextCompat.getDrawable(context, R.drawable.rounded_rectangle_grey)
        customWord?.setTextColor(ContextCompat.getColor(context, R.color.light_shade_of_gray))
        customWord?.setOnTouchListener(null)
        customWord?.setOnClickListener(null)
        removeView(view)
        addView(customWord, choice.sortOrder - 1)
    }

    fun addViewAt(view: View, index: Int) {
        if (getChildAt(index) != null) {
            removeViewAt(index)
            addView(view, index)
        } else {
            addView(view, childCount)
        }
    }

    fun isEmpty(): Boolean {
        return childCount <= 0
    }

    companion object {
        private const val TAG = "CustomLayout"
    }
}
