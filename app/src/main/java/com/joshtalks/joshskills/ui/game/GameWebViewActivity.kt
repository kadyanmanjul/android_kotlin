package com.joshtalks.joshskills.ui.game


import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.joshtalks.joshskills.R

class GameWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @Suppress("SameParameterValue")
    private fun createJsObject(
        webview: WebView,
        jsObjName: String,
        allowedOriginRules: Set<String>,
        onMessageReceived: (message: String) -> Unit
    ) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(
                webview, jsObjName, allowedOriginRules
            ) { _, message, _, _, _ -> onMessageReceived(message.data!!) }
        } else {
            webview.addJavascriptInterface(object {
                @JavascriptInterface
                fun postMessage(message: String) {

                }
            }, jsObjName)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        val jsObjName = "jsObject"
        val allowedOriginRules = setOf("https://raw.githubusercontent.com")

        setupWebViewConfig()
        title = getString(R.string.app_name)

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Enable Javascript

        // Create a JS object to be injected into frames; Determines if WebMessageListener
        // or WebAppInterface should be used
        createJsObject(
            webView,
            jsObjName,
            allowedOriginRules
        ) { message -> }

        // Load the content
        webView.loadUrl("https://game.vinayakg.me/2048/")

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewConfig() {
        webView.webChromeClient = MyWebChromeClient()
        webView.webViewClient = MyWebViewClient()
        val settings: WebSettings = webView.settings
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        settings.standardFontFamily = "Roboto-Regular"
        settings.loadsImagesAutomatically = true
        settings.setAppCacheEnabled(true)
        settings.cacheMode = LOAD_CACHE_ELSE_NETWORK
        settings.databaseEnabled = true
        settings.domStorageEnabled = true
        webView.keepScreenOn = true
        webView.settings.javaScriptEnabled = true
    }

    class MyWebChromeClient : WebChromeClient() {

        override fun onReceivedTitle(view: WebView, title: String) {
        }

        override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            Log.e("progress", "progress")
        }
    }

    private class MyWebViewClient :
        WebViewClientCompat()
}

