package com.joshtalks.joshskills.common.ui.online_test.util

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.common.ui.online_test.vh.AtsOptionView
import com.nex3z.flowlayout.FlowLayout

@SuppressLint("ClickableViewAccessibility")
fun FlowLayout.removeView(view: View, choice: Choice) {
    val atsOptionView = AtsOptionView(context, choice)
    atsOptionView.setPadding(
        com.joshtalks.joshskills.common.ui.online_test.vh.AtsOptionView.mPaddingLeft,
        com.joshtalks.joshskills.common.ui.online_test.vh.AtsOptionView.mPaddingTop,
        com.joshtalks.joshskills.common.ui.online_test.vh.AtsOptionView.mPaddingRight,
        com.joshtalks.joshskills.common.ui.online_test.vh.AtsOptionView.mPaddingBottom
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