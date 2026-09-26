# Notes Client App (`notesClientApp`)

Cross-platform client application for **NotesAlltogether**, built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

## 📱 Supported Platforms

- 🤖 **Android** (Jetpack Compose / Android 7.0+ API 24+)
- 🍏 **iOS** (SwiftUI + Compose Multiplatform)
- 🌐 **Web** (Kotlin/Wasm & Compose Multiplatform for Web)

---

## 🏗️ Architecture

```
notesClientApp/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/         # Shared Compose UI, state management & models
│   │   ├── androidMain/        # Android Activity & lifecycle integration
│   │   ├── iosMain/            # UIViewController entry point for iOS
│   │   └── wasmJsMain/         # Web/Wasm entry point & canvas setup
└── iosApp/                     # Native Xcode / SwiftUI project hosting ComposeView
```

---

## 🛠️ Prerequisites

- **JDK 17** or higher
- **Android Studio** (Koala / Ladybug or newer with Android SDK 34 installed)
- **Xcode** (for building iOS app on macOS)
- **Node.js** (for running Web/Wasm dev server)

---

## 💻 Running the Application

### 🌐 Web (Wasm)
```powershell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun --continuous
```
Opens in your browser at `http://localhost:8080` (or the port specified by webpack).

### 🤖 Android
Open the project in Android Studio, select the `composeApp` run configuration, choose an emulator or connected device, and click **Run**.

Or from the command line:
```powershell
.\gradlew.bat :composeApp:installDebug
```

### 🍏 iOS (macOS required)
Open `iosApp/iosApp.xcodeproj` in Xcode and click **Run** targeting an iPhone Simulator or physical device.

---

## 📦 Building

### Web Distribution:
```powershell
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
```
The output files will be in `composeApp/build/dist/wasmJs/productionExecutable/`.

### Android APK:
```powershell
.\gradlew.bat :composeApp:assembleDebug
```

---

## 📤 Publishing to GitHub

To push this repository to GitHub:

1. Create a new empty repository on [GitHub](https://github.com/new), named e.g. `notes-client-app`.
2. Link remote and push:
```bash
git remote add origin https://github.com/<YOUR_USERNAME>/notes-client-app.git
git branch -M main
git push -u origin main
```
