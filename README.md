# Edge Detection Viewer

A real-time edge detection application with native Android implementation and web-based viewer. The Android app captures camera frames, processes them using OpenCV's Canny edge detection via JNI, and renders results through OpenGL ES. The web viewer provides a TypeScript-based interface for displaying static frames with WebSocket support for real-time updates.

---

## ✅ Features Implemented

### Android Application
- **📱 Real-time Camera Processing**: Live camera feed with hardware acceleration
- **🔄 Dual View Modes**: Toggle between raw camera and edge-detected frames
- **⚡ Native Performance**: C++ JNI implementation using OpenCV for optimal speed
- **🎨 OpenGL ES Rendering**: Hardware-accelerated texture rendering with aspect ratio preservation
- **📊 FPS Monitoring**: Real-time frame rate display with performance metrics
- **🔐 Runtime Permissions**: Camera permission handling with Android 6.0+ support
- **📐 Adaptive Resolution**: Automatic camera resolution detection and display
- **🔧 Canny Edge Detection**: Gaussian blur + Canny algorithm with optimized parameters

### Web Viewer
- **🌐 Browser-Based Display**: Static image viewer with smooth transitions
- **🔄 Mode Toggle**: Switch between raw and edge-detected images
- **📡 WebSocket Integration**: Real-time updates from mock/live server
- **📊 Live Stats Display**: FPS and resolution overlay
- **💻 TypeScript Implementation**: Type-safe client code with modern ES6+
- **🔌 Connection Status**: Visual feedback for WebSocket connectivity

---

## 📷 Screenshots

### Android Application

**Raw Camera Mode**

<img src="https://github.com/user-attachments/assets/2c58dd5f-0cd2-4b36-a6df-9911906a3f9c" width="300"/>


**Edge Detection Mode**

<img src="https://github.com/user-attachments/assets/3a6af816-96fe-426a-8b84-aed7e706827d" width="300"/>


### Web Viewer

**Raw Image Display**

<img width="700" height="958" alt="Screenshot 2025-10-08 134035" src="https://github.com/user-attachments/assets/76905fca-4fb4-4d00-a3c2-8cd71441bae8" />


**Edge Detection Display**

<img width="700" height="958" alt="Screenshot 2025-10-08 134046" src="https://github.com/user-attachments/assets/45e78ab0-080b-4838-aef6-f02ac243a172" />


---

## ⚙️ Setup Instructions

### Prerequisites

#### For Android Development
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Android SDK**: API Level 24 (Android 7.0) minimum, API Level 36 (Android 14) target
- **Android NDK**: Version 21.3+ (for C++ compilation)
- **Gradle**: 8.10.1 (included with wrapper)
- **Java Development Kit**: JDK 17 or later
- **OpenCV Android SDK**: 4.5.0 or later

#### For Web Development
- **Node.js**: 16.x or later
- **npm**: 8.x or later
- **TypeScript**: 4.x or later (installed via npm)
- **Modern Web Browser**: Chrome, Firefox, Edge, or Safari

---

### Android Setup

#### 1. Install OpenCV Android SDK

Download and configure OpenCV for Android:

```powershell
# Download OpenCV Android SDK from https://opencv.org/releases/
# Extract to a known location, e.g., C:\opencv\OpenCV-android-sdk

# Update the CMakeLists.txt path (already configured in the project)
# File: app/src/main/cpp/CMakeLists.txt
# Line 6: set(OpenCV_DIR "C:/opencv/OpenCV-android-sdk/sdk/native/jni")
```

**Important**: Update `app/src/main/cpp/CMakeLists.txt` with your OpenCV SDK path:

```cmake
set(OpenCV_DIR "C:/opencv/OpenCV-android-sdk/sdk/native/jni")
```

#### 2. Configure NDK in Android Studio

1. Open **Android Studio** → **File** → **Settings** (or **Preferences** on macOS)
2. Navigate to **Appearance & Behavior** → **System Settings** → **Android SDK**
3. Click **SDK Tools** tab
4. Check **NDK (Side by side)** and **CMake**
5. Click **Apply** to download and install

Alternatively, specify NDK version in `local.properties`:
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\21.4.7075529
```

#### 3. Copy OpenCV Native Libraries

Copy OpenCV `.so` files to the project:

```powershell
# Source: OpenCV-android-sdk/sdk/native/libs/
# Destination: app/src/main/jniLibs/

# Copy for arm64-v8a
Copy-Item "C:\opencv\OpenCV-android-sdk\sdk\native\libs\arm64-v8a\*" `
  -Destination ".\app\src\main\jniLibs\arm64-v8a\" -Recurse

# Copy for armeabi-v7a (optional, for 32-bit devices)
Copy-Item "C:\opencv\OpenCV-android-sdk\sdk\native\libs\armeabi-v7a\*" `
  -Destination ".\app\src\main\jniLibs\armeabi-v7a\" -Recurse
