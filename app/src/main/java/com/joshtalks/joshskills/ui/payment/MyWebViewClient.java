package com.joshtalks.joshskills.ui.payment;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class MyWebViewClient extends WebViewClient {
    public final String url;

    private final WebViewCallback callback;
    public MyWebViewClient(String url, WebViewCallback callback) {
        this.url = url;
        this.callback=callback;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        webView.clearCache(true);
        super.onPageFinished(webView, url);
        callback.onUrl(url);


    }

    @Override
    public void onPageStarted(WebView webView, String url, Bitmap favicon) {
        callback.onStartPageLoad();

    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
                                String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);

    }


}