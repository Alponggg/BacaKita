package com.bacakita.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SourceType { MANUAL, TXT, PDF, IMAGE }

object DocumentLimits {
    const val MAX_FILE_BYTES = 10L * 1024 * 1024   // 10 MB (ditinjau ulang setelah pengujian)
    const val MAX_CHARS = 300_000                  // teks hasil impor
    const val MAX_MANUAL_CHARS = 100_000           // teks yang diketik/ditempel
    const val MAX_PDF_PAGES = 300
    const val MAX_OCR_PAGES = 40                   // OCR lambat; dibatasi per dokumen
    const val MIN_PAGE_TEXT_CHARS = 25             // halaman dengan teks lebih sedikit + ada gambar => OCR
}

sealed interface ImportResult {
    data class Success(
        val title: String,
        val text: String,
        val source: SourceType,
        /** Catatan untuk pengguna (mis. hasil OCR, gambar yang terdeteksi). */
        val notice: String? = null,
    ) : ImportResult

    data class Failure(val message: String) : ImportResult
}

/**
 * F-04: impor TXT, PDF, dan gambar. Semua proses (termasuk OCR) berjalan di perangkat.
 * - PDF: teks diekstrak per halaman; halaman yang hampir tanpa teks tetapi berisi gambar (mis. hasil pindai)
 *   dirender lalu dibaca dengan OCR.
 * - Gambar (JPG/PNG/WEBP): dibaca dengan OCR.
 */
