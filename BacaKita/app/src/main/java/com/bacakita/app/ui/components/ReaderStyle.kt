package com.bacakita.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bacakita.app.data.ReaderFonts
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.ui.theme.ReaderPalette

fun fontFamilyOf(id: String): FontFamily = when (id) {
    ReaderFonts.SERIF -> FontFamily.Serif
    ReaderFonts.MONO -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

/** Gaya teks pembaca. Satuan sp dipakai agar ikut skala font sistem. */
fun readerTextStyle(prefs: ReaderPreferences, palette: ReaderPalette): TextStyle = TextStyle(
    fontFamily = fontFamilyOf(prefs.fontFamily),
    fontSize = prefs.fontSize.sp,
    lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
    letterSpacing = prefs.letterSpacing.sp,
    color = palette.foreground,
)

private const val PREVIEW_TEXT =
    "Contoh teks: Membaca terasa lebih nyaman ketika tampilan sesuai dengan kebutuhan Anda."

/** Pratinjau langsung pengaturan membaca (dipakai di layar Pengaturan dan Rekomendasi). */
@Composable
fun ReaderPreview(prefs: ReaderPreferences, modifier: Modifier = Modifier) {
    val palette = ReaderPalette.of(prefs.theme)
    Surface(
        modifier = modifier.semantics { contentDescription = "Pratinjau tampilan teks" },
        color = palette.background,
        contentColor = palette.foreground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, palette.foreground.copy(alpha = 0.5f)),
    ) {
        Text(
            text = PREVIEW_TEXT,
            style = readerTextStyle(prefs, palette),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        )
    }
}
