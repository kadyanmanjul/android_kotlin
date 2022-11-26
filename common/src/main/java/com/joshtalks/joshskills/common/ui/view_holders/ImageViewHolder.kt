package com.joshtalks.joshskills.common.ui.view_holders

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.common.repository.local.eventbus.ImageShowEvent
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.pnikosis.materialishprogress.ProgressWheel
import java.lang.ref.WeakReference


class ImageViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel,previousMessage:ChatModel?) :
    BaseChatViewHolder(activityRef, message,previousMessage) {


    lateinit var imageView: AppCompatImageView


    lateinit var textMessageBody: JoshTextView



    lateinit var textMessageTime: AppCompatTextView



    lateinit var rootView: FrameLayout


    lateinit var rootSubView: FrameLayout



    lateinit var messageView: ViewGroup



    lateinit var downloadContainer: FrameLayout


    lateinit var ivCancelDownload: AppCompatImageView


    lateinit var ivStartDownload: AppCompatImageView


    lateinit var progressDialog: ProgressWheel

    lateinit var imageViewHolder: ImageViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        ivCancelDownload.visibility = GONE
        imageViewHolder = this
        message.sender?.let {
            setViewHolderBG(previousMessage?.sender,it, rootView, rootSubView, messageView)
        }
        message.parentQuestionObject?.run {
            addLinkToTagMessage(messageView, this, message.sender)
        }
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

        textMessageBody.visibility = GONE

        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                fileDownloadRunView()
               // download(message.url!!)
            } else {
                Utils.fileUrl(message.downloadedLocalPath, message.url)?.run {
                    setImageView(imageView, this)
                }
            }
        } else {
            message.question?.imageList?.getOrNull(0)?.let { imageObj ->
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                    fileDownloadRunView()
                    // download(imageObj.imageUrl)
                } else {
                    if (imageView.tag != null) {
                        if (imageView.tag.toString() != message.downloadedLocalPath) {
                            imageView.tag = null
                        }
                    }
                    Utils.fileUrl(imageObj.downloadedLocalPath!!, imageObj.imageUrl)?.run {
                        setImageView(imageView, this)
                    }
                }
            }
        }


        message.question?.let { question ->
            if (question.qText.isNullOrEmpty().not()) {
                textMessageBody.text = HtmlCompat.fromHtml(
                    question.qText!!,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                textMessageBody.visibility = VISIBLE
            } else {
                if (message.text.isNullOrEmpty().not()) {
                    textMessageBody.text = HtmlCompat.fromHtml(
                        question.qText!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textMessageBody.visibility = VISIBLE
                }
            }
        }

        textMessageTime.text = Utils.messageTimeConversion(message.created)
        updateTime(textMessageTime)
    }

    override fun getRoot(): FrameLayout {
        return rootView
    }

    private fun fileNotDownloadView() {
        // downloadContainer.visibility = VISIBLE
        progressDialog.visibility = GONE
        ivCancelDownload.visibility = GONE
        ivStartDownload.visibility = VISIBLE
    }

    private fun fileDownloadRunView() {
        // downloadContainer.visibility = VISIBLE
        progressDialog.visibility = VISIBLE
        ivCancelDownload.visibility = VISIBLE
        ivStartDownload.visibility = GONE
    }


    private fun setImageView(iv: AppCompatImageView, url: String) {
        setImageInImageView(iv, url)
    }


    
    fun onClick() {
        if (message.downloadedLocalPath != null && message.downloadedLocalPath?.isNotEmpty()!!) {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(ImageShowEvent(message.downloadedLocalPath, message.url))
        } else if (message.url != null) {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(ImageShowEvent(message.downloadedLocalPath, message.url))
        } else {
            message.question?.imageList?.get(0)?.imageUrl?.let {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(
                    ImageShowEvent(
                        message.question?.imageList?.get(0)?.downloadedLocalPath,
                        it,
                        message.question?.imageList?.get(0)?.id
                    )
                )
            }
        }
    }


    
    fun downloadStart() {
        //RxBus2.publish(DownloadMediaEventBus(this, message))
        AppAnalytics.create(AnalyticsEvent.IMAGE_DOWNLOAD.NAME).addParam("ChatId", message.chatId)
    }

    
    fun downloadCancel() {
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    
    fun downloadStart1() {
        // RxBus2.publish(DownloadMediaEventBus(this, message))
    }
}



