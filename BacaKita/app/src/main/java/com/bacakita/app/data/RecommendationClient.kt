package com.bacakita.app.data

import com.bacakita.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Pesan galat yang aman ditampilkan langsung kepada pengguna. */
class RecommendationException(val userMessage: String) : Exception(userMessage)

/** Klien backend BacaKita. Aplikasi TIDAK pernah berbicara langsung dengan Gemini. */
class RecommendationClient(private val baseUrl: String = BuildConfig.API_BASE_URL) {

    suspend fun fetch(input: AiPreferenceInput): Recommendation = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(baseUrl.trimEnd('/') + "/api/v1/recommendations")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { it.write(buildBody(input).toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) throw RecommendationException(errorMessage(code, body))
            try {
                RecommendationParser.parse(body)
            } catch (e: InvalidRecommendationException) {
                throw RecommendationException("Rekomendasi dari server tidak valid dan tidak diterapkan. Silakan coba lagi atau atur manual.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RecommendationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw RecommendationException("Server terlalu lama merespons. Coba lagi nanti atau atur tampilan secara manual.")
        } catch (e: UnknownHostException) {
            throw RecommendationException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.")
        } catch (e: IOException) {
            throw RecommendationException("Gagal terhubung ke server rekomendasi. Periksa koneksi Anda.")
        } catch (e: Exception) {
            throw RecommendationException("Terjadi kesalahan saat meminta rekomendasi.")
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildBody(i: AiPreferenceInput): String = JSONObject().put(
        "preferences",
        JSONObject()
            .put("fontSizePreference", i.fontSize)
            .put("lineSpacingPreference", i.lineSpacing)
            .put("letterSpacingPreference", i.letterSpacing)
            .put("contrastPreference", i.contrast)
            .put("focusModePreferred", i.focusMode),
    ).toString()

    private fun errorMessage(code: Int, body: String): String {
        val fromServer = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        if (fromServer.isNotBlank()) return fromServer
        return when {
            code == 429 -> "Batas penggunaan layanan AI tercapai. Coba lagi nanti atau atur manual."
            code >= 500 -> "Layanan rekomendasi sedang tidak tersedia. Anda tetap dapat mengatur tampilan secara manual."
            else -> "Permintaan rekomendasi ditolak (kode $code)."
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
