package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.UnlockNextClassEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference


@Layout(R.layout.unlock_class_item_layout)
class UnlockNextClassViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel,previousMessage:ChatModel?) :
    BaseChatViewHolder(activityRef, message,previousMessage) {
    @View(R.id.btn_start)
    lateinit var btnStart: MaterialButton

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.card_top)
    lateinit var subRootView: CardView

    lateinit var viewHolder: UnlockNextClassViewHolder


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        viewHolder = this
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

        message.question?.let { question ->
            btnStart.text =
                AppObjectController.joshApplication.getString(R.string.unlock_class_text)
        }
    }


    @Click(R.id.card_top)
    fun onClickRootView() {
        RxBus2.publish(UnlockNextClassEventBus())
    }

    @Click(R.id.btn_start)
    fun onClickStartView() {
        RxBus2.publish(UnlockNextClassEventBus())
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

}
