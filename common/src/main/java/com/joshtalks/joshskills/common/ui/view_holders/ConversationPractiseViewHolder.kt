package com.joshtalks.joshskills.common.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.eventbus.ConversationPractiseEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


class ConversationPractiseViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel,previousMessage:ChatModel?) :
    BaseChatViewHolder(activityRef, message,previousMessage) {


    lateinit var imageView: AppCompatImageView


    lateinit var receivedMessageTime: AppCompatTextView


    lateinit var rootView: FrameLayout


    lateinit var subRootView: FrameLayout


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
                titleTv.text = getAppContext().getString(R.string.conversation, "#$it")
            }
        }

        subRootView.setBackgroundResource(getViewHolderBGResource(previousMessage?.sender,message.sender))
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

    
    fun onClick() {
        openPractise()
    }

    
    fun onClickButton() {
        openPractise()
    }

    private fun openPractise() {
        message.question?.conversationPracticeId?.let {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                ConversationPractiseEventBus(
                    it,
                    message.question?.imageList?.getOrNull(0)?.imageUrl
                )
            )
        }
    }
}
