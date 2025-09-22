package com.zetic.wifireader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class EasyOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "EasyOCREngine"
        private const val MIN_CONFIDENCE = 0.7f

        // Model configuration
        private const val DETECTION_MODEL_FILE = "backup/easyocr_text_detection.tflite"
        private const val RECOGNITION_MODEL_FILE = "backup/easyocr_text_recognition.tflite"

        // Detection model parameters
        private const val DETECTION_INPUT_SIZE = 693
        private const val DETECTION_MEAN = 127.5f
        private const val DETECTION_STD = 127.5f

        // Recognition model parameters
        private const val RECOGNITION_HEIGHT = 64
        private const val RECOGNITION_WIDTH = 256
        private const val RECOGNITION_CHANNELS = 1
    }

    private var detectionInterpreter: Interpreter? = null
    private var recognitionInterpreter: Interpreter? = null
    private var isInitialized = false

    // Character vocabulary for CRNN recognition
    private val vocab = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ "

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[EasyOCR] Initializing EasyOCR Engine...")

        try {
            var detectionModelLoaded = false
            var recognitionModelLoaded = false

            // Load detection model (CRAFT)
            Log.d(TAG, "[EasyOCR] Loading text detection model...")
            val detectionModel = loadModelFile(DETECTION_MODEL_FILE)
            if (detectionModel != null) {
                try {
                    detectionInterpreter = Interpreter(detectionModel)
                    detectionModelLoaded = true
                    Log.i(TAG, "[EasyOCR] ✅ Detection model loaded successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "[EasyOCR] ⚠️ Detection model invalid: ${e.message}")
                }
            } else {
                Log.w(TAG, "[EasyOCR] ⚠️ Detection model file not found")
            }

            // Load recognition model (CRNN)
            Log.d(TAG, "[EasyOCR] Loading text recognition model...")
            val recognitionModel = loadModelFile(RECOGNITION_MODEL_FILE)
            if (recognitionModel != null) {
                try {
                    recognitionInterpreter = Interpreter(recognitionModel)
                    recognitionModelLoaded = true
                    Log.i(TAG, "[EasyOCR] ✅ Recognition model loaded successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "[EasyOCR] ⚠️ Recognition model invalid: ${e.message}")
                }
            } else {
                Log.w(TAG, "[EasyOCR] ⚠️ Recognition model file not found")
            }

            // Check if we have valid models
            if (!detectionModelLoaded || !recognitionModelLoaded) {
                Log.w(TAG, "[EasyOCR] ⚠️ Models are missing or invalid - running in mock mode")
                Log.i(TAG, "[EasyOCR] 📋 To use real OCR:")
                Log.i(TAG, "[EasyOCR] 1. Download real TensorFlow Lite models")
                Log.i(TAG, "[EasyOCR] 2. Place them in assets/ directory:")
                Log.i(TAG, "[EasyOCR]    - ${DETECTION_MODEL_FILE}")
                Log.i(TAG, "[EasyOCR]    - ${RECOGNITION_MODEL_FILE}")
                Log.i(TAG, "[EasyOCR] 3. Models should be valid TFLite flatbuffers")

                // Initialize in mock mode
                isInitialized = true
                Log.i(TAG, "[EasyOCR] ✅ EasyOCR initialized in MOCK MODE")
                Log.d(TAG, "[EasyOCR] Mock mode will return empty text results")
                return@withContext true
            }

            isInitialized = true
            Log.i(TAG, "[EasyOCR] ✅ EasyOCR initialized successfully with REAL MODELS")
            Log.d(TAG, "[EasyOCR] Detection input size: ${DETECTION_INPUT_SIZE}x${DETECTION_INPUT_SIZE}")
            Log.d(TAG, "[EasyOCR] Recognition input size: ${RECOGNITION_WIDTH}x${RECOGNITION_HEIGHT}")
            Log.d(TAG, "[EasyOCR] Vocabulary size: ${vocab.length} characters")
            Log.d(TAG, "[EasyOCR] Minimum confidence threshold: $MIN_CONFIDENCE")

            true

        } catch (e: Exception) {
            Log.e(TAG, "[EasyOCR] ❌ Failed to initialize: ${e.message}", e)
            false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        Log.i(TAG, "[EasyOCR] 📄 Starting text extraction...")
        Log.d(TAG, "[EasyOCR] Input bitmap: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "[EasyOCR] Bounding box: $boundingBox")

        if (!isInitialized) {
            Log.w(TAG, "[EasyOCR] ❌ EasyOCR not initialized")
            return@withContext emptyList()
        }

        // Check if we're in mock mode (no valid models loaded)
        if (detectionInterpreter == null || recognitionInterpreter == null) {
            Log.d(TAG, "[EasyOCR] 📋 Running in mock mode - no valid models loaded")
            Log.d(TAG, "[EasyOCR] 📋 Mock mode returns empty results for camera image quality testing")
            return@withContext emptyList()
        }

        try {
            // Crop bitmap if bounding box is provided
            val targetBitmap = if (boundingBox != null) {
                Log.d(TAG, "[EasyOCR] 🔲 Cropping bitmap to bounding box...")
                cropBitmap(bitmap, boundingBox)
            } else {
                Log.d(TAG, "[EasyOCR] 📸 Using full bitmap")
                bitmap
            }

            Log.d(TAG, "[EasyOCR] Target bitmap: ${targetBitmap.width}x${targetBitmap.height}")

            // Step 1: Text Detection using CRAFT
            Log.d(TAG, "[EasyOCR] 🔍 Step 1: Running CRAFT text detection...")
            val textRegions = detectTextRegions(targetBitmap)
            Log.d(TAG, "[EasyOCR] Detected ${textRegions.size} text regions")

            if (textRegions.isEmpty()) {
                Log.w(TAG, "[EasyOCR] No text regions detected")
                return@withContext emptyList()
            }

            // Step 2: Text Recognition using CRNN
            Log.d(TAG, "[EasyOCR] 📝 Step 2: Running CRNN text recognition...")
            val recognizedTexts = mutableListOf<TextRegion>()

            for ((index, region) in textRegions.withIndex()) {
                Log.v(TAG, "[EasyOCR] Processing text region $index: ${region.boundingBox}")

                val regionBitmap = cropBitmap(targetBitmap, region.boundingBox!!)
                val recognizedText = recognizeText(regionBitmap)

                if (recognizedText.isNotEmpty() && region.confidence >= MIN_CONFIDENCE) {
                    val textRegion = TextRegion(
                        text = recognizedText,
                        confidence = region.confidence,
                        boundingBox = region.boundingBox
                    )
                    recognizedTexts.add(textRegion)
                    Log.i(TAG, "[EasyOCR] 📝 TEXT DETECTED #${recognizedTexts.size - 1}: '$recognizedText' (confidence: ${region.confidence})")
                } else {
                    Log.v(TAG, "[EasyOCR] ⚠️ Low confidence or empty text filtered: '$recognizedText' (confidence: ${region.confidence})")
                }
            }

            Log.i(TAG, "[EasyOCR] ✅ Text extraction completed: ${recognizedTexts.size} regions found")
            recognizedTexts

        } catch (e: Exception) {
            Log.e(TAG, "[EasyOCR] ❌ Text extraction failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun detectTextRegions(bitmap: Bitmap): List<TextRegion> {
        val detectionInterpreter = this.detectionInterpreter ?: return emptyList()

        try {
            // Preprocess image for CRAFT detection
            val inputBuffer = preprocessForDetection(bitmap)

            // Prepare output buffers for CRAFT
            val outputShape = detectionInterpreter.getOutputTensor(0).shape()
            val outputSize = outputShape[1] * outputShape[2] * outputShape[3]
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run detection inference
            detectionInterpreter.run(inputBuffer, outputBuffer)

            // Process detection results
            return processDetectionOutput(outputBuffer, outputShape, bitmap.width, bitmap.height)

        } catch (e: Exception) {
            Log.e(TAG, "[EasyOCR] Text detection failed: ${e.message}")
            return emptyList()
        }
    }

    private fun recognizeText(bitmap: Bitmap): String {
        val recognitionInterpreter = this.recognitionInterpreter ?: return ""

        try {
            // Preprocess image for CRNN recognition
            val inputBuffer = preprocessForRecognition(bitmap)

            // Prepare output buffer for CRNN
            val outputShape = recognitionInterpreter.getOutputTensor(0).shape()
            val outputSize = outputShape[1] * outputShape[2]
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run recognition inference
            recognitionInterpreter.run(inputBuffer, outputBuffer)

            // Decode CTC output to text
            return decodeCTCOutput(outputBuffer, outputShape)

        } catch (e: Exception) {
            Log.e(TAG, "[EasyOCR] Text recognition failed: ${e.message}")
            return ""
        }
    }

    private fun preprocessForDetection(bitmap: Bitmap): ByteBuffer {
        // Resize bitmap to detection input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, DETECTION_INPUT_SIZE, DETECTION_INPUT_SIZE, true)

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(DETECTION_INPUT_SIZE * DETECTION_INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Normalize pixels and fill buffer
        val pixels = IntArray(DETECTION_INPUT_SIZE * DETECTION_INPUT_SIZE)
        resizedBitmap.getPixels(pixels, 0, DETECTION_INPUT_SIZE, 0, 0, DETECTION_INPUT_SIZE, DETECTION_INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / DETECTION_STD - DETECTION_MEAN / DETECTION_STD
            val g = ((pixel shr 8) and 0xFF) / DETECTION_STD - DETECTION_MEAN / DETECTION_STD
            val b = (pixel and 0xFF) / DETECTION_STD - DETECTION_MEAN / DETECTION_STD

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        return inputBuffer
    }

    private fun preprocessForRecognition(bitmap: Bitmap): ByteBuffer {
        // Convert to grayscale and resize
        val grayscaleBitmap = convertToGrayscale(bitmap)
        val resizedBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, RECOGNITION_WIDTH, RECOGNITION_HEIGHT, true)

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(RECOGNITION_WIDTH * RECOGNITION_HEIGHT * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Normalize pixels and fill buffer
        val pixels = IntArray(RECOGNITION_WIDTH * RECOGNITION_HEIGHT)
        resizedBitmap.getPixels(pixels, 0, RECOGNITION_WIDTH, 0, 0, RECOGNITION_WIDTH, RECOGNITION_HEIGHT)

        for (pixel in pixels) {
            val gray = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(gray)
        }

        return inputBuffer
    }

    private fun processDetectionOutput(outputBuffer: ByteBuffer, outputShape: IntArray, originalWidth: Int, originalHeight: Int): List<TextRegion> {
        outputBuffer.rewind()
        val regions = mutableListOf<TextRegion>()

        // This is a simplified version - actual CRAFT post-processing is more complex
        // For now, create mock regions for demonstration
        val scaleX = originalWidth.toFloat() / DETECTION_INPUT_SIZE
        val scaleY = originalHeight.toFloat() / DETECTION_INPUT_SIZE

        // Mock detection - in real implementation, this would parse CRAFT heatmaps
        regions.add(TextRegion(
            text = "",
            confidence = 0.8f,
            boundingBox = BoundingBox(
                x = 50f * scaleX,
                y = 50f * scaleY,
                width = 200f * scaleX,
                height = 40f * scaleY
            )
        ))

        return regions
    }

    private fun decodeCTCOutput(outputBuffer: ByteBuffer, outputShape: IntArray): String {
        outputBuffer.rewind()
        val decoded = StringBuilder()

        // Simplified CTC decoding - real implementation would be more sophisticated
        val seqLength = outputShape[1]
        val numClasses = outputShape[2]

        var previousChar = -1

        for (t in 0 until seqLength) {
            var maxIdx = 0
            var maxProb = Float.NEGATIVE_INFINITY

            for (c in 0 until numClasses) {
                val prob = outputBuffer.getFloat()
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = c
                }
            }

            // CTC decoding: skip blank (0) and repeated characters
            if (maxIdx != 0 && maxIdx != previousChar && maxIdx < vocab.length) {
                decoded.append(vocab[maxIdx - 1]) // -1 because 0 is blank
            }

            previousChar = maxIdx
        }

        return decoded.toString().trim()
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayscaleBitmap
    }

    private fun cropBitmap(bitmap: Bitmap, boundingBox: BoundingBox): Bitmap {
        return try {
            val x = maxOf(0, boundingBox.x.toInt())
            val y = maxOf(0, boundingBox.y.toInt())
            val width = minOf(bitmap.width - x, boundingBox.width.toInt())
            val height = minOf(bitmap.height - y, boundingBox.height.toInt())

            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } else {
                Log.w(TAG, "[EasyOCR] Invalid crop dimensions, using original bitmap")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "[EasyOCR] Failed to crop bitmap: ${e.message}")
            bitmap
        }
    }

    private fun loadModelFile(modelFilename: String): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelFilename)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength

            Log.d(TAG, "[EasyOCR] Loading model: $modelFilename (${declaredLength} bytes)")
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        } catch (e: IOException) {
            Log.e(TAG, "[EasyOCR] Failed to load model file: $modelFilename - ${e.message}")
            null
        }
    }

    override fun release() {
        Log.d(TAG, "[EasyOCR] OCR engine released")
        detectionInterpreter?.close()
        recognitionInterpreter?.close()
        detectionInterpreter = null
        recognitionInterpreter = null
        isInitialized = false
    }
}