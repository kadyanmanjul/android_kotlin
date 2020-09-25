package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PDF_ID = "pdf_id"
const val COURSE_NAME = "course_name"
const val MESSAGE_ID = "message_id"

class PdfViewerActivity : BaseActivity() {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private var pdfObject: PdfType? = null
    private var uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf_viewer)
        conversationBinding.handler = this
        setToolbar()
        showPdf()
    }

    private fun setToolbar() {
        intent.getStringExtra(COURSE_NAME)?.let {
            conversationBinding.titleTv.text = it
        }
    }
    private fun showPdf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pdfObject =
                    AppObjectController.appDatabase.chatDao()
                        .getPdfById(intent.getStringExtra(PDF_ID)!!)
                AppObjectController.uiHandler.post {
                    try {
                        val path: String = pdfObject?.downloadedLocalPath!!
                        conversationBinding.pdfView.fromFile(path)
                        conversationBinding.pdfView.show()
                        AppAnalytics.create(AnalyticsEvent.PDF_OPENED.NAME)
                            .addParam("URL", pdfObject?.url)
                            .push()
                        intent.getStringExtra(MESSAGE_ID)?.run {
                            DatabaseUtils.updateLastUsedModification(this)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pdfObject?.run {
            EngagementNetworkHelper.engagePdfApi(PdfEngage(this.id, this.totalView))
        }
        uiHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        fun startPdfActivity(
            context: Context,
            pdfId: String,
            courseName: String,
            messageId: String? = null

        ) {
            Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(PDF_ID, pdfId)
                putExtra(COURSE_NAME, courseName)
                putExtra(MESSAGE_ID, messageId)
            }.run {
                context.startActivity(this)
            }
        }

    }
}
