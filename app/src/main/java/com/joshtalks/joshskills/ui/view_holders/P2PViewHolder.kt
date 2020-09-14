package com.joshtalks.joshskills.ui.view_holders

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentStartEventBus
import com.joshtalks.joshskills.repository.local.eventbus.P2PStartEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference

@Layout(R.layout.p2p_view_holdder_layout)
class P2PViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.root_sub_view)
    lateinit var rootSubView: FrameLayout

    @View(R.id.text_message_body)
    lateinit var messageBody: JoshTextView

    @View(R.id.text_title)
    lateinit var titleView: JoshTextView

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.message_view)
    lateinit var messageView: ViewGroup


    lateinit var textViewHolder: P2PViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        titleView.text = EMPTY
        titleView.visibility = GONE
        textViewHolder = this
        message.sender?.let {
            updateView(it, rootView, rootSubView, messageView)
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
        addMessageAutoLink(messageBody)
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }
    @Click(R.id.btn_start)
    fun onClickStartView() {
        RxBus2.publish(P2PStartEventBus())
    }
}
