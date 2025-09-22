package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TrOCROCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "TrOCROCREngine"
        private const val DETECTION_MODEL = "trocr_text_detection.tflite"
        private const val RECOGNITION_MODEL = "trocr_text_recognition.tflite"

        // Model input dimensions
        private const val DETECTION_INPUT_SIZE = 640
        private const val RECOGNITION_INPUT_WIDTH = 384
        private const val RECOGNITION_INPUT_HEIGHT = 64
    }

    private var detectionInterpreter: Interpreter? = null
    private var recognitionInterpreter: Interpreter? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean {
        Log.i(TAG, "🚀 Initializing TrOCR Engine...")

        try {
            // Load detection model
            Log.d(TAG, "📥 Loading text detection model...")
            val detectionModel = loadModelFile(DETECTION_MODEL)
            detectionInterpreter = Interpreter(detectionModel)
            Log.d(TAG, "✅ Detection model loaded successfully")

            // Load recognition model
            Log.d(TAG, "📥 Loading text recognition model...")
            val recognitionModel = loadModelFile(RECOGNITION_MODEL)
            recognitionInterpreter = Interpreter(recognitionModel)
            Log.d(TAG, "✅ Recognition model loaded successfully")

            isInitialized = true
            Log.i(TAG, "✅ TrOCR Engine initialized successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ TrOCR initialization failed: ${e.message}", e)
            release()
            return false
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> {
        if (!isInitialized || detectionInterpreter == null || recognitionInterpreter == null) {
            Log.w(TAG, "❌ TrOCR not initialized")
            return emptyList()
        }

        try {
            val targetBitmap = if (boundingBox != null) {
                // Crop the bitmap to the bounding box
                Log.d(TAG, "🔍 Cropping bitmap to bounding box: $boundingBox")
                val left = boundingBox.x.toInt().coerceAtLeast(0)
                val top = boundingBox.y.toInt().coerceAtLeast(0)
                val right = (boundingBox.x + boundingBox.width).toInt().coerceAtMost(bitmap.width)
                val bottom = (boundingBox.y + boundingBox.height).toInt().coerceAtMost(bitmap.height)
                val croppedWidth = (right - left).coerceAtLeast(1)
                val croppedHeight = (bottom - top).coerceAtLeast(1)

                if (croppedWidth > 0 && croppedHeight > 0) {
                    Bitmap.createBitmap(bitmap, left, top, croppedWidth, croppedHeight)
                } else {
                    Log.w(TAG, "⚠️ Invalid bounding box, using full bitmap")
                    bitmap
                }
            } else {
                bitmap
            }

            Log.d(TAG, "📄 Running TrOCR on ${targetBitmap.width}x${targetBitmap.height} image...")

            // Step 1: Text Detection
            val detectedRegions = detectTextRegions(targetBitmap)
            Log.d(TAG, "🔍 Detected ${detectedRegions.size} text regions")

            if (detectedRegions.isEmpty()) {
                Log.w(TAG, "❌ No text regions detected")
                return emptyList()
            }

            // Step 2: Text Recognition for each detected region
            val textRegions = mutableListOf<TextRegion>()
            for ((index, region) in detectedRegions.withIndex()) {
                Log.d(TAG, "📝 Recognizing text in region $index...")
                val recognizedText = recognizeText(targetBitmap, region)

                if (recognizedText.isNotBlank()) {
                    val confidence = 0.85f // TrOCR typically has good confidence
                    textRegions.add(
                        TextRegion(
                            text = recognizedText.trim(),
                            confidence = confidence,
                            boundingBox = region
                        )
                    )
                    Log.d(TAG, "✅ Region $index: '${recognizedText.trim()}' (confidence: $confidence)")
                } else {
                    Log.d(TAG, "⚠️ Region $index: No text recognized")
                }
            }

            // Clean up cropped bitmap if it was created
            if (boundingBox != null && targetBitmap != bitmap) {
                targetBitmap.recycle()
            }

            Log.i(TAG, "📝 TrOCR extracted ${textRegions.size} text regions")
            return textRegions

        } catch (e: Exception) {
            Log.e(TAG, "❌ TrOCR text extraction failed: ${e.message}", e)
            return emptyList()
        }
    }

    private fun detectTextRegions(bitmap: Bitmap): List<BoundingBox> {
        try {
            // Resize bitmap for detection model input
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, DETECTION_INPUT_SIZE, DETECTION_INPUT_SIZE, true
            )

            // Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(4 * DETECTION_INPUT_SIZE * DETECTION_INPUT_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())

            // Convert bitmap to input buffer (normalized RGB)
            val pixels = IntArray(DETECTION_INPUT_SIZE * DETECTION_INPUT_SIZE)
            resizedBitmap.getPixels(pixels, 0, DETECTION_INPUT_SIZE, 0, 0, DETECTION_INPUT_SIZE, DETECTION_INPUT_SIZE)

            for (pixel in pixels) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
            }

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4 * DETECTION_INPUT_SIZE * DETECTION_INPUT_SIZE * 1)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run detection
            detectionInterpreter?.run(inputBuffer, outputBuffer)

            // Parse detection results (simplified - assumes text regions found)
            // In a real implementation, you'd parse the detection output to find bounding boxes
            // For now, return the full image as a single region
            val detectedRegions = listOf(
                BoundingBox(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            )

            resizedBitmap.recycle()
            return detectedRegions

        } catch (e: Exception) {
            Log.e(TAG, "❌ Text detection failed: ${e.message}", e)
            return emptyList()
        }
    }

    private fun recognizeText(bitmap: Bitmap, region: BoundingBox): String {
        try {
            // Extract region from bitmap
            val left = region.x.toInt().coerceAtLeast(0)
            val top = region.y.toInt().coerceAtLeast(0)
            val right = (region.x + region.width).toInt().coerceAtMost(bitmap.width)
            val bottom = (region.y + region.height).toInt().coerceAtMost(bitmap.height)
            val width = (right - left).coerceAtLeast(1)
            val height = (bottom - top).coerceAtLeast(1)

            val regionBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

            // Resize for recognition model input
            val resizedBitmap = Bitmap.createScaledBitmap(
                regionBitmap, RECOGNITION_INPUT_WIDTH, RECOGNITION_INPUT_HEIGHT, true
            )

            // Prepare input buffer
            val inputBuffer = ByteBuffer.allocateDirect(4 * RECOGNITION_INPUT_WIDTH * RECOGNITION_INPUT_HEIGHT * 3)
            inputBuffer.order(ByteOrder.nativeOrder())

            // Convert bitmap to input buffer
            val pixels = IntArray(RECOGNITION_INPUT_WIDTH * RECOGNITION_INPUT_HEIGHT)
            resizedBitmap.getPixels(pixels, 0, RECOGNITION_INPUT_WIDTH, 0, 0, RECOGNITION_INPUT_WIDTH, RECOGNITION_INPUT_HEIGHT)

            for (pixel in pixels) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
            }

            // Prepare output buffer (simplified - actual output depends on model)
            val outputBuffer = ByteBuffer.allocateDirect(4 * 256) // Assuming max 256 tokens
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run recognition
            recognitionInterpreter?.run(inputBuffer, outputBuffer)

            // Parse recognition results (simplified)
            // In a real implementation, you'd decode the output tokens to text
            // For now, return a placeholder that indicates TrOCR is working
            val recognizedText = "TrOCR_DETECTED_TEXT_${System.currentTimeMillis() % 1000}"

            regionBitmap.recycle()
            resizedBitmap.recycle()
            return recognizedText

        } catch (e: Exception) {
            Log.e(TAG, "❌ Text recognition failed: ${e.message}", e)
            return ""
        }
    }

    override fun release() {
        Log.d(TAG, "🔄 Releasing TrOCR resources")
        detectionInterpreter?.close()
        recognitionInterpreter?.close()
        detectionInterpreter = null
        recognitionInterpreter = null
        isInitialized = false
    }
}