package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.SourceType
import com.bacakita.app.ui.HistoryItem
import com.bacakita.app.ui.components.InfoCard
import com.bacakita.app.ui.components.SimpleTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sourceLabel(s: SourceType) = when (s) {
    SourceType.PDF -> "PDF"
    SourceType.TXT -> "TXT"
    SourceType.IMAGE -> "Gambar"
    SourceType.MANUAL -> "Teks"
}

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    loggedIn: Boolean,
    onOpen: (HistoryItem) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClearAll: () -> Unit,
    onGoAccount: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("d MMM yyyy HH:mm", Locale("id", "ID"))

    Scaffold(topBar = { SimpleTopBar(if (loggedIn) "Riwayat Dokumen" else "Dokumen Sesi Ini", onBack) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (loggedIn) {
                    InfoCard("Riwayat tersimpan di perangkat ini untuk akun Anda (maksimal 30 dokumen terbaru).")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard(
                            "Anda belum masuk. Dokumen di bawah hanya tersimpan selama aplikasi terbuka dan akan hilang saat aplikasi ditutup. " +
                                "Masuk untuk menyimpan riwayat secara permanen."
                        )
                        Button(onClick = onGoAccount, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Masuk / Daftar")
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Belum ada dokumen. Impor dokumen lewat tab Impor.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(items, key = { it.id }) { item ->
                    Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${sourceLabel(item.source)} • ${"%,d".format(Locale("id", "ID"), item.charCount)} karakter • " +
                                        dateFormat.format(Date(item.importedAt)),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus ${item.title}")
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Hapus Semua") }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus semua dokumen?") },
            text = { Text("Seluruh dokumen pada daftar ini akan dihapus dan tidak dapat dikembalikan.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}
