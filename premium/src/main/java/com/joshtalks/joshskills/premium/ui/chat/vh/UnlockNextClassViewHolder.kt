package com.joshtalks.joshskills.premium.ui.chat.vh

import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.repository.local.eventbus.UnlockNextClassEventBus


class UnlockNextClassViewHolder(view: android.view.View, userId: String) :
    BaseViewHolder(view, userId) {
    private val subRootView: CardView = view.findViewById(R.id.card_top)
    private val btnStart: MaterialButton = view.findViewById(R.id.btn_start)

    init {
        btnStart.also {
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
        btnStart.text = AppObjectController.joshApplication.getString(R.string.unlock_class_text)
    }

    override fun unBind() {

    }
}