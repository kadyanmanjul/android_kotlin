package com.joshtalks.joshskills.ui.view_holders

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Sender
import java.lang.ref.WeakReference


abstract class BaseChatViewHolder(
    val activityRef: WeakReference<FragmentActivity>,
    var message: ChatModel
) : BaseCell() {


    fun getLeftPaddingForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)
    fun getRightPaddingForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)
    fun getMarginForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)


    fun getLeftPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)
    fun getRightPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)
    fun getMarginForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)


    fun updateView(
        sender: Sender,
        root_view: FrameLayout,
        root_sub_view: FrameLayout,
        message_view: View
    ) {
        if (sender.id.equals(getUserId(), ignoreCase = true)) {

            root_view.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)

            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.END
            root_sub_view.layoutParams = params
            root_sub_view.setBackgroundResource(R.drawable.balloon_outgoing_normal)


            val paramsMessage = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            paramsMessage.setMargins(0, 0, getMarginForSender(), 0)
            message_view.layoutParams = paramsMessage

        } else {

            root_view.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)

            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.START
            root_sub_view.layoutParams = params
            root_sub_view.setBackgroundResource(R.drawable.balloon_incoming_normal)

            val paramsMessage = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            paramsMessage.setMargins(getMarginForReceiver(), 0, 0, 0)
            message_view.layoutParams = paramsMessage
        }
    }


    fun updateTime(text_message_time: AppCompatTextView) {
        if (message.sender?.id.equals(getUserId(), ignoreCase = true)) {
            text_message_time.compoundDrawablePadding = getDrawablePadding()



            if (message.isSync.not()) {
                text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_unsync_msz,
                    0
                )
                return
            }



            when {
                message.messageDeliverStatus == MESSAGE_DELIVER_STATUS.SENT -> {

                    text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_tick,
                        0
                    )
                }
                message.messageDeliverStatus == MESSAGE_DELIVER_STATUS.SENT_RECEIVED -> text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_sent_message_d_tick,
                    0
                )
                else -> {
                    text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_r_tick,
                        0
                    )
                }
            }


        } else {
            text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
        }
    }

}