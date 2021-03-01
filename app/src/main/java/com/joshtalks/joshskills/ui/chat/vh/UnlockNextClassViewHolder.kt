package com.joshtalks.joshskills.ui.chat.vh

import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.UnlockNextClassEventBus


class UnlockNextClassViewHolder(view: android.view.View, userId: String) :
    BaseViewHolder(view, userId) {
    private val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    private val subRootView: CardView = view.findViewById(R.id.card_top)

    private val btnStart: MaterialButton = view.findViewById(R.id.btn_start)
    private var message: ChatModel? = null

    init {
        rootView.also {
            it.setOnClickListener {
                RxBus2.publish(UnlockNextClassEventBus())
            }
        }
        subRootView.also {
            it.setOnClickListener {
                RxBus2.publish(UnlockNextClassEventBus())
            }
        }
    }


    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
        message.question?.let { question ->
            btnStart.text =
                AppObjectController.joshApplication.getString(R.string.unlock_class_text)
        }
    }

    override fun unBind() {

    }
}