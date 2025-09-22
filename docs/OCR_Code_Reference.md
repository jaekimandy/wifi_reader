# OCR Code Reference for WiFi Reader Project

This document contains all OCR-related code implementations for future reference, especially for ONNX integration.

## Current OCR Implementation

### 1. MLKitOCREngine.kt (Current Working Implementation)

```kotlin
package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MLKitOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "MLKitOCREngine"
        private const val MIN_CONFIDENCE = 0.7f
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[ML Kit] Initializing Google ML Kit OCR Engine...")
        try {
            isInitialized = true
            Log.i(TAG, "[ML Kit] ✅ Google ML Kit OCR initialized successfully")
            Log.d(TAG, "[ML Kit] Using Latin text recognizer with default options")
            Log.d(TAG, "[ML Kit] Minimum confidence threshold: $MIN_CONFIDENCE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "[ML Kit] ❌ Failed to initialize: ${e.message}")
            false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        Log.i(TAG, "[ML Kit] 📄 Starting text extraction...")
        Log.d(TAG, "[ML Kit] Input bitmap: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "[ML Kit] Bounding box: $boundingBox")

        if (!isInitialized) {
            Log.w(TAG, "[ML Kit] ❌ ML Kit OCR not initialized")
            return@withContext emptyList()
        }

        try {
            // Crop bitmap if bounding box is provided
            val targetBitmap = if (boundingBox != null) {
                Log.d(TAG, "[ML Kit] 🔲 Cropping bitmap to bounding box...")
                cropBitmap(bitmap, boundingBox)
            } else {
                Log.d(TAG, "[ML Kit] 📸 Using full bitmap")
                bitmap
            }

            Log.d(TAG, "[ML Kit] Target bitmap: ${targetBitmap.width}x${targetBitmap.height}")

            // Create InputImage for ML Kit
            val image = InputImage.fromBitmap(targetBitmap, 0)
            Log.d(TAG, "[ML Kit] 🔍 Created InputImage for ML Kit processing")

            // Run text recognition
            Log.d(TAG, "[ML Kit] 📝 Running text recognition...")
            val result = suspendCancellableCoroutine<List<TextRegion>> { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val textRegions = mutableListOf<TextRegion>()

                        Log.d(TAG, "[ML Kit] 📊 Found ${visionText.textBlocks.size} text blocks")

                        for ((blockIndex, block) in visionText.textBlocks.withIndex()) {
                            Log.v(TAG, "[ML Kit] Block $blockIndex: '${block.text}' (confidence: ${block.confidence ?: 0f})")

                            for ((lineIndex, line) in block.lines.withIndex()) {
                                Log.v(TAG, "[ML Kit]   Line $lineIndex: '${line.text}' (confidence: ${line.confidence ?: 0f})")

                                for ((elementIndex, element) in line.elements.withIndex()) {
                                    val confidence = element.confidence ?: 0f
                                    Log.v(TAG, "[ML Kit]     Element $elementIndex: '${element.text}' (confidence: $confidence)")

                                    if (confidence >= MIN_CONFIDENCE) {
                                        val boundingRect = element.boundingBox
                                        val textRegion = TextRegion(
                                            text = element.text,
                                            confidence = confidence,
                                            boundingBox = if (boundingRect != null) {
                                                BoundingBox(
                                                    x = boundingRect.left.toFloat(),
                                                    y = boundingRect.top.toFloat(),
                                                    width = boundingRect.width().toFloat(),
                                                    height = boundingRect.height().toFloat()
                                                )
                                            } else null
                                        )
                                        textRegions.add(textRegion)

                                        Log.i(TAG, "[ML Kit] 📝 TEXT DETECTED #${textRegions.size - 1}: '${element.text}'")
                                    } else {
                                        Log.v(TAG, "[ML Kit] ⚠️ Low confidence text filtered: '${element.text}' ($confidence < $MIN_CONFIDENCE)")
                                    }
                                }
                            }
                        }

                        Log.i(TAG, "[ML Kit] ✅ Text extraction completed: ${textRegions.size} regions found")
                        continuation.resume(textRegions)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "[ML Kit] ❌ Text recognition failed: ${exception.message}")
                        continuation.resume(emptyList())
                    }
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "[ML Kit] ❌ Text extraction failed: ${e.message}")
            emptyList()
        }
    }

    private fun cropBitmap(bitmap: Bitmap, boundingBox: BoundingBox): Bitmap {
        return try {
            // Ensure coordinates are within bitmap bounds
            val x = maxOf(0, boundingBox.x.toInt())
            val y = maxOf(0, boundingBox.y.toInt())
            val width = minOf(bitmap.width - x, boundingBox.width.toInt())
            val height = minOf(bitmap.height - y, boundingBox.height.toInt())

            // Ensure positive dimensions
            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } else {
                Log.w(TAG, "[ML Kit] Invalid crop dimensions, using original bitmap")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ML Kit] Failed to crop bitmap: ${e.message}")
            bitmap
        }
    }

    override fun release() {
        Log.d(TAG, "[ML Kit] OCR engine released")
        isInitialized = false
    }
}
```

### 2. OCREngine Interface

```kotlin
package com.zetic.wifireader.ocr

import android.graphics.Bitmap
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion

interface OCREngine {
    suspend fun initialize(): Boolean
    suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox? = null): List<TextRegion>
    fun release()
}
```

### 3. Model Classes

```kotlin
// TextRegion.kt
data class TextRegion(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
)

// BoundingBox.kt
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
```

## Future ONNX Implementation Template

### OnnxOCREngine.kt (Template for ONNX integration)

