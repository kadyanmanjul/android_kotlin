package com.joshtalks.joshskills.ui.view_holders

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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
import timber.log.Timber
import java.lang.ref.WeakReference

@Layout(R.layout.pdf_view_holder)
class PdfViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var rootSubView: FrameLayout

    @View(R.id.message_view)
    lateinit var messageView: ViewGroup

    @View(R.id.tv_pdf_info)
    lateinit var tvPdfInfo: TextView

    @View(R.id.text_message_time)
    lateinit var receivedMessageTime: AppCompatTextView

    @View(R.id.tv_message_detail)
    lateinit var messageDetail: AppCompatTextView

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.iv_cancel_download)
    lateinit var ivCancelDownload: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var ivStartDownload: AppCompatImageView

    @View(R.id.progress_dialog)
    lateinit var progressDialog: ProgressWheel

    lateinit var pdfViewHolder: PdfViewHolder
    private var eta = 0L

    private lateinit var appAnalytics: AppAnalytics


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
            appAnalytics.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Cancelled").push()

        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            eta = System.currentTimeMillis() - eta
            if (eta >= 10000000)
                eta = 500
            appAnalytics.addParam(AnalyticsEvent.TIME_TAKEN_DOWNLOAD.NAME, eta)
            appAnalytics.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Completed").push()
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
            appAnalytics.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Failed error").push()

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
            eta = System.currentTimeMillis()
            Timber.tag("ETA").e("ETA STArted $eta")

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        pdfViewHolder = this
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)
        updateTime(receivedMessageTime)

        message.parentQuestionObject?.run {
            addLinkToTagMessage(messageView, this, message.sender)
        }
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }

        message.sender?.let {
            updateView(it, rootView, rootSubView, messageView)
        }

        message.question?.run {
            this.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    pdfObj.pages?.let {
                        messageDetail.text = getAppContext().getString(R.string.pdf_desc, it)
                    }
                    Uri.parse(pdfObj.url).let {
                        tvPdfInfo.text = it.pathSegments[it.pathSegments.size - 1].split(".")[0]
                    }
                    Utils.fileUrl(pdfObj.thumbnail, pdfObj.thumbnail)?.run {
                        setImageInImageView(imageView, this)
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

        appAnalytics = AppAnalytics.create(AnalyticsEvent.PDF_VH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("ChatId", message.chatId)
    }

    private fun fileDownloadSuccess() {
        ivStartDownload.visibility = android.view.View.GONE
        progressDialog.visibility = android.view.View.GONE
        ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileNotDownloadView() {
        appAnalytics.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "Not downloaded")
        ivStartDownload.visibility = android.view.View.VISIBLE
        progressDialog.visibility = android.view.View.GONE
        ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileDownloadingInProgressView() {
        ivStartDownload.visibility = android.view.View.GONE
        progressDialog.visibility = android.view.View.VISIBLE
        ivCancelDownload.visibility = android.view.View.VISIBLE
    }

    @Click(R.id.container_fl)
    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnable(activityRef.get()!!).not()) {
            PermissionUtils.storageReadAndWritePermission(activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                appAnalytics.addParam(
                                    AnalyticsEvent.PDF_VIEW_STATUS.NAME,
                                    "pdf Opened"
                                ).push()
                                openPdf()
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
            return
        }
        openPdf()
    }

    private fun openPdf() {
        message.question?.pdfList?.getOrNull(0)?.let { pdfObj ->
            if (pdfObj.url.isBlank()) {
                appAnalytics.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "pdf url Blank").push()
                return
            }
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED && AppDirectory.isFileExist(
                    pdfObj.downloadedLocalPath
                )
            ) {
                RxBus2.publish(PdfOpenEventBus(pdfObj))
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

    override fun getRoot(): FrameLayout {
        return rootView
    }
}