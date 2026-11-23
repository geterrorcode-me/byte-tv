# Byte TV 📺

Aplikasi **Android TV + HP** untuk nonton TV online streaming Indonesia gratis.
Dibuat oleh **ByteAI** (Byte Agent) untuk PrimeByte Studio.

## Fitur
- 📡 Channel TV Indonesia (default: playlist `dhanytv` 920+ channel)
- 🎬 Player HLS (Media3 / ExoPlayer) full-screen
- 📱 Mendukung **Android TV** (leanback launcher + D-pad) & HP (touch)
- 🎨 UI dark mode, ringan, minim dependensi
- ⚡ Auto-load playlist M3U dari URL

## Build APK (GitHub Actions)
APK di-build otomatis di GitHub (gratis, cloud), tanpa perlu Android SDK di mesin lokal.

1. Push ke branch `main`
2. Buka tab **Actions** → build jalan otomatis
3. Download artifact **byte-tv-apk** (file `.apk`)

Atau trigger manual: **Actions → Build ByteTV APK → Run workflow**.

## Konfigurasi Channel
Edit `DEFAULT_PLAYLIST` di `MainActivity.kt` untuk ganti sumber playlist M3U.
Default: `https://raw.githubusercontent.com/dhasap/dhanytv/main/dhanytv.m3u`

## Tech Stack
- Kotlin + Jetpack Compose (Material3)
- Media3 / ExoPlayer (HLS)
- Gradle 8.9 + AGP 8.5.2, JDK 17
- minSdk 23 (Android 6.0+), targetSdk 34

## Lisensi
MIT — bebas dipakai & dimodifikasi.