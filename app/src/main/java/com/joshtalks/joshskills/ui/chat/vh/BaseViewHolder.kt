package com.joshtalks.joshskills.ui.chat.vh

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Sender
import java.util.Date
import java.util.concurrent.TimeUnit

abstract class BaseViewHolder(
    view: android.view.View, val userId: String
) : RecyclerView.ViewHolder(view) {
    fun getAppContext() = AppObjectController.joshApplication
    fun getDrawablePadding() = Utils.dpToPx(getAppContext(), 4f)

    fun addMessageAutoLink(textMessageBody: JoshTextView) {
        textMessageBody.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_PHONE -> Utils.call(getAppContext(), matchedText)
                AutoLinkMode.MODE_URL -> Utils.openUrl(matchedText, getAppContext())
                else -> {

                }
            }
        }
    }

    fun addDrawableOnTime(message: ChatModel, tv: AppCompatTextView) {
        if (message.sender?.id.equals(userId, ignoreCase = true)) {
            tv.compoundDrawablePadding = getDrawablePadding()
            if (message.isSync.not()) {
                tv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_unsync_msz,
                    0
                )
                return
            }


            when (message.messageDeliverStatus) {
                MESSAGE_DELIVER_STATUS.SENT -> {
                    tv.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_tick,
                        0
                    )
                }
                MESSAGE_DELIVER_STATUS.SENT_RECEIVED -> tv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_sent_message_d_tick,
                    0
                )
                else -> {
                    tv.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_r_tick,
                        0
                    )
                }
            }


        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
        }
    }


    fun setViewHolderBG(
        cMessage: ChatModel, lMessage: ChatModel?, root: FrameLayout
    ) {

        if (lMessage?.sender == null) {
            setViewBgNewMessage(root, cMessage.sender!!, lMessage?.sender)
        } else {
            if (isDateChange(cMessage.created, lMessage.created)) {
                setViewBgNewMessage(root, cMessage.sender!!, lMessage.sender)
            }else{
                setViewBgMessageStack(root, cMessage.sender!!, lMessage.sender!!)
            }
        }
    }

    fun setViewHolderBGRound(
        cMessage: ChatModel, lMessage: ChatModel?, root: FrameLayout
    ) {
        setViewBgBasicMessageStack(root, cMessage.sender!!)
    }

    private fun isDateChange(cDate: Date, lDate: Date): Boolean {
        val days = TimeUnit.DAYS.convert(cDate.time - lDate.time, TimeUnit.MILLISECONDS)
        if (days > 0) {
            return true
        }
        return false
    }

    private fun setViewBgNewMessage(root: FrameLayout, cSender: Sender, lSender: Sender?) {
            if (cSender.id.equals(userId, ignoreCase = true)) {
                root.setBackgroundResource(R.drawable.outgoing_message_normal_bg)
            } else {
                root.setBackgroundResource(R.drawable.incoming_message_normal_bg)
            }
        }


    private fun setViewBgMessageStack(root: FrameLayout, cSender: Sender, lSender: Sender) {
            if (lSender.id == cSender.id || lSender.id == userId) { // no balloon bg
                if (cSender.id.equals(userId, ignoreCase = true)) {
                    root.setBackgroundResource(R.drawable.outgoing_message_same_bg)
                } else {
                    root.setBackgroundResource(R.drawable.incoming_message_same_bg)
                }
            } else { // balloon bg
                if (cSender.id.equals(userId, ignoreCase = true)) {
                    root.setBackgroundResource(R.drawable.outgoing_message_normal_bg)
                } else {
                    root.setBackgroundResource(R.drawable.incoming_message_normal_bg)
                }
        }
    }

    private fun setViewBgBasicMessageStack(root: FrameLayout, cSender: Sender) {
        if (cSender.id.equals(userId, ignoreCase = true)) {
            root.setBackgroundResource(R.drawable.outgoing_message_same_bg)
        } else {
            root.setBackgroundResource(R.drawable.incoming_message_same_bg)
        }
    }

    protected fun getUrlForDownload(it: ChatModel): String? {
        if (it.url == null) {
            if (it.question?.material_type == BASE_MESSAGE_TYPE.AU) {
                return it.question?.audioList?.getOrNull(0)?.audio_url
            } else if (it.question?.material_type == BASE_MESSAGE_TYPE.PD) {
                return it.question?.pdfList?.getOrNull(0)?.url
            } else if (it.question?.material_type == BASE_MESSAGE_TYPE.VI) {
                return it.question?.videoList?.getOrNull(0)?.video_url
            }
        } else {
            return it.url
        }
        return null
    }


    abstract fun bind(message: ChatModel, previousSender: ChatModel?)
    abstract fun unBind()

}