package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import org.jetbrains.anko.backgroundResource
import java.lang.ref.WeakReference

@Layout(R.layout.conversation_practise_layout)
class ConversationPractiseViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel,previousMessage:ChatModel?) :
    BaseChatViewHolder(activityRef, message,previousMessage) {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.text_message_time)
    lateinit var receivedMessageTime: AppCompatTextView

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var subRootView: CardView

    @View(R.id.tv_title)
    lateinit var titleTv: AppCompatTextView


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
        updateTime(receivedMessageTime)
        message.question?.imageList?.getOrNull(0)?.imageUrl?.run {
            setBlurImageInImageView(imageView, this)
        }
        message.question?.run {
            this.conversationPracticeId?.let {
                titleTv.text = "Conversation #$it"
            }
        }
        subRootView.setBackgroundResource(getViewHolderBGResource(previousMessage?.sender,message.sender))

    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Click(R.id.root_sub_view)
    fun onClick() {
        openPractise()
    }

    @Click(R.id.btn_start)
    fun onClickButton() {
        openPractise()
    }

    private fun openPractise() {
        message.question?.conversationPracticeId?.let {
            RxBus2.publish(
                ConversationPractiseEventBus(
                    it,
                    message.question?.imageList?.getOrNull(0)?.imageUrl
                )
            )

        }
    }
}
