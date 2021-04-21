package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout
import java.util.ArrayList

class CustomLayout(context: Context?) : FlowLayout(context) {
    var customWord: CustomWord? = null

    fun push(word: View) {
        addView(word)
    }

    fun removeViewCustomLayout(view: View, choice: Choice) {
        customWord = CustomWord(context, choice)
        customWord?.background =
            ContextCompat.getDrawable(context, R.drawable.rounded_rectangle_grey)
        customWord?.setTextColor(ContextCompat.getColor(context, R.color.light_shade_of_gray))
//        val params = LinearLayout.LayoutParams(view.width, view.height)
//        params.setMargins(20, 20, 20, 0)
//        customWord!!.layoutParams = params
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
