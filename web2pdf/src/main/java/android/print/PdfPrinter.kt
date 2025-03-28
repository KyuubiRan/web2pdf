// Modified from: https://github.com/mddanishansari/html-to-pdf-convertor
package android.print

import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PrintDocumentAdapter.LayoutResultCallback
import me.kyuubiran.web2pdf.IWeb2PdfResultCallback
import java.io.File
import java.io.IOException

// bypass android package-private
class PdfPrinter(private val attrs: PrintAttributes) {

    private var cancellationSignal: CancellationSignal? = null

    @Throws(IOException::class)
    fun generate(
        adapter: PrintDocumentAdapter,
        outputFile: File,
        onFinish: IWeb2PdfResultCallback
    ) {
        if (outputFile.exists())
            outputFile.delete()

        try {
            Handler(Looper.getMainLooper()).post {
                adapter.onLayout(null, attrs, null, object : LayoutResultCallback() {
                    override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                        try {
                            adapter.onWrite(
                                arrayOf(PageRange.ALL_PAGES),
                                getOutputFile(outputFile),
                                CancellationSignal().also {
                                    cancellationSignal = it
                                    it.setOnCancelListener {
                                        onFinish.onResult(false, null, null)
                                    }
                                },
                                object : PrintDocumentAdapter.WriteResultCallback() {
                                    override fun onWriteFinished(pages: Array<PageRange>) {
                                        super.onWriteFinished(pages)
                                        onFinish.onResult(true, outputFile, null)
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            onFinish.onResult(false, null, e)
                            return
                        }
                    }
                }, null)
            }
        } catch (e: Exception) {
            onFinish.onResult(false, null, e)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun getOutputFile(file: File): ParcelFileDescriptor? {
        val fileDirectory = file.parentFile
        if (fileDirectory != null && !fileDirectory.exists()) {
            fileDirectory.mkdirs()
        }
        try {
            file.createNewFile()
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        } catch (e: IOException) {
            throw e
        }
    }

    fun cancel() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}
