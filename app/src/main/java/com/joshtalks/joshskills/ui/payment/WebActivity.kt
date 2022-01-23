package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
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
            Log.e("web_sakshii", url)
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
          //  view.loadUrl(url)
            FullScreenProgressDialog.hideProgressBar(this@WebActivity)
        }

    }

  override fun onBackPressed() {
        super.onBackPressed()
    }

}