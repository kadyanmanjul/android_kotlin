package com.joshtalks.joshskills.ui.view_holders

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.util.*


@Layout(R.layout.time_view_holder_layout)
class TimeViewHolder(var time: Date) : BaseCell() {

    @View(R.id.text_title)
    lateinit var textView: JoshTextView


    @Resolve
    fun onViewInflated() {
        textView.text = Utils.dateHeaderDateFormat(time).toUpperCase(Locale.getDefault())
    }
}
