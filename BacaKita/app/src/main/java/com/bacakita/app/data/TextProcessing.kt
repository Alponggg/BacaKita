package com.bacakita.app.data

/** Fungsi murni untuk menyiapkan teks sebelum ditampilkan / dibacakan. */
object TextProcessing {
    private const val MAX_PARAGRAPH_CHARS = 1200

    fun normalize(raw: String): String = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace("\u0000", "")
        .replace(Regex("[ \t]+\n"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    /**
     * TXT yang dibungkus per-baris (hard wrap): bila ada baris kosong sebagai pemisah paragraf,
     * baris-baris di dalam satu blok digabung menjadi satu paragraf.
     * Bila tidak ada baris kosong, setiap baris dianggap satu paragraf.
     */
    fun reflowBlocks(text: String): String {
        if (!text.contains("\n\n")) return text
        return text.split(Regex("\n\\s*\n")).joinToString("\n") { block ->
            block.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        }
    }

    /** PDF: baris yang tidak diakhiri tanda baca akhir kalimat digabung dengan baris berikutnya. */
    fun reflowPdf(raw: String): String {
        val sb = StringBuilder()
        var prevEndsSentence = true
        for (line in raw.lines()) {
            val t = line.trim()
            if (t.isEmpty()) {
                if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                prevEndsSentence = true
                continue
            }
            if (sb.isNotEmpty() && sb.last() != '\n') {
                if (prevEndsSentence) sb.append('\n') else sb.append(' ')
            }
            sb.append(t)
            prevEndsSentence = t.last() in ".!?:;\"'”’)"
        }
        return sb.toString().trim()
    }

    /** Memecah teks menjadi paragraf (satu baris = satu paragraf) dan memecah paragraf yang terlalu panjang. */
    fun toParagraphs(text: String): List<String> =
        text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.flatMap { chunk(it) }

    private fun chunk(p: String): List<String> {
        if (p.length <= MAX_PARAGRAPH_CHARS) return listOf(p)
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        for (sentence in p.split(Regex("(?<=[.!?])\\s+"))) {
            if (sentence.length > MAX_PARAGRAPH_CHARS) {
                if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() }
                out.addAll(sentence.chunked(MAX_PARAGRAPH_CHARS))
                continue
            }
            if (cur.isNotEmpty() && cur.length + sentence.length + 1 > MAX_PARAGRAPH_CHARS) {
                out.add(cur.toString()); cur.clear()
            }
            if (cur.isNotEmpty()) cur.append(' ')
            cur.append(sentence)
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }
}
