package com.joshtalks.joshskills.ui.view_holders

import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Sender
import com.mindorks.placeholderview.annotations.Resolve
import java.lang.ref.WeakReference


abstract class BaseChatViewHolder(
    val activityRef: WeakReference<FragmentActivity>,
    var message: ChatModel
) : BaseCell() {

    private val params = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )


    fun getLeftPaddingForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f)
    fun getRightPaddingForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)
    fun getMarginForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)


    fun getLeftPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)
    fun getRightPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f)
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
            // root_sub_view.setBackgroundResource(R.drawable.recived_message_selector)
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

    fun addMessageAutoLink(text_message_body: JoshTextView) {
        text_message_body.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_PHONE -> Utils.call(getAppContext(), matchedText)
                AutoLinkMode.MODE_URL -> activityRef.get()?.let { Utils.openUrl(matchedText, it) }
            }
        }
    }


    fun updateView(rootView: RelativeLayout, sender: Sender) {
        if (sender.id.equals(getUserId(), ignoreCase = true)) {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(
                com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f),
                0,
                com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f),
                0
            )
            params.gravity = Gravity.END
            rootView.layoutParams = params
            rootView.setBackgroundResource(R.drawable.balloon_outgoing_normal)
        } else {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.START
            params.setMargins(
                com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f),
                0,
                com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f),
                0
            )
            rootView.layoutParams = params
            rootView.setBackgroundResource(R.drawable.balloon_incoming_normal)
        }
    }

    companion object {
        private val senderParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1.0f
            gravity = Gravity.END

        }

        private val receiverParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1.0f
            gravity = Gravity.START
        }
        private var START_MARGIN = 80F
        private var END_MARGIN = 10F


        fun getSenderParams(): LinearLayout.LayoutParams {
            //senderParams.resolveLayoutDirection()
            /*senderParams.setMargins(
                com.vanniktech.emoji.Utils.dpToPx(
                    AppObjectController.joshApplication,
                    80f
                ),
                0,
                com.vanniktech.emoji.Utils.dpToPx(AppObjectController.joshApplication, 0f),
                0
            )*/
            return senderParams

        }

        fun getReceiverParams(): LinearLayout.LayoutParams {
            receiverParam.gravity = Gravity.END

            /*receiverParam.setMargins(
                com.vanniktech.emoji.Utils.dpToPx(
                    AppObjectController.joshApplication,
                    0f
                ),
                0,
                com.vanniktech.emoji.Utils.dpToPx(
                    AppObjectController.joshApplication,
                    80f
                ),
                0
            )*/
            return receiverParam
        }


    }

    open fun onViewInflated() {
       // Log.e("getclass", javaClass.simpleName)
        RxBus2.publish(message)
    }


}