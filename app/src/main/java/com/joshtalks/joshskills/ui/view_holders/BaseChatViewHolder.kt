package com.joshtalks.joshskills.ui.view_holders

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.PlayerUtil
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.Sender
import com.joshtalks.joshskills.repository.local.eventbus.GotoChatEventBus
import java.lang.ref.WeakReference


abstract class BaseChatViewHolder(
    val activityRef: WeakReference<FragmentActivity>,
    var message: ChatModel, var previousMessage: ChatModel?
) : BaseCell() {

    companion object {
        var sId = EMPTY
    }

    protected fun getLeftPaddingForReceiver() = Utils.dpToPx(getAppContext(), 7f)
    protected fun getRightPaddingForReceiver() =
        Utils.dpToPx(getAppContext(), 80f)

    protected fun getMarginForReceiver() = Utils.dpToPx(getAppContext(), 0f)
    protected fun getLeftPaddingForSender() = Utils.dpToPx(getAppContext(), 80f)
    protected fun getRightPaddingForSender() = Utils.dpToPx(getAppContext(), 7f)
    protected fun getMarginForSender() = Utils.dpToPx(getAppContext(), 0f)


    fun setViewHolderBG(
        lSender: Sender?,
        cSender: Sender,
        rootView: FrameLayout,
        rootsubView: FrameLayout,
        messageView: ViewGroup?
    ) {
        if (lSender == null) {
            if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                setBgForOutgoingMessage(
                    R.drawable.outgoing_message_normal_bg,
                    rootView,
                    rootsubView,
                    messageView
                )
            } else {
                setBgForIncomingMessage(
                    R.drawable.incoming_message_normal_bg,
                    rootView,
                    rootsubView,
                    messageView
                )
            }
        } else {
            if (lSender.id == cSender.id || lSender.id == getUserId()) { // no balloon bg
                if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                    setBgForOutgoingMessage(
                        R.drawable.outgoing_message_same_bg,
                        rootView,
                        rootsubView,
                        messageView
                    )
                } else {
                    setBgForIncomingMessage(
                        R.drawable.incoming_message_same_bg,
                        rootView,
                        rootsubView,
                        messageView
                    )
                }
            } else { // balloon bg
                if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                    setBgForOutgoingMessage(
                        R.drawable.outgoing_message_normal_bg,
                        rootView,
                        rootsubView,
                        messageView
                    )
                } else {
                    setBgForIncomingMessage(
                        R.drawable.incoming_message_normal_bg,
                        rootView,
                        rootsubView,
                        messageView
                    )
                }
            }
        }
    }

    fun getViewHolderBGResource(lSender: Sender?, cSender: Sender?): Int {
        if (cSender == null) {
            return R.drawable.incoming_message_same_bg
        }
        if (lSender == null) {
            return if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                R.drawable.outgoing_message_normal_bg
            } else {
                R.drawable.incoming_message_normal_bg
            }
        } else {
            return if (lSender.id == cSender.id || lSender.id == getUserId()) { // no balloon bg
                if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                    R.drawable.outgoing_message_same_bg
                } else {
                    R.drawable.incoming_message_same_bg
                }
            } else { // balloon bg
                if (cSender.id.equals(getUserId(), ignoreCase = true)) {
                    R.drawable.outgoing_message_normal_bg
                } else {
                    R.drawable.incoming_message_normal_bg
                }
            }
        }
    }

    private fun setBgForOutgoingMessage(
        resourceId: Int, rootView: FrameLayout,
        rootSubView: FrameLayout,
        messageView: ViewGroup?
    ) {
        rootView.setPadding(getLeftPaddingForSender(), 0, getRightPaddingForSender(), 0)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.END
        rootSubView.layoutParams = params
        rootSubView.setBackgroundResource(resourceId)
        val paramsMessage = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        paramsMessage.setMargins(0, 0, getMarginForSender(), 0)
        messageView?.layoutParams = paramsMessage
    }

    private fun setBgForIncomingMessage(
        resourceId: Int, rootView: FrameLayout,
        rootSubView: FrameLayout,
        messageView: ViewGroup?
    ) {
        rootView.setPadding(getLeftPaddingForReceiver(), 0, getRightPaddingForReceiver(), 0)

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.START
        rootSubView.layoutParams = params
        rootSubView.setBackgroundResource(resourceId)
        val paramsMessage = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        paramsMessage.setMargins(getMarginForReceiver(), 0, 0, 0)
        messageView?.layoutParams = paramsMessage
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

    fun addMessageAutoLink(textMessageBody: JoshTextView) {
        textMessageBody.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_PHONE -> Utils.call(getAppContext(), matchedText)
                AutoLinkMode.MODE_URL -> activityRef.get()?.let { Utils.openUrl(matchedText, it) }
                else -> {

                }
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    fun addLinkToTagMessage(
        rootView: ViewGroup,
        linkObj: Question,
        sender: Sender?
    ) {
        try {
            rootView.findViewById<ViewGroup>(R.id.tag_view).visibility = View.VISIBLE
            rootView.findViewById<ViewGroup>(R.id.tag_view).setOnClickListener {
                RxBus2.publish(GotoChatEventBus(linkObj.chatId))
            }
            rootView.findViewById<ViewGroup>(R.id.sub_rl).setOnClickListener {
                RxBus2.publish(GotoChatEventBus(linkObj.chatId))
            }
            val tvSenderName = rootView.findViewById<JoshTextView>(R.id.tv_sender_name)
            sender?.user?.run {
                tvSenderName.text = this.first_name.plus(" ".plus(this.last_name))
            }

            val tvLastMESSAGE = rootView.findViewById<AppCompatTextView>(R.id.tv_detail_last)
            var imageUrl: String? = null
            val text: StringBuilder? = StringBuilder(getAppContext().getString(R.string.practice))
            linkObj.practiceNo?.run {
                text?.append(" #$this")
            }

            tvLastMESSAGE.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_assignment,
                0,
                0,
                0
            )


            if (linkObj.material_type != BASE_MESSAGE_TYPE.TX) {
                when (linkObj.material_type) {
                    BASE_MESSAGE_TYPE.IM -> {
                        tvLastMESSAGE.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_image,
                            0,
                            0,
                            0
                        )
                        text?.append(getAppContext().getString(R.string.photo))
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
                        text?.append(getAppContext().getString(R.string.voice_message))
                        if (duration.isNullOrEmpty().not()) {
                            text?.append("($duration)")
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
                        text?.append(getAppContext().getString(R.string.video))
                        if (duration.isNullOrEmpty().not()) {
                            text?.append("($duration)")
                        }

                    }

                    else -> {

                    }
                }
                if (imageUrl.isNullOrEmpty().not()) {
                    val imageLastView =
                        rootView.findViewById<AppCompatImageView>(R.id.iv_detail_last)
                    setDefaultImageView(imageLastView, imageUrl!!)
                    imageLastView.visibility = View.VISIBLE
                }
            }
            if (text.isNullOrEmpty().not()) {
                tvLastMESSAGE.text = text.toString()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    protected fun highlightedViewForSomeTime(view: FrameLayout) {
        try {
            view.background =
                ColorDrawable(ContextCompat.getColor(getAppContext(), R.color.forground_bg))
            val colorFrom: Int = Color.parseColor("#AA34B7F1")
            val colorTo: Int = Color.TRANSPARENT
            val duration = 1000L

            val animate = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                ArgbEvaluator(),
                colorFrom,
                colorTo
            )
            animate.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    sId = EMPTY
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }

            })
            animate.startDelay = 500
            animate.duration = duration
            animate.start()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun onViewInflated() {
        RxBus2.publish(message)
    }

    abstract fun getRoot(): FrameLayout
}