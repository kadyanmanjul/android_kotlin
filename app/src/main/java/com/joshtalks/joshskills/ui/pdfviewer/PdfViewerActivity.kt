package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import es.voghdev.pdfviewpager.library.PDFViewPager
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter


const val PDF_URL = "pdf_url"
const val COURSE_NAME = "course_name"

class PdfViewerActivity : BaseActivity() {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private lateinit var pdfObject: PdfType
    private var pdfViewPager: PDFViewPager? = null
    private var gestureDetector: GestureDetector? = null
    private var uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf_viewer)
        pdfObject = intent.getSerializableExtra(PDF_URL) as PdfType
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
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
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
        val pdfViewPager = PDFViewPager(applicationContext, pdfObject.downloadedLocalPath)
        conversationBinding.remotePdfRoot.addView(pdfViewPager)
        AppAnalytics.create(AnalyticsEvent.PDF_OPENED.NAME).addParam("URL", pdfObject.url).push()

    }

    override fun onPause() {
        super.onPause()
        EngagementNetworkHelper.engagePdfApi(PdfEngage(pdfObject.id, pdfObject.totalView))
        uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            (pdfViewPager?.adapter as PDFPagerAdapter).close()
        } catch (ex: Exception) {

        }
    }

    companion object {
        fun startPdfActivity(
            context: Context,
            pdfUrl: PdfType,
            courseName: String
        ) {
            val intent = Intent(context, PdfViewerActivity::class.java).apply {

            }
            intent.putExtra(PDF_URL, pdfUrl)
            intent.putExtra(COURSE_NAME, courseName)
            context.startActivity(intent)


        }

    }

    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
    }

}