```

Expected structure:
```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libopencv_java4.so
└── armeabi-v7a/
    └── libopencv_java4.so
```

#### 4. Build and Run

Open the project in Android Studio:

```powershell
# Open Android Studio
# File → Open → Select EdgeDetectionViewer folder

# Or use command line
cd C:\Projects\EdgeDetectionViewer
.\gradlew assembleDebug
```

**Build APK**:
```powershell
.\gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Install on Device**:
```powershell
# Connect Android device via USB with debugging enabled
.\gradlew installDebug
```

**Run from Android Studio**:
1. Select device/emulator from dropdown
2. Click **Run** (green play button) or press `Shift+F10`

---

### Web Viewer Setup

#### 1. Install Dependencies

Navigate to the web directory and install packages:

```powershell
cd C:\Projects\EdgeDetectionViewer\web
npm install
# or
npm install typescript --save-dev
```

#### 2. Compile TypeScript

Compile the TypeScript source to JavaScript:

```powershell
# Compile once
npx tsc

# Or watch mode for development
npx tsc --watch
```

This generates `dist/main.js` from `main.ts`.

#### 3. Serve the Application

Use any static file server. Options:

**Option A: Python (if installed)**
```powershell
# Python 3
python -m http.server 8000

# Open browser to http://localhost:8000
```

**Option B: Node.js http-server**
```powershell
npm install -g http-server
http-server -p 8000

# Open browser to http://localhost:8000
```

**Option C: VS Code Live Server Extension**
1. Install "Live Server" extension in VS Code
2. Right-click `index.html` → **Open with Live Server**

#### 4. Mock WebSocket Server (Optional)

For testing real-time WebSocket updates:

```powershell
cd C:\Projects\EdgeDetectionViewer\web\mock-ws-server
npm install
npm start

# Server runs on ws://localhost:8080
```

The web viewer automatically attempts to connect. If unavailable, it falls back to simulated updates.

---

### Troubleshooting

#### Android Issues

**Problem**: `OpenCV not found` during CMake build
- **Solution**: Verify `OpenCV_DIR` path in `CMakeLists.txt` points to correct ABI directory

**Problem**: `UnsatisfiedLinkError: libopencv_java4.so`
- **Solution**: Ensure `.so` files are copied to `app/src/main/jniLibs/<abi>/`

**Problem**: Camera not starting
- **Solution**: Grant camera permission in device settings: Settings → Apps → Edge Detection Viewer → Permissions

**Problem**: Black screen in edge mode
- **Solution**: Check Logcat for GL errors; ensure device supports OpenGL ES 2.0

#### Web Issues

**Problem**: TypeScript compilation errors
- **Solution**: Ensure `tsconfig.json` is present and run `npx tsc --project tsconfig.json`

**Problem**: Images not loading
- **Solution**: Verify files exist at `web/assets/raw_image.jpg` and `web/assets/edge_image.jpg`

**Problem**: WebSocket not connecting
- **Solution**: Start mock server or disable auto mode; viewer works in fallback mode

---

## 🧠 Architecture Overview

### Android Application Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity.kt                         │
│  • Camera permission management                              │
│  • UI controls (toggle button, FPS display)                  │
│  • CvCameraViewListener2 implementation                      │
└────────────┬────────────────────────────┬────────────────────┘
             │                            │
             │ onCameraFrame()            │ setFrame()
             │ (Mat from camera)          │ (processed Mat)
             │                            │
             ▼                            ▼
┌─────────────────────────┐    ┌──────────────────────────┐
│   OpenCV Camera Bridge  │    │   GLRenderer.kt          │
│  • Captures camera      │    │  • OpenGL ES 2.0         │
│  • Provides Mat frames  │    │  • Texture rendering     │
│  • Resolution handling  │    │  • Aspect preservation   │
└────────────┬────────────┘    └────────────┬─────────────┘
             │                              │
             │ Mat (RGBA)                   │ Mat → Texture
             │                              │
             ▼                              │
┌───────────────────────────────────────────▼──────────────┐
│                 JNI Native Layer                          │
│              native-lib.cpp (C++)                         │
│  • processFrame(matAddr: Long)                            │
│  • RGBA → Grayscale conversion                            │
│  • GaussianBlur (5x5, sigma=1.5)                          │
│  • Canny edge detection (100, 200)                        │
│  • Grayscale → RGBA conversion                            │
│  • Thread-local Mat buffers (optimization)                │
└────────────┬──────────────────────────────────────────────┘
             │
             │ Uses
             ▼
