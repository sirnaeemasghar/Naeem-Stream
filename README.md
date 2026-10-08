# Naeem Stream

<p align="center">
  <strong>Your media, your way.</strong><br>
  A modern, high-performance, open-source video and audio downloader for Android.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_10+-3DDC84?logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="UI">
  <img src="https://img.shields.io/badge/Engine-yt--dlp_%2B_FFmpeg-FF0000" alt="Engine">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Version-1.1.0-brightgreen" alt="Version">
</p>

---

## 📥 Download the App

Download the latest pre-built Android APK ready for installation on your phone or emulator:

👉 **[Download Naeem Stream v1.1.0 APK](https://github.com/sirnaeemasghar/Naeem-Stream/releases/latest)**

### Quick Install via ADB
If you have your device or BlueStacks connected:
```bash
adb install -r Naeem-Stream-v1.1.0.apk
```

---

## 🌟 Overview

**Naeem Stream** is a personal, multi-platform media downloader designed for speed, flexibility, and complete privacy. It processes video extraction and downloading directly on your Android device using **yt-dlp** and **FFmpeg**, without routing your data through any third-party servers.

Whether you want to save a tutorial in 4K, grab a reel from Instagram or TikTok, or download a music playlist as high-quality audio files, **Naeem Stream** gives you full control over resolutions, formats, and storage locations.

---

## ✨ Features

- **🌐 Multi-Platform Support**: Paste or share links from **YouTube, Instagram Reels, Facebook, TikTok, X (Twitter), Reddit**, and hundreds of other supported platforms.
- **🎬 All Available Qualities**: Select any detected stream resolution up to **4K (2160p)**, 2K (1440p), 1080p Full HD, 720p HD, 480p, 360p, or 240p.
- **🎵 Audio-Only Extraction**: Extract high-quality audio tracks directly in **M4A format** with FFmpeg post-processing.
- **📁 Custom Download Location**: Choose where your files are saved in device storage (`Downloads/NaeemVideos`, `Downloads/NaeemStream`, or any custom folder) using Android's MediaStore API.
- **⚡ Sequential Background Queue**: Download single videos or entire playlists sequentially with automatic retry on network drops, progress tracking, and persistent job states.
- **🔄 In-App Engine Updates**: Check for and atomically install official `yt-dlp` updates verified against official SHA-256 checksums to stay ahead of platform changes.
- **🔒 100% Private & On-Device**: No accounts, no subscriptions, no telemetry, no ads, and no proxy servers. All processing happens locally on your hardware.
- **🎨 Premium Material 3 UI**: Built with Jetpack Compose featuring elegant dark/light themes, smooth card geometry, responsive layout, and refined typography.

---

## 🛠️ What We Solved

1. **Bypassed YouTube Anti-Bot Challenges**: 
   Configured custom Innertube player client extractors (`tv_downgraded`, `android`) that bypass YouTube web anti-bot rate limits and `HTTP 403: Forbidden / YouTube declined this request` errors.
2. **Bundled Standalone Engine**:
   Integrated bundled executable assets to guarantee the latest compatible engine is unpacked and executable on first run, preventing outdated AAR library fallbacks.
3. **Adaptive Format Detection**:
   Extracted both standard landscape and vertical resolutions (such as `1080x1920` reels/shorts), giving users full freedom to choose their preferred quality.
4. **Scoped Storage Optimization**:
   Integrated MediaStore relative path routing to allow custom folder organization under `Downloads/` without requiring intrusive storage permissions on Android 10+.

---

## 🏗️ Architecture & Tech Stack

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (Screens.kt · LibraryScreens.kt · StreamNestTheme)   │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                      ViewModel                         │
│                  (MainViewModel.kt)                    │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                 Download Repository                    │
│     (Persistent SQLite Queue & MediaStore Publisher)   │
└───────────────▲───────────────────────────▲────────────┘
                │                           │
┌───────────────┴───────────────┐ ┌─────────┴────────────┐
│      YtDlpEngine Adapter      │ │    MediaStorage      │
│  (yt-dlp + FFmpeg + QuickJS)  │ │ (Scoped Storage SAF) │
└───────────────────────────────┘ └──────────────────────┘
```

- **Language**: Kotlin 2.1.0
- **UI Toolkit**: Jetpack Compose with Material 3
- **Media Engine**: [youtubedl-android](https://github.com/yausername/youtubedl-android) (`yt-dlp` + `FFmpeg`)
- **Concurrency**: Kotlin Coroutines & Structured Concurrency
- **Image Loading**: Coil Compose 2.7.0
- **Minimum Android Version**: Android 10 (API 29+)

---

## 🚀 Building from Source

### Prerequisites
- [Android Studio Ladybug | 2024.2+](https://developer.android.com/studio)
- JDK 17 or higher
- Android SDK 36 (compileSdk 36, minSdk 29)

### Build Commands

Clone the repository and build the debug APK:

```bash
# Clone the repository
git clone https://github.com/sirnaeemasghar/Naeem-Stream.git
cd Naeem-Stream

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Open Source & Contributing

Naeem Stream is free and open-source software! Contributions, bug reports, and feature suggestions are welcome:

1. **Fork** the repository.
2. Create a new branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a **Pull Request**.

---

## 👨‍💻 Developer & Credits

- **Developer**: Naeem Asghar
- **GitHub**: [@sirnaeemasghar](https://github.com/sirnaeemasghar)
- **Contact / Email**: [contact.naeemasghar@gmail.com](mailto:contact.naeemasghar@gmail.com)

**Made in Pakistan ❤️**

---

## 📄 License & Legal Notice

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details.

*Disclaimer: Naeem Stream is an independent open-source project and is not affiliated with or endorsed by YouTube, Meta, TikTok, X, or any other media provider. Please only download content that you own or have explicit permission to download.*
