package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.unread_message_view)
class UnreadMessageViewHolder(var unread: Int) : BaseCell() {

    @View(R.id.tv_unread)
    lateinit var tv_unread: AppCompatTextView


    @Resolve
    fun onResolved() {
        tv_unread.text = getAppContext().getString(R.string.unread_message, unread.toString())
    }
}