┌───────────────────────────────────────────────────────────┐
│               OpenCV C++ Library                          │
│  • cv::cvtColor()                                         │
│  • cv::GaussianBlur()                                     │
│  • cv::Canny()                                            │
│  • Linked via CMake (.so files)                           │
└───────────────────────────────────────────────────────────┘
```

#### Frame Flow (Android)

1. **Camera Capture**: OpenCV `CameraBridgeViewBase` captures frames from device camera
2. **Callback**: `onCameraFrame()` receives `CvCameraViewFrame` with RGBA Mat
3. **Mode Check**: 
   - **Raw mode**: Return unmodified Mat → displayed by camera view
   - **Edge mode**: Pass Mat to JNI → process → pass to GLRenderer
4. **JNI Processing**: 
   - Convert RGBA → Grayscale
   - Apply Gaussian blur to reduce noise
   - Canny edge detection with thresholds (100, 200)
   - Convert grayscale edges back to RGBA
   - In-place modification for efficiency
5. **GL Rendering**: 
   - GLRenderer receives processed Mat
   - Uploads pixel data to OpenGL texture
   - Renders textured quad with aspect-preserved viewport
   - Throttled to ~15 FPS for GPU efficiency
6. **Display**: GLSurfaceView shows rendered texture overlaying camera view

#### Key Design Decisions

**Why JNI?**
- Native C++ provides 5-10x performance improvement over Java for image processing
- Direct memory access to Mat pixel buffers eliminates Java ↔ Native copying overhead
- OpenCV's C++ API is more mature and feature-complete than Java bindings

**Why OpenGL ES?**
- Hardware-accelerated rendering offloads CPU
- Efficient texture uploads using GPU DMA
- Smooth 30+ FPS even on mid-range devices
- Aspect ratio preservation without CPU-side scaling

**Why Thread-Local Mats?**
- Avoids per-frame memory allocation (major bottleneck)
- Reuses existing buffers for intermediate stages
- Reduces garbage collection pressure
- Critical for real-time performance

**Optimization Techniques**:
- In-place Mat operations where possible
- Single allocation for vertex/texture coordinate buffers
- Rendermode `WHEN_DIRTY` to avoid unnecessary GL redraws
- Atomic flags for thread-safe frame buffer access

---

### Web Viewer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         index.html                           │
│  • DOM structure (image, overlay, controls)                  │
│  • Loads compiled JavaScript bundle                          │
└────────────┬────────────────────────────────────────────────┘
             │
             │ <script type="module" src="dist/main.js">
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    main.ts (TypeScript)                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  1. DOM Element Bindings                                ││
│  │     • frame: <img> element                              ││
│  │     • fpsEl, resEl: stat displays                       ││
│  │     • toggleBtn: mode switch button                     ││
│  │     • autoModeEl: checkbox for auto-switching           ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │  2. State Management                                    ││
│  │     • mode: 'raw' | 'edges'                             ││
│  │     • simulatedFps: random 28-33 FPS                    ││
│  │     • statusBadge: connection indicator                 ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │  3. Event Handlers                                      ││
│  │     • toggleMode(): switches image source               ││
│  │     • tick(): updates FPS every 1s                      ││
│  │     • handleFrameData(): processes WS messages          ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │  4. WebSocket Integration                               ││
│  │     • Attempts connection to ws://localhost:8080        ││
│  │     • Receives {fps, resolution, mode} payloads         ││
│  │     • Falls back to simulated updates if unavailable    ││
│  │     • Respects auto mode checkbox setting               ││
│  └─────────────────────────────────────────────────────────┘│
└────────────┬────────────────────────────────────────────────┘
             │
             │ WebSocket
             ▼
┌─────────────────────────────────────────────────────────────┐
│              Mock WebSocket Server (Optional)                │
│                    mock-ws-server/server.js                  │
│  • Node.js + ws library                                      │
│  • Broadcasts mock frame data every 1s                       │
│  • Payload: {fps: 28-33, resolution: "1920x1080",           │
│              mode: random "raw" | "edges"}                   │
└─────────────────────────────────────────────────────────────┘
```

#### Frame Flow (Web)

1. **Page Load**: HTML loads, TypeScript compiled to `dist/main.js`
2. **Initialization**:
   - Bind DOM elements
   - Set initial mode to 'raw'
   - Display `assets/raw_image.jpg`
   - Start FPS simulation timer
3. **User Interaction**:
   - Click **toggle button** → switch mode → update image src
   - Check **auto mode** → enable WebSocket mode switching
4. **WebSocket Flow** (if server available):
   - Connect to `ws://localhost:8080`
   - Receive JSON: `{fps: 30, resolution: "1920x1080", mode: "edges"}`
   - Update FPS/resolution displays
   - If auto mode enabled: switch image to match incoming mode
   - Update status badge to "Connected"
