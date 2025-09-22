package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TesseractOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "TesseractOCREngine"
        private const val TESSDATA_FOLDER = "tessdata"
        private const val DEFAULT_LANGUAGE = "eng"
        private const val LANGUAGE_PACK = "eng+spa+fra+deu" // English, Spanish, French, German
    }

    private var tesseract: TessBaseAPI? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean {
        Log.i(TAG, "🚀 Initializing Tesseract OCR Engine...")

        try {
            // Copy language data from assets to internal storage
            val tessDataPath = copyTessDataToInternalStorage()
            if (tessDataPath == null) {
                Log.e(TAG, "❌ Failed to copy tessdata files")
                return false
            }

            // Initialize Tesseract
            tesseract = TessBaseAPI()
            val initResult = tesseract?.init(tessDataPath, LANGUAGE_PACK)

            if (initResult == true) {
                // Configure Tesseract for better OCR performance on router labels
                configureTesseract()
                isInitialized = true
                Log.i(TAG, "✅ Tesseract OCR initialized successfully with languages: $LANGUAGE_PACK")
                return true
            } else {
                Log.e(TAG, "❌ Failed to initialize Tesseract")
                tesseract?.end()
                tesseract = null
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Tesseract initialization failed: ${e.message}", e)
            tesseract?.end()
            tesseract = null
            return false
        }
    }

    private fun copyTessDataToInternalStorage(): String? {
        try {
            val tessDataDir = File(context.filesDir, TESSDATA_FOLDER)
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }

            val languageFiles = listOf("eng.traineddata", "spa.traineddata", "fra.traineddata", "deu.traineddata")

            for (languageFile in languageFiles) {
                val destFile = File(tessDataDir, languageFile)
                if (!destFile.exists()) {
                    Log.d(TAG, "📄 Copying $languageFile to internal storage...")
                    copyAssetToFile(languageFile, destFile)
                    Log.d(TAG, "✅ Copied $languageFile (${destFile.length() / 1024 / 1024}MB)")
                } else {
                    Log.d(TAG, "✅ $languageFile already exists (${destFile.length() / 1024 / 1024}MB)")
                }
            }

            return context.filesDir.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to copy tessdata: ${e.message}", e)
            return null
        }
    }

    private fun copyAssetToFile(assetName: String, destFile: File) {
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to copy asset $assetName: ${e.message}", e)
            throw e
        }
    }

    private fun configureTesseract() {
        tesseract?.apply {
            // Configure for better recognition of router labels and test text
            // Use LSTM OCR Engine Mode for better accuracy
            setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.:()[]!@#$%^&*+=?/\\|~`'\";,<> ")

            // Page segmentation mode - treat image as single text block or auto-detect
            pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO

            // Set engine mode to LSTM only for better accuracy
            setVariable("tessedit_ocr_engine_mode", "1")

            // Improve text recognition for various text sizes
            setVariable("textord_min_linesize", "0.5")
            setVariable("preserve_interword_spaces", "1")

            // Additional settings for better synthetic text recognition
            setVariable("tessedit_char_blacklist", "")  // Don't blacklist any characters
            setVariable("classify_enable_learning", "0")  // Disable learning for consistent results
            setVariable("textord_really_old_xheight", "1")  // Better handling of text heights

            Log.d(TAG, "🔧 Tesseract configured for router label recognition and testing")
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> {
        if (!isInitialized || tesseract == null) {
            Log.w(TAG, "❌ Tesseract not initialized")
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

            Log.d(TAG, "📄 Running Tesseract OCR on ${targetBitmap.width}x${targetBitmap.height} image...")

            // Set the image for Tesseract
            tesseract?.setImage(targetBitmap)

            // Get the recognized text
            val recognizedText = tesseract?.utF8Text?.trim() ?: ""

            // Get confidence score
            val confidence = tesseract?.meanConfidence() ?: 0

            Log.d(TAG, "📝 Tesseract result:")
            Log.d(TAG, "   Text: '$recognizedText'")
            Log.d(TAG, "   Confidence: $confidence%")

            val textRegions = if (recognizedText.isNotEmpty()) {
                listOf(
                    TextRegion(
                        text = recognizedText,
                        confidence = confidence / 100f, // Convert percentage to 0-1 range
                        boundingBox = boundingBox ?: BoundingBox(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                    )
                )
            } else {
                Log.w(TAG, "❌ No text detected by Tesseract")
                emptyList()
            }

            // Clean up cropped bitmap if it was created
            if (boundingBox != null && targetBitmap != bitmap) {
                targetBitmap.recycle()
            }

            return textRegions

        } catch (e: Exception) {
            Log.e(TAG, "❌ Tesseract OCR failed: ${e.message}", e)
            return emptyList()
        }
    }

    override fun release() {
        Log.d(TAG, "🔄 Releasing Tesseract resources")
        tesseract?.end()
        tesseract = null
        isInitialized = false
    }
}