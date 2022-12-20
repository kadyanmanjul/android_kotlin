package com.joshtalks.joshskills.common.ui.chat.vh

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel

class TextViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {
    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)
    val messageView: ViewGroup = view.findViewById(R.id.message_view)
    val messageBody: JoshTextView = view.findViewById(R.id.text_message_body)
    val titleView: JoshTextView = view.findViewById(R.id.text_title)
    val textMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        if (null != message.sender) {
            setViewHolderBG(message, previousMessage, rootSubView)
        }
        titleView.text = EMPTY
        titleView.visibility = GONE

        if (message.text.isNullOrEmpty()) {
            message.question?.run {
                this.qText?.let {
                    messageBody.text =
                        HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
                this.title?.let { text ->
                    if (text.isNotEmpty()) {
                        titleView.visibility = VISIBLE
                        titleView.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                }
            }
        } else {
            messageBody.text = HtmlCompat.fromHtml(message.text!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
            messageBody.visibility = VISIBLE
        }

        textMessageTime.text = Utils.messageTimeConversion(message.created)
        addDrawableOnTime(message, textMessageTime)
        addMessageAutoLink(messageBody)
    }

    override fun unBind() {
    }
}