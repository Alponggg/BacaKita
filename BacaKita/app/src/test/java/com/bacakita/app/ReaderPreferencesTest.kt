package com.bacakita.app

import com.bacakita.app.data.ReaderLimits
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.data.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPreferencesTest {
    @Test fun valuesOutOfRangeAreClamped() {
        val s = ReaderPreferences(fontSize = 999f, lineSpacing = -5f, letterSpacing = 50f).sanitized()
        assertEquals(ReaderLimits.FONT_SIZE_MAX, s.fontSize, 0f)
        assertEquals(ReaderLimits.LINE_SPACING_MIN, s.lineSpacing, 0f)
        assertEquals(ReaderLimits.LETTER_SPACING_MAX, s.letterSpacing, 0f)
    }

    @Test fun nanFallsBackToDefault() {
        val s = ReaderPreferences(fontSize = Float.NaN).sanitized()
        assertEquals(ReaderLimits.FONT_SIZE_DEFAULT, s.fontSize, 0f)
    }

    @Test fun unknownThemeAndFontFallBack() {
        val s = ReaderPreferences(theme = "neon", fontFamily = "comic").sanitized()
        assertEquals(ReaderThemes.LIGHT, s.theme)
        assertEquals("sans", s.fontFamily)
    }
}
