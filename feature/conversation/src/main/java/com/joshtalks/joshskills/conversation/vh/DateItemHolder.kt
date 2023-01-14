package com.joshtalks.joshskills.conversation.vh

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel

class DateItemHolder internal constructor(itemView: View) : BaseViewHolder(itemView, "") {
    var txtMessageDate: AppCompatTextView = itemView.findViewById(R.id.txt_message_date)
    override fun bind(message: ChatModel, previousSender: ChatModel?) {

    }

    override fun unBind() {

    }

}