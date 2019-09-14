package com.joshtalks.joshskills.ui.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityPdfViewerBinding
import com.joshtalks.joshskills.repository.local.entity.PdfType
import es.voghdev.pdfviewpager.library.PDFViewPager


const val PDF_URL = "pdf_url"
class PdfViewerActivity : BaseActivity(){
    private lateinit var conversationBinding: ActivityPdfViewerBinding
    private lateinit var pdfObject: PdfType
    var pdfViewPager: PDFViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfObject = intent.getSerializableExtra(PDF_URL) as PdfType
        supportActionBar?.hide()
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf_viewer)

        showPdf()

    }

   fun showPdf(){
       val pdfViewPager = PDFViewPager(applicationContext, pdfObject.downloadedLocalPath)
       setContentView(pdfViewPager)

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