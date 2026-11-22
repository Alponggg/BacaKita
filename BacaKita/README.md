# BacaKita

Aplikasi Android (Kotlin + Jetpack Compose) untuk menyesuaikan pengalaman membaca: pembaca teks aksesibel,
rekomendasi pengaturan berbasis Gemini (lewat backend), Text-to-Speech, dan impor TXT/PDF.
Diturunkan dari PRD v1.0 (nama produk di PRD, "AksesBaca", diganti menjadi **BacaKita**).

## Struktur
```
BacaKita/
├── app/                      # Aplikasi Android
│   └── src/main/java/com/bacakita/app/
│       ├── data/             # ReaderPreferences, DataStore, klien API, impor dokumen, pemrosesan teks
│       ├── tts/              # TtsController (pembungkus Android TextToSpeech)
│       └── ui/               # MainViewModel, navigasi, screens/, components/, theme/
└── backend/                  # FastAPI + Gemini (main.py, tests/)
```

## Menjalankan backend
```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export GEMINI_API_KEY=...            # dari Google AI Studio (jangan di-commit)
export GEMINI_MODEL=gemini-2.5-flash # sesuaikan dengan model free tier yang tersedia
uvicorn main:app --reload --port 8000
pytest                               # 11 tes backend (Gemini di-mock)
```

## Menjalankan aplikasi
1. Buka folder `BacaKita/` di **Android Studio** (Ladybug atau lebih baru, JDK 17), tunggu Gradle sync.
2. Emulator: backend lokal otomatis dijangkau lewat `http://10.0.2.2:8000/` (build debug mengizinkan HTTP).
3. Perangkat fisik / rilis: set `BACAKITA_API_URL=https://backend-anda/` di `gradle.properties`
   (rilis hanya mengizinkan HTTPS).
4. Fitur selain rekomendasi AI (pembaca, TTS, impor, pengaturan) berjalan tanpa backend.

Unit test Android: `./gradlew test` (preferensi, parser rekomendasi, pemrosesan teks).

## Keputusan yang diambil (PRD bagian 18)
| Item | Keputusan |
|---|---|
| minSdk | 26 (Android 8.0; dibutuhkan PBKDF2-SHA256); target/compile 35 |
| Ekstraksi PDF | `pdfbox-android` 2.0.27.0 (teks) + `PdfRenderer` & ML Kit Text Recognition (OCR on-device untuk halaman gambar) |
| Rentang | Font 14–40 sp, jarak baris 1.0–2.5×, jarak huruf 0–3 sp (konstanta `ReaderLimits`, sama di backend) |
| Batas file | 10 MB, teks hasil impor 300.000 karakter, teks manual 100.000 karakter, OCR maks. 40 halaman PDF |
| Tema | Terang, Gelap, Kontras Tinggi, Sepia |
| Model Gemini | dari env `GEMINI_MODEL` (default `gemini-2.5-flash`) — verifikasi ketersediaan & kuota free tier saat deploy |
| Room | dipakai untuk akun lokal dan riwayat dokumen (maks. 30 dokumen terbaru per akun) |

## Fitur tambahan (revisi 2)
- **Deteksi gambar & OCR:** PDF dianalisis per halaman. Halaman yang hampir tanpa teks tetapi berisi gambar (mis. hasil pindai) dirender dan dibaca dengan OCR. File gambar (JPG/PNG/WEBP) juga dapat diimpor. Jumlah gambar yang terdeteksi dan halaman yang di-OCR ditampilkan sebagai catatan di atas teks Reader. Foto/ilustrasi tidak dapat dideskripsikan.
- **Logo:** placeholder ada di `app/src/main/res/drawable/app_logo.xml`. Hapus file itu lalu salin logo Anda dengan nama `app_logo.png`. Ikon peluncur (launcher) diganti lewat Android Studio: klik kanan `res` > New > Image Asset.
- **Login:** akun lokal (nama pengguna + kata sandi, PBKDF2-SHA256 + salt). Login bertahan setelah aplikasi ditutup. Tanpa login, dokumen tetap bisa dibuka dan tampil di Riwayat selama sesi, tetapi hilang saat aplikasi ditutup. Dengan login, riwayat tersimpan permanen di database perangkat.
  Keterbatasan: akun dan riwayat hanya ada di perangkat itu (tanpa sinkronisasi, tanpa pemulihan kata sandi).
- **Menu bawah:** Beranda, Impor, Riwayat, Pengaturan, Akun.

## Perilaku yang perlu diketahui
- Request ke AI hanya berisi pilihan enum; **isi teks/dokumen tidak pernah dikirim**. Ada persetujuan eksplisit sebelum meminta rekomendasi.
- Respons AI divalidasi dua kali (backend + aplikasi): tipe, rentang (di-clamp), tema harus dikenal; klaim medis pada `reason` diganti teks netral.
- Jeda TTS = berhenti dan mengingat paragraf; Lanjutkan mengulang paragraf itu dari awal (Android TTS tidak punya pause asli).
- TTS berhenti saat meninggalkan layar Reader (juga saat rotasi layar).
- Mode fokus: paragraf selain yang terpilih/sedang dibacakan diredupkan dan jarak paragraf diperlebar.
- Hosting gratis bisa mengalami cold start; aplikasi menampilkan status loading dan pesan galat, pengaturan manual tetap tersedia.

## Status verifikasi
- Backend: 11 tes otomatis lulus (dijalankan).
- Unit test Android (preferensi, parser, teks, autentikasi) ditulis tetapi belum dijalankan.
- Aplikasi Android: **belum dikompilasi/dijalankan** di lingkungan pembuatan (tanpa Android SDK). Lakukan sync + build di Android Studio,
  lalu isi hasil pengujian aktual (T-01…T-12 di PRD) pada laporan.
