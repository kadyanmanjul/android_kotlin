package com.joshtalks.joshskills.ui.view_holders

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.pnikosis.materialishprogress.ProgressWheel
import java.lang.ref.WeakReference

@Layout(R.layout.image_view_holder)
class ImageViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.text_message_body)
    lateinit var textMessageBody: JoshTextView


    @View(R.id.text_message_time)
    lateinit var textMessageTime: AppCompatTextView


    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var rootSubView: FrameLayout


    @View(R.id.message_view)
    lateinit var messageView: ViewGroup


    @View(R.id.download_container)
    lateinit var downloadContainer: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var ivCancelDownload: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var ivStartDownload: AppCompatImageView

    @View(R.id.progress_dialog)
    lateinit var progressDialog: ProgressWheel

    lateinit var imageViewHolder: ImageViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        ivCancelDownload.visibility = GONE
        imageViewHolder = this
        message.sender?.let {
            updateView(it, rootView, rootSubView, messageView)
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
                download(message.url!!)
            } else {
                Utils.fileUrl(message.downloadedLocalPath, message.url)?.run {
                    setImageView(imageView, this)
                }
            }
        } else {
            message.question?.imageList?.getOrNull(0)?.let { imageObj ->
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                    fileDownloadRunView()
                    download(imageObj.imageUrl)
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

    private fun download(url: String) {
        DownloadUtils.downloadImage(
            this,
            message,
            url,
            AppDirectory.imageReceivedFile().absolutePath
        )

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


    @Click(R.id.image_view)
    fun onClick() {
        if (message.downloadedLocalPath != null && message.downloadedLocalPath?.isNotEmpty()!!) {
            RxBus2.publish(ImageShowEvent(message.downloadedLocalPath, message.url))
        } else if (message.url != null) {
            RxBus2.publish(ImageShowEvent(message.downloadedLocalPath, message.url))
        } else {
            message.question?.imageList?.get(0)?.imageUrl?.let {
                RxBus2.publish(
                    ImageShowEvent(
                        message.question?.imageList?.get(0)?.downloadedLocalPath,
                        it,
                        message.question?.imageList?.get(0)?.id
                    )
                )
            }
        }
    }


    @Click(R.id.download_container)
    fun downloadStart() {
        RxBus2.publish(DownloadMediaEventBus(this, message))
        AppAnalytics.create(AnalyticsEvent.IMAGE_DOWNLOAD.NAME).addParam("ChatId", message.chatId)
    }

    @Click(R.id.iv_cancel_download)
    fun downloadCancel() {
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    @Click(R.id.iv_start_download)
    fun downloadStart1() {
        RxBus2.publish(DownloadMediaEventBus(this, message))
    }
}



