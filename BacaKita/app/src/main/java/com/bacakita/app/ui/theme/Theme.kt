package com.bacakita.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bacakita.app.data.ReaderThemes

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B5C7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFEAF5),
    onPrimaryContainer = Color(0xFF00212E),
    secondary = Color(0xFF4A6572),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7CC4E0),
    onPrimary = Color(0xFF00212E),
    primaryContainer = Color(0xFF0B4A63),
    onPrimaryContainer = Color(0xFFCFEAF5),
    secondary = Color(0xFFB0C9D6),
)

/** Tema aplikasi mengikuti pengaturan sistem; tema membaca diatur terpisah lewat [ReaderPalette]. */
@Composable
fun BacaKitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content,
    )
}

/** Palet untuk area baca (Reader dan pratinjau). */
@Immutable
data class ReaderPalette(
    val background: Color,
    val foreground: Color,
    val surface: Color,
    val accent: Color,
    val onAccent: Color,
) {
    companion object {
        private val Light = ReaderPalette(Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFFF1F1F1), Color(0xFF0B5C7A), Color(0xFFFFFFFF))
        private val Dark = ReaderPalette(Color(0xFF121212), Color(0xFFE8E8E8), Color(0xFF1E1E1E), Color(0xFF7CC4E0), Color(0xFF00212E))
        private val HighContrast = ReaderPalette(Color(0xFF000000), Color(0xFFFFFF00), Color(0xFF1C1C1C), Color(0xFFFFFF00), Color(0xFF000000))
        private val Sepia = ReaderPalette(Color(0xFFF4ECD8), Color(0xFF3B2F1E), Color(0xFFEADFC3), Color(0xFF7A4B12), Color(0xFFFFFFFF))

        fun of(theme: String): ReaderPalette = when (theme) {
            ReaderThemes.DARK -> Dark
            ReaderThemes.HIGH_CONTRAST -> HighContrast
            ReaderThemes.SEPIA -> Sepia
            else -> Light
        }
    }
}
