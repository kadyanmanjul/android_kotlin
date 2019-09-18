package com.joshtalks.joshskills.ui.view_holders

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import java.lang.ref.WeakReference


@Layout(R.layout.chat_text_message_holder)
class TextViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.parent_layout)
    lateinit var parent_layout: android.view.View

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout


    @View(R.id.text_message_body)
    lateinit var text_message_body: JoshTextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout


    @View(R.id.message_view)
    lateinit var message_view: FrameLayout



    @Resolve
    fun onResolved() {
        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }
        text_message_time.text = Utils.messageTimeConversion(message.created)
        updateTime(text_message_time)

        if (message.text != null) {
            text_message_body.text =  HtmlCompat.fromHtml(message.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            message.question?.qText?.let {
                text_message_body.text =  HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
        addMessageAutoLink(text_message_body)
    }
}