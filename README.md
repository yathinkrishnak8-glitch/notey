# 🪶 Notey — Modern Notes, Visual Canvas & Workflow Workspace

[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B_(API_28%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_BOM_2024.02.00-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Design-Material_Design_3-purple)](https://m3.material.io)
[![Release](https://img.shields.io/github/v/release/yathinkrishnak8-glitch/notey?color=brightgreen)](https://github.com/yathinkrishnak8-glitch/notey/releases)

> **"Capture anything. Organize everything. Connect ideas."**  
> *Built by Yathin*

**Notey** is a modern, lightweight, customizable notes and productivity workspace built with Jetpack Compose and Material Design 3. Designed to be fast, simple by default, and powerful when needed, Notey enables you to capture thoughts, organize ideas into folders and tags, visually diagram concepts on a freeform canvas, and map processes in a connected workflow builder.

---

## ✨ Features & Capabilities

### 1. 🏠 Home Dashboard
- **Personalized Header**: Greeting and daily overview with quick access to global search.
- **Quick Action Bar**: One-tap access to create a *Note*, *Checklist*, *Canvas*, or *Folder*.
- **Pinned Items**: Fixed elevated section for essential notes and active workflows.
- **Recent Notes Stream**: Recent thoughts and checklists sorted by last modified timestamp.
- **Canvases & Workflows Carousel**: Quick-switch horizontally scrollable cards for visual whiteboards.

### 2. 📝 Notes System
- **Reader Mode (Preview)**:
  - Clean reading layout with word counters and formatted timestamps (*Created & Last Modified*).
  - High-resolution media gallery and one-tap Android text sharing.
  - Quick action toolbar: pin toggle, favorite star, edit, and delete.
- **Editor Mode**:
  - Formatting toolbar with quick inserts: **Bold**, *Italic*, Headings (`# H1`), Task Checklists (`☑️ Task`), Code blocks, and File Attachments.
  - Accent tint color selector (7 vibrant palette tags).
  - Folder categorization and tag assignment.
  - Automatic debounced saving to prevent content loss.
- **Organization & Filters**:
  - Filter chips: `[All]`, `[Recent]`, `[Favorites ★]`, `[Checklists]`, and custom `[Folders 📁]`.
  - Folder creation and management dialog.

### 3. 🔀 Visual Canvas & Workflow Builder
- **Freeform 2D Canvas**: Smooth pan gestures and zoom controls (`- 100% +`), center/reset view button.
- **Interactive Nodes**:
  - Node types: **Start**, **End**, **Task / Step**, **Decision** (diamond branch), **Action**, **Sticky Note**, and **Shape**.
  - Drag-and-drop node positioning with live connector recalculation.
  - Shape styles: rounded rectangles, pills, circles, and diamonds.
- **Connectors & Directional Arrows**:
  - Link any two nodes with directional arrows and optional branch labels (`Yes`, `No`, `Next`).
  - Interactive "Connect ➜" tool for rapid diagramming.
- **Multiple Workspaces**:
  - Switch between independent Canvases and Flowcharts.
  - Built-in templates: *Project Flow* (Canvas) and *Content Creation Workflow* (Flowchart).

### 4. 📁 Library & Media Player
- **Universal Attachment Manager**: Aggregate all photos, videos, and documents attached across all notes.
- **Filter Chips**: Filter by `[All]`, `[Images]`, `[Videos]`, or `[Documents]`.
- **In-App Media Player Popup**:
  - **Image Lightbox**: High-resolution image preview powered by `BitmapFactory`.
  - **Native Video Player**: Embedded `VideoView` with playback transport controls (`MediaController`).
  - **Document Launcher**: System intent opener via secure Android `FileProvider`.

### 5. ⚙️ Settings — Full Customization Center
- **🎨 Appearance**:
  - Theme modes: **Light**, **Dark**, and **Follow System**.
  - 8 Accent color swatches and 7 curated gradient theme presets (*Amethyst Glow, Aurora Borealis, Sunset Ember, Midnight Obsidian, Emerald Grove, Rose Petal, Frost Light*).
  - UI Shape style: **Rounded (20dp)** vs **Compact (12dp)** with live interactive preview card.
- **⚡ Animation & Interaction**:
  - Toggle animations on/off, reduce-motion mode, animation speed control (*Slow, Normal, Fast*), and haptic feedback.
- **📝 Notes & Editing**:
  - Default sorting (*Modified, Created, Title*), auto-save toggle, and delete confirmation dialogs.
- **🔀 Canvas Preferences**:
  - Alignment grid toggle (dots pattern) and snap-to-grid option.
- **📁 Files & Storage**:
  - Storage statistics for sandboxed attachments, temporary cache cleaner, and JSON backup export.
- **🛡️ Data & Privacy**:
  - 100% on-device local storage. No network tracking or cloud sync telemetry. Clear trash or reset all data with explicit safeguards.
- **ℹ️ About Notey**:
  - Version info (1.0.0, Build 1), feather emblem, developer attribution (**Built by Yathin**), and direct **"View Source Code"** button opening the GitHub repository.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android 8.0+ (API 28+)
- **Language**: Kotlin 2.0.20
- **UI Framework**: Jetpack Compose (BOM `2024.02.00`)
- **Design System**: Material Design 3 (`androidx.compose.material3`)
- **Security**: AndroidX `FileProvider` with sandboxed internal file storage
- **Media**: Native Android Framework APIs (`BitmapFactory`, `VideoView`, `MediaController`)
- **Persistence**: SharedPreferences JSON serialization with model migrations
- **Build System**: Gradle 8.14.3 with Android Gradle Plugin 8.7.0

---

## 🚀 Building & Running

### Clone the Repository
```bash
git clone https://github.com/yathinkrishnak8-glitch/notey.git
cd notey
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install onto Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Download

Get the pre-built APK directly from GitHub Releases:
👉 **[Download Notey APK Releases](https://github.com/yathinkrishnak8-glitch/notey/releases)**

---

## 📄 License & Attribution

- **Developer**: Built by Yathin
- **Repository**: [https://github.com/yathinkrishnak8-glitch/notey](https://github.com/yathinkrishnak8-glitch/notey)
- **License**: Apache 2.0
