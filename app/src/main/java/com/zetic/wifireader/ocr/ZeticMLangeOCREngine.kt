package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import com.zetic.wifireader.model.ZeticTextDetector
import com.zetic.wifireader.model.ZeticTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZeticMLangeOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "ZeticMLangeOCREngine"
        private const val MIN_CONFIDENCE = 0.5f
    }

    private val textDetector by lazy { ZeticTextDetector(context) }
    private val textRecognizer by lazy { ZeticTextRecognizer(context) }
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[ZeticMLange] Initializing Zetic MLange Text Detection and Recognition Engine...")

        try {
            // Initialize both text detector and recognizer following YOLOv8 pattern
            val detectorInit = textDetector.initialize()
            val recognizerInit = textRecognizer.initialize()

            isInitialized = detectorInit && recognizerInit

            if (isInitialized) {
                Log.i(TAG, "[ZeticMLange] Successfully initialized both text detection and recognition engines")
            } else {
                Log.e(TAG, "[ZeticMLange] Failed to initialize - Detector: $detectorInit, Recognizer: $recognizerInit")
            }

            return@withContext isInitialized

        } catch (e: Exception) {
            Log.e(TAG, "[ZeticMLange] Failed to initialize: ${e.message}", e)
            isInitialized = false
            return@withContext false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            Log.e(TAG, "❌ [ZeticMLange] ENGINE NOT INITIALIZED - Models failed to load!")
            Log.e(TAG, "❌ [ZeticMLange] Cannot extract text - returning empty list")
            Log.e(TAG, "❌ [ZeticMLange] Check model URLs and API key for: jkim711/text_detect2, jkim711/text_recog3")
            return@withContext emptyList()
        }

        try {
            Log.d(TAG, "[ZeticMLange] Running two-stage text detection and recognition on bitmap ${bitmap.width}x${bitmap.height}")

            // Crop bitmap if bounding box is provided
            val processedBitmap = if (boundingBox != null) {
                Log.d(TAG, "[ZeticMLange] Cropping with bounding box: $boundingBox")
                cropBitmap(bitmap, boundingBox)
            } else {
                bitmap
            }

            // Stage 1: Run text detection to find text regions
            val detections = textDetector.run(processedBitmap)

            // Filter by confidence
            val filteredDetections = detections.filter { it.confidence >= MIN_CONFIDENCE }
            Log.i(TAG, "[ZeticMLange] Detected ${filteredDetections.size} text regions")

            // Stage 2: Run text recognition on each detected region
            val recognizedRegions = filteredDetections.map { detection ->
                try {
                    // Crop bitmap to the detected text region
                    val regionBitmap = cropBitmap(processedBitmap, detection.boundingBox)

                    // Recognize text in this region
                    val recognizedText = textRecognizer.recognizeText(regionBitmap)

                    Log.d(TAG, "[ZeticMLange] Region recognized: '$recognizedText'")

                    // Return new TextRegion with recognized text
                    TextRegion(
                        text = recognizedText,
                        confidence = detection.confidence,
                        boundingBox = detection.boundingBox
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "[ZeticMLange] Failed to recognize text in region: ${e.message}")
                    // Return original detection if recognition fails
                    detection
                }
            }

            Log.i(TAG, "[ZeticMLange] Successfully processed ${recognizedRegions.size} text regions with recognition")
            return@withContext recognizedRegions

        } catch (e: Exception) {
            Log.e(TAG, "[ZeticMLange] Text extraction failed: ${e.message}", e)
            return@withContext emptyList()
        }
    }


    private fun cropBitmap(bitmap: Bitmap, boundingBox: BoundingBox): Bitmap {
        val x = boundingBox.x.toInt().coerceAtLeast(0)
        val y = boundingBox.y.toInt().coerceAtLeast(0)
        val width = boundingBox.width.toInt().coerceAtMost(bitmap.width - x)
        val height = boundingBox.height.toInt().coerceAtMost(bitmap.height - y)

        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    override fun release() {
        Log.i(TAG, "[ZeticMLange] Releasing Zetic MLange Text Detection and Recognition Engine")
        textDetector.release()
        textRecognizer.release()
        isInitialized = false
    }
}