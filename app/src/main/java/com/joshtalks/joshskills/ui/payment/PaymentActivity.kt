package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import kotlinx.android.synthetic.main.activity_payment.*

const val PAYMENT_URL_KEY = "payment_url"
const val PAYMENT_CHECKOUT_URL_KEY = "payment_checkout_url"
const val PAYMENT_TITLE = "payment_title"

class PaymentActivity : AppCompatActivity(), WebViewCallback {


    lateinit var paymentUrl: String
    lateinit var checkoutUrl: String

    companion object {
        fun startPaymentActivity(context: Activity,requestCode:Int) {
            val intent = Intent(context, PaymentActivity::class.java)
            context.startActivityForResult(intent,requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        toolbar.title =AppObjectController.getFirebaseRemoteConfig().getString(PAYMENT_TITLE)
        paymentUrl = AppObjectController.getFirebaseRemoteConfig().getString(PAYMENT_URL_KEY)
        checkoutUrl =
            AppObjectController.getFirebaseRemoteConfig().getString(PAYMENT_CHECKOUT_URL_KEY)
        webView.webViewClient = MyWebViewClient(paymentUrl, this)
        setWebSetting()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView?,
                progress: Int
            ) {
            }
        }
        webView.loadUrl(paymentUrl)

    }

    private fun setWebSetting() {
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.setAppCacheEnabled(true)
        webView.clearCache(true)
        webView.clearHistory()
        webView.settings.javaScriptCanOpenWindowsAutomatically = true;
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean { // Check if the key event was the Back button and if there's history
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack()
                return true
            }
            if (webView.canGoBack().not()) {
                paymentFailed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun paymentFailed() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun paymentSuccess() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onUrl(url: String) {
        if (url == checkoutUrl) {
            AppObjectController.uiHandler.postDelayed({
                paymentSuccess()
            }, 2000)
        }

    }
}

interface WebViewCallback {
    fun onUrl(url: String)
}
