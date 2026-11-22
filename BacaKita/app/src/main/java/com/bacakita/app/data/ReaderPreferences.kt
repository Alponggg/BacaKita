package com.bacakita.app.data

/** Rentang nilai pengaturan. Nilai di luar rentang selalu dipaksa kembali ke batas ini. */
object ReaderLimits {
    const val FONT_SIZE_MIN = 14f
    const val FONT_SIZE_MAX = 40f
    const val FONT_SIZE_DEFAULT = 18f

    const val LINE_SPACING_MIN = 1.0f
    const val LINE_SPACING_MAX = 2.5f
    const val LINE_SPACING_DEFAULT = 1.5f

    const val LETTER_SPACING_MIN = 0f
    const val LETTER_SPACING_MAX = 3f
    const val LETTER_SPACING_DEFAULT = 0f

    private fun clamp(value: Float, min: Float, max: Float, default: Float): Float =
        if (value.isNaN() || value.isInfinite()) default else value.coerceIn(min, max)

    fun fontSize(v: Float) = clamp(v, FONT_SIZE_MIN, FONT_SIZE_MAX, FONT_SIZE_DEFAULT)
    fun lineSpacing(v: Float) = clamp(v, LINE_SPACING_MIN, LINE_SPACING_MAX, LINE_SPACING_DEFAULT)
    fun letterSpacing(v: Float) = clamp(v, LETTER_SPACING_MIN, LETTER_SPACING_MAX, LETTER_SPACING_DEFAULT)
}

object ReaderThemes {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val HIGH_CONTRAST = "high_contrast"
    const val SEPIA = "sepia"

    val all = listOf(LIGHT, DARK, HIGH_CONTRAST, SEPIA)

    fun label(id: String): String = when (id) {
        LIGHT -> "Terang"
        DARK -> "Gelap"
        HIGH_CONTRAST -> "Kontras Tinggi"
        SEPIA -> "Sepia"
        else -> "Terang"
    }
}

object ReaderFonts {
    const val SANS = "sans"
    const val SERIF = "serif"
    const val MONO = "mono"

    val all = listOf(SANS, SERIF, MONO)

    fun label(id: String): String = when (id) {
        SANS -> "Sans-serif"
        SERIF -> "Serif"
        MONO -> "Monospace"
        else -> "Sans-serif"
    }
}

/** Pengaturan membaca (model F-01 / F-05). */
data class ReaderPreferences(
    val fontSize: Float = ReaderLimits.FONT_SIZE_DEFAULT,
    val fontFamily: String = ReaderFonts.SANS,
    val lineSpacing: Float = ReaderLimits.LINE_SPACING_DEFAULT,
    val letterSpacing: Float = ReaderLimits.LETTER_SPACING_DEFAULT,
    val theme: String = ReaderThemes.LIGHT,
    val focusMode: Boolean = false,
) {
    /** Memastikan semua nilai valid; dipakai setelah membaca penyimpanan, input pengguna, dan rekomendasi AI. */
    fun sanitized(): ReaderPreferences = copy(
        fontSize = ReaderLimits.fontSize(fontSize),
        fontFamily = if (fontFamily in ReaderFonts.all) fontFamily else ReaderFonts.SANS,
        lineSpacing = ReaderLimits.lineSpacing(lineSpacing),
        letterSpacing = ReaderLimits.letterSpacing(letterSpacing),
        theme = if (theme in ReaderThemes.all) theme else ReaderThemes.LIGHT,
    )
}
