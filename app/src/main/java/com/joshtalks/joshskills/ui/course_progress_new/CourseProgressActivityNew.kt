package com.joshtalks.joshskills.ui.course_progress_new

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.decorator.StickHeaderItemDecoration
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse
import com.joshtalks.joshskills.repository.server.course_overview.PdfInfo
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class CourseProgressActivityNew : BaseActivity(),
    CourseProgressAdapter.ProgressItemClickListener {

    private var courseOverviewResponse: List<CourseOverviewResponse>? = null

    private var lastAvailableLessonNo: Int? = null
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

        CoroutineScope(Dispatchers.IO).launch {
            lastAvailableLessonNo = viewModel.getLastLessonForCourse(courseId)

            PrefManager.put(COURSE_PROGRESS_OPENED, true)
        }

        viewModel.getCourseOverview(courseId)

        binding.progressLayout.visibility = View.VISIBLE
        viewModel.progressLiveData.observe(this, {

            courseOverviewResponse = it.responseData
            pdfInfo = it.pdfInfo

            pdfInfo?.let {
                binding.pdfNameTv.text = it.coursePdfName
                binding.sizeTv.text = "${it.coursePdfSize} kB"
                binding.pageCountTv.text = "${it.coursePdfPageCount} pages"
                binding.pdfView.visibility = View.GONE
                binding.progressLayout.visibility = View.GONE
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
            val data = ArrayList<CourseOverviewResponse>()
            it.responseData?.forEach {
                val courseOverviewResponse = CourseOverviewResponse()
                courseOverviewResponse.title = it.title
                courseOverviewResponse.unLockCount = it.unLockCount
                courseOverviewResponse.type = 10
                data.add(courseOverviewResponse)
                data.add(it)
            }

            adapter =
                ProgressActivityAdapter(
                    this,
                    data,
                    this,
                    it.conversationId ?: "0",
                    lastAvailableLessonNo
                )
            binding.progressRv.adapter = adapter

            val stickHeaderDecoration = StickHeaderItemDecoration(adapter.getListner())
            binding.progressRv.addItemDecoration(stickHeaderDecoration)

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
        CoroutineScope(Dispatchers.IO).launch {
            val lessonModel = viewModel.getLesson(item.lessonId)
            runOnUiThread {
                if (lessonModel != null) {
                    val dayWiseActivityListener: ActivityResultLauncher<Intent> =
                        registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                viewModel.getCourseOverview(courseId)
                            }
                        }

                    dayWiseActivityListener.launch(
                        LessonActivity.getActivityIntent(
                            this@CourseProgressActivityNew,
                            item.lessonId
                        )
                    )
                } else {
                    showAlertMessage(
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.INCOMPLETE_LESSONS_TITLE),
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.PROGRESS_MESSAGE)
                    )
                }
            }
        }

    }

    override fun onCertificateExamClick(
        previousLesson: CourseOverviewItem, conversationId: String,
        chatMessageId: String,
        certificationId: Int,
        cExamStatus: CExamStatus,
        parentPosition: Int,
        title: String

    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val lessonModel = viewModel.getLesson(previousLesson.lessonId)
            runOnUiThread {
                if (lessonModel == null) {
                    courseOverviewResponse?.let {
                        ExamUnlockDialogFragment(
                            it[parentPosition].examInstructions,
                            it[parentPosition].ceMarks,
                            it[parentPosition].ceQue,
                            it[parentPosition].ceMin,
                            it[parentPosition].totalCount,
                            it[parentPosition].unLockCount,
                            title
                        ).show(
                            supportFragmentManager,
                            "ExamUnlockDialogFragment"
                        )
                    }
                } else {
                    val cExamActivityListener: ActivityResultLauncher<Intent> =
                        registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                result.data.getStringExtra(MESSAGE_ID).let { chatId ->
                                    viewModel.getCourseOverview(courseId)
                                }
                            }
                        }
                    cExamActivityListener.launch(
                        CertificationBaseActivity.certificationExamIntent(
                            this@CourseProgressActivityNew,
                            conversationId = conversationId,
                            chatMessageId = chatMessageId,
                            certificationId = certificationId,
                            cExamStatus = cExamStatus
                        )
                    )
                }
            }
        }

    }

    private fun showAlertMessage(title: String, message: String) {

        CustomDialog(
            this,
            title,
            message
        ).show()
    }

    private fun askStoragePermission() {

        PermissionUtils.storageReadAndWritePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                this@CourseProgressActivityNew,
                                R.string.storage_permission_message
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
            if (PermissionUtils.isStoragePermissionEnabled(this) && AppDirectory.getFileSize(
                    File(
                        AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath
                    )
                ) > 0
            ) {
                PdfViewerActivity.startPdfActivity(
                    context = this,
                    pdfId = "$courseId",
                    courseName = it.coursePdfName,
                    pdfPath = AppDirectory.docsReceivedFile(it.coursePdfUrl).absolutePath

                )
            } else {
                download()
            }
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
        binding.ivDownloadCompleted.visibility = View.VISIBLE
    }

    private fun fileNotDownloadView() {
        binding.pdfNameTv.isClickable = false
        binding.downloadContainer.isClickable = true
        binding.downloadContainer.visibility = View.VISIBLE
        binding.ivStartDownload.visibility = View.VISIBLE
        binding.progressDialog.visibility = View.GONE
        binding.ivCancelDownload.visibility = View.GONE
        binding.ivDownloadCompleted.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        binding.ivStartDownload.visibility = View.GONE
        binding.progressDialog.visibility = View.VISIBLE
        binding.ivCancelDownload.visibility = View.VISIBLE
        binding.ivDownloadCompleted.visibility = View.GONE
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