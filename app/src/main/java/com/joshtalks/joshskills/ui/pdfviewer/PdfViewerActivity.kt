package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import es.voghdev.pdfviewpager.library.PDFViewPager
import es.voghdev.pdfviewpager.library.RemotePDFViewPager
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import es.voghdev.pdfviewpager.library.remote.DownloadFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PDF_ID = "pdf_id"
const val COURSE_NAME = "course_name"

class PdfViewerActivity : BaseActivity(), DownloadFile.Listener {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private var pdfObject: PdfType? = null

    private var pdfViewPager: PDFViewPager? = null
    private var gestureDetector: GestureDetector? = null
    private var uiHandler = Handler(Looper.getMainLooper())
    private var adapter: PDFPagerAdapter? = null
    private var remotePDFViewPager: RemotePDFViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf_viewer)
        setToolbar()
        showPdf()
    }

    private fun setToolbar() {
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.overlay)
        intent.getStringExtra(COURSE_NAME)?.let {
            conversationBinding.textMessageTitle.text = it
        }
        conversationBinding.ivBack.setOnClickListener {
            finish()
        }
        gestureDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (conversationBinding.toolbar.visibility == View.VISIBLE) {
                        conversationBinding.toolbar.visibility = View.GONE
                        enterIntoFullScreen()
                    } else {
                        exitFromFullScreen()
                        conversationBinding.toolbar.visibility = View.VISIBLE
                        uiHandler.postDelayed({
                            conversationBinding.toolbar.visibility = View.GONE
                            enterIntoFullScreen()
                        }, 3000)

                    }
                    return true
                }
            }
        )
    }

    private fun enterIntoFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun exitFromFullScreen() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }


    private fun showPdf() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                pdfObject =
                    AppObjectController.appDatabase.chatDao()
                        .getPdfById(intent.getStringExtra(PDF_ID))
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if (pdfObject?.downloadedLocalPath.isNullOrEmpty()) {

                            remotePDFViewPager = RemotePDFViewPager(
                                applicationContext,
                                pdfObject?.url,
                                this@PdfViewerActivity
                            )
                            remotePDFViewPager?.pageMargin = 20

                        } else {
                            val pdfViewPager =
                                PDFViewPager(applicationContext, pdfObject?.downloadedLocalPath)
                            pdfViewPager.pageMargin = 20
                            conversationBinding.remotePdfRoot.addView(pdfViewPager)

                        }
                        AppAnalytics.create(AnalyticsEvent.PDF_OPENED.NAME)
                            .addParam("URL", pdfObject?.url)
                            .push()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            (pdfViewPager?.adapter as PDFPagerAdapter).close()
        } catch (ex: Exception) {

        }
        try {
            adapter?.close()
        } catch (ex: Exception) {

        }

    }

    companion object {
        fun startPdfActivity(
            context: Context,
            pdfId: String,
            courseName: String
        ) {
            Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(PDF_ID, pdfId)
                putExtra(COURSE_NAME, courseName)
            }.run {
                context.startActivity(this)
            }


        }

    }

    override fun onSuccess(url: String?, destinationPath: String?) {
        destinationPath?.let {
            adapter = PDFPagerAdapter(this, it)
            remotePDFViewPager?.adapter = adapter
            conversationBinding.remotePdfRoot.addView(remotePDFViewPager)
        }

    }

    override fun onFailure(e: java.lang.Exception?) {
    }

    override fun onProgressUpdate(progress: Int, total: Int) {
    }

}