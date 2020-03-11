package com.joshtalks.joshskills.ui.view_holders

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


@Layout(R.layout.chat_text_message_holder)
class TextViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.root_sub_view)
    lateinit var rootSubView: FrameLayout

    @View(R.id.text_message_body)
    lateinit var messageBody: JoshTextView

    @View(R.id.text_title)
    lateinit var titleView: JoshTextView

    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.message_view)
    lateinit var messageView: FrameLayout

    lateinit var textViewHolder: TextViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()

        titleView.text = EMPTY
        titleView.visibility = GONE
        textViewHolder = this
        message.sender?.let {
            updateView(it, rootView, rootSubView, messageView)
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

        // updateForegroundView()
    }


    /* private fun updateForegroundView() {
         if (message.isSelected) {
             root_view.foreground = ColorDrawable(
                 ContextCompat.getColor(
                     getAppContext(),
                     R.color.select_forground_color
                 )
             )
         } else {
             root_view.foreground =
                 ColorDrawable(ContextCompat.getColor(getAppContext(), R.color.transparent))
         }
     }

     @LongClick(R.id.root_view)
     fun onLongClick() {
         message.isSelected = message.isSelected.not()
         updateForegroundView()
         RxBus2.publish(DeleteMessageEventBus(message))

     }

     @Click(R.id.root_view)
     fun onSelect() {
         message.isSelected = false
         root_view.foreground =
             ColorDrawable(ContextCompat.getColor(getAppContext(), R.color.transparent))
         RxBus2.publish(DeleteMessageEventBus(message))

     }
 */
}
