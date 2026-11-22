package com.bacakita.app

import com.bacakita.app.data.InvalidRecommendationException
import com.bacakita.app.data.RecommendationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationParserTest {
    private fun body(theme: String = "\"high_contrast\"", size: String = "22", focus: String = "true") = """
        {"success": true, "recommendation": {"fontSize": $size, "lineSpacingMultiplier": 1.6,
        "letterSpacing": 0.5, "theme": $theme, "focusMode": $focus, "reason": "Titik awal."}}
    """.trimIndent()

    @Test fun validResponseIsParsed() {
        val r = RecommendationParser.parse(body())
        assertEquals(22f, r.fontSize, 0f)
        assertEquals("high_contrast", r.theme)
        assertTrue(r.focusMode)
    }

    @Test fun extremeNumbersAreClamped() {
        assertEquals(40f, RecommendationParser.parse(body(size = "500")).fontSize, 0f)
    }

    @Test(expected = InvalidRecommendationException::class)
    fun unsupportedThemeIsRejected() { RecommendationParser.parse(body(theme = "\"neon\"")) }

    @Test(expected = InvalidRecommendationException::class)
    fun wrongTypeIsRejected() { RecommendationParser.parse(body(focus = "\"ya\"")) }

    @Test(expected = InvalidRecommendationException::class)
    fun nonJsonIsRejected() { RecommendationParser.parse("bukan json") }

    @Test(expected = InvalidRecommendationException::class)
    fun failureFlagIsRejected() { RecommendationParser.parse("""{"success": false}""") }
}
