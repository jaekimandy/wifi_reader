# WiFi Reader - Android App with Zetic.MLange

A Kotlin Android application that detects WiFi SSID and password text from router labels using Zetic.MLange AI framework for complete end-to-end detection and recognition.

## Features

- **Real-time Detection**: Uses camera to detect router labels in real-time
- **Unified AI Framework**:
  - Zetic.MLange YOLOv8n for detecting router label regions
  - Zetic.MLange text detection for identifying text regions
  - Zetic.MLange text recognition for extracting text content
  - Zetic.MLange LLM for intelligent WiFi credential parsing
- **Smart Text Parsing**: Automatically identifies SSID and password from extracted text
- **User-friendly UI**: Clean camera interface with overlay detection results
- **Copy to Clipboard**: Easy copying of detected credentials
- **Flash Support**: Toggle camera flash for better detection in low light

## Architecture

### Core Components

1. **ZeticMLangeDetector**: YOLOv8n model integration for router label detection
2. **ZeticMLangeOCREngine**: Two-stage OCR (detection + recognition) using Zetic models
3. **ZeticMLangeLLMParser**: Intelligent WiFi credential extraction and validation
4. **WiFiDetectionPipeline**: Coordinates detection, OCR, and parsing
5. **CameraManager**: Handles camera operations and image capture
6. **MainActivity**: Main UI controller with permission handling

### Detection Pipeline

```
Camera Frame → YOLOv8 Detection → Text Detection → Text Recognition → LLM Parsing → Results
```

### Zetic MLange Models

- **Object Detection**: `Ultralytics/YOLOv8n`
- **Text Detection**: `jkim711/text_detect2`
- **Text Recognition**: `jkim711/text_recog3`

## Setup

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Camera permission
- Internet connection for initial model downloads

### Quick Start

1. **Clone Repository**:
   ```bash
   git clone <repository-url>
   cd wifi_reader2
   ```

2. **Open in Android Studio**:
   - Open the project in Android Studio
   - Sync Gradle dependencies
   - Wait for indexing to complete

3. **Build and Run**:
   ```bash
   ./gradlew app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Model Configuration

All models are automatically downloaded and managed by Zetic MLange SDK:
- No manual model file downloads required
- Models are cached on device after first use
- Automatic optimization for your device architecture
- Cloud-based model management

The app uses pre-configured API keys for Zetic MLange services. Models are downloaded on first initialization.

## Usage

1. Launch the app
2. Grant camera permission when prompted
3. Point camera at router label
4. Wait for automatic detection (processes every 2 seconds)
5. View detected credentials in the bottom panel
6. Tap any credential to copy to clipboard

## Technical Details

### Supported Router Label Formats

- Standard format: "SSID: NetworkName, Password: Password123"
- Network format: "Network Name (SSID): MyWiFi, Network Key (Password): SecurePass123!"
- WiFi format: "WiFi: NetworkName, PWD: Password123"
- Multiple language support through intelligent parsing

### Performance Optimizations

- Detection throttling (2-second intervals)
- Background processing with Kotlin coroutines
- Efficient bitmap processing and memory management
- Lazy initialization of AI models
- Automatic model caching

### Technology Stack

**AI/ML Framework:**
- Zetic MLange SDK 1.3.0 (unified framework for all AI operations)

**Android Components:**
- CameraX for camera operations
- Material Design components
- RecyclerView for results display
- Lifecycle-aware components
- Kotlin Coroutines for async operations

**Dependencies:**
- `com.zeticai.mlange:mlange:1.3.0` - AI framework
- AndroidX libraries (core, appcompat, camera, lifecycle)
- Gson for JSON parsing

## Project Structure

```
wifi_reader2/
├── app/src/main/java/com/zetic/wifireader/
│   ├── MainActivity.kt                          # Main UI
│   ├── pipeline/WiFiDetectionPipeline.kt        # Detection orchestrator
│   ├── ml/ZeticMLangeDetector.kt                # YOLOv8 detection
│   ├── ocr/ZeticMLangeOCREngine.kt              # OCR engine
│   ├── model/ZeticTextDetector.kt               # Text detection
│   ├── model/ZeticTextRecognizer.kt             # Text recognition
│   ├── llm/ZeticMLangeLLMParser.kt              # Credential parsing
│   └── camera/CameraManager.kt                  # Camera handling
├── docs/
│   ├── ADAPTATION_NOTES.md                      # YOLOv8 notes
│   ├── PROJECT_STRUCTURE.md                     # Project organization
│   └── ZeticMLangeIntegration.md                # Zetic integration guide
└── README.md                                     # This file
```

For detailed project structure, see [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md).

## Development

### Building the APK

```bash
# Debug build
./gradlew app:assembleDebug

# Release build (requires signing configuration)
./gradlew app:assembleRelease
```

### Installing on Device

```bash
# Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or use Gradle
./gradlew installDebug
```

### Testing

The app includes instrumentation tests for the OCR engine and detection pipeline:

```bash
./gradlew connectedAndroidTest
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/YourFeature`)
3. Commit changes (`git commit -m 'Add YourFeature'`)
4. Push to branch (`git push origin feature/YourFeature`)
5. Create Pull Request

## License

This project is licensed under the MIT License.

## Notes

- **Physical Device Required**: Camera functionality requires a physical Android device
- **Internet Connection**: Required for initial model downloads (models are cached after first use)
- **Performance**: Varies based on device capabilities and lighting conditions
- **Best Results**: Ensure good lighting and hold camera steady for 2-3 seconds
- **Privacy**: All processing happens on-device after initial model download
