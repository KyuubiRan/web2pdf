package me.kyuubiran.web2pdf

import java.io.File

fun interface IWeb2PdfResultCallback {
    fun onResult(success: Boolean, file: File?, exception: Exception?)
}