package com.bacakita.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bacakita.app.data.AiPreferenceInput
import com.bacakita.app.data.AppDatabase
import com.bacakita.app.data.AuthRepository
import com.bacakita.app.data.AuthResult
import com.bacakita.app.data.DocumentEntity
import com.bacakita.app.data.DocumentImporter
import com.bacakita.app.data.ImportResult
import com.bacakita.app.data.PreferencesRepository
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.data.Recommendation
import com.bacakita.app.data.RecommendationClient
import com.bacakita.app.data.RecommendationException
import com.bacakita.app.data.SampleText
import com.bacakita.app.data.SessionStore
import com.bacakita.app.data.SourceType
import com.bacakita.app.data.TextProcessing
import com.bacakita.app.data.User
import com.bacakita.app.tts.TtsController
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderContent(
    val title: String,
    val text: String,
    val source: SourceType,
    val notice: String? = null,
)

/** Satu baris riwayat. saved=true: tersimpan di akun (database); false: hanya selama sesi aplikasi (tamu). */
data class HistoryItem(
    val id: Long,
    val title: String,
    val source: SourceType,
    val importedAt: Long,
    val charCount: Int,
    val saved: Boolean,
)

private class SessionDoc(val id: Long, val content: ReaderContent, val importedAt: Long)

sealed interface ImportState {
    data object Idle : ImportState
    data class Loading(val progress: String? = null) : ImportState
    data class Error(val message: String) : ImportState
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface RecommendationState {
    data object Idle : RecommendationState
    data object Loading : RecommendationState
    data class Success(val recommendation: Recommendation) : RecommendationState
    data class Error(val message: String) : RecommendationState
}

sealed interface UiEvent {
    data object OpenReader : UiEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PreferencesRepository(app)
    private val importer = DocumentImporter(app)
    private val client = RecommendationClient()
    private val tts = TtsController(app)

    private val db = AppDatabase.get(app)
    private val docDao = db.documentDao()
    private val auth = AuthRepository(db.userDao())
    private val session = SessionStore(app)

