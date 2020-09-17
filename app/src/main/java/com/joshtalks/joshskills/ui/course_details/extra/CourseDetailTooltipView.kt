package com.joshtalks.joshskills.ui.course_details.extra

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R

class CourseDetailTooltipView : FrameLayout {

    private lateinit var ballonTV: AppCompatTextView

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.course_detail_tooltip_view, this)
        ballonTV = findViewById(R.id.balloon_text)
        //setText("Bade Bhaiya Tip: Agar aap in course ke alawa kuch aur bhi seekhna chahte hai to agle x dino tak aap free mei unlimited courses unlock kar sakte hai")

    }

    fun setText(text: String) {
        ballonTV.text = text
    }
}