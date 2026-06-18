# 🧠 Smart Storage Cleaner AI: Comprehensive Clean-Sweep Ecosystem

**Smart Storage Cleaner AI** is an enterprise-grade, privacy-first, on-device storage optimization and monetization ecosystem for Android. Built with a unified design ethos based on our premium **Organic Obsidian Cyberpunk** theme, this platform delivers complete client-to-server capability encompassing:

1.  **📱 Android App (Jetpack Compose)**: On-device photo similarity embeddings, blurry Laplacians, cached sweeps, and local database vaults.
2.  **⚡ Backend Node.js Engine (Express & MongoDB)**: Real-time user handshake synchronizer, RTDN Play Store receipt managers, and cascade-purge GDPR utilities.
3.  **📊 Administrative Dashboard (React 18 + Vite + Tailwind)**: Executive panel compiling revenue funnels, detailed user ledgers, and live premium toggling.

---
inactive 
```text
    ┌────────────────────────────────────────────────────────┐
    │              📊 React Admin Dashboard (Vite)            │
    │     - Live User Auditing      - Subscription Ledgers    │
    │     - Funnel Conversions      - Retention Cohorts       │
    └───────────────┬────────────────────────┬───────────────┘
                    │ JWT Auth               │ JWT Auth
                    ▼                        ▼
    ┌────────────────────────────────────────────────────────┐
    │             ⚡ Backend REST API (Node.js/Express)      │
    │     - Firebase Token Verifier  - Play Store Billing    │
    │     - Cascade Account Purge    - MongoDB Database      │
    └────────────────────────────────────────▲───────────────┘
                                             │ HTTPS Sync
                                             │
    ┌────────────────────────────────────────┴───────────────┐
    │             📱 Jetpack Compose Android Client          │
    │     - Room DB: Photo Embeds    - DataStore: Preferences│
    │     - TensorFlow Lite Mock     - WorkManager Scheduler │
    └────────────────────────────────────────────────────────┘
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
*   **Smart Similarity Vector Indexer**: Combines decimal hash indices (`dHash`) with a simulated 1024-float embedding model to group visual lookalikes.
*   **Blur & Quality Estimator**: Automatically evaluates photo quality based on Laplacian variance matrices to recommend a clean "Keeper", safely queueing secondary blurry files.
*   **Multi-Category Deep Sweeping**: Direct client scanners targeting bulky **WhatsApp** downloads, temporary **App Caches**, obsolete **Uninstalled APKs**, and massive videos.
*   **Flexible Work Scheduler**: Restricts automatic background cleanups to active Wi-Fi networks and charging slots to protect battery and cell-data usage.
*   **Type-Safe Navigation Compose**: Uses Kotlin `@Serializable` objects to prevent route string typos and parameters mismatches.

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
*   **Single-Step Firebase Synchronization**: Validates secure Google OIDC credentials, registers user profiles within Mongo, and issues custom application JWTs.
*   **Play Billing receipt verification**: Verifies Purchases and active Google Play Console RTDN subscriptions.
*   **Privacy-First Purge Engine**: Deletes user accounts and cascades deleted data (cleanup logs, subscription records, search snapshots) in compliance with Google Play Store GDPR policies.
*   **Admin Audit Control**: Rich analytical routers calculating cumulative bytes freed across the platform.

### ⚙️ Quick Start (Local Backend Running)
1.  **Configure local variables**: Set up `/backend/.env`:
    ```ini
    PORT=3000
    MONGODB_URI="mongodb://localhost:27017/cleaner_ai"
    JWT_SECRET="your_highly_secure_signing_secret"
    ADMIN_SECRET_KEY="highly_secure_admin_bypass_token"
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
*   **Analytical Overview Control**: Charts cumulative clean-up volumes, active subscribers ratio, and average on-device system loads.
*   **Dynamic User Registry Manager**: Directly queries MongoDB to search registered accounts, review storage statistics, and toggle individual entitlements.
*   **Force-Sync Purchase Ledger**: Automatically maps purchase receipt hashes and updates accounts directly.
*   **Premium Comp Entitlement**: Administrators can grant immediate VIP status to testing sandbox accounts directly via the interface with one click.

### ⚙️ Quick Start (Dashboard Dev Mode)
1.  **Enter Environment Parameters**: Create `/dashboard/.env`:
    ```ini
    VITE_API_URL="http://localhost:3000"
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

Record new reference baselines after customized theme alignments:
```bash
gradle :app:recordRoborazziDebug
```

---

## 💡 Benign System Log Notes

During live deployment and updates within the Streaming Android environment, you might notice the following log output in Logcat:
```text
E/InputDispatcher: channel '...' ~ Channel is unrecoverably broken and will be disposed!
```
**What does this represent?**  
This is a standard, benign notice printed by the native Android Input Manager whenever an application is forcefully closed or replaced. Because Google AI Studio terminates the active background process to hot-swap a newly compiled APK, the system window channel disconnects. It is **NOT** an application crash or memory leak; you can safely overlook it during rapid iterative development!
