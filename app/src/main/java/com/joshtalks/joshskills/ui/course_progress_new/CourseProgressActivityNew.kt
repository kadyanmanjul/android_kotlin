package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.repository.server.course_overview.PdfInfo
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.day_wise_course.DayWiseCourseActivity
import com.joshtalks.joshskills.ui.pdfviewer.MESSAGE_ID
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.util.CustomDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import java.io.File

class CourseProgressActivityNew : AppCompatActivity(),
    CourseProgressAdapter.ProgressItemClickListener {

    lateinit var binding: CourseProgressActivityNewBinding
    lateinit var adapter: ProgressActivityAdapter
    var courseId: Int = 0
    var pdfInfo: PdfInfo? = null
//    var courseOverviewResponse: CourseOverviewResponse? = null


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
//            message?.downloadStatus = DOWNLOAD_STATUS.FAILED
            fileNotDownloadView()

        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
//            message?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
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
//            message?.downloadStatus = DOWNLOAD_STATUS.FAILED
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


    private val viewModel: CourseOverviewViewModel by lazy {
        ViewModelProvider(this).get(CourseOverviewViewModel::class.java)
    }

    companion object {
        private val COURSE_ID = "course_id"
        fun getCourseProgressActivityNew(
            context: Context,
            courseId: Int
        ) = Intent(context, CourseProgressActivityNew::class.java).apply {
            putExtra(COURSE_ID, courseId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.course_progress_activity_new)

        setupToolbar()
        if (intent.hasExtra(COURSE_ID).not())
            finish()

        courseId = intent.getIntExtra(COURSE_ID, 0)

        viewModel.getCourseOverview(courseId)
        viewModel.progressLiveData.observe(this, {
            binding.pdfNameTv.text = it.pdfInfo.coursePdfName
            binding.sizeTv.text = "${it.pdfInfo.coursePdfSize} kB"
            binding.pageCountTv.text = "${it.pdfInfo.coursePdfPageCount} pages"
            pdfInfo = it.pdfInfo

            pdfInfo?.let {
                binding.pdfView.visibility = View.VISIBLE
                if (PermissionUtils.isStoragePermissionEnabled(this) && AppDirectory.getFileSize(
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
            adapter =
                ProgressActivityAdapter(this, it.responseData!!, this, it.conversationId ?: "0")
            binding.progressRv.adapter = adapter

        })

        setupUi()
    }

    private fun setupUi() {
        binding.pdfNameTv.setOnClickListener {
            openPdf()
        }
        binding.downloadContainer.setOnClickListener {
            download()
        }
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.back_iv).setOnClickListener {
            onBackPressed()
        }
    }

    override fun onProgressItemClick(item: CourseOverviewItem, previousItem: CourseOverviewItem?) {
        if  (item.status == QUESTION_STATUS.NA.name) {
            showAlertMessage()
        } else {
            val dayWiseActivityListener: ActivityResultLauncher<Intent> =
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.getCourseOverview(courseId)
                    }
                }

            dayWiseActivityListener.launch(
                DayWiseCourseActivity.getDayWiseCourseActivityIntent(
                    this, item.lessonId,
                    courseId = courseId.toString()
                )
            )
        }
    }

    override fun onCertificateExamClick(
        previousLesson: CourseOverviewItem, conversationId: String,
        chatMessageId: String,
        certificationId: Int,
        cExamStatus: CExamStatus
    ) {
        if (previousLesson.status != LESSON_STATUS.CO.name) {
            showAlertMessage()
        } else {
            val cExamActivityListener: ActivityResultLauncher<Intent> =
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.getStringExtra(MESSAGE_ID)?.let { chatId ->
                            viewModel.getCourseOverview(courseId)
                        }
                    }
                }
            cExamActivityListener.launch(
                CertificationBaseActivity.certificationExamIntent(
                    this,
                    conversationId = conversationId,
                    chatMessageId = chatMessageId,
                    certificationId = certificationId,
                    cExamStatus = cExamStatus
                )
            )
        }
    }

    private fun showAlertMessage() {
        CustomDialog(
            this,
            "Incomplete lessons",
            "Please complete all previous lessons to unlock"
        ).show()
    }

    fun askStoragePermission() {

        PermissionUtils.storageReadAndWritePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                this@CourseProgressActivityNew,
                                R.string.record_permission_message
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
    }

    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnabled(this)) {
            PermissionUtils.storageReadAndWritePermission(
                this,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                openPdf()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    this@CourseProgressActivityNew
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
        if (PermissionUtils.isStoragePermissionEnabled(this).not()) {
            askStoragePermission()
            return
        }

        pdfInfo?.let {
            PdfViewerActivity.startPdfActivity(
                context = this,
                pdfId = "$courseId",
                courseName = it.coursePdfName,
                pdfPath = AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath

            )
        }
    }

    private fun fileDownloadSuccess() {
        binding.pdfNameTv.isClickable = true
        binding.downloadContainer.isClickable = false
        binding.downloadContainer.visibility = View.GONE
        binding.ivStartDownload.visibility = View.GONE
        binding.progressDialog.visibility = View.GONE
        binding.ivCancelDownload.visibility = View.GONE
        binding.pdfView.isClickable = false
    }

    private fun fileNotDownloadView() {
        binding.pdfNameTv.isClickable = false
        binding.downloadContainer.isClickable = true
        binding.downloadContainer.visibility = View.VISIBLE
        binding.ivStartDownload.visibility = View.VISIBLE
        binding.progressDialog.visibility = View.GONE
        binding.ivCancelDownload.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        binding.ivStartDownload.visibility = View.GONE
        binding.progressDialog.visibility = View.VISIBLE
        binding.ivCancelDownload.visibility = View.VISIBLE
    }


    fun downloadCancel() {
        fileNotDownloadView()
//        message?.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    fun downloadStart() {
        download()
    }

    private fun download() {
        if (PermissionUtils.isStoragePermissionEnabled(this).not()) {
            askStoragePermission()
            return
        }
        pdfInfo?.let {
            DownloadUtils.downloadFile(
                it.coursePdfUrl,
                AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath,
                "$courseId",
                downloadListener
            )


        }
    }

}