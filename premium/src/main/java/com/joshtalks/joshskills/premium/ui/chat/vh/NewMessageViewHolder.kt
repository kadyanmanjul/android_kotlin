package com.joshtalks.joshskills.premium.ui.chat.vh

import android.view.View
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel


class NewMessageViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {
    val title: JoshTextView = view.findViewById(R.id.text_title)
    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        title.text = message.text
    }

    override fun unBind() {

    }

}
