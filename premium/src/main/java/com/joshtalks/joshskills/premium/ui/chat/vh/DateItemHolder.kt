package com.joshtalks.joshskills.premium.ui.chat.vh

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel

class DateItemHolder internal constructor(itemView: View) : BaseViewHolder(itemView, "") {
    var txtMessageDate: AppCompatTextView = itemView.findViewById(R.id.txt_message_date)
    override fun bind(message: ChatModel, previousSender: ChatModel?) {

    }

    override fun unBind() {

    }

}