package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
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
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import androidx.core.content.ContextCompat



const val PDF_URL = "pdf_url"
const val COURSE_NAME = "course_name"

class PdfViewerActivity : BaseActivity() {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private lateinit var pdfObject: PdfType
    private var pdfViewPager: PDFViewPager? = null

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
    }


    fun showPdf() {
        val pdfViewPager = PDFViewPager(applicationContext, pdfObject.downloadedLocalPath)
        conversationBinding.remotePdfRoot.addView(pdfViewPager)
        //setContentView(pdfViewPager)
        AppAnalytics.create(AnalyticsEvent.PDF_OPENED.NAME).addParam("URL", pdfObject.url).push()

    }

    override fun onPause() {
        super.onPause()
        EngagementNetworkHelper.engagePdfApi(PdfEngage(pdfObject.id, pdfObject.totalView))
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