5. **Fallback Flow** (no WebSocket):
   - Display "Disconnected" badge
   - Simulate FPS updates locally
   - User manually controls mode toggle

#### TypeScript Design

**Type Safety**:
```typescript
let mode: 'raw' | 'edges' = 'raw';  // Union type prevents typos
const frame = document.getElementById('frame') as HTMLImageElement;  // Type assertion
```

**Payload Interface** (implicit):
```typescript
interface FrameData {
  fps: number;
  resolution: string;
  mode: 'raw' | 'edges';
}
```

**Key Benefits**:
- Compile-time type checking prevents runtime errors
- IDE autocomplete improves development speed
- Refactoring safety (rename variables, detect unused code)
- Better maintainability for future developers

**Modern ES6+ Features**:
- Arrow functions: `() => { ... }`
- Template literals: `` `Mode: ${mode}` ``
- `const`/`let` block scoping
- Ternary operators for concise conditionals
- Module imports (when extended)

---

### Integration Points

**Android → Web Export** (manual process):
1. Run Android app in edge detection mode
2. Capture screenshot or export frame
3. Save to `web/assets/edge_image.jpg`
4. Web viewer displays exported result

**Potential Live Integration** (future enhancement):
- Android app acts as WebSocket server
- Streams frame data (base64 JPEG) over network
- Web client decodes and displays in real-time
- Requires additional Android networking code

---

## 📦 Project Structure

```
EdgeDetectionViewer/
├── app/                              # Android application
│   ├── src/main/
│   │   ├── java/com/example/edgedetectionviewer/
│   │   │   ├── MainActivity.kt       # Main activity, camera handling
│   │   │   └── gl/GLRenderer.kt      # OpenGL ES renderer
│   │   ├── cpp/
│   │   │   ├── native-lib.cpp        # JNI edge detection implementation
│   │   │   └── CMakeLists.txt        # CMake build config, OpenCV linking
│   │   ├── res/                      # Android resources (layouts, strings)
│   │   └── AndroidManifest.xml       # App manifest, permissions
│   └── build.gradle.kts              # App-level Gradle config
├── openCVLibrary/                    # OpenCV Android SDK integration
│   └── build.gradle                  # OpenCV module config
├── web/                              # Web viewer
│   ├── index.html                    # Main HTML page
│   ├── main.ts                       # TypeScript source
│   ├── dist/main.js                  # Compiled JavaScript (generated)
│   ├── styles.css                    # Styling
│   ├── tsconfig.json                 # TypeScript compiler config
│   ├── assets/
│   │   ├── raw_image.jpg             # Sample raw frame
│   │   └── edge_image.jpg            # Sample edge-detected frame
│   └── mock-ws-server/
│       ├── server.js                 # Mock WebSocket server
│       └── package.json              # Node.js dependencies
├── build.gradle.kts                  # Root Gradle config
├── settings.gradle.kts               # Gradle settings
└── README.md                         # This file
```

---

## 🚀 Usage

### Android
1. Launch app on device/emulator
2. Grant camera permission when prompted
3. App starts in **Raw mode** showing live camera feed
4. Tap **"Show Edges"** to switch to edge detection mode
5. Tap **"Show Raw"** to return to camera view
6. Observe FPS display at top of screen

### Web
1. Open `http://localhost:8000` in browser
2. Default view shows raw image
3. Click **"Mode: Raw"** button to switch to edges view
4. Click **"Mode: Edges"** to return to raw view
5. Check **"Auto mode"** to enable WebSocket control (requires server)
6. Observe FPS and resolution stats in overlay

---

## 🔧 Technologies Used

### Android
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose + XML layouts
- **Build System**: Gradle 8.10.1 with Kotlin DSL
- **Native Layer**: C++17 with JNI
- **Image Processing**: OpenCV 4.5+ (C++ API)
- **Graphics**: OpenGL ES 2.0
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 36)

### Web
- **Language**: TypeScript 4.x
- **Runtime**: Browser (ES6+)
- **WebSocket**: Native WebSocket API
- **Styling**: CSS3
- **Build Tool**: TypeScript Compiler (tsc)
- **Server** (optional): Node.js + ws library

### Tools
- **IDE**: Android Studio Arctic Fox+, VS Code
- **Version Control**: Git
- **NDK**: CMake 3.10.2+
- **Package Manager**: npm (web), Gradle (Android)

---

## 📝 License

This project is part of a technical assessment. Contact the repository owner for usage terms.

---

## 👤 Author

**Repository**: rjk51/flam-assessment  
**Date**: October 2025
