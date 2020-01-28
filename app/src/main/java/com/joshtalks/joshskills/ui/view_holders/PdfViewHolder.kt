package com.joshtalks.joshskills.ui.view_holders

import android.net.Uri
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PdfOpenEventBus
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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

@Layout(R.layout.pdf_view_holder)
class PdfViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    var context = AppObjectController.joshApplication


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout

    @View(R.id.message_view)
    lateinit var message_view: android.view.View

    @View(R.id.tv_pdf_info)
    lateinit var tv_pdf_info: TextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.tv_message_detail)
    lateinit var messageDetail: AppCompatTextView


    @View(R.id.image_view)
    lateinit var image_view: AppCompatImageView


    @View(R.id.download_container)
    lateinit var download_container: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var iv_cancel_download: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var iv_start_download: AppCompatImageView


    @View(R.id.progress_dialog)
    lateinit var progress_dialog: ProgressWheel

    @View(R.id.ll_container)
    lateinit var ll_container: RelativeLayout


    lateinit var pdfViewHolder: PdfViewHolder


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {

        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            CoroutineScope(Dispatchers.IO).launch {
                DownloadUtils.updateDownloadStatus(download.file, download.extras).let {
                    RxBus2.publish(DownloadCompletedEventBus(pdfViewHolder, message))
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
    override fun onViewInflated() {
        super.onViewInflated()
        pdfViewHolder = this
        text_message_time.text = Utils.messageTimeConversion(message.created)
        updateTime(text_message_time)

        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }

        message.question?.run {
            this.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    pdfObj.pages?.let {
                        messageDetail.text = context.getString(R.string.pdf_desc, it)
                    }
                    Uri.parse(pdfObj.url).let {
                        tv_pdf_info.text = it.pathSegments[it.pathSegments.size - 1].split(".")[0]
                    }
                    Utils.fileUrl(pdfObj.thumbnail, pdfObj.thumbnail)?.run {
                        setImageInImageView(image_view, this)
                    }

                    if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        fileDownloadingInProgressView()
                        download(pdfObj.url)
                    } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED && AppDirectory.isFileExist(
                            pdfObj.downloadedLocalPath
                        )
                    ) {
                        fileDownloadSuccess()
                    } else {
                        fileNotDownloadView()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun fileDownloadSuccess() {
        iv_start_download.visibility = android.view.View.GONE
        progress_dialog.visibility = android.view.View.GONE
        iv_cancel_download.visibility = android.view.View.GONE
    }

    private fun fileNotDownloadView() {
        iv_start_download.visibility = android.view.View.VISIBLE
        progress_dialog.visibility = android.view.View.GONE
        iv_cancel_download.visibility = android.view.View.GONE

    }

    private fun fileDownloadingInProgressView() {
        iv_start_download.visibility = android.view.View.GONE
        progress_dialog.visibility = android.view.View.VISIBLE
        iv_cancel_download.visibility = android.view.View.VISIBLE

    }


    @Click(R.id.container_fl)
    fun onClickPdfContainer() {
        message.question?.pdfList?.get(0)?.let { pdfObj ->
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED && AppDirectory.isFileExist(
                    pdfObj.downloadedLocalPath
                )
            ) {
                PermissionUtils.storageReadAndWritePermission(activityRef.get(),
                    object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            report?.areAllPermissionsGranted()?.let { flag ->
                                if (flag) {
                                    RxBus2.publish(PdfOpenEventBus(pdfObj))
                                    return
                                }
                                if (report.isAnyPermissionPermanentlyDenied) {
                                    PermissionUtils.permissionPermanentlyDeniedDialog(
                                        activityRef.get()!!
                                    )
                                    return
                                }
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }
                    })
            } else {
                RxBus2.publish(DownloadMediaEventBus(pdfViewHolder, message))
            }
        }
    }


    @Click(R.id.iv_cancel_download)
    fun downloadCancel() {
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    @Click(R.id.iv_start_download)
    fun downloadStart() {
        if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
            return
        }
        RxBus2.publish(DownloadMediaEventBus(this, message))
    }

    private fun download(url: String) {
        DownloadUtils.downloadFile(
            url,
            AppDirectory.docsReceivedFile(url).absolutePath,
            message.chatId,
            message,
            downloadListener
        )

    }

}