# Phantom

Aplikasi YouTube client untuk Android berbasis Jetpack Compose, tanpa iklan, mendukung pemutaran audio di latar belakang, integrasi SponsorBlock, dan navigasi bubble dock.

## Fitur Utama

- **Bebas Iklan**: Pemutaran video dan musik tanpa gangguan iklan.
- **Background Playback**: Audio tetap berjalan saat layar dimatikan atau saat berpindah aplikasi.
- **Picture-in-Picture (PiP)**: Masuk otomatis ke mode PiP saat keluar dari video.
- **SponsorBlock**: Melewati segmen sponsor, self-promo, intro, dan outro secara otomatis dengan opsi pembatalan (undo).
- **YouTube Dark Theme & Bubble Dock**: Antarmuka gelap yang nyaman untuk layar AMOLED dengan floating bubble navigation dock di bagian bawah.
- **Riwayat & Koleksi Lokal**: Riwayat tontonan, riwayat pencarian, dan bookmark video disimpan secara lokal di perangkat menggunakan Room Database.
- **Pengaturan Pemutar Lengkap**:
  - Sleep Timer (pengatur waktu tidur)
  - Pengatur kecepatan pemutaran (0.25x - 2.0x)
  - Durasi lompatan ketuk dua kali (5s - 30s)
  - Mode Audio Only (layar mati untuk menghemat daya baterai)
  - Mode Loop pemutaran ulang

## Persyaratan Sistem

- Android 8.0 (Oreo / API level 26) ke atas
- Arsitektur prosesor: armeabi-v7a, arm64-v8a, x86, x86_64

## Build & Instalasi

Proyek ini menggunakan Gradle dan GitHub Actions untuk proses build otomatis.

### Build via GitHub Actions
Setiap kali commit di-push ke branch `main`, workflow GitHub Actions di `.github/workflows/build.yml` akan secara otomatis melakukan kompilasi dan menyediakan file APK debug di tab **Actions** -> **Artifacts**.

### Build Lokal
```bash
./gradlew assembleDebug
```
Hasil file APK berada di:
`app/build/outputs/apk/debug/app-debug.apk`

## Lisensi
Proyek ini dikembangkan untuk penggunaan pribadi dan edukasi.
