package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.repository.server.course_overview.PdfInfo
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pnikosis.materialishprogress.ProgressWheel
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import java.io.File

class PdfCourseProgressView : FrameLayout {
    private lateinit var pdfNameTv: MaterialTextView
    private lateinit var sizeTv: MaterialTextView
    private lateinit var pageCountTv: MaterialTextView
    private lateinit var pdfView: ConstraintLayout
    private lateinit var downloadContainer: FrameLayout
    private lateinit var ivStartDownload: AppCompatImageView
    private lateinit var ivCancelDownload: AppCompatImageView
    private lateinit var ivDownloadCompleted: AppCompatImageView
    private lateinit var progressDialog: ProgressWheel

    //private lateinit var progressLayout: RelativeLayout
    private var pdfInfo: PdfInfo? = null

    //private var activity: CourseProgressActivityNew? = null
    private var courseId: String = EMPTY
    private var conversationId: String = EMPTY

    private var callback: Callback? = null

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            fileNotDownloadView()
        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            fileDownloadSuccess()
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
            DownloadUtils.removeCallbackListener(download.tag)
            fileNotDownloadView()
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
            fileDownloadingInProgressView()
        }

        override fun onWaitingNetwork(download: Download) {
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.pdf_course_progress_view, this)
        pdfNameTv = findViewById(R.id.pdf_name_tv)
        sizeTv = findViewById(R.id.size_tv)
        pageCountTv = findViewById(R.id.page_count_tv)
        pdfView = findViewById(R.id.pdf_view)

        downloadContainer = findViewById(R.id.download_container)
        ivStartDownload = findViewById(R.id.iv_start_download)
        progressDialog = findViewById(R.id.progress_dialog)
        ivCancelDownload = findViewById(R.id.iv_cancel_download)
        ivDownloadCompleted = findViewById(R.id.iv_download_completed)
        //progressLayout = findViewById(R.id.progress_layout)

        pdfNameTv.setOnClickListener {
            openPdf()
        }

        downloadContainer.setOnClickListener {
            download()
        }
        ivCancelDownload.setOnClickListener {
            downloadCancel()
        }

    }


    fun setup(
        pdfInfo: PdfInfo?,
        courseId: String,
        conversationId: String
    ) {
        this.pdfInfo = pdfInfo
        this.courseId = courseId
        this.conversationId = conversationId
        this.callback = callback

        pdfInfo?.let {
            pdfNameTv.text = it.coursePdfName
            sizeTv.text = "${it.coursePdfSize} kB"
            pageCountTv.text = "${it.coursePdfPageCount} pages"
            pdfView.visibility = View.GONE

            //progressLayout.visibility = View.GONE


            if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.getFileSize(
                    File(
                        AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath
                    )
                ) > 0
            ) {
                fileDownloadSuccess()
            } else {
                fileNotDownloadView()
            }
        }

    }

    private fun openPdf() {
        if (PermissionUtils.isStoragePermissionEnabled(context).not()) {
            askStoragePermission()
            return
        }

        this.pdfInfo?.let {
            if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.getFileSize(
                    File(
                        AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath
                    )
                ) > 0
            ) {
                PdfViewerActivity.startPdfActivity(
                    context = context,
                    pdfId = courseId,
                    courseName = it.coursePdfName,
                    pdfPath = AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath,
                    conversationId = conversationId
                )
            } else {
                download()
            }
        }
    }


    private fun fileDownloadSuccess() {
        pdfNameTv.isClickable = true
        downloadContainer.isClickable = false
        downloadContainer.visibility = View.GONE
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
        pdfView.isClickable = false
        ivDownloadCompleted.visibility = View.VISIBLE
    }

    private fun fileNotDownloadView() {
        pdfNameTv.isClickable = false
        downloadContainer.isClickable = true
        downloadContainer.visibility = View.VISIBLE
        ivStartDownload.visibility = View.VISIBLE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
        ivDownloadCompleted.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = View.VISIBLE
        ivCancelDownload.visibility = View.VISIBLE
        ivDownloadCompleted.visibility = View.GONE
    }

    fun downloadCancel() {
        fileNotDownloadView()
    }

    fun downloadStart() {
        download()
    }

    private fun download() {
        if (PermissionUtils.isStoragePermissionEnabled(context).not()) {
            askStoragePermission()
            return
        }
        pdfInfo?.let {
            DownloadUtils.downloadFile(
                it.coursePdfUrl,
                AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath,
                courseId,
                downloadListener
            )
        }
    }

    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnabled(context)) {
            PermissionUtils.storageReadAndWritePermission(
                context,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                openPdf()
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                callback?.showDialog(-1)
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
                }
            )
            return
        }
        openPdf()
    }

    private fun askStoragePermission() {

        PermissionUtils.storageReadAndWritePermission(
            context,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            callback?.showDialog(R.string.storage_permission_message)
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
            }
        )
    }


    fun addCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun showDialog(idString: Int)
    }

}