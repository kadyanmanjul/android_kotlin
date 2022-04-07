package com.joshtalks.joshskills.ui.view_holders

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.SpecialPracticeEventBus
import com.joshtalks.joshskills.ui.chat.vh.BaseViewHolder


class SpecialPracticeClassViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    lateinit var viewHolder: SpecialPracticeClassViewHolder

    private val subRootView: LinearLayout = view.findViewById(R.id.special_practice_card)
    private val txtMessage: AppCompatTextView = view.findViewById(R.id.txt_message)
    private var message: ChatModel? = null

    init {
        subRootView.setOnClickListener {
            RxBus2.publish(SpecialPracticeEventBus(message?.specialPractice?.id.toString()))
        }
    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
        txtMessage.text = message.specialPractice?.mainText
    }

    override fun unBind() {
    }
}
