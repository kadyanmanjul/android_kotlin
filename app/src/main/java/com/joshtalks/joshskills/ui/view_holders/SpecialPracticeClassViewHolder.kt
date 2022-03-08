package com.joshtalks.joshskills.ui.view_holders

import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.SpecialPracticeEventBus
import com.joshtalks.joshskills.ui.chat.vh.BaseViewHolder
import com.mindorks.placeholderview.annotations.Layout


@Layout(R.layout.special_item_layout)
class SpecialPracticeClassViewHolder(view: android.view.View, userId: String) :
    BaseViewHolder(view, userId) {

    lateinit var viewHolder: SpecialPracticeClassViewHolder

    private val subRootView: LinearLayout = view.findViewById(R.id.special_practice_card)
    private val btnStart: AppCompatTextView = view.findViewById(R.id.btn_start_special_practice)
    private val txtMessage: AppCompatTextView = view.findViewById(R.id.txt_message)
    private var message: ChatModel? = null

    init {
//        showToast(message?.specialPractice?.mainText.toString())
//        txtMessage.text = message?.specialPractice?.mainText

        btnStart.also { it ->
            it.setOnClickListener {
                message?.specialPractice?.let {
                    RxBus2.publish(SpecialPracticeEventBus(it.id ?: -1))
                }
            }
        }

        subRootView.also { it ->
            it.setOnClickListener {
                message?.specialPractice?.let {
                    RxBus2.publish(SpecialPracticeEventBus(it.id ?: -1))
                }
            }
        }

    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
    }

    override fun unBind() {

    }

}
