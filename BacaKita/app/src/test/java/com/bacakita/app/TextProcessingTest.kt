package com.bacakita.app

import com.bacakita.app.data.TextProcessing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessingTest {
    @Test fun normalizeUnifiesLineEndingsAndBlankLines() {
        assertEquals("a\n\nb", TextProcessing.normalize("a\r\n\r\n\r\n\r\nb  "))
    }

    @Test fun paragraphsSplitOnNewlineAndDropBlanks() {
        assertEquals(listOf("satu", "dua"), TextProcessing.toParagraphs("satu\n\n dua \n"))
    }

    @Test fun longParagraphIsChunked() {
        val long = (1..300).joinToString(" ") { "Kalimat nomor $it." }
        val parts = TextProcessing.toParagraphs(long)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 1200 })
    }

    @Test fun hardWrappedBlocksAreJoined() {
        val out = TextProcessing.reflowBlocks("baris satu\nbaris dua\n\nparagraf baru")
        assertEquals("baris satu baris dua\nparagraf baru", out)
    }

    @Test fun pdfLinesJoinUntilSentenceEnd() {
        val out = TextProcessing.reflowPdf("Ini kalimat yang\nterpotong di PDF.\nKalimat baru.")
        assertEquals("Ini kalimat yang terpotong di PDF.\nKalimat baru.", out)
    }
}
