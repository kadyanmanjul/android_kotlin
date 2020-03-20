package com.joshtalks.joshskills.ui.view_holders

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.PlayerUtil
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.GotoChatEventBus
import java.lang.ref.WeakReference


abstract class BaseChatViewHolder(
    val activityRef: WeakReference<FragmentActivity>,
    var message: ChatModel
) : BaseCell() {

    companion object {
        var sId = EMPTY
    }

    private val params = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )


    private fun getLeftPaddingForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f)
    private fun getRightPaddingForReceiver() =
        com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)

    private fun getMarginForReceiver() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)
    private fun getLeftPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f)
    private fun getRightPaddingForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f)
    private fun getMarginForSender() = com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 0f)


    fun updateView(
        sender: Sender,
        root_view: FrameLayout,
        root_sub_view: FrameLayout,
        message_view: ViewGroup
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


            when (message.messageDeliverStatus) {
                MESSAGE_DELIVER_STATUS.SENT -> {

                    text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_tick,
                        0
                    )
                }
                MESSAGE_DELIVER_STATUS.SENT_RECEIVED -> text_message_time.setCompoundDrawablesWithIntrinsicBounds(
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
                else -> {

                }
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

    @SuppressLint("ClickableViewAccessibility")
    fun addLinkToTagMessage(rootView: ViewGroup, linkObj: Question) {
        try {
            rootView.findViewById<ViewGroup>(R.id.tag_view).visibility = View.VISIBLE
            rootView.findViewById<ViewGroup>(R.id.tag_view).setOnClickListener {
                RxBus2.publish(GotoChatEventBus(linkObj.chatId))
            }
            rootView.findViewById<ViewGroup>(R.id.sub_rl).setOnClickListener {
                RxBus2.publish(GotoChatEventBus(linkObj.chatId))
            }
            val tvLastMESSAGE = rootView.findViewById<JoshTextView>(R.id.tv_detail_last)
            var imageUrl: String? = null
            var text: String? = linkObj.title

            if (linkObj.material_type != BASE_MESSAGE_TYPE.TX) {
                when (linkObj.material_type) {
                    BASE_MESSAGE_TYPE.IM -> {
                        tvLastMESSAGE.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_image,
                            0,
                            0,
                            0
                        )
                        imageUrl = linkObj.imageList?.getOrNull(0)?.imageUrl
                    }
                    BASE_MESSAGE_TYPE.AU -> {
                        tvLastMESSAGE.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_inbox_audio,
                            0,
                            0,
                            0
                        )
                        val duration =
                            PlayerUtil.toTimeSongString(linkObj.audioList?.getOrNull(0)?.duration)
                        text = "Voice Message "
                        if (duration.isNullOrEmpty().not()) {
                            text += "($duration)"
                        }
                    }
                    BASE_MESSAGE_TYPE.VI -> {
                        tvLastMESSAGE.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_inbox_video,
                            0,
                            0,
                            0
                        )
                        imageUrl = linkObj.videoList?.getOrNull(0)?.video_image_url
                        val duration =
                            PlayerUtil.toTimeSongString(linkObj.videoList?.getOrNull(0)?.duration)
                        if (duration.isNullOrEmpty().not()) {
                            text += "($duration)"
                        }

                    }
                    else -> text = linkObj.title
                }
                if (imageUrl.isNullOrEmpty().not()) {
                    val imageLastView =
                        rootView.findViewById<AppCompatImageView>(R.id.iv_detail_last)
                    setDefaultImageView(imageLastView, imageUrl!!)
                    imageLastView.visibility = View.VISIBLE
                }
            }
            if (text.isNullOrEmpty().not()) {
                tvLastMESSAGE.text = text
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    protected fun highlightedViewForSomeTime(view: FrameLayout) {
        try {
            view.background =
                ColorDrawable(ContextCompat.getColor(getAppContext(), R.color.forground_bg))


            AppObjectController.uiHandler.postDelayed({
                sId = EMPTY
                view.background =
                    ColorDrawable(ContextCompat.getColor(getAppContext(), R.color.transparent))
            }, 1200)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun onViewInflated() {
        RxBus2.publish(message)
    }

    abstract fun getRoot(): FrameLayout
}