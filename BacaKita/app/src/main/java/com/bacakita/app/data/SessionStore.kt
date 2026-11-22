package com.bacakita.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(
    name = "session",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Mengingat akun yang sedang masuk agar login bertahan setelah aplikasi ditutup. */
class SessionStore(private val context: Context) {
    private val key = longPreferencesKey("user_id")

    val userId: Flow<Long?> = context.sessionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p -> runCatching { p[key] }.getOrNull() }

    suspend fun set(id: Long?) {
        context.sessionDataStore.edit { p -> if (id == null) p.remove(key) else p[key] = id }
    }
}
