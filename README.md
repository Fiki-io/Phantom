# 👻 Phantom - Production-Ready YouTube Client for Android

**Phantom** adalah client YouTube Android modern yang dirancang untuk performa tinggi, kebebasan kustomisasi antarmuka (100% Native Jetpack Compose), dan **kebal dari pembaruan enkripsi YouTube** menggunakan arsitektur **Ghost Player**.

---

## 💎 Fitur Utama

- 👻 **Ghost Player Architecture**: Menjalankan player resmi YouTube di balik layar tanpa UI YouTube (`controls=0`). Memecahkan tantangan **BotGuard** dan **PO Token (Proof of Origin)** Google secara otomatis melalui runtime Chromium Android. Bebas repot update scraper setiap kali YouTube update cipher.
- 🌊 **Liquid Glass (Glassmorphism) UI/UX**: Tampilan latar belakang obsidian/gelap pekat dipadukan dengan tombol, kartu, scrubber timeline, navigation dock, dan kontrol pemutar bernuansa kaca cair (*refractive multi-stop border gradient, frosted glass tint, specular gloss highlights*).
- ⚡ **Zero Mock Data (100% Real Live Engine)**: Home feed (kategori Musik, Gaming, Berita, Podcast, Teknologi), live search dengan auto-complete suggestions Google, dan antrean "Up Next" diambil langsung secara real-time dari server YouTube (**InnerTube API**).
- 🛡️ **SponsorBlock Otomatis**: Integrasi langsung dengan API publik SponsorBlock. Bagian promosi/sponsor otomatis dilewati dengan notifikasi mengambang dan opsi tombol **Batalkan (Undo)**.
- 📜 **Riwayat Lokal Privat (Room Database)**: Menyimpan riwayat video dan titik *resume playback* (detik terakhir ditonton) secara lokal di memori HP. Tidak butuh login akun Google, bebas pelacakan, dan bisa dihapus kapan saja.
- 🔖 **Koleksi & Favorit**: Simpan video favorit ke dalam database lokal dengan satu ketukan.
- 🎧 **Background Audio & MediaSession**: Tetap memutar audio saat layar HP dimatikan atau membuka aplikasi lain. Dilengkapi kontrol notifikasi resmi Android 13+ (squiggly progress bar) dan dukungan tombol Bluetooth / TWS.
- 📺 **Picture-in-Picture (PiP)**: Otomatis masuk ke mode layar kecil melayang saat menekan tombol Home.

---

## 🏗️ Struktur Arsitektur Proyek

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── player.html                 # Ghost Player HTML5 Template (controls=0)
├── java/com/phantom/tube/
│   ├── PhantomApp.kt               # Application entry point & Singletons
│   ├── MainActivity.kt             # Single Activity with Compose Navigation & PiP
│   ├── core/
│   │   ├── database/
│   │   │   ├── PhantomDatabase.kt  # Room Database instance
│   │   │   ├── WatchHistoryEntity.kt & Dao
│   │   │   └── FavoriteEntity.kt & Dao
│   │   └── theme/
│   │       ├── Color.kt            # Obsidian Dark & Liquid Glass tokens
│   │       ├── LiquidGlass.kt      # Modifier.liquidGlass & Modifier.liquidGlassButton
│   │       ├── Theme.kt            # PhantomTheme Material 3
│   │       └── Type.kt             # Typography
│   ├── data/
│   │   ├── innertube/
│   │   │   ├── InnerTubeClient.kt  # Real HTTP client to /browse, /search, /next
│   │   │   └── InnerTubeParser.kt  # JSON parser for videoRenderer & lockupViewModel
│   │   ├── model/
│   │   │   └── VideoModels.kt      # VideoItem, VideoDetail, NextQueue, SponsorSegment
│   │   ├── repository/
│   │   │   └── PhantomRepository.kt# Central Repository Layer
│   │   └── sponsorblock/
│   │       └── SponsorBlockClient.kt # Live SponsorBlock skip segments client
│   ├── player/
│   │   ├── PhantomPlayerBridge.kt  # @JavascriptInterface bidirectional bridge
│   │   ├── PhantomPlayerView.kt    # Controller & Compose AndroidView
│   │   ├── PlayerState.kt          # Reactive player state (seek, buffer, rate)
│   │   └── service/
│   │       └── PhantomMediaService.kt # Foreground Service & MediaSessionCompat
│   └── ui/
│       ├── components/
│       │   ├── LiquidGlassBottomNav.kt # Floating Glass Navigation Dock
│       │   ├── LiquidGlassButton.kt    # Glass IconButtons & Category Chips
│       │   ├── LiquidGlassCard.kt      # Glassmorphic Video Item Card
│       │   ├── LiquidGlassSlider.kt    # Custom Scrubber with Buffer & Glow Thumb
│       │   ├── LiquidGlassTopBar.kt    # Brand Header & Search Trigger
│       │   └── SponsorSkipPill.kt      # Floating Glass Alert with Undo
│       └── screens/
│           ├── history/HistoryScreen.kt # Local Watch History with Resume Bar
│           ├── home/HomeScreen.kt       # Live YouTube Category Feed
│           ├── library/LibraryScreen.kt # Bookmarked Video Collection
│           ├── player/PlayerScreen.kt   # 100% Native Player Screen with Gestures
│           └── search/SearchScreen.kt   # Live Search with Real-time Suggestions
└── res/                            # Vector icons, themes, strings, XML rules
```

---

## 🚀 Cara Menjalankan & Membangun APK

### Menggunakan Android Studio
1. Buka folder `/home/gardenxxxxx/Downloads/youtube` di **Android Studio (Ladybug / Iguana atau lebih baru)**.
2. Tunggu Gradle Sync selesai secara otomatis.
3. Hubungkan perangkat fisik Android (atau emulator) dengan USB Debugging aktif.
4. Klik **Run** (Shift + F10) atau pilih menu `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

### Menggunakan Command Line
```bash
# Menjalankan unit test
./gradlew testDebugUnitTest

# Membangun file APK Debug
./gradlew assembleDebug
```
File APK akan dihasilkan di: `app/build/outputs/apk/debug/app-debug.apk`.
