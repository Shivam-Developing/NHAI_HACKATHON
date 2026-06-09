# 🛡️ Datalake Face Auth — NHAI Hackathon 7.0

**Datalake Face Auth** is a highly secure, **offline-first** biometric authentication and multi-stage liveness verification Android client built for NHAI's remote field operations. Engineered with **Kotlin**, **Jetpack Compose (Material 3)**, **Google ML Kit Face Detection**, and **TensorFlow Lite (MobileFaceNet)**, it delivers sub-100ms on-device AI identification — no cloud dependency required.

> 🏆 Built for **NHAI Hackathon 7.0** — Problem Statement: Secure Biometric Identity Verification for Remote Highway Inspection Sites

---

## 📋 Table of Contents

1. [System Architecture](#1-system-architecture)
2. [Key Features](#2-key-features)
3. [🎬 Demo Video](#3-demo-video)
4. [Download & Install APK](#4-download--install-apk)
5. [Data Lake API Integration](#5-data-lake-api-integration)
6. [Local Developer REST Server](#6-local-developer-rest-server)
7. [Identification Model — Technical Deep Dive](#7-identification-model--technical-deep-dive)
8. [Liveness Detection Protocol](#8-liveness-detection-protocol)
9. [Background Auto-Sync](#9-background-auto-sync)
10. [Security Hardening](#10-security-hardening)
11. [Tech Stack](#11-tech-stack)
12. [Source Directory Map](#12-source-directory-map)

---

## 1. System Architecture

The application is built on a clean **MVVM** (Model-View-ViewModel) pattern with strict decoupling between hardware-level camera analysis, ML inference, UI rendering, and local storage.

```
┌──────────────────────────────────────────────────────────────┐
│                     Compose UI (Presentation)                 │
│    Dashboard  /  EnrollmentScanner  /  AuthScanner Screens   │
└────────────────────────────┬─────────────────────────────────┘
                             │  collectAsStateWithLifecycle()
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                   FaceAuthViewModel (State)                   │
│   Phase Manager · Liveness Orchestrator · Sync Trigger       │
└────────────────────────────┬─────────────────────────────────┘
                             │  Repository calls
                             ▼
┌──────────────────────────────────────────────────────────────┐
│          FaceAuthRepository  +  AppDatabase (Room)           │
│   UserFace embeddings · SyncEvent log · DAO abstractions     │
└───────────┬────────────────────────────────┬─────────────────┘
            │  Camera frames                 │  Sync queue
            ▼                                ▼
┌───────────────────────────┐   ┌────────────────────────────┐
│    CameraX + ML Kit       │   │  SyncManager + Retrofit 2  │
│  Face Detection (Local)   │   │  → Datalake Cloud Endpoint │
└───────────┬───────────────┘   └────────────────────────────┘
            │  Face crop bitmap
            ▼
┌───────────────────────────────────────────────────────────────┐
│         TensorFlow Lite — MobileFaceNet (On-Device)           │
│  112×112 crop → 128-D L2-normalized embedding vector         │
└───────────────────────────────────────────────────────────────┘
```

### Component Breakdown

| Layer | Component | Responsibility |
|---|---|---|
| **Presentation** | Jetpack Compose | Camera view, overlays, animation, liveness UI |
| **State** | `FaceAuthViewModel` | Phase transitions, challenge timers, result routing |
| **Domain** | `LivenessDetector`, `FaceFeatureExtractor` | Gesture evaluation, neural inference, similarity |
| **Data** | `FaceAuthRepository` + Room | Local embed storage, event logging |
| **Sync** | `SyncManager` + Retrofit | Offline queue → Datalake cloud push |
| **IPC** | `LocalDevServer` (port 12345) | REST API for external system integration |

---

## 2. Key Features

| # | Feature | Description |
|---|---|---|
| 1 | **On-Device AI** | MobileFaceNet TFLite — 128D face embeddings, fully offline |
| 2 | **3-Phase Liveness** | Blink → Identity → Secondary gesture, preventing replay attacks |
| 3 | **Room DB Vault** | Encrypted local SQLite storage for face profiles and event logs |
| 4 | **Local REST API** | Developer HTTP server on port 12345 for system integration |
| 5 | **Auto-Sync Queue** | Cached events pushed to Datalake when connectivity restored |
| 6 | **ContentProvider IPC** | Android inter-process access to user and event data |
| 7 | **Configurable Security** | Relaxed / Standard / Paranoid strictness presets |
| 8 | **GPS Tagging** | Auth events stamped with lat/long for remote site logging |

---

## 3. 🎬 Demo Video

> Watch the full end-to-end demonstration of **Datalake Face Auth** — covering worker enrollment, 3-phase liveness challenge, offline face identification, and background sync to the Datalake endpoint.

<div align="center">

[![▶️ Watch Demo Video](https://img.shields.io/badge/▶%20Watch%20Demo%20Video-Google%20Drive-blue?style=for-the-badge&logo=google-drive&logoColor=white)](https://drive.google.com/file/d/1ihTKfutqfpl9AkmQo6slHlws5Myiq70w/view?usp=sharing)

**[🎥 Click here to watch the full demo on Google Drive](https://drive.google.com/file/d/1ihTKfutqfpl9AkmQo6slHlws5Myiq70w/view?usp=sharing)**

</div>

### What the demo covers:

| Timestamp | Feature Demonstrated |
|---|---|
| `0:00` | App launch, Dashboard overview |
| `0:15` | Worker face enrollment flow |
| `0:40` | 3-phase liveness challenge (Blink → Identity → Gesture) |
| `1:10` | Successful offline authentication |
| `1:30` | Background auto-sync to Datalake endpoint |
| `1:50` | Local REST API live query demo |

---

## 4. Download & Install APK

### Step 1 — Navigate to the GitHub Repository

Open your browser and go to:

```
https://github.com/<your-org>/NHAI_HACKATHON
```

> Replace `<your-org>` with the actual GitHub organization or username hosting the project.

---

### Step 2 — Go to the Releases Page

1. On the repository's main page, look at the **right sidebar**.
2. Click the **"Releases"** section (or navigate directly to):

```
https://github.com/<your-org>/NHAI_HACKATHON/releases
```

3. Find the latest release tag (e.g., `v1.0.0` or `hackathon-build`).

---

### Step 3 — Download the APK

1. Click the release tag to expand it.
2. Scroll down to the **"Assets"** section.
3. Click **`datalake-face-auth-release.apk`** to download it.

> The APK is under **20 MB** — optimized for ARM devices with R8 minification and ABI stripping (`armeabi-v7a`, `arm64-v8a`).

---

### Step 4 — Enable Unknown Sources on Android

Before installing, allow installation from unknown sources:

**Android 8.0+ (Oreo and above):**
1. Open **Settings → Apps & Notifications → Special App Access**
2. Tap **"Install Unknown Apps"**
3. Select your file manager or browser
4. Toggle **"Allow from this source"** → ON

**Android 7.0 and below:**
1. Open **Settings → Security**
2. Enable **"Unknown Sources"**

---

### Step 5 — Install the APK

1. Open your device's **file manager** or tap the downloaded APK from your browser's notification bar.
2. Tap **"Install"** when prompted.
3. Wait for installation to complete, then tap **"Open"**.

---

### Step 6 — Grant Required Permissions

On first launch, the app will request:

| Permission | Purpose |
|---|---|
| 📷 **Camera** | Live face capture for enrollment and verification |
| 📍 **Location** | GPS tagging of authentication events for NHAI field sites |
| 🌐 **Internet** | Background sync of event logs to the Datalake endpoint |

Tap **"Allow"** for each when prompted.

---

### Step 7 — First-Time Setup

1. The **Dashboard** loads with a live camera preview.
2. Tap **"Register Face"** → enter your **Worker ID** and **Full Name**.
3. Hold your face centered in the oval guide — the app captures **3 frames automatically**.
4. After enrollment is complete, you are returned to the Dashboard, ready to authenticate.

---

## 5. Data Lake API Integration

The app integrates with a central **Datalake v3.0** REST endpoint for syncing authentication events from remote field sites. Here is a step-by-step guide to connecting your backend.

---

### Step 1 — Set Your Datalake Endpoint URL

In the app Dashboard, scroll to the **"Sync Configuration"** panel:

1. Tap the **Sync Server URL** input field.
2. Enter your Datalake API base URL, for example:

```
https://api.datalake3.aws/prod/
```

3. Tap **"Save"** to persist the endpoint.

> The URL is stored in `SharedPreferences` (`sync_prefs` → `sync_endpoint`) and used by `SyncManager` for all outbound requests.

---

### Step 2 — Required API Contract

Your Datalake backend must expose a `POST /sync` endpoint that accepts the following JSON payload:

**Request — `POST {BASE_URL}sync`**

```json
{
  "deviceId": "a1b2c3d4-uuid-of-device",
  "appVersion": "1.0.0",
  "events": [
    {
      "id": "event-uuid-string",
      "type": "auth",
      "userId": "worker-123",
      "name": "Ramesh Kumar",
      "success": true,
      "similarity": 0.87,
      "timestamp": 1717660800000
    },
    {
      "id": "event-uuid-string-2",
      "type": "enrollment",
      "userId": "worker-456",
      "name": "Priya Sharma",
      "success": true,
      "similarity": 1.0,
      "timestamp": 1717660900000
    }
  ]
}
```

**Field Reference:**

| Field | Type | Description |
|---|---|---|
| `deviceId` | `String` | Auto-generated UUID per device installation |
| `appVersion` | `String` | App version identifier (`"1.0.0"`) |
| `events[].id` | `String` | Unique UUID for each event |
| `events[].type` | `String` | `"auth"` or `"enrollment"` |
| `events[].userId` | `String` | NHAI worker identifier entered during enrollment |
| `events[].name` | `String` | Full name of the enrolled worker |
| `events[].success` | `Boolean` | `true` if biometric match passed, `false` if rejected |
| `events[].similarity` | `Float` | Cosine similarity score (`0.0` to `1.0`) |
| `events[].timestamp` | `Long` | Unix epoch milliseconds |

---

### Step 3 — Required API Response

Your server **must** return HTTP `200` with this response body, otherwise the app will retry on next connectivity event:

```json
{
  "message": "Sync successful",
  "received": 2
}
```

| Field | Type | Description |
|---|---|---|
| `message` | `String` | Human-readable status string (logged locally) |
| `received` | `Int` | Number of events processed by server |

---

### Step 4 — Backend Integration Sample (Python / FastAPI)

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import uuid, datetime

app = FastAPI()

class SyncEvent(BaseModel):
    id: str
    type: str
    userId: str
    name: str
    success: bool
    similarity: float
    timestamp: int

class SyncPayload(BaseModel):
    deviceId: str
    appVersion: str
    events: List[SyncEvent]

class SyncResponse(BaseModel):
    message: str
    received: int

@app.post("/prod/sync", response_model=SyncResponse)
async def receive_sync(payload: SyncPayload):
    print(f"Device {payload.deviceId} sent {len(payload.events)} event(s)")
    for event in payload.events:
        # Insert into your database here
        print(f"  [{event.type.upper()}] {event.name} ({event.userId}) "
              f"— success={event.success}, similarity={event.similarity:.2f}")
    return SyncResponse(
        message="Sync successful",
        received=len(payload.events)
    )
```

---

### Step 5 — Verify Sync is Working

1. In the app Dashboard, tap **"Manual Sync Now"**.
2. Watch the sync status badge:
   - 🔵 **Syncing...** — request in flight
   - ✅ **Synced (N events)** — server acknowledged
   - ❌ **Error** — check your URL or server logs
3. The `SyncEvent` rows in local Room DB will have `synced = true` after a successful push.

---

### Step 6 — Enable Auto-Sync

Toggle **"Background Auto-Sync"** ON in the Dashboard. The `SyncManager` registers a `NetworkCallback` that automatically triggers `performSync()` the moment internet connectivity is restored — critical for remote highway sites with intermittent network.

---

## 6. Local Developer REST Server

The app embeds a lightweight **Socket-based HTTP server** running on port **12345**, ideal for integrating the app with local web dashboards, audit tools, or companion systems — without needing any external SDK.

> **Access URL (same-network):** `http://<device-ip>:12345`  
> **Access URL (USB/ADB forward):** `http://localhost:12345`

### Enable ADB Port Forward (USB-connected device)

```bash
adb forward tcp:12345 tcp:12345
```

### Available Endpoints

#### `GET /api/status`
Returns system diagnostics.

```bash
curl http://localhost:12345/api/status
```

```json
{
  "status": "online",
  "timestamp": 1717660800000,
  "os": "Android 13",
  "arch": "arm64-v8a",
  "service": "Datalake Face Auth v1.0.0"
}
```

---

#### `GET /api/users`
Returns all enrolled face profiles including their full 128-float neural embedding vectors.

```bash
curl http://localhost:12345/api/users
```

```json
[
  {
    "userId": "worker-123",
    "name": "Ramesh Kumar",
    "enrolledAt": 1717660800000,
    "embedding": [0.042, -0.317, 0.891, ...]
  }
]
```

> ⚠️ **Note:** Embeddings are `128-float` arrays (MobileFaceNet) or `512-float` arrays (fallback geometric projection). Always check `embedding.length` before processing.

---

#### `GET /api/events`
Returns the complete local authentication and enrollment event log.

```bash
curl http://localhost:12345/api/events
```

```json
[
  {
    "id": "uuid-string",
    "type": "auth",
    "userId": "worker-123",
    "name": "Ramesh Kumar",
    "success": true,
    "similarity": 0.87,
    "timestamp": 1717660800000,
    "synced": false,
    "latitude": 34.1526,
    "longitude": 77.5771
  }
]
```

---

### CORS Support

All endpoints emit full **CORS headers**, so browser-based dashboards can query the device directly:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, OPTIONS
Access-Control-Allow-Headers: Content-Type
```

---

### ContentProvider IPC (Android-to-Android)

For companion Android apps on the same device, query data via standard `ContentProvider`:

```kotlin
// Query all enrolled users
val cursor = contentResolver.query(
    Uri.parse("content://com.example.provider/users"),
    null, null, null, null
)

// Query all auth events
val eventCursor = contentResolver.query(
    Uri.parse("content://com.example.provider/events"),
    null, null, null, null
)
```

---

## 7. Identification Model — Technical Deep Dive

### Pipeline Overview

```
┌──────────┐   ┌──────────┐   ┌─────────────┐   ┌──────────────┐
│  CameraX │   │  ML Kit  │   │ Rotation Fix │   │ Bounding Box │
│ ImageProxy├──►│ Detector ├──►│ (Affine Mtx) ├──►│  Crop & Clip │
└──────────┘   └──────────┘   └─────────────┘   └──────┬───────┘
                                                         │
                                                         ▼
┌──────────┐   ┌──────────┐   ┌─────────────┐   ┌──────────────┐
│ Cosine / │   │ TFLite   │   │ Scale/Resize │   │ Pixel Norm   │
│ L2 Score │◄──│ Inference│◄──│  to 112×112  │◄──│ (x-127.5)    │
└──────────┘   └──────────┘   └─────────────┘   └──────────────┘
```

### Phase A — Rotation Correction
- Raw `ImageProxy` sensor buffers are rotated relative to display (typically 90°/270°).
- An **affine transformation matrix** is computed from `imageInfo.rotationDegrees` to produce a correctly oriented bitmap.
- ML Kit bounding box coordinates are then projected onto this corrected bitmap.

### Phase B — Crop & Standardization
- Face sub-region is extracted from the upright bitmap.
- Rescaled to exactly **112 × 112 pixels** via bilinear interpolation.
- Each pixel normalized: `(channel - 127.5) / 127.5` → float range `[-1.0, 1.0]`.

### Phase C — Neural Embedding (MobileFaceNet)
- Normalized image passed through MobileFaceNet's depthwise-separable convolutional layers.
- Output: **128-dimensional float vector**.
- Undergoes **L2-Normalization** so `‖v‖₂ = 1.0` (unit hypersphere).

```
‖v‖₂ = √(Σᵢ vᵢ²) = 1.0   for i = 1..128
```

### Phase D — Cosine Similarity Matching
Since both vectors are unit-normalized, cosine similarity equals the dot product:

```
Similarity(A, B) = cos(θ) = A · B = Σᵢ Aᵢ × Bᵢ
```

| Score | Decision |
|---|---|
| `≥ 0.70` (default) | ✅ **MATCH** — access granted |
| `< 0.70` | ❌ **MISMATCH** — access denied |

> Threshold is configurable via slider in Dashboard: range `0.50` → `0.95`.

### Fallback — Geometric Projection (No TFLite)
If TFLite inference fails, a **512-dimensional fallback** activates:
- 32 scale-invariant facial proportions computed from ML Kit landmarks.
- **Zero-centered** against a 32D population median distribution.
- Projected through a **deterministic orthogonal matrix** into 512D space.
- Result: Different individuals score ≤ 0.35; authentic users score ≥ 0.75.

---

## 8. Liveness Detection Protocol

Prevents photo/video replay spoofing through a **3-phase sequential challenge**:

```
          ┌────────────────────────────────────┐
          │           START SCAN               │
          └─────────────────┬──────────────────┘
                            ▼
          ┌────────────────────────────────────┐
          │  Phase 1: Liveness 1               │
          │  Random gesture (Blink / Smile /   │
          │  Turn Left / Turn Right) — 7s      │
          └────────┬───────────────────────────┘
           Pass    │              Timeout → FAIL
                   ▼
          ┌────────────────────────────────────┐
          │  Phase 2: Identification           │
          │  Face alignment check + 128D       │
          │  embedding cosine match — 7s       │
          └────────┬───────────────────────────┘
           Match   │              Mismatch → FAIL
                   ▼
          ┌────────────────────────────────────┐
          │  Phase 3: Liveness 2               │
          │  Different secondary gesture — 7s  │
          └────────┬───────────────────────────┘
           Pass    │              Timeout → FAIL
                   ▼
          ┌────────────────────────────────────┐
          │    ✅ ACCESS VERIFIED               │
          └────────────────────────────────────┘
```

### Challenge Thresholds by Strictness

| Challenge | Relaxed | Standard | Paranoid |
|---|---|---|---|
| **Blink** | Eye open prob < 0.45 | < 0.35 | < 0.22 |
| **Smile** | Smile prob > 0.45 | > 0.60 | > 0.78 |
| **Turn Left** | Yaw > 12° | > 18° | > 24° |
| **Turn Right** | Yaw < -12° | < -18° | < -24° |

---

## 9. Background Auto-Sync

```
[Auth Event Logged]
        │
        ├─ Network Available? ──YES──► POST /sync immediately
        │
        └─ NO ──► Store in Room DB (synced = false)
                          │
                 [Network Restored]
                          │
                          ▼
              ConnectivityManager.NetworkCallback
                     triggers SyncManager
                          │
                          ▼
              Retrofit POST → Datalake endpoint
                          │
                 [200 OK received]
                          │
                          ▼
              Mark events synced = true in Room DB
```

The `SyncManager` class registers a `NetworkRequest` callback at app start. When connectivity is detected, it fetches all `synced = false` `SyncEvent` rows and batches them into a single `POST` request.

---

## 10. Security Hardening

| Mechanism | Implementation |
|---|---|
| **Biometric Isolation** | Embeddings stored only in sandboxed Room SQLite — never transmitted raw |
| **Liveness Anti-Spoofing** | 3-phase challenge prevents static photo, video, and 3D mask attacks |
| **Thread Safety** | TFLite inference on `Dispatchers.Default`, DB on `Dispatchers.IO` |
| **Frame Rate Limiting** | Enrollment captures 1 frame/second max to prevent jitter averaging |
| **Proximity Guard** | Face bounding box must cover minimum viewport area |
| **Eyes-Open Guard** | Enrollment rejected if eyes are not sufficiently open |
| **R8 Minification** | Dead-code elimination, ProGuard rules protect GSON/Moshi/TFLite bindings |
| **ABI Stripping** | x86/x86_64 architectures stripped; ARM-only deployment |

---

## 11. Tech Stack

| Category | Library / Tool | Version |
|---|---|---|
| Language | Kotlin + Coroutines + StateFlow | 1.9+ |
| UI | Jetpack Compose (Material 3) | Latest Stable |
| Camera | CameraX | 1.3+ |
| Face Detection | Google ML Kit Face Detection | Local SDK |
| AI Inference | TensorFlow Lite (MobileFaceNet) | 2.x |
| Database | Room Persistence Library | 2.6+ |
| Networking | Retrofit 2 + OkHttp 3 + Moshi | Latest |
| Min SDK | Android 7.0 (API 24) | — |
| Target SDK | Android 14 (API 34) | — |
| Build | Gradle + R8 + ProGuard | — |

---

## 12. Source Directory Map

```
app/src/main/java/com/example/
 ├── MainActivity.kt                    # Edge-to-edge Compose host
 ├── camera/
 │    └── CameraPreview.kt             # CameraX + ML Kit frame analyzer
 ├── data/
 │    ├── FaceAuthRepository.kt        # Database abstraction layer
 │    ├── sync/
 │    │    ├── SyncManager.kt          # Network-aware event sync engine
 │    │    ├── SyncApiService.kt       # Retrofit interface definitions
 │    │    └── LocalDevServer.kt       # Embedded REST server (port 12345)
 │    └── local/
 │         ├── AppDatabase.kt          # Room DB configuration
 │         ├── FaceAuthDao.kt          # SQL DAO bindings
 │         ├── FaceAuthEntity.kt       # UserFace + SyncEvent entities
 │         └── FaceAuthContentProvider.kt  # IPC ContentProvider
 ├── liveness/
 │    └── LivenessDetector.kt          # Challenge evaluator (Blink/Smile/Turn)
 ├── recognition/
 │    └── FaceFeatureExtractor.kt      # TFLite inference + geometric fallback
 └── ui/
      ├── CameraActivityScreens.kt     # Scanner overlays, laser animations
      ├── DashboardScreen.kt           # Config panel, DB management
      ├── FaceAuthViewModel.kt         # State machine: phases, sync, sessions
      └── theme/
           ├── Color.kt               # CyberTeal, ElectricBlue, CosmicSlate
           └── Theme.kt               # Material 3 dark theme schema
```

---

## 🚀 Startup Flow

```
[App Launch]
    │
    ├──► Local REST Server starts on :12345 (background thread)
    │
    ├──► FaceAuthViewModel initializes StateFlows + SharedPreferences
    │
    ├──► Room DB integrity verified
    │
    ├──► SyncManager registers NetworkCallback
    │
    └──► Dashboard screen renders with live camera feed
```

---

## 🤝 Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: describe your change"`
4. Push and open a Pull Request against `main`.

---

## 📄 License

This project was developed for **NHAI Hackathon 7.0**. All rights reserved by the development team. Contact the repository owner for licensing inquiries.

---

*Datalake Face Auth — Securing India's Highways, One Face at a Time.* 🛣️
