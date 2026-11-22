package com.bacakita.app.data

import org.json.JSONException
import org.json.JSONObject

/** Jawaban pengguna untuk halaman "Preferensi AI". Hanya pilihan terbatas, tanpa teks bebas dan tanpa isi dokumen. */
data class AiPreferenceInput(
    val fontSize: String = "large",
    val lineSpacing: String = "wide",
    val letterSpacing: String = "normal",
    val contrast: String = "normal",
    val focusMode: Boolean = false,
)

/** Saran dari AI. Hanya saran: baru berlaku jika pengguna menerapkannya. */
data class Recommendation(
    val fontSize: Float,
    val lineSpacing: Float,
    val letterSpacing: Float,
    val theme: String,
    val focusMode: Boolean,
    val reason: String,
)

class InvalidRecommendationException(message: String) : Exception(message)

/** Validasi ulang respons di sisi aplikasi (backend juga memvalidasi). */
object RecommendationParser {

    fun parse(body: String): Recommendation {
        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw InvalidRecommendationException("Respons bukan JSON valid")
        }
        if (!root.optBoolean("success", false)) throw InvalidRecommendationException("success bukan true")
        val rec = root.optJSONObject("recommendation")
            ?: throw InvalidRecommendationException("recommendation tidak ada")

        fun number(key: String): Float {
            val v = rec.opt(key)
            if (v !is Number) throw InvalidRecommendationException("$key bukan angka")
            val f = v.toFloat()
            if (f.isNaN() || f.isInfinite()) throw InvalidRecommendationException("$key tidak valid")
            return f
        }

        val theme = rec.opt("theme") as? String
        if (theme == null || theme !in ReaderThemes.all) {
            throw InvalidRecommendationException("tema tidak didukung")
        }
        val focus = rec.opt("focusMode") as? Boolean
            ?: throw InvalidRecommendationException("focusMode bukan boolean")
        val reason = (rec.opt("reason") as? String).orEmpty().trim().take(300)

        return Recommendation(
            fontSize = ReaderLimits.fontSize(number("fontSize")),
            lineSpacing = ReaderLimits.lineSpacing(number("lineSpacingMultiplier")),
            letterSpacing = ReaderLimits.letterSpacing(number("letterSpacing")),
            theme = theme,
            focusMode = focus,
            reason = reason,
        )
    }
}
