package com.joshtalks.joshskills.ui.view_holders


import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.new_message_layout)
class NewMessageViewHolder(var label: String) : BaseCell() {

    @View(R.id.text_title)
    lateinit var textView: JoshTextView


    @Resolve
    fun onViewInflated() {
        textView.text = label
    }
}
