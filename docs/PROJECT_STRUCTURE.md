# WiFi Reader Project Structure

## Directory Organization

```
wifi_reader2/
├── app/                          # Android application source code
│   ├── src/main/
│   │   ├── java/                 # Kotlin/Java source files
│   │   ├── res/                  # Android resources
│   │   └── assets/               # Runtime assets (vocab.txt)
│   └── build.gradle.kts          # App-level Gradle configuration
├── backup/                       # Backup files and old implementations
├── docs/                         # Project documentation
│   ├── ADAPTATION_NOTES.md       # YOLOv8n router detection notes
│   ├── PROJECT_STRUCTURE.md      # This file
│   └── ZeticMLangeIntegration.md # Zetic MLange implementation guide
├── tests/                        # Test files and resources
│   └── test_wifi_text.html       # WiFi credential test display
├── build.gradle.kts              # Project-level Gradle configuration
├── settings.gradle.kts           # Gradle settings
├── PROJECT_STRUCTURE.md          # Project overview
└── README.md                     # Main project documentation
```

## Core Components

### `/app/src/main/java/com/zetic/wifireader/`

**Main Activity**
- `MainActivity.kt` - Main UI controller with camera integration

**Detection Pipeline**
- `pipeline/WiFiDetectionPipeline.kt` - Main detection orchestrator
- `ml/ZeticMLangeDetector.kt` - YOLOv8 router label detection

**OCR Engine**
- `ocr/OCREngine.kt` - OCR interface
- `ocr/ZeticMLangeOCREngine.kt` - Zetic-based OCR implementation
- `model/ZeticTextDetector.kt` - Text region detection
- `model/ZeticTextRecognizer.kt` - Text recognition

**LLM Parser**
- `llm/ZeticMLangeLLMParser.kt` - WiFi credential extraction

**Camera**
- `camera/CameraManager.kt` - CameraX integration

**UI Components**
- `ui/OverlayView.kt` - Detection overlay
- `ui/WiFiCredentialsAdapter.kt` - Results display

**Models**
- `model/WiFiCredentials.kt` - WiFi credential data model
- `model/DetectionResult.kt` - Detection result model
- `model/TextRegion.kt` - Text region model
- `model/BoundingBox.kt` - Bounding box model

### `/docs/`

- **`ADAPTATION_NOTES.md`**: Notes on adapting YOLOv8n for router detection
  - Current model limitations and workarounds
  - Custom model training guidance
  - Performance optimization strategies

- **`ZeticMLangeIntegration.md`**: Complete Zetic MLange integration guide
  - Architecture overview
  - Model configuration
  - API usage patterns
  - Implementation details

- **`PROJECT_STRUCTURE.md`**: This file - project organization guide

### `/tests/`

- **`test_wifi_text.html`**: Test page displaying WiFi credentials
  - Bold, large text for OCR testing
  - Multiple WiFi network examples
  - Optimized for mobile camera capture

## Building the App

```bash
# Build debug APK
./gradlew app:assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Or install with reinstall flag
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Technology Stack

### AI/ML Framework
- **Zetic MLange 1.3.0** - All model inference
  - YOLOv8n for object detection
  - Text detection model (jkim711/text_detect2)
  - Text recognition model (jkim711/text_recog3)

### Android Components
- **CameraX** - Camera integration
- **Material Design** - UI components
- **Coroutines** - Asynchronous operations
- **Lifecycle** - Lifecycle-aware components

### Dependencies
- AndroidX Core, AppCompat, ConstraintLayout
- CameraX libraries (core, camera2, lifecycle, view)
- Zetic MLange SDK
- Kotlin Coroutines
- Gson for JSON parsing

## Clean Architecture Benefits

- **Separation of Concerns**: Clear separation between UI, business logic, and ML models
- **Single Framework**: All AI models use Zetic MLange (no TFLite dependencies)
- **Easy Navigation**: Well-organized directory structure
- **Maintainability**: Each component has a clear responsibility
- **Testability**: Components can be tested independently
