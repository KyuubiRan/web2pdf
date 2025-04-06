package me.kyuubiran.web2pdf.interfaces

import android.webkit.WebView

fun interface IOnPageLoadFinishedCallback {
    fun onLoadFinished(webView: WebView, url: String)
}