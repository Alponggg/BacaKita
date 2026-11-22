package com.bacakita.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.readerDataStore by preferencesDataStore(
    name = "reader_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** F-05: penyimpanan preferensi membaca secara lokal (tanpa akun). */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val THEME = stringPreferencesKey("theme")
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
    }

    /** Data rusak / tipe salah tidak menyebabkan crash: kembali ke nilai awal. */
    val preferences: Flow<ReaderPreferences> = context.readerDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            runCatching {
                val d = ReaderPreferences()
                ReaderPreferences(
                    fontSize = p[Keys.FONT_SIZE] ?: d.fontSize,
                    fontFamily = p[Keys.FONT_FAMILY] ?: d.fontFamily,
                    lineSpacing = p[Keys.LINE_SPACING] ?: d.lineSpacing,
                    letterSpacing = p[Keys.LETTER_SPACING] ?: d.letterSpacing,
                    theme = p[Keys.THEME] ?: d.theme,
                    focusMode = p[Keys.FOCUS_MODE] ?: d.focusMode,
                ).sanitized()
            }.getOrDefault(ReaderPreferences())
        }

    suspend fun save(prefs: ReaderPreferences) {
        val s = prefs.sanitized()
        context.readerDataStore.edit { p ->
            p[Keys.FONT_SIZE] = s.fontSize
            p[Keys.FONT_FAMILY] = s.fontFamily
            p[Keys.LINE_SPACING] = s.lineSpacing
            p[Keys.LETTER_SPACING] = s.letterSpacing
            p[Keys.THEME] = s.theme
            p[Keys.FOCUS_MODE] = s.focusMode
        }
    }
}
