package me.kyuubiran.web2pdf

import android.webkit.WebView

fun interface IOnPageLoadFinishedCallback {
    fun onLoadFinished(webView: WebView, url: String)
}