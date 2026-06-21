# 🧠 Smart Storage Cleaner AI: Comprehensive Clean-Sweep Ecosystem

**Smart Storage Cleaner AI** is an enterprise-grade, privacy-first, on-device storage optimization and monetization ecosystem for Android. Built with a unified design ethos based on our premium **Organic Obsidian Cyberpunk** theme, this platform delivers complete client-to-server capability.

![Project Status: Inactive](https://www.repostatus.org/badges/latest/inactive.svg)

---

## 🏛 Architecture Overview

```text
    ┌────────────────────────────────────────────────────────┐
    │              📊 React Admin Dashboard (Vite)            │
    │     - Live User Auditing      - Recharts Telemetry      │
    │     - Firebase Admin Auth     - Executive Insights      │
    └───────────────┬────────────────────────┬───────────────┘
                    │ JWT Auth               │ HTTPS Sync
                    ▼                        ▼
    ┌────────────────────────────────────────────────────────┐
    │             ⚡ Backend REST API (Node.js/Express)      │
    │     - MongoDB (Mongoose) DB    - Gemini 3.5 AI Engine  │
    │     - Scan Session Analyzer    - Storage Recommendations│
    └────────────────────────────────────────▲───────────────┘
                                             │ REST API
                                             │
    ┌────────────────────────────────────────┴───────────────┐
    │             📱 Jetpack Compose Android Client          │
    │     - Room DB: Photo Embeds    - Jetpack Navigation    │
    │     - WorkManager Scheduler    - TFLite Model          │
    └────────────────────────────────────────────────────────┘
```

---

## 📂 Project Structure

```text
├── app/                  # Module 1: Native Android Application
│   ├── src/main/java/com/example/
│   │   ├── data/         # Repositories (Auth, PhotoCleaner, Settings)
│   │   ├── database/     # Room DB (AppDatabase, PhotoEmbedding)
│   │   ├── ui/           # Jetpack Compose Screens, Theme, Navigation
│   │   ├── viewmodel/    # MVVM Architecture ViewModels
│   │   ├── work/         # AutoCleanWorker for background sweeping
│   │   └── network/      # API Clients
│   └── src/main/assets/  # Contains mobilenet_v3.tflite model
│
├── backend/              # Module 2: Administrative Express Server
│   ├── models/           # Mongoose Schemas (User, ScanSession, CleanupLog)
│   ├── routes/           # API endpoints (auth, scan, cleanup, billing)
│   ├── middleware/       # JWT Auth and Rate Limiter
│   ├── tests/            # Integration and Unit Tests
│   └── server.js         # Entry point connecting to MongoDB
│
└── dashboard/            # Module 3: Executive React Administration Board
    ├── src/pages/        # React Views (Overview, Users, Subscriptions, Funnel)
    ├── src/App.tsx       # Main routing and Sidebar layout
    ├── src/firebase.ts   # Firebase Authentication configuration
    └── vite.config.ts    # Vite bundler settings
```

---

## 🎨 Visual Identity & Premium Aesthetics

The system rejects traditional flat, boring layouts in favor of an **Organic Obsidian Dark Theme** tailored for elite high-contrast scanning and balanced negative space:

| Accent Color | Hex Value | Semantic Usage |
| :--- | :--- | :--- |
| **Obsidian Black Base** | `#0F110E` | High-comfort, eye-safe canvas dark background |
| **Sage Leaf Green** | `#B2D183` | Main call-to-actions, successful cleanup status gauges, micro-success badges |
| **Sprout Lime** | `#D6E6B0` | High-mid contrast segment cards, system bounds metrics, secondary active headers |
| **Electric Pink Neon** | `#FF60BA` | Exclusive VIP premium indicators, high-speed AI mode, pro upgrade packages |
| **Muted Forest Matte** | `#1E211D` | Interactive card surfaces, border strokes, and segment background matrices |

---

## 📱 Module 1: The Native Android Application (`/app`)

Built with **Jetpack Compose (Material 3)**, modern Kotlin-first guidelines, and local persistence tools.

### 🔋 Elite Capabilities
*   **Smart Similarity Vector Indexer**: Combines decimal hash indices (`dHash`) with a simulated `1024-float` embedding model via TensorFlow Lite to group visual lookalikes. Data is stored securely in a local **Room Database** (`PhotoEmbedding`).
*   **Flexible Work Scheduler**: Restricts automatic background cleanups to active Wi-Fi networks and charging slots to protect battery and cell-data usage using **WorkManager** (`AutoCleanWorker.kt`). Clears old APKs and app caches automatically.
*   **Type-Safe Navigation Compose**: Uses decoupled navigation constants for robust transitions across `HOME`, `DUPLICATES`, `JUNK_CLEANER`, and `PAYWALL` screens.

### ⚙️ Quick Start (Android Local Build)
1.  **Configure API Keys Securely**: Enter variables into the secure **Secrets panel in AI Studio** or update your `.env` file in the root:
    ```ini
    API_BASE_URL="https://your-express-endpoint.com"
    ```
2.  **Build Debug App**: Execute compilation using your local Gradle installation:
    ```bash
    gradle assembleDebug
    ```
    The compiled package will be available under:  
    `/app/build/outputs/apk/debug/app-debug.apk`.

---

## ⚡ Module 2: Administrative Express Server (`/backend`)

High-performance Express.js server interfacing with MongoDB database adapters and Google Play Billing APIs.

### 🔋 Core Features
*   **Gemini AI Recommendations**: Integrates with the **Google Generative AI (Gemini 3.5 Flash)** to analyze storage Scan Sessions and produce tailored, actionable advice for users on what to delete first.
*   **Single-Step Firebase Synchronization**: Validates secure Google OIDC credentials, registers user profiles within Mongo, and issues custom application JWTs (`auth.js`).
*   **Privacy-First Purge Engine**: Deletes user accounts and cascades deleted data (cleanup logs, subscription records, search snapshots) in compliance with Google Play Store GDPR policies.

### ⚙️ Quick Start (Local Backend Running)
1.  **Configure local variables**: Set up `/backend/.env`:
    ```ini
    PORT=3000
    MONGODB_URI="mongodb://localhost:27017/smart_cleaner"
    JWT_SECRET="your_highly_secure_signing_secret"
    ADMIN_SECRET_KEY="highly_secure_admin_bypass_token"
    GEMINI_API_KEY="your_gemini_api_key_here"
    ```
2.  **Install dependencies and run**:
    ```bash
    cd backend
    npm install
    npm start
    ```

---

## 📊 Module 3: Executive React Administration Board (`/dashboard`)

A fast, highly integrated React 18 dashboard styled with Tailwind CSS, TypeScript, and Lucide Icons.

### 🔋 Core Features
*   **Analytical Overview Control**: Leverages **Recharts** to plot Area Charts (Signups vs Cleanups), Pie Charts (Revenue Mix), and Bar Charts (Daily Sweeping Composition).
*   **Secure Administration**: Secures the executive panel via Firebase Authentication, ensuring only verified administrators can view Live Telemetry and edit user states.
*   **Dynamic User Registry Manager**: Directly queries MongoDB to search registered accounts, review storage statistics, and toggle individual entitlements.

### ⚙️ Quick Start (Dashboard Dev Mode)
1.  **Enter Environment Parameters**: Create `/dashboard/.env`:
    ```ini
    VITE_API_URL="http://localhost:3000"
    VITE_ADMIN_SECRET_KEY="highly_secure_admin_bypass_token"
    ```
2.  **Initialize Node server**:
    ```bash
    cd dashboard
    npm install
    npm run dev
    ```

---

## 🧪 Verified Assurance Platform

The Android application is paired with a comprehensive JVM unit testing environment:

*   **Robolectric**: Exercises Compose states, ViewModel actions, and Navigation backstack transitions locally on the JVM without requiring slow emulator loads.
*   **Roborazzi**: Takes, stores, and compares exact pixel-precision screenshots of UI components to capture any regressions early.

### Diagnostic Testing Commands
Runs standard behaviors and ViewModel assertions:
```bash
gradle :app:testDebugUnitTest
```

Verify current screen visuals against baseline snapshots (Roborazzi):
```bash
gradle :app:verifyRoborazziDebug
```

---

## 💡 Benign System Log Notes

During live deployment and updates within the Streaming Android environment, you might notice the following log output in Logcat:
```text
E/InputDispatcher: channel '...' ~ Channel is unrecoverably broken and will be disposed!
```
**What does this represent?**  
This is a standard, benign notice printed by the native Android Input Manager whenever an application is forcefully closed or replaced. Because Google AI Studio terminates the active background process to hot-swap a newly compiled APK, the system window channel disconnects. It is **NOT** an application crash or memory leak; you can safely overlook it during rapid iterative development!
