package com.bacakita.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.DocumentLimits
import com.bacakita.app.ui.ImportState
import com.bacakita.app.ui.components.InfoCard
import com.bacakita.app.ui.components.SimpleTopBar

@Composable
fun ImportScreen(
    state: ImportState,
    loggedIn: Boolean,
    onPick: (Uri) -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPick(uri)
    }
    Scaffold(topBar = { SimpleTopBar("Impor Dokumen", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Format yang didukung", style = MaterialTheme.typography.titleMedium)
            Text(
                "• TXT\n" +
                    "• PDF — teks biasa dibaca langsung; halaman berupa gambar/hasil pindai dibaca dengan OCR\n" +
                    "• Gambar JPG, PNG, WEBP — teks pada gambar dibaca dengan OCR\n\n" +
                    "Ukuran file maksimal ${DocumentLimits.MAX_FILE_BYTES / (1024 * 1024)} MB. " +
                    "OCR pada PDF dibatasi ${DocumentLimits.MAX_OCR_PAGES} halaman dan bisa memerlukan waktu.",
                style = MaterialTheme.typography.bodyLarge,
            )
            InfoCard("Dokumen dan OCR diproses di perangkat Anda; isi dokumen tidak dikirim ke server atau layanan AI.")
            InfoCard(
                if (loggedIn) "Dokumen yang Anda buka akan tersimpan di Riwayat akun Anda."
                else "Anda belum masuk. Dokumen tetap bisa dibuka, tetapi riwayatnya hilang saat aplikasi ditutup. Masuk lewat tab Akun untuk menyimpannya."
            )

            Button(
                onClick = {
                    onDismissError()
                    launcher.launch(arrayOf("application/pdf", "text/plain", "image/*"))
                },
                enabled = state !is ImportState.Loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Pilih Dokumen") }

            when (state) {
                is ImportState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(16.dp))
                    Text(state.progress ?: "Membaca dokumen…", style = MaterialTheme.typography.bodyLarge)
                }
                is ImportState.Error -> InfoCard("Gagal membuka dokumen. ${state.message}", isError = true)
                ImportState.Idle -> Unit
            }
        }
    }
}
