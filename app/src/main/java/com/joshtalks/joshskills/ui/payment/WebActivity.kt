package com.joshtalks.joshskills.ui.payment

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog

class WebActivity : AppCompatActivity() {
    lateinit var webView : WebView
    lateinit var url : String
        @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        var backbtn : AppCompatImageView = findViewById(R.id.iv_back)
        backbtn.setOnClickListener {
            onBackPressed()
        }

        FullScreenProgressDialog.showProgressBar(this)
        url = intent.getStringExtra("pdf_url").toString()
        webView = findViewById(R.id.web)
        webView.getSettings().setSupportZoom(true)
        webView.getSettings().setJavaScriptEnabled(true)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://docs.google.com/gview?embedded=true&url=" + url)

    }
    inner class WebViewClient : android.webkit.WebViewClient(){

        // load url
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String
        ): Boolean {
                view.loadUrl(url)
            return true
        }


        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            FullScreenProgressDialog.hideProgressBar(this@WebActivity)
        }

    }

  override fun onBackPressed() {
        super.onBackPressed()
    }

}