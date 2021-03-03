package com.joshtalks.joshskills.ui.chat.vh

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.ShimmerImageView
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.extension.setImageViewPH
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PdfOpenEventBus
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pnikosis.materialishprogress.ProgressWheel
import java.lang.ref.WeakReference

class PdfViewHolder(
    view: View,
    private val activityRef: WeakReference<FragmentActivity>,
    userId: String
) : BaseViewHolder(view, userId) {

    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)
    val messageView: ViewGroup = view.findViewById(R.id.message_view)
    val receivedMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)
    val tvPdfInfo: AppCompatTextView = view.findViewById(R.id.tv_pdf_info)
    val messageDetail: JoshTextView = view.findViewById(R.id.tv_message_detail)
    val imageView: ShimmerImageView = view.findViewById(R.id.image_view)

    private val downloadContainer: FrameLayout = view.findViewById(R.id.download_container)
    private val ivCancelDownload: AppCompatImageView = view.findViewById(R.id.iv_cancel_download)
    private val ivStartDownload: AppCompatImageView = view.findViewById(R.id.iv_start_download)
    private val progressDialog: ProgressWheel = view.findViewById(R.id.progress_dialog)
    private var eta = 0L
    private var appAnalytics: AppAnalytics? = null
    private var message: ChatModel? = null

    init {
        imageView.also { it ->
            it.setOnClickListener {
                onClickPdfContainer()
            }
        }
        ivStartDownload.also { it ->
            it.setOnClickListener {
                onClickPdfContainer()
            }
        }

    }

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        this.message = message
        if (null != message.sender) {
            setViewHolderBG(previousMessage?.sender, message.sender!!, rootSubView)
        }
        message.text = EMPTY
        messageDetail.text = EMPTY
        receivedMessageTime.text = Utils.messageTimeConversion(message.created)

        message.question?.run {
            this.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    pdfObj.pages?.let {
                        messageDetail.text = getAppContext().getString(R.string.pdf_desc, it)
                    }
                    Uri.parse(pdfObj.url).let {
                        tvPdfInfo.text = it.pathSegments[it.pathSegments.size - 1].split(".")[0]
                    }
                    pdfObj.thumbnail?.let {
                        imageView.setImageViewPH(it, placeholderImage = R.drawable.image_ph)
                    }

                    if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        fileDownloadingInProgressView()
                    } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
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
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
    }

    private fun fileNotDownloadView() {
        appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "Not downloaded")
        ivStartDownload.visibility = View.VISIBLE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = View.VISIBLE
        ivCancelDownload.visibility = View.VISIBLE
    }

    private fun openPdf() {
        message?.let {
            it.question?.pdfList?.getOrNull(0)?.let { pdfObj ->
                if (pdfObj.url.isBlank()) {
                    appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "pdf url Blank")
                        ?.push()
                    return
                }
                JoshSkillExecutors.BOUNDED.submit {
                    val dStatus = message?.downloadStatus ?: DOWNLOAD_STATUS.NOT_START
                    val exist = AppDirectory.isFileExist(pdfObj.downloadedLocalPath)
                    if ((dStatus == DOWNLOAD_STATUS.DOWNLOADED).not() || exist.not()) {
                        RxBus2.publish(
                            DownloadMediaEventBus(
                                DOWNLOAD_STATUS.REQUEST_DOWNLOADING,
                                it.chatId,
                                url = pdfObj.url,
                                type = BASE_MESSAGE_TYPE.PD,
                                chatModel = it
                            )
                        )
                        return@submit
                    }
                    RxBus2.publish(PdfOpenEventBus(it.chatId, pdfObj))
                }
            }
        }
    }


    fun downloadCancel() {
        fileNotDownloadView()
        message?.downloadStatus = DOWNLOAD_STATUS.NOT_START
    }

    fun downloadStart() {
        if (message?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
            return
        }
        //RxBus2.publish(DownloadMediaEventBus(this, message))
    }

    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnabled(activityRef.get()!!)) {
            openPdf()
        } else {
            PermissionUtils.storageReadAndWritePermission(
                activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                appAnalytics?.addParam(
                                    AnalyticsEvent.PDF_VIEW_STATUS.NAME,
                                    "pdf Opened"
                                )?.push()
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
    }


    override fun unBind() {

    }
}