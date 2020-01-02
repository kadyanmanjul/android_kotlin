package com.joshtalks.joshskills.ui.view_holders

import android.Manifest
import android.view.View.GONE
import android.view.View.VISIBLE
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
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
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
    lateinit var image_view: AppCompatImageView


    @View(R.id.parent_layout)
    lateinit var parent_layout: android.view.View


    @View(R.id.text_message_body)
    lateinit var text_message_body: JoshTextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout

    @View(R.id.message_view)
    lateinit var message_view: FrameLayout


    @View(R.id.download_container)
    lateinit var download_container: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var iv_cancel_download: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var iv_start_download: AppCompatImageView


    @View(R.id.progress_dialog)
    lateinit var progress_dialog: ProgressWheel

    lateinit var imageViewHolder: ImageViewHolder

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        download_container.visibility = GONE
        imageViewHolder = this
        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }
       // text_message_body.setShadowLayer(1F, 0F, 0F, Color.RED);
        text_message_body.visibility = GONE


        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath!!)) {
                    Dexter.withActivity(activityRef.get())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                if (image_view.tag != null) {
                                    if (image_view.tag.toString() != message.downloadedLocalPath) {
                                        image_view.tag = null
                                    }
                                }
                                setImageView(image_view, message.downloadedLocalPath!!, false)
                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                                setImageView(image_view, message.url!!, true)
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest,
                                token: PermissionToken
                            ) {

                            }
                        }).check()
                } else {
                    setImageViewImageNotFound(image_view)
                    message.disable=true
                    //   fileNotDownloadView()
                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                fileDownloadRunView()
                download(message.url!!)
            } else {
                setImageViewImageNotFound(image_view)
                message.disable=true

               // setImageView(image_view, message.url!!, true)
               // fileNotUploadingView()

            }
        } else {
            message.question?.imageList?.get(0)?.let { imageObj ->
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {

                    if (AppDirectory.isFileExist(imageObj.downloadedLocalPath!!)) {
                        Dexter.withActivity(activityRef.get())
                            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            .withListener(object : PermissionListener {
                                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                    if (image_view.tag != null) {
                                        if (image_view.tag.toString() != message.downloadedLocalPath) {
                                            image_view.tag = null
                                        }
                                    }
                                    setImageView(image_view, imageObj.downloadedLocalPath!!, false)
                                }

                                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                                    setImageView(image_view, imageObj.imageUrl, true)
                                }

                                override fun onPermissionRationaleShouldBeShown(
                                    permission: PermissionRequest,
                                    token: PermissionToken
                                ) {
                                }
                            }).check()
                    } else {
                        // fileNotDownloadView()
                        setImageView(image_view, imageObj.imageUrl, false)
                    }

                } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                    // fileDownloadRunView()
                    download(imageObj.imageUrl)
                } else {
                    setImageView(image_view, imageObj.imageUrl, false)
                    // fileNotDownloadView()

                }
            }
        }


        message.question?.let { question ->
            if (question.qText.isNullOrEmpty().not()) {
                text_message_body.text = HtmlCompat.fromHtml(
                    question.qText!!,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                text_message_body.visibility = VISIBLE
            }else{
                if (message.text.isNullOrEmpty().not()){
                    text_message_body.text = HtmlCompat.fromHtml(
                        question.qText!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    text_message_body.visibility = VISIBLE
                }
            }
        }

        text_message_time.text = Utils.messageTimeConversion(message.created)
        updateTime(text_message_time)
    }

    private fun download(url: String) {
        DownloadUtils.downloadImage(
            this,
            message,
            url,
            AppDirectory.imageReceivedFile().absolutePath
        )

    }


    private fun fileNotUploadingView() {
        download_container.visibility = VISIBLE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE
        iv_start_download.visibility = VISIBLE
        iv_start_download.setImageResource(R.drawable.ic_file_upload)
    }

    private fun fileNotDownloadView() {
        download_container.visibility = VISIBLE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE
        iv_start_download.visibility = VISIBLE
    }

    private fun fileDownloadRunView() {
        download_container.visibility = VISIBLE
        progress_dialog.visibility = VISIBLE
        iv_cancel_download.visibility = VISIBLE
        iv_start_download.visibility = GONE
    }


    private fun setImageView(iv: AppCompatImageView, url: String, blur: Boolean) {
        if (blur) {
            setBlurImageInImageView(iv, url);
        } else {
            setImageInImageView(iv, url);
        }

    }


    @Click(R.id.image_view)
    fun onClick() {
        if (message.disable) {
            return
        }

        if (message.downloadedLocalPath != null && message.downloadedLocalPath?.isNotEmpty()!!) {
            RxBus2.publish(ImageShowEvent(message.downloadedLocalPath))
        } else if (message.url != null) {
            RxBus2.publish(ImageShowEvent(message.url))
        } else {
            message.question?.imageList?.get(0)?.imageUrl?.let {
                RxBus2.publish(ImageShowEvent(it, message.question?.imageList?.get(0)?.id))
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



