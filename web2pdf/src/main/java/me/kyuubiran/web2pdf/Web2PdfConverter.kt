@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package me.kyuubiran.web2pdf

import android.content.Context
import android.print.PdfPrinter
import android.print.PrintAttributes
import android.print.PrintAttributes.Resolution
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.util.UUID
import java.util.concurrent.CancellationException

class Web2PdfConverter(context: Context) : AutoCloseable, Closeable {

    data class ProcessDataOptions(
        val baseUrl: String? = null,
        val mineType: String? = null,
        val encoding: String? = null
    )

// region Private Properties

    private lateinit var outputFile: File
    private lateinit var onFinishCallBack: IWeb2PdfResultCallback
    private val coScope = MainScope()
    private val webView: WebView = WebView(context).apply {
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // avoid multiple calls
                if (currentJob != null)
                    return

                onPageLoadFinished?.onLoadFinished(view, url)

                currentJob = coScope.launch {
                    delayBeforeConvert?.let { delay(it) }
                    val printer = PdfPrinter(
                        attrs = customPrintAttributes ?: PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(Resolution("Web2pdf", Context.PRINT_SERVICE, 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                    )
                    printer.generate(
                        adapter = view.createPrintDocumentAdapter(UUID.randomUUID().toString()),
                        outputFile = outputFile,
                        onFinish = onFinishCallBack
                    )
                }.also { it.invokeOnCompletion { currentJob = null; isTaskStarted = false } }
            }
        }
    }
// endregion

// region Public Properties

    /**
     * Check if the task is started.
     */
    var isTaskStarted = false
        @Synchronized private set
        @Synchronized get

    /**
     * Callback for when the page load is finished.
     */
    var onPageLoadFinished: IOnPageLoadFinishedCallback? = null

    /**
     * The current coroutine job for PDF generation.
     * This is used to track if a conversion is already in progress.
     */
    var currentJob: Job? = null
        private set

    /**
     * Enable or disable JavaScript in the WebView.
     */
    var enableJs: Boolean
        set(v) {
            webView.settings.javaScriptEnabled = v
        }
        get() = webView.settings.javaScriptEnabled

    /**
     * Customize the PrintAttributes, or null to use default settings.
     */
    var customPrintAttributes: PrintAttributes? = null

    /**
     * Delay before converting the HTML to PDF.
     * This is useful for pages that load content dynamically or waiting for animated elements.
     */
    var delayBeforeConvert: Long? = null
//    endregion

// region Private Functions

    private fun processInternal(
        url: String,
        outputFile: File,
        onFinish: IWeb2PdfResultCallback
    ) {
        isTaskStarted = true
        this.outputFile = outputFile
        this.onFinishCallBack = onFinish
        webView.loadUrl(url)
    }

    private fun processDataInternal(
        baseUrl: String? = null,
        data: String,
        mineType: String? = null,
        encoding: String? = null,
        outputFile: File,
        onFinish: IWeb2PdfResultCallback,
    ) {
        isTaskStarted = true
        this.outputFile = outputFile
        this.onFinishCallBack = onFinish
        webView.loadDataWithBaseURL(baseUrl, data, mineType ?: "text/html", encoding ?: "UTF-8", null)
    }
//    endregion

// region Public Functions

    /**
     * Customize the WebView settings.
     * @param block A lambda function to modify the WebView instance.
     */
    fun customizeWebView(block: WebView.() -> Unit) {
        webView.block()
    }

    /**
     * Process the URL and generate a PDF file.
     * @param url The URL to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @throws IllegalStateException if a conversion is already in progress.
     */
    @Throws(IllegalStateException::class)
    fun processUrl(url: String, outputFile: File, onFinish: IWeb2PdfResultCallback) {
        if (isTaskStarted)
            throw IllegalStateException("PDF generation is already in progress.")

        processInternal(url, outputFile, onFinish)
    }

    /**
     * Try to process the URL and generate a PDF file.
     * @param url The URL to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @return true if the conversion started successfully, false if it was already in progress.
     */
    fun tryProcessUrl(url: String, outputFile: File, onFinish: IWeb2PdfResultCallback): Boolean {
        if (isTaskStarted) {
            onFinish.onResult(false, null, IllegalStateException("PDF generation is already in progress."))
            return false
        }

        processInternal(url, outputFile, onFinish)
        return true
    }

    /**
     * Process the HTML file and generate a PDF file.
     * @param htmlFile The HTML file to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @throws IllegalStateException if a conversion is already in progress.
     */
    @Throws(IllegalStateException::class)
    fun processHtmlFile(htmlFile: File, outputFile: File, onFinish: IWeb2PdfResultCallback) {
        if (isTaskStarted)
            throw IllegalStateException("PDF generation is already in progress.")

        processInternal(htmlFile.toURI().toString(), outputFile, onFinish)
    }

    /**
     * Try to process the HTML file and generate a PDF file.
     * @param htmlFile The HTML file to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @return true if the conversion started successfully, false if it was already in progress.
     */
    fun tryProcessHtmlFile(htmlFile: File, outputFile: File, onFinish: IWeb2PdfResultCallback): Boolean {
        if (isTaskStarted) {
            onFinish.onResult(false, null, IllegalStateException("PDF generation is already in progress."))
            return false
        }

        processInternal(htmlFile.toURI().toString(), outputFile, onFinish)
        return true
    }

    /**
     * Process the data and generate a PDF file.
     * @param data The data to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @param options Optional parameters for base URL, MIME type, and encoding.
     * @throws IllegalStateException if a conversion is already in progress.
     */
    @Throws(IllegalStateException::class)
    fun processData(data: String, outputFile: File, options: ProcessDataOptions? = null, onFinish: IWeb2PdfResultCallback) {
        if (isTaskStarted)
            throw IllegalStateException("PDF generation is already in progress.")

        processDataInternal(options?.baseUrl, data, options?.mineType, options?.encoding, outputFile, onFinish)
    }

    /**
     * Try to process the data and generate a PDF file.
     * @param data The data text to convert to PDF.
     * @param outputFile The file where the PDF will be saved.
     * @param onFinish Callback to handle the result of the conversion.
     * @param options Optional parameters for base URL, MIME type, and encoding.
     * @return true if the conversion started successfully, false if it was already in progress.
     */
    fun tryProcessData(data: String, outputFile: File, options: ProcessDataOptions? = null, onFinish: IWeb2PdfResultCallback): Boolean {
        if (isTaskStarted) {
            onFinish.onResult(false, null, IllegalStateException("PDF generation is already in progress."))
            return false
        }

        processDataInternal(options?.baseUrl, data, options?.mineType, options?.encoding, outputFile, onFinish)
        return true
    }
// endregion

    override fun close() {
        webView.destroy()
        currentJob?.cancel(CancellationException("Web2PdfConverter closed"))
    }
}
