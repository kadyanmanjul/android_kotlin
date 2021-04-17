package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R

class GrammarHeadingView : FrameLayout {
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
        View.inflate(context, R.layout.cell_grammar_heading_layout, this)

    }

    fun setup() {

    }

}