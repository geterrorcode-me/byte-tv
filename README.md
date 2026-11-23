# Byte TV 📺

Aplikasi **Android TV + HP** untuk nonton TV online streaming Indonesia gratis.
Dibuat oleh **ByteAI** (Byte Agent) untuk PrimeByte Studio.

## ✨ Fitur
- 📡 **195 channel TV Indonesia** (nasional, berita, religi, TV lokal, TVRI nasional + daerah)
- 🔎 **Search** — cari channel by nama atau kategori
- 📂 **Grup otomatis** — channel dikelompokin per kategori + jumlah channel
- 🎬 **Player HLS** (Media3 / ExoPlayer) full-screen
- 🔑 **Dukungan channel premium** — baca header `#EXTVLCOPT` (User-Agent + Referer) untuk channel yang butuh header khusus (RCTI, MNCTV, GTV, ANTV, Trans7, TransTV)
- 📱 **Android TV** (leanback launcher + D-pad) & **HP** (touch) dalam satu APK
- 🎨 UI dark mode (navy `#121326` + cyan `#02DEEF`), ringan, minim dependensi
- ⚡ **Offline-first** — daftar channel di-bundle di APK, gak perlu download playlist tiap buka
- ⬅️ Tombol Back balik ke daftar channel (bukan keluar app) saat streaming

## 📦 Build APK (GitHub Actions)
APK di-build otomatis di GitHub (gratis, cloud) — tanpa perlu Android SDK di mesin lokal.

1. Push ke branch `main`
2. Buka tab **Actions** → build jalan otomatis
3. Hasil: `app-release.apk` (rilis/produksi) + `app-debug.apk` (testing) di **Releases**

Atau trigger manual: **Actions → Build ByteTV APK → Run workflow**.

## 📥 Download
- **Release (rekomendasi):** https://github.com/geterrorcode-me/byte-tv/releases/latest
- **Langsung (build terbaru):** `app-release.apk`

## ⚙️ Konfigurasi Channel
Edit `app/src/main/assets/channels.m3u` untuk mengubah daftar channel.
Format mendukung:
- `#EXTINF` — info channel + `group-title`
- `#EXTVLCOPT:http-user-agent=` — UA khusus (mis. `NSPlayer`, `DENSGO`, `TVRIKLIK`)
- `#EXTVLCOPT:http-referrer=` — Referer (mis. `https://www.rctiplus.com/`)

Contoh channel premium:
```
#EXTINF:-1 tvg-name="RCTI" group-title="Nasional & Umum",RCTI
#EXTVLCOPT:http-user-agent=NSPlayer/12.00.9651.000
#EXTVLCOPT:http-referrer=https://www.rctiplus.com/
https://liveplus.rctiplus.id/RTMP-IN.m3u8
```

## 🛠️ Tech Stack
- Kotlin + Jetpack Compose (Material3)
- Media3 / ExoPlayer (HLS, per-channel HTTP headers)
- Gradle 8.9 + AGP 8.5.2, JDK 17
- minSdk 23 (Android 6.0+), targetSdk 34
- versionName 1.4.0

## 📜 Lisensi
MIT — bebas dipakai & dimodifikasi.