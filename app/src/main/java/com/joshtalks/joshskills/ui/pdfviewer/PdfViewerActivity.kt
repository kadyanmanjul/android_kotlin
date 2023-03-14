package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.server.engage.SharePdfEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PDF_ID = "pdf_id"
const val COURSE_NAME = "course_name"
const val MESSAGE_ID = "message_id"
const val PDF_PATH = "pdf_path"
const val CURRENT_VIDEO_PROGRESS_POSITION = "current_video_progress_position"

class PdfViewerActivity : BaseActivity() {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private var pdfObject: PdfType? = null
    private var pdfId: String? = null
    private var uiHandler = Handler(Looper.getMainLooper())
    private var path: String = EMPTY

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

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setToolbar() {
        intent.getStringExtra(COURSE_NAME)?.let {
            conversationBinding.titleTv.text = it
        }
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        super.onBackPressed()
    }

    private fun showPdf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pdfId =intent.getStringExtra(PDF_ID)

                    path = if (intent.hasExtra(PDF_PATH)) {
                    intent.getStringExtra(PDF_PATH)!!
                } else {

                    pdfObject =
                        AppObjectController.appDatabase.chatDao()
                            .getPdfById(intent.getStringExtra(PDF_ID)!!)
                    pdfObject?.downloadedLocalPath!!
                }
                AppObjectController.uiHandler.post {
                    try {
                        conversationBinding.pdfView.fromFile(path)
                        conversationBinding.pdfView.show()
                        conversationBinding.ivShare.isVisible = true
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

    fun sharePDF(v: View) {
        try {

            val shareFilePath = AppDirectory.getDocsShareFile(
                null,
                pdfExtension = ".pdf"
            ).absolutePath
            AppDirectory.copy(path, shareFilePath!!)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, Uri.parse(shareFilePath))
            }
            pdfId?.run {
                EngagementNetworkHelper.savePdfShareImpression(SharePdfEngage(this, Mentor.getInstance().getId()?: EMPTY,"Share"))
            }
            startActivity(Intent.createChooser(intent, "Share PDF"))
        } catch (e: Exception) {
            e.showAppropriateMsg()
            e.printStackTrace()
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
            messageId: String? = null,
            pdfPath: String = EMPTY,
            conversationId: String? = null,
        ) {
            Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(PDF_ID, pdfId)
                putExtra(COURSE_NAME, courseName)
                putExtra(MESSAGE_ID, messageId)
                putExtra(CONVERSATION_ID, conversationId)
                if (pdfPath.isNotEmpty())
                    putExtra(PDF_PATH, pdfPath)
            }.run {
                context.startActivity(this)
            }
        }
    }
}
