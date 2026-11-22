package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bacakita.app.ui.components.AppLogo
import com.bacakita.app.ui.components.MenuCard

@Composable
fun HomeScreen(
    username: String?,
    hasContent: Boolean,
    onContinue: () -> Unit,
    onInput: () -> Unit,
    onAi: () -> Unit,
    onSample: () -> Unit,
    onHelp: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppLogo(size = 72.dp)
                Column {
                    Text(
                        "BacaKita",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (username != null) "Halo, $username" else "Anda belum masuk (mode tamu)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                "Satu konten, cara membaca yang sesuai untuk setiap pengguna.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (hasContent) {
                MenuCard(Icons.Default.AutoStories, "Lanjutkan Membaca", "Buka kembali teks yang terakhir dibuka", onContinue, highlighted = true)
            }
            MenuCard(Icons.Default.Edit, "Masukkan Teks", "Ketik atau tempel teks untuk dibaca", onInput)
            MenuCard(Icons.Default.AutoAwesome, "Rekomendasi Pengaturan AI", "Dapatkan saran tampilan membaca sebagai titik awal", onAi)
            MenuCard(Icons.Default.Description, "Coba dengan Teks Contoh", "Lihat pembaca dan Text-to-Speech tanpa menyiapkan file", onSample)
            MenuCard(Icons.Default.Info, "Tentang & Bantuan", "Keterbatasan aplikasi dan informasi privasi", onHelp)
        }
    }
}
