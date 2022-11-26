package com.joshtalks.joshskills.common.ui.view_holders

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


class TextViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) : BaseChatViewHolder(activityRef, message, previousMessage) {


    lateinit var rootView: FrameLayout


    lateinit var rootSubView: FrameLayout


    lateinit var messageView: ViewGroup


    lateinit var messageBody: JoshTextView


    lateinit var titleView: JoshTextView


    lateinit var text_message_time: AppCompatTextView

    lateinit var textViewHolder: TextViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        messageView.findViewById<ViewGroup>(R.id.tag_view).visibility = GONE
        titleView.text = EMPTY
        titleView.visibility = GONE
        textViewHolder = this
        message.sender?.let {
            setViewHolderBG(previousMessage?.sender, it, rootView, rootSubView, messageView)
        }
        message.parentQuestionObject?.run {
            addLinkToTagMessage(messageView, this, message.sender)
        }
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

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
            if (message.text.isNullOrEmpty().not()) {
                messageBody.text =
                    HtmlCompat.fromHtml(message.text!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }

        text_message_time.text = Utils.messageTimeConversion(message.created)
        updateTime(text_message_time)
        addMessageAutoLink(messageBody)
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }
}
