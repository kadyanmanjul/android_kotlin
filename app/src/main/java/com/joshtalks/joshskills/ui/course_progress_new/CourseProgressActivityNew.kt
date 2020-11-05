package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.databinding.CourseProgressActivityNewBinding
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.ui.day_wise_course.DayWiseCourseActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class CourseProgressActivityNew : AppCompatActivity(),
    CourseProgressAdapter.ProgressItemClickListener {

    lateinit var binding: CourseProgressActivityNewBinding
    lateinit var adapter: ProgressActivityAdapter
    var courseId: Int = 0

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
            adapter = ProgressActivityAdapter(this, it, this)
            binding.progressRv.adapter = adapter

        })

        setupUi()
    }

    private fun setupUi() {
        binding.downloadIv.setOnClickListener {

        }
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.back_iv).setOnClickListener {
            onBackPressed()
        }
    }

    override fun onProgressItemClick(item: CourseOverviewItem) {
        if (item.status == LESSON_STATUS.NO.name) {
            showAlertMessage()
        } else
            startActivity(DayWiseCourseActivity.getDayWiseCourseActivityIntent(this, item.lessonId))
    }

    override fun onCertificateExamClick() {
        showAlertMessage()
    }

    private fun showAlertMessage() {
        val builder = AlertDialog.Builder(this)
            .setMessage("Please complete all previous lessons to unlock")
            .setPositiveButton("Okay") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .show()
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

    /*
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
        message?.question?.pdfList?.getOrNull(0)?.let { pdfType ->
            binding.additionalMaterialTv.setOnClickListener {
                PdfViewerActivity.startPdfActivity(
                    this,
                    pdfType.id,
                    message!!.question!!.title!!
                )

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
        download(message?.url)
    }

    private fun download(url: String?) {

        if (PermissionUtils.isStoragePermissionEnabled(this).not()) {
            askStoragePermission()
            return
        }
        message?.question?.pdfList?.let {
            if (it.size > 0) {
                DownloadUtils.downloadFile(
                    it.get(0).url,
                    AppDirectory.docsReceivedFile(it.get(0).url).absolutePath,
                    message!!.chatId,
                    message!!,
                    downloadListener
                )
            } else if (BuildConfig.DEBUG) {
                showToast("Pdf size is 0")
            }
        }
    }*/

}