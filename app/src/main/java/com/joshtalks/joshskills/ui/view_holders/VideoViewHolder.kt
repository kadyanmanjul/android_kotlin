package com.joshtalks.joshskills.ui.view_holders

import android.Manifest
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PdfOpenEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PlayVideoEvent
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
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


@Layout(R.layout.video_view_holder)
class VideoViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.image_view)
    lateinit var image_view: AppCompatImageView

    var context = AppObjectController.joshApplication


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout

    @View(R.id.message_view)
    lateinit var message_view: android.view.View

    @View(R.id.text_title)
    lateinit var text_title: TextView

    @View(R.id.text_message_body)
    lateinit var text_message_body: JoshTextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.download_container)
    lateinit var download_container: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var iv_cancel_download: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var iv_start_download: AppCompatImageView


    @View(R.id.progress_dialog)
    lateinit var progress_dialog: ProgressWheel

    @View(R.id.ll_container)
    lateinit var ll_container: LinearLayout


    lateinit var videoViewHolder: VideoViewHolder


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {

        }

        override fun onCompleted(download: Download) {

            DownloadUtils.removeCallbackListener(download.tag)
            CoroutineScope(Dispatchers.IO).launch {
                DownloadUtils.updateDownloadStatus(download.file, download.extras).let {
                    RxBus2.publish(DownloadCompletedEventBus(videoViewHolder, message))
                }
            }


        }

        override fun onDeleted(download: Download) {

        }

        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int
        ) {

        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {

        }

        override fun onPaused(download: Download) {

        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {

        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {

        }

        override fun onRemoved(download: Download) {

        }

        override fun onResumed(download: Download) {

        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }


    @Resolve
    fun onResolved() {
        videoViewHolder = this
        download_container.visibility = GONE

        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }


        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath!!)) {
                    Dexter.withActivity(activityRef.get())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {

                                setImageInImageView(image_view, message.downloadedLocalPath!!);


                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest,
                                token: PermissionToken
                            ) {

                            }
                        }).check()
                } else {
                    fileNotDownloadView()

                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
                fileDownloadingInProgressView()
                setImageInImageView(image_view, message.downloadedLocalPath!!);

            } else {

            }
        } else {
            message.question?.let { question ->

                question.videoList?.get(0)?.let { videoObj ->
                    if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {

                        if (AppDirectory.isFileExist(videoObj.downloadedLocalPath!!)) {
                            Dexter.withActivity(activityRef.get())
                                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                .withListener(object : PermissionListener {
                                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                        fileDownloadSuccess()
                                        if (image_view.tag != null) {
                                            if (image_view.tag.toString() != message.downloadedLocalPath) {
                                                image_view.tag = null
                                            }
                                        }
                                        setImageView(image_view, videoObj.video_image_url, false)
                                    }

                                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                                        fileNotDownloadView()
                                        setImageView(image_view, videoObj.video_image_url, true)

                                    }

                                    override fun onPermissionRationaleShouldBeShown(
                                        permission: PermissionRequest,
                                        token: PermissionToken
                                    ) {
                                        fileNotDownloadView()
                                        setImageView(image_view, videoObj.video_image_url, true)


                                    }
                                }).check()
                        } else {
                            fileNotDownloadView()
                            setImageView(image_view, videoObj.video_image_url, true)


                        }

                    } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        setImageView(image_view, videoObj.video_image_url, true)

                        videoObj.video_url?.let {
                            fileDownloadingInProgressView()
                            download(it)
                        }

                    } else {
                        fileNotDownloadView()
                        setImageView(image_view, videoObj.video_image_url, true)

                    }
                }
            }

            message.question?.let { question ->

                question.title?.isNotEmpty().let {
                    text_title.text = question.title
                    text_title.visibility = VISIBLE

                }
                question.qText?.isNotEmpty().let {
                    text_message_body.text = HtmlCompat.fromHtml(question.qText.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                    text_message_body.visibility = VISIBLE

                }
            }


        }


        updateTime(text_message_time)
        addMessageAutoLink(text_message_body)

    }

    fun fileDownloadSuccess() {
        return
        download_container.visibility = VISIBLE
        iv_start_download.visibility = GONE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE

    }

    fun fileNotDownloadView() {
        return
        download_container.visibility = VISIBLE
        iv_start_download.visibility = VISIBLE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE

    }

    private fun fileDownloadingInProgressView() {

        download_container.visibility = VISIBLE
        iv_start_download.visibility = GONE
        progress_dialog.visibility = VISIBLE
        iv_cancel_download.visibility = VISIBLE

    }


    private fun download(url: String) {

        // ExoPlayerDownloader.download(activityRef.get(),url)
        /* DownloadUtils.downloadFile(
             url,
             AppDirectory.videoReceivedFile().absolutePath,
             message.chatId,
             message,
             downloadListener
         )*/

    }


    private fun setImageView(iv: AppCompatImageView, url: String, blur: Boolean) {
        if (blur) {
            setBlurImageInImageView(iv, url);
        } else {
            setImageInImageView(iv, url);
        }

    }

    @Click(R.id.video_container_fl)
    fun onClick() {

        RxBus2.publish(PlayVideoEvent(message))
/*

        message.question?.pdfList?.get(0)?.let { pdfObj ->
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(pdfObj.downloadedLocalPath!!)) {
                    Dexter.withActivity(activityRef.get())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                RxBus2.publish(PlayVideoEvent(message))
                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                                RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest,
                                token: PermissionToken
                            ) {
                                RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))

                            }
                        }).check()
                } else {

                    RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))

                }
            } else {
                RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))
            }
        }
*/
    }


    @Click(R.id.download_container)
    fun downloadStart() {
        RxBus2.publish(DownloadMediaEventBus(this, message))
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