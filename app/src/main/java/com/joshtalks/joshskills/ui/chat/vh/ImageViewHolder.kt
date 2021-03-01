package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.extension.setImageViewPH
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent

class ImageViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {
    lateinit var rootView: FrameLayout
    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)
    val messageView: ViewGroup = view.findViewById(R.id.message_view)
    val messageBody: JoshTextView = view.findViewById(R.id.text_message_body)
    val textMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)
    val imageView: AppCompatImageView = view.findViewById(R.id.image_view)
    private var message: ChatModel? = null

    init {
        imageView.also {
            it.setOnClickListener {
                if (message?.downloadedLocalPath != null && message?.downloadedLocalPath?.isNotEmpty()!!) {
                    RxBus2.publish(ImageShowEvent(message?.downloadedLocalPath, message?.url))
                } else if (message?.url != null) {
                    RxBus2.publish(ImageShowEvent(message?.downloadedLocalPath, message?.url))
                } else {
                    message?.question?.imageList?.get(0)?.imageUrl?.let {
                        RxBus2.publish(
                            ImageShowEvent(
                                message?.question?.imageList?.get(0)?.downloadedLocalPath,
                                it,
                                message?.question?.imageList?.get(0)?.id
                            )
                        )
                    }
                }
            }
        }

    }

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        this.message = message
        if (null != message.sender) {
            setViewHolderBG(previousMessage?.sender, message.sender!!, rootSubView)
        }
        message.text = EMPTY
        messageBody.text = EMPTY

        if (message.url != null) {
            Utils.fileUrl(message.downloadedLocalPath, message.url)?.run {
                imageView.setImageViewPH(this)
            }
        } else {
            message.question?.imageList?.getOrNull(0)?.let { imageObj ->
                Utils.fileUrl(imageObj.downloadedLocalPath!!, imageObj.imageUrl)?.run {
                    imageView.setImageViewPH(this)
                }
            }
        }

        message.question?.let { question ->
            if (question.qText.isNullOrEmpty().not()) {
                messageBody.text =
                    HtmlCompat.fromHtml(question.qText ?: EMPTY, HtmlCompat.FROM_HTML_MODE_LEGACY)
                messageBody.visibility = VISIBLE
            } else {
                if (message.text.isNullOrEmpty().not()) {
                    messageBody.text = HtmlCompat.fromHtml(
                        question.qText ?: EMPTY,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    messageBody.visibility = VISIBLE
                }
            }
        }
        textMessageTime.text = Utils.messageTimeConversion(message.created)
        addMessageAutoLink(messageBody)
        addDrawableOnTime(message, textMessageTime)
    }

    override fun unBind() {

    }
}