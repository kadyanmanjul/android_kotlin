package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.server.engage.VideoEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import es.voghdev.pdfviewpager.library.PDFViewPager


const val PDF_URL = "pdf_url"

class PdfViewerActivity : BaseActivity() {
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private lateinit var pdfObject: PdfType
    var pdfViewPager: PDFViewPager? = null
    var total: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfObject = intent.getSerializableExtra(PDF_URL) as PdfType
        supportActionBar?.hide()
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf_viewer)
        showPdf()

    }

    fun showPdf() {
        val pdfViewPager = PDFViewPager(applicationContext, pdfObject.downloadedLocalPath)
        setContentView(pdfViewPager)
        AppAnalytics.create(AnalyticsEvent.PDF_OPENED.NAME).addParam("URL", pdfObject.url)

    }

    override fun onPause() {
        super.onPause()
        EngagementNetworkHelper.engagePdfApi(PdfEngage(pdfObject.id,pdfObject.totalView))
    }

    override fun onDestroy() {
        super.onDestroy()
        (pdfViewPager?.adapter as PDFPagerAdapter).close()

    }

    companion object {
        fun startPdfActivity(context: Context, pdfUrl: PdfType) {
            val intent = Intent(context, PdfViewerActivity::class.java).apply {

            }
            intent.putExtra(PDF_URL, pdfUrl)
            context.startActivity(intent)


        }

    }
}