    // --- Preferensi membaca ---
    private val _prefs = MutableStateFlow(ReaderPreferences())
    val prefs: StateFlow<ReaderPreferences> = _prefs.asStateFlow()
    private val saveRequests = MutableSharedFlow<ReaderPreferences>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // --- Konten ---
    private val _content = MutableStateFlow<ReaderContent?>(null)
    val content: StateFlow<ReaderContent?> = _content.asStateFlow()
    val paragraphs: StateFlow<List<String>> = _content
        .map { c -> c?.let { TextProcessing.toParagraphs(it.text) }.orEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Draf teks manual disimpan di ViewModel (bukan saved-state) agar teks panjang tidak membebani Bundle. */
    var draftText by mutableStateOf("")

    // --- Impor ---
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // --- Akun & riwayat ---
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    /** Dokumen milik tamu: hanya di memori, hilang saat aplikasi ditutup. */
    private val _sessionDocs = MutableStateFlow<List<SessionDoc>>(emptyList())
    private var nextSessionId = -1L

    val history: StateFlow<List<HistoryItem>> = combine(_user, _sessionDocs) { u, s -> u to s }
        .flatMapLatest { (u, s) ->
            if (u != null) {
                docDao.observe(u.id).map { list ->
                    list.map {
                        HistoryItem(
                            id = it.id,
                            title = it.title,
                            source = runCatching { SourceType.valueOf(it.sourceType) }.getOrDefault(SourceType.TXT),
                            importedAt = it.importedAt,
                            charCount = it.charCount,
                            saved = true,
                        )
                    }
                }
            } else {
                flowOf(
                    s.map {
                        HistoryItem(it.id, it.content.title, it.content.source, it.importedAt, it.content.text.length, saved = false)
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- AI ---
    private val _aiInput = MutableStateFlow(AiPreferenceInput())
    val aiInput: StateFlow<AiPreferenceInput> = _aiInput.asStateFlow()
    private val _recState = MutableStateFlow<RecommendationState>(RecommendationState.Idle)
    val recState: StateFlow<RecommendationState> = _recState.asStateFlow()
    private var recJob: Job? = null

    // --- TTS ---
    val ttsState = tts.state

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { _prefs.value = repo.preferences.first() }
        viewModelScope.launch { saveRequests.debounce(300).collect { repo.save(it) } }
        // Pulihkan sesi login (jika ada)
        viewModelScope.launch {
            val id = runCatching { session.userId.first() }.getOrNull() ?: return@launch
            val u = runCatching { auth.getUser(id) }.getOrNull()
            if (u != null) _user.value = u else session.set(null)
        }
    }

    // ===== Preferensi =====
    fun updatePrefs(persistNow: Boolean = false, transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(_prefs.value).sanitized()
        _prefs.value = updated
        if (persistNow) viewModelScope.launch { repo.save(updated) } else saveRequests.tryEmit(updated)
    }

    fun resetPrefs() = updatePrefs(persistNow = true) { ReaderPreferences() }

    // ===== Konten =====
    fun openDraft() {
        val text = TextProcessing.normalize(draftText)
        if (text.isBlank()) return
        openContent(ReaderContent("Teks Manual", text, SourceType.MANUAL))
    }

    fun openSample() = openContent(ReaderContent(SampleText.TITLE, SampleText.TEXT, SourceType.MANUAL))

    private fun openContent(c: ReaderContent) {
        tts.stop()
        _content.value = c
        _events.tryEmit(UiEvent.OpenReader)
    }

    fun importDocument(uri: Uri) {
        if (_importState.value is ImportState.Loading) return
        _importState.value = ImportState.Loading()
        viewModelScope.launch {
            val result = importer.import(uri) { msg -> _importState.value = ImportState.Loading(msg) }
            when (result) {
                is ImportResult.Success -> {
                    val c = ReaderContent(result.title, result.text, result.source, result.notice)
                    saveToHistory(c)
                    _importState.value = ImportState.Idle
                    openContent(c)
                }
                is ImportResult.Failure -> _importState.value = ImportState.Error(result.message)
            }
        }
    }

    fun clearImportError() {
        if (_importState.value is ImportState.Error) _importState.value = ImportState.Idle
    }

    // ===== Riwayat =====
    private suspend fun saveToHistory(c: ReaderContent) {
        val now = System.currentTimeMillis()
        val u = _user.value
        if (u != null) {
            try {
                docDao.insert(
                    DocumentEntity(userId = u.id, title = c.title, sourceType = c.source.name, text = c.text, importedAt = now)
                )
                docDao.trim(u.id, MAX_SAVED_DOCS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Gagal menyimpan tidak boleh menghalangi pengguna membaca dokumen.
            }
        } else {
            _sessionDocs.update { (listOf(SessionDoc(nextSessionId--, c, now)) + it).take(MAX_SESSION_DOCS) }
        }
    }

    fun openHistory(item: HistoryItem) {
        viewModelScope.launch {
            val content: ReaderContent? = if (item.saved) {
                _user.value?.let { u ->
                    docDao.getText(item.id, u.id)?.let { ReaderContent(item.title, it, item.source) }
                }
            } else {
                _sessionDocs.value.firstOrNull { it.id == item.id }?.content
            }
            if (content != null) openContent(content)
        }
    }

    fun deleteHistory(item: HistoryItem) {
        if (item.saved) {
            val u = _user.value ?: return
            viewModelScope.launch { docDao.delete(item.id, u.id) }
        } else {
            _sessionDocs.update { list -> list.filterNot { it.id == item.id } }
        }
    }

    fun clearHistory() {
        val u = _user.value
        if (u != null) viewModelScope.launch { docDao.clear(u.id) } else _sessionDocs.value = emptyList()
    }

    // ===== Akun =====
    fun login(username: String, password: String) = authenticate { auth.login(username, password) }
    fun register(username: String, password: String) = authenticate { auth.register(username, password) }

    private fun authenticate(block: suspend () -> AuthResult) {
        if (_authState.value is AuthUiState.Loading) return
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val r = block()) {
                is AuthResult.Success -> {
                    session.set(r.user.id)
                    _user.value = r.user
                    _authState.value = AuthUiState.Idle
                }
                is AuthResult.Failure -> _authState.value = AuthUiState.Error(r.message)
            }
        }
    }

    fun clearAuthError() {
        if (_authState.value is AuthUiState.Error) _authState.value = AuthUiState.Idle
    }

    fun logout() {
        _user.value = null
        _authState.value = AuthUiState.Idle
        viewModelScope.launch { session.set(null) }
    }

    // ===== Rekomendasi AI =====
    fun updateAiInput(transform: (AiPreferenceInput) -> AiPreferenceInput) = _aiInput.update(transform)

    fun requestRecommendation() {
        recJob?.cancel()
        _recState.value = RecommendationState.Loading
        val input = _aiInput.value
        recJob = viewModelScope.launch {
            try {
                _recState.value = RecommendationState.Success(client.fetch(input))
            } catch (e: RecommendationException) {
                _recState.value = RecommendationState.Error(e.userMessage)
            }
        }
    }

    /** Menerapkan saran. Nilai divalidasi ulang lewat sanitized() sebelum dipakai. */
    fun applyRecommendation(rec: Recommendation) {
        updatePrefs(persistNow = true) {
            it.copy(
                fontSize = rec.fontSize,
                lineSpacing = rec.lineSpacing,
                letterSpacing = rec.letterSpacing,
                theme = rec.theme,
                focusMode = rec.focusMode,
            )
        }
        _recState.value = RecommendationState.Idle
    }

    fun dismissRecommendation() {
        recJob?.cancel()
        _recState.value = RecommendationState.Idle
    }

    // ===== TTS =====
    fun speakFrom(index: Int) = tts.start(paragraphs.value, index)
    fun pauseTts() = tts.pause()
    fun resumeTts() = tts.resume()
    fun stopTts() = tts.stop()

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }

    private companion object {
        const val MAX_SAVED_DOCS = 30
        const val MAX_SESSION_DOCS = 10
    }
}
