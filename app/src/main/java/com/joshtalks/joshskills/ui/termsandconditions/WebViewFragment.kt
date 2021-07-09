package com.joshtalks.joshskills.ui.termsandconditions

import android.annotation.TargetApi
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentWebViewBinding

class WebViewFragment : DialogFragment() {

    private lateinit var binding: FragmentWebViewBinding
    private var webUrl: String = EMPTY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        changeDialogConfiguration()
        arguments?.let {
            webUrl = it.getString(WEB_URL, EMPTY)
        }
        if (webUrl.isBlank()) {
            dismiss()
        }
        //viewModel.getUrlFor3DWebView(award?.get(0)?.id.toString())
    }

    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_web_view, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.visibility = View.VISIBLE

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.setSupportZoom(false)
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
        //binding.webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)

        /* binding.webView.loadDataWithBaseURL(
             null,
             code,
             "text/html",
             "utf-8",
             null
         )*/

        binding.webView.webViewClient = object : WebViewClient() {

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                showToast(description ?: "Error")
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                req: WebResourceRequest,
                rerr: WebResourceError
            ) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(
                    view,
                    rerr.errorCode,
                    rerr.description.toString(),
                    req.url.toString()
                )
            }
        }
        binding.webView.loadUrl(webUrl)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {

        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "WebViewFragment"
        const val WEB_URL = "web_url"

        fun newInstance(webUrl: String) =
            WebViewFragment().apply {
                arguments = Bundle().apply {
                    putString(WEB_URL, webUrl)
                }
            }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            webUrl: String,
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(webUrl).show(supportFragmentManager, TAG)
        }

    }
}
