# 🧠 Smart Storage Cleaner AI

**Smart Storage Cleaner AI** is a premium, privacy-first, on-device storage optimization and space-management application for Android. Built entirely with **Jetpack Compose (Material 3)**, **Kotlin**, and modern architectural components, the application delivers a highly responsive experience designed around a unique **Organic Cyberpunk** visual theme.

---

## 🎨 Visual Identity & Design Philosophy

The application rejects common flat blue templates in favor of a bespoke **Organic Obsidian Dark Theme** tailored for high-contrast visual scanning, generous negative space, and a premium edge-to-edge layout:

*   **Obsidian Black Base (`#0F110E`)**: A dark background that minimizes eye strain and maximizes element separation.
*   **Sage Leaf Green Accent (`#B2D183`)**: A radiant, eco-inspired green used for major action buttons, success states, and progress indicators.
*   **Electric Pink Accent (`#FF60BA`) / Sprout Lime (`#D6E6B0`)**: Custom colors reserved for highlighting pro/premium indicators.
*   **Muted Matte Surfaces (`#1E211D` to `#2A2D28`)**: Deep forest midtones that frame interactive cards, borders, and category segments.
*   **A11y Compliance**: Every interactive element satisfies Material 3's minimum **48dp x 48dp touch targets** with standard Material ripples for responsive feedback.

---

## 🔋 Core Capabilities

### 1. Smart Photo Similarity Indexer (On-Device AI)
*   **High-Dimensional Embeddings**: Simulates a 1024-float vector matching pipeline (inspired by MobileNet/TFLite models) to group similar photos.
*   **dHash Matching**: Fast decimal hash calculations that isolate exact duplicate matches, forwarded files, and burst photos.
*   **Smart Recommendation**: Employs an intelligent comparative algorithm based on blur scores (Laplacian variance) and modified times to automatically recommend a "Keeper" image per group, safely marking others for cleaning.

### 2. Multi-Category Deep Junk Space Cleaner
*   **Targeted Collectors**: Dedicated scan categories targeting old **WhatsApp** media downloads, temporary **App Cache**, obsolete **Old APKs**, and large video files.
*   **Real-Time Progress Feed**: Micro-animations and progress updates fueled by asynchronous Kotlin Coroutines and Flows during real-time indexing.

### 3. Rule-Based Customizer & Constraints
*   **Critical Storage Alert**: Slide-based preference adjustment to change system alert thresholds.
*   **Execution Safeguards**: Options to restrict automatic scans to Wi-Fi connectivity or active charging cycles to conserve battery life.

### 4. Subscription & Paywall Framework
*   **Tiered Premium Licenses**: Sleek tier selection showcasing exclusive premium perks (e.g., unlimited background cleaning, experimental high-speed AI scans).
*   **Animated Paywall**: Polished package selectors displaying lifetime, monthly, and annual subscription durations with seamless validation hooks.

---

## 🛠️ Architectural Stack & Engineering Highlights

The codebase strictly adheres to Google's Android Jetpack architecture and modern guidelines:

```
  ┌────────────────────────────────────────────────────────┐
  │                        UI Layer                        │
  │     (Jetpack Compose Screens, Theme, Navigation)       │
  └───────────────────────────┬────────────────────────────┘
                              │ Exposes State (StateFlow)
                              ▼
  ┌────────────────────────────────────────────────────────┐
  │                     ViewModel Layer                    │
  │     (AuthViewModel, CleanerViewModel, SettingsVM)      │
  └───────────────────────────┬────────────────────────────┘
                              │ Calls repository operations
                              ▼
  ┌────────────────────────────────────────────────────────┐
  │                    Repository Layer                    │
  │    (AuthRepository, PhotoRepository, UserPrefRepo)    │
  └───────────────────────────┬────────────────────────────┘
                              │ Stores & Fetches Data
                              ▼
               ┌──────────────┴──────────────┐
               ▼                             ▼
       ┌──────────────┐              ┌──────────────┐
       │   Room DB    │              │  DataStore  │
       │ (Embeddings) │              │ (Settings)   │
       └──────────────┘              └──────────────┘
```

