package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import kotlinx.android.synthetic.main.activity_payment.*

const val PAYMENT_URL_KEY = "payment_url"
const val PAYMENT_CHECKOUT_URL_KEY = "payment_checkout_url"
const val PAYMENT_TITLE = "payment_title"
const val PAYMENT_COURSE_KEY = "payment_course"
const val SCREEN_NAME="Payment"


class PaymentActivity : AppCompatActivity(), WebViewCallback {

    lateinit var paymentUrl: String
    lateinit var checkoutUrl: String
    private var pageLoad = false
    var screenEngagementModel: ScreenEngagementModel = ScreenEngagementModel(SCREEN_NAME)

    companion object {
        fun startPaymentActivity(
            context: Activity,
            requestCode: Int,
            paymentUrl: String = "",
            courseName: String = ""
        ) {
            val intent = Intent(context, PaymentActivity::class.java)
            intent.putExtra(PAYMENT_URL_KEY, paymentUrl)
            intent.putExtra(PAYMENT_COURSE_KEY, courseName)
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        initView()
        if (intent.hasExtra(PAYMENT_URL_KEY)) {
            paymentUrl = intent.getStringExtra(PAYMENT_URL_KEY)
        }

        if (paymentUrl.isEmpty()) {
            paymentUrl = AppObjectController.getFirebaseRemoteConfig().getString(PAYMENT_URL_KEY)
        }

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

        if (Utils.isInternetAvailable().not()) {
            return
        }

        webView.loadUrl(paymentUrl)

    }

    private fun initView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = AppObjectController.getFirebaseRemoteConfig().getString(PAYMENT_TITLE)
        if (intent.getStringExtra(PAYMENT_COURSE_KEY).isNotEmpty()) {
            titleView.text = intent.getStringExtra(PAYMENT_COURSE_KEY)
        }


        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            this@PaymentActivity.finish()
        }
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
    ): Boolean {
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

    override fun onStartPageLoad() {
        pageLoad = true
        screenEngagementModel.startTime=System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if(pageLoad){
            screenEngagementModel.startTime=System.currentTimeMillis()
        }
    }

    override fun onStop() {
        screenEngagementModel.endTime=System.currentTimeMillis()
        WorkMangerAdmin.screenAnalyticsWorker(screenEngagementModel)
        super.onStop()
    }
}

interface WebViewCallback {
    fun onUrl(url: String)
    fun onStartPageLoad()
}
