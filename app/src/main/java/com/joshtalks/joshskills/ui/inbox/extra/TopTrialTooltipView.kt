package com.joshtalks.joshskills.ui.inbox.extra

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R

class TopTrialTooltipView : FrameLayout {

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
        View.inflate(context, R.layout.top_trial_tooltip_view, this)
        ballonTV=findViewById(R.id.balloon_text)
        ballonTV.setText("Bade Bhaiya Tip: Agar aap in course ke alawa kuch aur bhi seekhna chahte hai to agle x dino tak aap free mei unlimited courses unlock kar sakte hai")

    }
}