```kotlin
package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ONNX-based OCR Engine for custom text recognition models
 *
 * Benefits over ML Kit:
 * - Custom models trained on specific text types (router labels, etc.)
 * - Offline operation with no Google dependency
 * - Potentially better performance on camera images
 * - Control over preprocessing pipeline
 */
class OnnxOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "OnnxOCREngine"
        private const val MODEL_NAME = "text_recognition_model.onnx"
    }

    private var isInitialized = false
    // TODO: Add ONNX Runtime session
    // private var ortSession: OrtSession? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[ONNX] Initializing ONNX OCR Engine...")

        try {
            // TODO: Initialize ONNX Runtime
            // 1. Load model from assets
            // 2. Create ORT environment
            // 3. Create inference session
            // 4. Validate input/output shapes

            Log.i(TAG, "[ONNX] Steps for ONNX implementation:")
            Log.i(TAG, "[ONNX] 1. Add ONNX Runtime dependency to build.gradle")
            Log.i(TAG, "[ONNX] 2. Download/train text recognition ONNX model")
            Log.i(TAG, "[ONNX] 3. Implement preprocessing (resize, normalize, etc.)")
            Log.i(TAG, "[ONNX] 4. Implement postprocessing (decode text from logits)")
            Log.i(TAG, "[ONNX] 5. Handle text detection + recognition pipeline")

            isInitialized = false // Set to true when implemented
            isInitialized

        } catch (e: Exception) {
            Log.e(TAG, "[ONNX] ❌ Failed to initialize: ${e.message}")
            false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        Log.i(TAG, "[ONNX] 📄 Starting text extraction...")

        if (!isInitialized) {
            Log.w(TAG, "[ONNX] ❌ ONNX OCR not initialized")
            return@withContext emptyList()
        }

        try {
            // TODO: Implement ONNX inference
            // 1. Preprocess bitmap (crop, resize, normalize)
            // 2. Convert to tensor format
            // 3. Run inference
            // 4. Postprocess results (decode text)
            // 5. Apply confidence filtering
            // 6. Return TextRegion objects

            Log.w(TAG, "[ONNX] 📋 Placeholder - ONNX inference would happen here")
            emptyList<TextRegion>()

        } catch (e: Exception) {
            Log.e(TAG, "[ONNX] ❌ Text extraction failed: ${e.message}")
            emptyList()
        }
    }

    override fun release() {
        Log.d(TAG, "[ONNX] OCR engine released")
        // TODO: Release ONNX session
        // ortSession?.close()
        isInitialized = false
    }
}
```

## ONNX Integration Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // ONNX Runtime for Android
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // Optional: ONNX Runtime GPU support
    // implementation("com.microsoft.onnxruntime:onnxruntime-android-gpu:1.16.3")
}
```

## Camera Image Enhancement Code

```kotlin
// From CameraManager.kt - Image enhancement for OCR
private fun enhanceForOCR(bitmap: Bitmap): Bitmap {
    return try {
        // Create enhanced bitmap with same dimensions
        val enhanced = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        // Process each pixel to enhance contrast and reduce noise
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Convert to grayscale using luminance formula
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // Apply adaptive contrast enhancement
            val enhanced = when {
                gray < 85 -> 0        // Dark areas -> pure black
                gray > 170 -> 255     // Light areas -> pure white
                else -> {
                    // Mid-tones: apply S-curve for better contrast
                    val normalized = gray / 255.0
                    val curved = Math.pow(normalized, 0.8) // Slight gamma correction
                    (curved * 255).toInt().coerceIn(0, 255)
                }
            }

            // Create high-contrast pixel
            val enhancedPixel = (0xFF shl 24) or (enhanced shl 16) or (enhanced shl 8) or enhanced
            pixels[i] = enhancedPixel
        }

        enhanced.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        enhanced

    } catch (e: Exception) {
        Log.e(TAG, "Failed to enhance image for OCR: ${e.message}")
        bitmap // Return original if enhancement fails
    }
}
```

## Detection Pipeline Integration

```kotlin
// From WiFiDetectionPipeline.kt
class WiFiDetectionPipeline(private val context: Context) {

    // Current: ML Kit OCR
    private val ocrEngine: OCREngine = MLKitOCREngine(context)

    // Future: Switch to ONNX when ready
    // private val ocrEngine: OCREngine = OnnxOCREngine(context)

    fun getOCREngine(): OCREngine = ocrEngine

    suspend fun detectWiFiCredentials(bitmap: Bitmap): List<WiFiCredentials> {
        // ... YOLO detection logic ...

        // OCR extraction from detected regions
        val textRegions = ocrEngine.extractText(bitmap, detection.boundingBox)

        // ... text parsing logic ...
    }
}
```

## Key Findings and Recommendations

### Current Issues:
1. **Camera image quality**: ML Kit OCR detects 0 text regions from camera images
2. **Enhancement algorithm**: May be destroying text instead of improving it
3. **Focus/lighting**: Camera settings may not be optimal for text capture

### For ONNX Implementation:
1. **Text Detection + Recognition**: Consider separate models for detection and recognition
2. **Custom Training**: Train on router label dataset for better accuracy
3. **Preprocessing**: Implement perspective correction, dewarping, noise reduction
4. **Model Selection**: Consider TrOCR, PaddleOCR, or EasyOCR ONNX models

### Alternative OCR Solutions:
1. **Tesseract 5.0+**: Native Android integration
2. **Google Vision API**: Cloud-based, very accurate
3. **Custom ONNX models**: Trained specifically for router labels
4. **Ensemble approach**: Multiple models voting for best results

## Next Steps:
1. Analyze gallery images to understand camera quality issues
2. Improve camera settings and image preprocessing
3. Research ONNX text recognition models
4. Consider training custom model on router label dataset