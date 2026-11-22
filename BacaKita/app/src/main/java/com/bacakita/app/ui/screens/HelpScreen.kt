package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bacakita.app.ui.components.SimpleTopBar

private val sections = listOf(
    "Tentang BacaKita" to "BacaKita membantu Anda menyesuaikan tampilan teks (ukuran huruf, jarak, tema, mode fokus), mendengarkan teks lewat suara, dan membuka teks dari file TXT/PDF.",
    "Rekomendasi AI" to "Saran pengaturan dibuat oleh AI dan hanya berfungsi sebagai titik awal. BacaKita tidak mendiagnosis kondisi medis dan tidak menjamin peningkatan kemampuan membaca. Anda selalu dapat mengubah atau mengabaikan saran.",
    "Privasi" to "Dokumen, gambar, dan hasil OCR diproses di perangkat dan tidak dikirim ke server. Saat Anda meminta rekomendasi AI, hanya pilihan preferensi (bukan isi teks) yang dikirim ke server BacaKita dan layanan AI eksternal. Akun dan riwayat dokumen disimpan di perangkat ini saja.",
    "Dokumen & OCR" to "Didukung: TXT, PDF, dan gambar JPG/PNG/WEBP. Halaman PDF yang berupa gambar (hasil pindai) dan gambar berisi teks dibaca dengan OCR; hasilnya bisa kurang akurat, terutama pada foto buram atau tulisan tangan. Foto dan ilustrasi tidak dapat dideskripsikan dan tidak ditampilkan di pembaca. PDF yang dilindungi kata sandi tidak dapat dibuka.",
    "Akun & riwayat" to "Tanpa akun, semua fitur tetap dapat dipakai, tetapi riwayat dokumen hilang saat aplikasi ditutup. Dengan akun, dokumen yang Anda buka tersimpan di Riwayat (maksimal 30 terbaru). Akun bersifat lokal dan belum tersinkron antarperangkat; kata sandi yang terlupa tidak dapat dipulihkan.",
    "Text-to-Speech" to "Suara dibacakan oleh mesin Text-to-Speech di perangkat Anda. Jika suara Bahasa Indonesia belum tersedia, unduh data suara di pengaturan Text-to-Speech perangkat. Tombol Jeda menghentikan sementara; Lanjutkan mengulang paragraf yang sedang dibaca dari awal. Pembacaan berhenti saat Anda meninggalkan layar baca.",
    "Pengaturan manual" to "Seluruh pengaturan tampilan tetap dapat diubah secara manual, termasuk saat layanan AI tidak tersedia atau tidak ada koneksi internet.",
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(topBar = { SimpleTopBar("Tentang & Bantuan", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            sections.forEach { (title, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(body, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text("BacaKita v1.0.0 — prototipe akademik", style = MaterialTheme.typography.bodySmall)
        }
    }
}
