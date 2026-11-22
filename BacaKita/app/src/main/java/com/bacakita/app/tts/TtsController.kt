package com.bacakita.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EngineStatus { INITIALIZING, READY, UNAVAILABLE, LANGUAGE_MISSING }
enum class PlaybackStatus { IDLE, PLAYING, PAUSED }

data class TtsState(
    val engine: EngineStatus = EngineStatus.INITIALIZING,
    val playback: PlaybackStatus = PlaybackStatus.IDLE,
    /** Indeks paragraf yang sedang dibacakan (atau terakhir dibacakan saat jeda). */
    val currentIndex: Int = -1,
    val message: String? = null,
) {
    val needsEngineSettings: Boolean
        get() = engine == EngineStatus.UNAVAILABLE || engine == EngineStatus.LANGUAGE_MISSING
}

/**
 * F-03: pembungkus TextToSpeech Android.
 * Teks dibacakan per paragraf dengan antrean kecil (look-ahead) agar dokumen panjang tidak membanjiri antrean mesin.
 * Android TTS tidak punya "pause" asli: Jeda = berhenti dan mengingat paragraf; Lanjutkan = mengulang paragraf tersebut dari awal.
 */
class TtsController(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val lock = Any()
    private var tts: TextToSpeech? = null
    private var paragraphs: List<String> = emptyList()
    private var generation = 0     // membatalkan callback dari sesi lama
    private var nextToQueue = 0

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val (gen, index) = parse(utteranceId) ?: return
            synchronized(lock) {
                if (gen != generation) return
                _state.update { it.copy(playback = PlaybackStatus.PLAYING, currentIndex = index) }
            }
        }

        override fun onDone(utteranceId: String?) {
            val (gen, index) = parse(utteranceId) ?: return
            synchronized(lock) {
                if (gen != generation) return
                if (index >= paragraphs.lastIndex) {
                    _state.update { it.copy(playback = PlaybackStatus.IDLE, currentIndex = -1) }
                } else if (nextToQueue < paragraphs.size) {
                    speakLocked(nextToQueue, TextToSpeech.QUEUE_ADD)
                    nextToQueue++
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            handleError(utteranceId)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            handleError(utteranceId)
        }
    }

    init {
        tts = TextToSpeech(appContext) { status -> onInit(status) }
    }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _state.update {
                it.copy(
                    engine = EngineStatus.UNAVAILABLE,
                    message = "Mesin Text-to-Speech tidak tersedia di perangkat ini. Pastikan mesin suara (misalnya Google Text-to-Speech) terpasang dan aktif.",
                )
            }
            return
        }
        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(listener)
        val result = engine.setLanguage(Locale.forLanguageTag("id-ID"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.update {
                it.copy(
                    engine = EngineStatus.LANGUAGE_MISSING,
                    message = "Suara Bahasa Indonesia belum tersedia. Unduh data suara di pengaturan Text-to-Speech perangkat.",
                )
            }
        } else {
            _state.update { it.copy(engine = EngineStatus.READY, message = null) }
        }
    }

    fun start(list: List<String>, fromIndex: Int) {
        synchronized(lock) {
            val s = _state.value
            if (s.engine != EngineStatus.READY) {
                if (s.engine == EngineStatus.INITIALIZING) {
                    _state.update { it.copy(message = "Mesin suara masih disiapkan. Coba lagi sebentar lagi.") }
                }
                return
            }
            if (list.isEmpty()) return
            val from = fromIndex.coerceIn(0, list.lastIndex)
            generation++
            paragraphs = list
            tts?.stop()
            _state.update { it.copy(playback = PlaybackStatus.PLAYING, currentIndex = from, message = null) }
            val end = minOf(list.size, from + LOOKAHEAD)
            for (i in from until end) {
                speakLocked(i, if (i == from) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD)
            }
            nextToQueue = end
        }
    }

    fun pause() {
        synchronized(lock) {
            if (_state.value.playback != PlaybackStatus.PLAYING) return
            generation++
            tts?.stop()
            _state.update { it.copy(playback = PlaybackStatus.PAUSED) }
        }
    }

    fun resume() {
        synchronized(lock) {
            val s = _state.value
            if (s.playback != PlaybackStatus.PAUSED) return
            start(paragraphs, s.currentIndex)
        }
    }

    fun stop() {
        synchronized(lock) {
            generation++
            tts?.stop()
            _state.update { it.copy(playback = PlaybackStatus.IDLE, currentIndex = -1) }
        }
    }

    /** Melepas sumber daya TTS. Dipanggil saat ViewModel dibersihkan. */
    fun shutdown() {
        synchronized(lock) {
            generation++
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    private fun speakLocked(index: Int, mode: Int) {
        val engine = tts ?: return
        val result = engine.speak(paragraphs[index], mode, null, "$generation:$index")
        if (result == TextToSpeech.ERROR) {
            generation++
            _state.update {
                it.copy(playback = PlaybackStatus.IDLE, currentIndex = -1, message = "Gagal memulai pembacaan suara.")
            }
        }
    }

    private fun handleError(utteranceId: String?) {
        val (gen, _) = parse(utteranceId) ?: return
        synchronized(lock) {
            if (gen != generation) return
            generation++
            tts?.stop()
            _state.update {
                it.copy(
                    playback = PlaybackStatus.IDLE,
                    currentIndex = -1,
                    message = "Terjadi kesalahan saat membacakan teks. Coba lagi.",
                )
            }
        }
    }

    private fun parse(id: String?): Pair<Int, Int>? {
        val parts = id?.split(":") ?: return null
        if (parts.size != 2) return null
        val g = parts[0].toIntOrNull() ?: return null
        val i = parts[1].toIntOrNull() ?: return null
        return g to i
    }

    private companion object {
        const val LOOKAHEAD = 3
    }
}
