package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.nex3z.flowlayout.FlowLayout

class CustomWord(context: Context) : AppCompatTextView(context) {

    lateinit var choice: Choice

    constructor(context: Context, choice: Choice) : this(context) {
        this.choice = choice
        text = choice.text
    }

    fun changeViewGroup(optionsLayout: CustomLayout, answerLayout: FlowLayout) {
        if (parent is CustomLayout) {
            optionsLayout.removeViewCustomLayout(this, choice)
            answerLayout.addView(this)
            choice.apply {
                this.userSelectedOrder = answerLayout.childCount
            }
        } else {
            answerLayout.removeView(this)
            optionsLayout.addViewAt(this, choice.sortOrder - 1)
            choice.apply {
                this.userSelectedOrder = 100
            }
        }
    }

    companion object {
        private const val TAG = "CustomWord"
    }

    init {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(20, 20, 20, 0)
        setTextColor(resources.getColor(R.color.black))
        setLayoutParams(layoutParams)
        textAlignment = TEXT_ALIGNMENT_CENTER
        textSize = 20f
        background =
            ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_with_border)
    }
}
