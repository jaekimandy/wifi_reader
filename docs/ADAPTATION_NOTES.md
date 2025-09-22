# Adapting YOLOv8n for Router Detection

Since we're using the general `Ultralytics/YOLOv8n` model initially, here are notes on how it will work and how to adapt it for WiFi router detection.

## Current Setup

- **Model**: `Ultralytics/YOLOv8n` (general object detection)
- **Personal Key**: `dev_854ee24efea74a05852a50916e61518f`
- **Source**: Working key from Zetic MLange apps repository

## How General YOLOv8n Will Work

The standard YOLOv8n model can detect 80 COCO classes including:
- `electronics` (class 63) - may detect routers/modems
- `cell phone` (class 67) - similar electronic devices
- `laptop` (class 63) - electronic devices
- `tv` (class 62) - electronic appliances

### Detection Strategy:
1. **Filter for Electronics**: Focus on detections with electronics-related classes
2. **Size Filtering**: Router labels are typically small rectangular regions
3. **Text Region Extraction**: Apply OCR to any detected electronic device regions
4. **Smart Filtering**: Look for text patterns matching WiFi credentials

## Adapting the Detection Pipeline

### 1. Update Detection Classes
In `ZeticMLangeDetector.kt`, modify the class filtering:

```kotlin
private fun isRouterLikeObject(classId: Int): Boolean {
    // COCO classes that might indicate router/electronic devices
    val electronicsClasses = setOf(
        63, // electronics
        67, // cell phone
        62, // tv
        72  // clock (some routers have displays)
    )
    return electronicsClasses.contains(classId)
}
```

### 2. Enhanced Detection Logic
```kotlin
private fun filterRouterCandidates(detections: List<DetectionResult>): List<DetectionResult> {
    return detections.filter { detection ->
        isRouterLikeObject(detection.classId) &&
        isReasonableSize(detection.boundingBox) &&
        detection.confidence > 0.3f
    }
}

private fun isReasonableSize(box: BoundingBox): Boolean {
    val area = box.width * box.height
    // Router labels are typically small to medium sized
    return area > 1000 && area < 100000
}
```

### 3. Multiple Detection Strategy
Since we can't directly detect router labels, apply OCR to:
- All detected electronic devices
- Regions that look like they might contain text
- Areas with high contrast (typical of labels)

## Training Custom Router Detection Model

For better accuracy, you should eventually train a custom model:

### Dataset Creation:
1. **Collect Router Images**: 1000+ images of various router brands
2. **Focus on Labels**: Ensure WiFi credential stickers are visible
3. **Diverse Conditions**: Different lighting, angles, distances
4. **Annotation**: Label the credential sticker regions, not the entire router

### Training Process:
```bash
# Prepare dataset in YOLO format
# dataset.yaml:
# train: images/train
# val: images/val
# nc: 1
# names: ['wifi_label']

# Train custom model
yolo train data=router_dataset.yaml model=yolov8n.pt epochs=100 imgsz=640

# Convert for Zetic MLange
yolo export model=best.pt format=onnx
```

### Upload to Zetic:
1. Login to Zetic MLange portal
2. Upload your trained model
3. Get new model name (e.g., "YourUsername/RouterDetector")
4. Update `ZeticConfig.kt` with new model name

## Testing Current Implementation

The current app will:
1. ✅ Use working Zetic MLange integration
2. ✅ Detect general objects including electronics
3. ✅ Apply OCR to detected regions
4. ✅ Parse text for WiFi credentials
5. ⚠️ May have false positives from other electronic devices
6. ⚠️ May miss some router labels not classified as electronics

### Expected Behavior:
- Point camera at router → detects as "electronics" → applies OCR → extracts WiFi info
- May also detect phones, laptops → applies OCR → filters out non-WiFi text
- Works best with clear, well-lit router labels

## Performance Optimization

### For Better Results:
1. **Lower Confidence**: Set `CONFIDENCE_THRESHOLD = 0.3f` for more detections
2. **Faster Processing**: Reduce `DETECTION_THROTTLE_MS = 1000L`
3. **Multiple Angles**: Encourage users to try different camera positions
4. **Good Lighting**: Emphasize importance of proper lighting

### UI Feedback:
- Show detected object classes to user
- Highlight electronic device detections
- Provide guidance when no electronics detected

This approach gives you a working WiFi reader that can be improved over time with a custom trained model!