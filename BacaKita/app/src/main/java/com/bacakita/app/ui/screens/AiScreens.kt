package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.AiPreferenceInput
import com.bacakita.app.data.ReaderThemes
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.data.Recommendation
import com.bacakita.app.ui.RecommendationState
import com.bacakita.app.ui.components.ChoiceGroup
import com.bacakita.app.ui.components.InfoCard
import com.bacakita.app.ui.components.ReaderPreview
import com.bacakita.app.ui.components.SimpleTopBar
import com.bacakita.app.ui.components.SwitchRow
import kotlin.math.roundToInt

@Composable
fun AiPreferencesScreen(
    input: AiPreferenceInput,
    onChange: ((AiPreferenceInput) -> AiPreferenceInput) -> Unit,
    onSubmit: () -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit,
) {
    var consent by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { SimpleTopBar("Preferensi Membaca", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard(
                "Jawaban Anda akan dikirim ke server BacaKita lalu diteruskan ke layanan AI eksternal (Gemini, Google) " +
                    "untuk menyusun saran pengaturan. Yang dikirim hanya pilihan di bawah ini; isi teks atau dokumen Anda tidak dikirim. " +
                    "Hasilnya hanya saran, bukan diagnosis atau jaminan kenyamanan membaca."
            )
            ChoiceGroup(
                "Ukuran huruf yang Anda inginkan",
                listOf("normal" to "Normal", "large" to "Besar", "extra_large" to "Sangat besar"),
                input.fontSize,
            ) { v -> onChange { it.copy(fontSize = v) } }
            ChoiceGroup(
                "Jarak antarbaris",
                listOf("normal" to "Normal", "wide" to "Lebar", "extra_wide" to "Sangat lebar"),
                input.lineSpacing,
            ) { v -> onChange { it.copy(lineSpacing = v) } }
            ChoiceGroup(
                "Jarak antarhuruf",
                listOf("normal" to "Normal", "wide" to "Lebar"),
                input.letterSpacing,
            ) { v -> onChange { it.copy(letterSpacing = v) } }
            ChoiceGroup(
                "Kontras tampilan",
                listOf("normal" to "Normal (terang)", "high" to "Kontras tinggi", "low_glare" to "Redup / lembut di mata"),
                input.contrast,
            ) { v -> onChange { it.copy(contrast = v) } }
            SwitchRow(
                title = "Mode fokus",
                description = "Saya ingin paragraf lain diredupkan saat membaca.",
                checked = input.focusMode,
                onCheckedChange = { c -> onChange { it.copy(focusMode = c) } },
            )

            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .toggleable(value = consent, role = Role.Checkbox, onValueChange = { consent = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = consent, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Saya mengerti dan setuju pilihan di atas dikirim ke layanan AI eksternal.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Button(
                onClick = onSubmit,
                enabled = consent,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Minta Rekomendasi") }
            OutlinedButton(
                onClick = onManual,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Atur Manual Saja") }
        }
    }
}

@Composable
fun AiRecommendationScreen(
    state: RecommendationState,
    currentPrefs: ReaderPreferences,
    onApply: (Recommendation) -> Unit,
    onApplyAndEdit: (Recommendation) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { SimpleTopBar("Rekomendasi", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                RecommendationState.Idle -> {
                    Text("Belum ada rekomendasi.", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Kembali") }
                }

                RecommendationState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(16.dp))
                        Text("Menyusun rekomendasi… (server gratis bisa memerlukan beberapa detik)", style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = onDismiss) { Text("Batalkan") }
                }

                is RecommendationState.Error -> {
                    InfoCard(state.message, isError = true)
                    Text("Anda tetap dapat mengatur tampilan secara manual.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Coba Lagi") }
                    OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Atur Manual") }
                }

                is RecommendationState.Success -> {
                    val rec = state.recommendation
                    val preview = currentPrefs.copy(
                        fontSize = rec.fontSize,
                        lineSpacing = rec.lineSpacing,
                        letterSpacing = rec.letterSpacing,
                        theme = rec.theme,
                        focusMode = rec.focusMode,
                    ).sanitized()

                    Text("Saran pengaturan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    ReaderPreview(preview, Modifier.fillMaxWidth().heightIn(max = 220.dp))
                    InfoCard(
                        "Ukuran huruf: ${rec.fontSize.roundToInt()} sp\n" +
                            "Jarak antarbaris: ${"%.1f".format(rec.lineSpacing)}×\n" +
                            "Jarak antarhuruf: ${"%.1f".format(rec.letterSpacing)} sp\n" +
                            "Tema: ${ReaderThemes.label(rec.theme)}\n" +
                            "Mode fokus: ${if (rec.focusMode) "Aktif" else "Nonaktif"}"
                    )
                    if (rec.reason.isNotBlank()) {
                        Text("Alasan: ${rec.reason}", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        "Ini hanya saran awal dari AI, bukan diagnosis. Anda dapat menerapkan, mengubah, atau mengabaikannya.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { onApply(rec) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Terapkan") }
                    OutlinedButton(onClick = { onApplyAndEdit(rec) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("Terapkan lalu Sesuaikan")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Abaikan") }
                }
            }
        }
    }
}