class DocumentImporter(context: Context) {
    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun import(uri: Uri, onProgress: (String) -> Unit = {}): ImportResult = withContext(Dispatchers.IO) {
        try {
            doImport(uri, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            ImportResult.Failure("Aplikasi tidak diizinkan membaca file ini. Pilih ulang file melalui pemilih dokumen.")
        } catch (e: OutOfMemoryError) {
            ImportResult.Failure("File terlalu besar untuk diproses di perangkat ini.")
        } catch (e: Exception) {
            ImportResult.Failure("File tidak dapat dibaca. File mungkin rusak atau tidak didukung.")
        }
    }

    private suspend fun doImport(uri: Uri, onProgress: (String) -> Unit): ImportResult {
        val (name, size) = queryMeta(uri)
        val mime = resolver.getType(uri)
        val ext = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val type = when {
            mime == "application/pdf" || ext == "pdf" -> SourceType.PDF
            mime == "text/plain" || ext == "txt" -> SourceType.TXT
            mime?.startsWith("image/") == true || ext in IMAGE_EXTENSIONS -> SourceType.IMAGE
            else -> return ImportResult.Failure("Format file tidak didukung. Pilih file TXT, PDF, atau gambar (JPG/PNG).")
        }
        val tooBig = "File terlalu besar (maksimal ${DocumentLimits.MAX_FILE_BYTES / (1024 * 1024)} MB)."
        if (size != null && size > DocumentLimits.MAX_FILE_BYTES) return ImportResult.Failure(tooBig)
        if (size == 0L) return ImportResult.Failure("File kosong.")

        val bytes = readBounded(uri) ?: return ImportResult.Failure(tooBig)
        if (bytes.isEmpty()) return ImportResult.Failure("File kosong.")

        val title = name?.substringBeforeLast('.')?.ifBlank { null } ?: "Dokumen"
        var notice: String? = null
        val raw: String = when (type) {
            SourceType.TXT ->
                TextProcessing.reflowBlocks(TextProcessing.normalize(decodeText(bytes)))

            SourceType.IMAGE -> {
                onProgress("Mengenali teks pada gambar…")
                val bmp = decodeBitmap(bytes) ?: return ImportResult.Failure("Gambar tidak dapat dibaca.")
                val text = try { recognize(bmp) } finally { bmp.recycle() }
                notice = "Teks pada gambar dikenali otomatis (OCR) dan mungkin tidak sepenuhnya akurat. Periksa hasilnya."
                TextProcessing.normalize(TextProcessing.reflowPdf(text))
            }

            SourceType.PDF -> when (val r = extractPdf(bytes, onProgress)) {
                is PdfOutcome.Error -> return ImportResult.Failure(r.message)
                is PdfOutcome.Text -> {
                    notice = r.notice
                    TextProcessing.normalize(TextProcessing.reflowPdf(r.value))
                }
            }

            SourceType.MANUAL -> error("tidak berlaku")
        }

        if (raw.isBlank()) {
            return ImportResult.Failure(
                when (type) {
                    SourceType.IMAGE -> "Tidak ada teks yang terdeteksi pada gambar."
                    SourceType.PDF -> "Tidak ada teks yang dapat dikenali pada PDF ini." +
                        (notice?.let { " $it" } ?: "")
                    else -> "File tidak berisi teks."
                }
            )
        }
        var text = raw
        if (text.length > DocumentLimits.MAX_CHARS) {
            text = text.take(DocumentLimits.MAX_CHARS)
            notice = listOfNotNull(notice, "Teks dipotong pada ${DocumentLimits.MAX_CHARS} karakter pertama.").joinToString(" ")
        }
        return ImportResult.Success(title, text, type, notice)
    }

    // ---------- PDF ----------
    private sealed interface PdfOutcome {
        data class Text(val value: String, val notice: String?) : PdfOutcome
        data class Error(val message: String) : PdfOutcome
    }

    private suspend fun extractPdf(bytes: ByteArray, onProgress: (String) -> Unit): PdfOutcome {
        ensurePdfBox()
        val pageTexts = mutableListOf<String>()
        val ocrCandidates = mutableListOf<Int>()
        var imageCount = 0
        var totalPages = 0
        var truncatedPages = false

        try {
            PDDocument.load(bytes).use { doc ->
                if (doc.isEncrypted) return PdfOutcome.Error("PDF ini dilindungi sehingga tidak dapat dibaca.")
                truncatedPages = doc.numberOfPages > DocumentLimits.MAX_PDF_PAGES
                totalPages = minOf(doc.numberOfPages, DocumentLimits.MAX_PDF_PAGES)
                val stripper = PDFTextStripper()
                stripper.setSortByPosition(true)
                for (i in 0 until totalPages) {
                    onProgress("Membaca halaman ${i + 1} dari $totalPages…")
                    stripper.setStartPage(i + 1)
                    stripper.setEndPage(i + 1)
                    val text = stripper.getText(doc).trim()
                    val images = countImages(doc.getPage(i))
                    imageCount += images
                    pageTexts.add(text)
                    if (text.length < DocumentLimits.MIN_PAGE_TEXT_CHARS && images > 0) ocrCandidates.add(i)
                }
            }
        } catch (e: InvalidPasswordException) {
            return PdfOutcome.Error("PDF ini dilindungi kata sandi dan tidak dapat dibuka.")
        } catch (e: IOException) {
            return PdfOutcome.Error("PDF rusak atau tidak dapat dibaca.")
        }

        // OCR untuk halaman berupa gambar (mis. hasil pindai)
        val ocrPages = ocrCandidates.take(DocumentLimits.MAX_OCR_PAGES)
        val ocrTexts = mutableMapOf<Int, String>()
        var ocrFailed = false
        if (ocrPages.isNotEmpty()) {
            try {
                ocrPdfPages(bytes, ocrPages, onProgress, ocrTexts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ocrFailed = true
            }
        }

        val combined = (0 until totalPages).joinToString("\n") { i -> ocrTexts[i] ?: pageTexts[i] }

        val notes = mutableListOf<String>()
        if (imageCount > 0) notes.add("Terdeteksi $imageCount gambar pada dokumen.")
        if (ocrTexts.isNotEmpty()) {
            notes.add("${ocrTexts.size} halaman berupa gambar dibaca dengan OCR dan mungkin tidak sepenuhnya akurat.")
        }
        if (ocrCandidates.size > ocrPages.size) {
            notes.add("OCR dibatasi ${DocumentLimits.MAX_OCR_PAGES} halaman; sisanya tidak dibaca.")
        }
        if (ocrFailed) notes.add("OCR gagal dijalankan pada sebagian atau seluruh halaman gambar.")
        if (imageCount > 0) notes.add("Foto dan ilustrasi tidak ditampilkan di pembaca teks.")
        if (truncatedPages) notes.add("Hanya ${DocumentLimits.MAX_PDF_PAGES} halaman pertama yang diproses.")
        return PdfOutcome.Text(combined, notes.joinToString(" ").ifBlank { null })
    }

    private fun countImages(page: PDPage): Int {
        val resources = page.resources ?: return 0
        var count = 0
        for (name in resources.xObjectNames) {
            try {
                if (resources.getXObject(name) is PDImageXObject) count++
            } catch (e: IOException) {
                // objek rusak diabaikan
            }
        }
        return count
    }

    private suspend fun ocrPdfPages(
        bytes: ByteArray,
        pages: List<Int>,
        onProgress: (String) -> Unit,
        out: MutableMap<Int, String>,
    ) {
        val file = File.createTempFile("ocr_", ".pdf", appContext.cacheDir)
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pages.forEachIndexed { n, pageIndex ->
                        onProgress("Memindai halaman ${pageIndex + 1} dengan OCR (${n + 1}/${pages.size})…")
                        renderer.openPage(pageIndex).use { page ->
                            val scale = minOf(
                                OCR_TARGET_WIDTH.toFloat() / page.width,
                                OCR_MAX_DIMENSION.toFloat() / maxOf(page.width, page.height),
                            )
                            val w = (page.width * scale).toInt().coerceAtLeast(1)
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            try {
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val text = recognize(bmp).trim()
                                if (text.isNotEmpty()) out[pageIndex] = text
                            } finally {
                                bmp.recycle()
                            }
                        }
                    }
                }
            }
        } finally {
            file.delete()
        }
    }

    // ---------- OCR & gambar ----------
    private suspend fun recognize(bitmap: Bitmap): String =
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > OCR_MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    // ---------- Util ----------
    private fun queryMeta(uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
            }
        }
        return name to size
    }

    /** Membaca maksimal MAX_FILE_BYTES; null jika melebihi batas. */
    private fun readBounded(uri: Uri): ByteArray? {
        val input = resolver.openInputStream(uri) ?: throw IOException("Tidak dapat membuka file")
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                if (total > DocumentLimits.MAX_FILE_BYTES) return null
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        var start = 0
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) start = 3
        val buffer = ByteBuffer.wrap(bytes, start, bytes.size - start)
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buffer).toString()
        } catch (e: CharacterCodingException) {
            String(bytes, start, bytes.size - start, charset("windows-1252"))
        }
    }

    private fun ensurePdfBox() {
        if (!pdfReady) {
            synchronized(Companion) {
                if (!pdfReady) {
                    PDFBoxResourceLoader.init(appContext)
                    pdfReady = true
                }
            }
        }
    }

    private companion object {
        @Volatile var pdfReady = false
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        const val OCR_TARGET_WIDTH = 1600
        const val OCR_MAX_DIMENSION = 3000
    }
}
