package me.kyuubiran.web2pdf.interfaces

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView

fun interface IOnResourceErrorCallback {
    fun onResourceError(view: WebView, request: WebResourceRequest?, error: WebResourceError?)
}