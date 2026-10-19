# 🔮 Notey — Liquid Glass Notes & Media Journal

[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B_(API_24%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_BOM_2024.09.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Design-Material_3_Glassmorphism-purple)](https://m3.material.io)
[![Release](https://img.shields.io/github/v/release/yathinkrishnak8-glitch/notey?color=brightgreen)](https://github.com/yathinkrishnak8-glitch/notey/releases)

**Notey** is a modern, privacy-focused Android note-taking and media journaling application featuring a sleek **Liquid Glass (Glassmorphic)** aesthetic. Built entirely with Jetpack Compose and Material 3, Notey delivers a responsive, tactile, and visually immersive experience.

---

## ✨ Features

### 🔮 Liquid Glass Design
- **Multi-layer Glass Cards**: Translucent vertical gradient overlays with frosted glass surfaces and specular edge highlights.
- **Glowing Ambient Accents**: Soft, glowing borders and subtle drop shadows that respond dynamically to note color tags and global themes.

### 🎨 5 Dynamic Ambient Themes
Switch themes anytime with instant UI updates:
1. **Amethyst Glow** (Default): Mystical deep violet and luminous neon purple gradients.
2. **Aurora Borealis**: Serene nocturnal cyan, emerald green, and teal lightwaves.
3. **Sunset Ember**: Warm twilight coral, amber, and golden horizon shades.
4. **Midnight Obsidian**: Ultra-clean deep space charcoal with crisp neon blue accents.
5. **Frost Light**: Crisp, bright frosted-crystal aesthetic for well-lit environments.

### 📌 Note Pinning Hierarchy
- Pin essential notes, checklists, and memories to keep them fixed at the top of your stream.
- Quick one-tap toggle for pinning both on note cards and within the reader view.

### 📖 Reader / Preview Mode vs ✍️ Editor Mode
- **Dedicated Reader Mode**:
  - Distraction-free presentation of your notes.
  - Word counter and formatted timestamps (*Created & Last Modified*).
  - High-resolution media gallery supporting images, videos, and files.
  - Quick actions: Share note as plain text, toggle pin, edit note, or delete.
- **Focused Editor Mode**:
  - Effortless title and body composition.
  - Real-time accent palette picker (7 vibrant glass tint options).
  - One-tap attachment drawer to attach photos, videos, and files.
  - Live attachment preview chips with instant removal.

### 🎬 Built-in Popup Media Player & Viewer
- **Image Lightbox**: View high-resolution photos in a clean, dark modal with zoom support.
- **Native Video Player**: Embedded Android `VideoView` with playback controls (`MediaController`) for smooth local playback.
- **File Opener**: View metadata for attached documents and open them in external apps via Android system intents.

### 🛡️ Privacy & Offline Architecture
- **100% Offline**: All notes and preferences are saved locally on device.
- **Internal Storage Sandboxing**: Attached media files are safely cloned into app-private internal storage (`context.filesDir/attachments/`), preventing broken links even if originals are moved or deleted.
- **Zero External SDK Bloat**: Built using standard Android Framework APIs (`BitmapFactory`, `VideoView`, `MediaController`) without third-party tracking or heavy dependencies.

---

## 📱 App Overview

```
┌─────────────────────────────────────────────────────────────┐
│ 🔮 Notey: Liquid Glass Notes                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🎨 Theme Selector: [Amethyst] [Aurora] [Sunset] [Dark]   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ 📌 PINNED                                                   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✦ Project Blueprint                        📌 [Amethyst]│ │
│ │ Next milestone checklist and wireframe renders...       │ │
│ │ 📷 2 Photos  🎬 1 Video                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ 📝 ALL NOTES                                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✦ Reading List & Thoughts                     [Emerald] │ │
│ │ Highlights from latest research papers...               │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│                                                     [ + ]   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0.20
- **UI Framework**: Jetpack Compose (BOM `2024.09.00`)
- **Design System**: Material Design 3 (`androidx.compose.material3:material3`)
- **Activity & Lifecycle**: `androidx.activity:activity-compose:1.9.2`
- **Data Persistence**: SharedPreferences JSON serialization with model migrations
- **File Management**: App-sandboxed internal storage (`context.filesDir`)
- **Media Playback**: Android Framework `android.widget.VideoView`, `android.widget.MediaController`, `android.graphics.BitmapFactory`
- **Build System**: Gradle 8.14.3 with Android Gradle Plugin 8.7.0

---

## 🚀 Building & Running

### Prerequisites
- Android SDK 35 (Minimum SDK: 24, Target SDK: 35)
- Java 17+ (or bundled JDK in Android Studio)
- Gradle 8.14+

### Clone the Repository
```bash
git clone https://github.com/yathinkrishnak8-glitch/notey.git
cd notey
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install onto Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Download

Get the latest pre-built APK directly from GitHub Releases:
👉 **[Download Notey v1.0 APK](https://github.com/yathinkrishnak8-glitch/notey/releases/latest)**

---

## 📄 License

This project is licensed under the Apache 2.0 License.
