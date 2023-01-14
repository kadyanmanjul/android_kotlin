package com.joshtalks.joshskills.conversation.vh

import android.view.View
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel


class NewMessageViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {
    val title: JoshTextView = view.findViewById(R.id.text_title)
    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        title.text = message.text
    }

    override fun unBind() {

    }

}
