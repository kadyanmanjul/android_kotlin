package com.joshtalks.joshskills.ui.day_wise_course.grammar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.databinding.FragmentGrammarLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.PdfOpenEventBus
import com.joshtalks.joshskills.ui.day_wise_course.fragments.PRACTISE_OBJECT
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.Click
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import java.util.ArrayList

class GrammarFragment : Fragment() {

    private var appAnalytics: AppAnalytics? = null
    private var chatModelList: ArrayList<ChatModel>? = null
    lateinit var binding: FragmentGrammarLayoutBinding

    companion object {
        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = GrammarFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
//            appAnalytics?.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Cancelled")?.push()

        }

        override fun onCompleted(download: Download) {
            /* DownloadUtils.removeCallbackListener(download.tag)
             eta = System.currentTimeMillis() - eta
             if (eta >= 10000000)
                 eta = 500
             appAnalytics?.addParam(AnalyticsEvent.TIME_TAKEN_DOWNLOAD.NAME, eta)
             appAnalytics?.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Completed")?.push()
             DownloadUtils.updateDownloadStatus(download.file, download.extras).let {
                 RxBus2.publish(DownloadCompletedEventBus(pdfViewHolder, message))
             }*/
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
//            appAnalytics?.addParam(AnalyticsEvent.PDF_DOWNLOAD_STATUS.NAME, "Failed error")?.push()

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
//            eta = System.currentTimeMillis()
//            Timber.tag("ETA").e("ETA STArted $eta")

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList<ChatModel>(PRACTISE_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_grammar_layout, container, false)
        binding.handler = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PDF_VH.NAME)
            .addBasicParam()
            .addUserDetails()
//            .addParam("ChatId", message.chatId)
    }

    private fun setupUi() {


    }

    private fun setUpPdfView(message: ChatModel) {
        message.question?.run {
            this.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
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
        binding.ivStartDownload.visibility = android.view.View.GONE
        binding.progressDialog.visibility = android.view.View.GONE
        binding.ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileNotDownloadView() {
        appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "Not downloaded")
        binding.ivStartDownload.visibility = android.view.View.VISIBLE
        binding.progressDialog.visibility = android.view.View.GONE
        binding.ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileDownloadingInProgressView() {
        binding.ivStartDownload.visibility = android.view.View.GONE
        binding.progressDialog.visibility = android.view.View.VISIBLE
        binding.ivCancelDownload.visibility = android.view.View.VISIBLE
    }


    @Click(R.id.image_view)
    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnabled(requireActivity()).not()) {
            PermissionUtils.storageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                appAnalytics?.addParam(
                                    AnalyticsEvent.PDF_VIEW_STATUS.NAME,
                                    "pdf Opened"
                                )?.push()
//                                openPdf()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    requireActivity()
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
//        openPdf()
    }

    private fun openPdf(message: ChatModel) {
        message.question?.pdfList?.getOrNull(0)?.let { pdfObj ->
            if (pdfObj.url.isBlank()) {
                appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "pdf url Blank")?.push()
                return
            }
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED && AppDirectory.isFileExist(
                    pdfObj.downloadedLocalPath
                )
            ) {
                RxBus2.publish(PdfOpenEventBus(message.chatId, pdfObj))
            } else {
//                RxBus2.publish(DownloadMediaEventBus(pdfViewHolder, message))
            }
        }

    }

    @Click(R.id.iv_cancel_download)
    fun downloadCancel() {
        fileNotDownloadView()
//        message.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    @Click(R.id.iv_start_download)
    fun downloadStart() {
        /*   if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
               return
           }
           RxBus2.publish(DownloadMediaEventBus(this, message))*/
    }

    private fun download(url: String) {
        chatModelList?.let {
            DownloadUtils.downloadFile(
                url,
                AppDirectory.docsReceivedFile(url).absolutePath,
                it.get(0).chatId,
                it.get(0),
                downloadListener
            )

        }
    }

}