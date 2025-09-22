# WiFi Reader - Android App with Zetic.MLange

A Kotlin Android application that detects WiFi SSID and password text from router labels using a hybrid approach with Zetic.MLange YOLOv8 for object detection and OCR for text recognition.

## Features

- **Real-time Detection**: Uses camera to detect router labels in real-time
- **Hybrid AI Approach**:
  - Zetic.MLange YOLOv8 for detecting router label regions
  - EasyOCR/TensorFlow Lite OCR for text extraction
- **Smart Text Parsing**: Automatically identifies SSID and password from extracted text
- **User-friendly UI**: Clean camera interface with overlay detection results
- **Copy to Clipboard**: Easy copying of detected credentials
- **Flash Support**: Toggle camera flash for better detection in low light

## Architecture

### Core Components

1. **ZeticMLangeDetector**: YOLOv8 model integration for router label detection
2. **OCREngine**: Text recognition with multiple engine support (EasyOCR, TensorFlow Lite)
3. **WiFiDetectionPipeline**: Coordinates detection and OCR processing
4. **CameraManager**: Handles camera operations and image capture
5. **MainActivity**: Main UI controller with permission handling

### Detection Pipeline

```
Camera Frame → YOLOv8 Detection → Text Region Extraction → OCR → Text Parsing → Results
```

## Setup

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Camera permission
- **Zetic MLange Account** (https://mlange.zetic.ai/)

### Quick Start

1. **Get Zetic MLange Account**:
   - Visit https://mlange.zetic.ai/
   - Create account and generate Personal Key

2. **Configuration Ready**:
   - App is pre-configured with working Zetic MLange keys
   - Uses `dev_854ee24efea74a05852a50916e61518f` personal key
   - Uses `Ultralytics/YOLOv8n` model for object detection

3. **Build and Run**:
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Model Setup

**Option 1: Use Zetic MLange (Recommended)**
- Models are managed through Zetic cloud platform
- Automatic optimization for mobile devices
- No manual model file management required

**Option 2: Local Models (Fallback)**
- Place model files in `app/src/main/assets/models/`
- See `SETUP_GUIDE.md` for detailed instructions

For detailed setup instructions, see [SETUP_GUIDE.md](SETUP_GUIDE.md)

## Usage

1. Launch the app
2. Grant camera permission when prompted
3. Point camera at router label
4. Wait for automatic detection
5. Tap detected credentials to copy to clipboard

## Technical Details

### Supported Router Label Formats

- Standard format: "SSID: NetworkName, Password: Password123"
- Network format: "Network: NetworkName, Key: Password123"
- WiFi format: "WiFi: NetworkName, PWD: Password123"

### Performance Optimizations

- Detection throttling (2-second intervals)
- Background processing with coroutines
- Efficient bitmap processing
- Memory management for models

### Dependencies

- CameraX for camera operations
- TensorFlow Lite for AI models
- RecyclerView for results display
- Material Design components

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

This project is licensed under the MIT License.

## Notes

- Requires physical device for testing (camera needed)
- Model files not included in repository (add your own trained models)
- Performance varies based on device capabilities
- Best results with good lighting and clear router labels