*   **UI Layer**: Implemented using **Jetpack Compose** with Material 3 components.
*   **State Management**: Follows **MVVM (Model-View-ViewModel)** with unidirectionally managed state. States are exposed to UI via immutable `StateFlow` structures.
*   **Structured Persistence**:
    *   **Room Database**: Local SQLite client indexing photo embeddings, blur metrics, and scan results.
    *   **Jetpack DataStore**: Secure Preferences client managing user sessions, preferences, and custom thresholds.
*   **Type-Safe Navigation**: Declared using modern Navigation Compose with argument parsing.
*   **Asynchronous Flow**: Uses Kotlin **Coroutines** mapped to `viewModelScope` to schedule multi-threaded database transactions and avoid UI blocks.

---

## 📂 Project Directory Structure

```text
app/src/main/java/com/example
├── MainActivity.kt                  # NavHost Setup, Navigation Router Entrypoint
├── SmartCleanerApplication.kt       # Firebase + Dependency Injection Setup
├── data                             # Infrastructure & Storage
│   ├── AuthRepository.kt            # Handles OAuth / Sandbox authentication state
│   ├── NetworkClient.kt             # OkHttpClient Interceptors and Retrofit client
│   ├── PhotoCleanerRepository.kt    # Embedding generation, dHash comparative matching
│   ├── UserPreferencesRepository.kt # DataStore settings serializations
│   └── database                     # Room Entities, Daos, Database
│       ├── AppDatabase.kt           # Room Database builder & fallback migrations
│       └── PhotoEmbedding.kt        # PhotoEmbedding schema & queries
├── domain                           # Domain Models
│   └── Models.kt                    # Platform-independent core models (User, JunkItem)
└── ui                               # Beautiful Presentation Layer
    ├── Theme.kt                     # Centralized Organic Cyberpunk Theme definitions
    ├── Color.kt                     # Theme Palette (CyberPrimary, Obsidian Black)
    └── screens                      # Dynamic Screen composables
        ├── HomeScreen.kt            # Available Storage Dashboard & AI Scan Launcher
        ├── DuplicatesScreen.kt      # AI Image group comparison/selection screen
        ├── JunkCleanerScreen.kt     # Deep sweeping utility (Cache, APKs, WhatsApp)
        ├── SettingsScreen.kt        # Advanced Schedule rules and alert threshold configuration
        ├── SignInScreen.kt          # Beautiful Credential-Manager page with Developer Sandbox
        └── PaywallScreen.kt         # Secure premium upgrade and validation interface
```

---

## 🧪 Testing Coverage

The codebase includes an enterprise-grade, local JVM testing harness:

*   **Robolectric**: Executes realistic Activity-level lifecycles, navigation actions, and Compose assertions locally on the JVM without requiring slow emulator boot-ups.
*   **Roborazzi**: Automatically records, verifies, and highlights potential UI regressions at pixel-precision level using screen snapshots.

### Running Assertions & Snapshots
Execute standard unit and screen composition tests:
```bash
gradle :app:testDebugUnitTest
```

Verify visual layout status against reference snapshots (Roborazzi):
```bash
gradle :app:verifyRoborazziDebug
```

Record updated screenshots as standard baseline references after modification:
```bash
gradle :app:recordRoborazziDebug
```

---

## 📦 Build & Deployment

To verify and build a debug Gradle build APK locally:
```bash
gradle assembleDebug
```
The compiled package will be available under:
`/app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔍 Troubleshooting & Logs

### 💡 Understanding Benign "Broken Input Channel" Logs
During development, you may occasionally encounter the following logcat entries:
```text
E/InputDispatcher: channel '...' ~ Channel is unrecoverably broken and will be disposed!
```
**What does this mean?**
This is a standard, benign system-level warning printed by the Android OS's Input Window Manager whenever an active application window or its hosting process is closed, swiped away, or forcefully terminated.

*   This occurs **every time you rebuild or redeploy a new APK version** from Google AI Studio.
*   The system forcefully terminates the active app process to install the new version, causing the window's input buffer (InputChannel) to close.
*   It is **NOT** a crash or memory leak in our application. You can safely ignore this log line during redeployment cycles!
