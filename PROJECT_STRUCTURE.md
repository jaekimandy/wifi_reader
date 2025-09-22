# WiFi Reader Project Structure

## Directory Organization

```
wifi_reader2/
├── app/                          # Android application source code
│   ├── src/main/
│   │   ├── java/                 # Kotlin/Java source files
│   │   ├── res/                  # Android resources
│   │   └── assets/               # Model files and assets
│   └── build.gradle.kts          # App-level Gradle configuration
├── opencv/                       # OpenCV Android SDK integration
├── scripts/                      # Utility scripts
│   └── download_ocr_models.py    # OCR model downloader script
├── docs/                         # Project documentation
│   ├── DETECTION_PIPELINE_ANALYSIS.md
│   └── TECHNICAL_SUMMARY.md
├── tests/                        # Test files and resources
│   └── test_wifi_text.html       # WiFi credential test display
├── build.gradle.kts              # Project-level Gradle configuration
├── settings.gradle.kts           # Gradle settings
└── README.md                     # Main project documentation
```

## File Descriptions

### `/scripts/`
- **`download_ocr_models.py`**: Python script to download and prepare TensorFlow Lite OCR models
  - Downloads text detection and recognition models
  - Converts to TFLite format if needed
  - Places models in correct Android assets directory

### `/docs/`
- **`SETUP_GUIDE.md`**: Complete setup and configuration guide
  - Zetic MLange account setup
  - Model configuration and deployment
  - Build and installation instructions
  - Troubleshooting common issues
- **`ADAPTATION_NOTES.md`**: Notes on adapting YOLOv8n for router detection
  - Current model limitations and workarounds
  - Custom model training guidance
  - Performance optimization strategies
- **`DETECTION_PIPELINE_ANALYSIS.md`**: Comprehensive analysis of the detection pipeline
  - Architecture overview
  - Component status and issues
  - Detailed logging analysis
  - Performance metrics
- **`TECHNICAL_SUMMARY.md`**: Executive summary of technical findings
  - Root cause analysis
  - Solution options with effort estimates
  - Implementation recommendations

### `/tests/`
- **`test_wifi_text.html`**: Test page displaying WiFi credentials
  - Bold, large text for OCR testing
  - Multiple WiFi network examples
  - Optimized for mobile camera capture

## Usage Instructions

### Running Scripts
```bash
# Download OCR models
cd scripts
python download_ocr_models.py

# Or from project root
python scripts/download_ocr_models.py
```

### Viewing Test Content
```bash
# Open test page in browser
start tests/test_wifi_text.html
```

### Building the App
```bash
# Build debug APK
./gradlew.bat app:assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Development Workflow

1. **Setup**: Run `scripts/download_ocr_models.py` to prepare models
2. **Test**: Open `tests/test_wifi_text.html` for OCR testing
3. **Build**: Use Gradle to build the Android APK
4. **Debug**: Check `docs/` for troubleshooting information

## Clean Project Structure Benefits

- **Separation of Concerns**: Code, docs, scripts, and tests are properly separated
- **Easy Navigation**: Clear directory structure for developers
- **Maintainability**: Each file type has its designated location
- **Collaboration**: Standard structure familiar to developers
- **Automation**: Scripts can be run independently of source code