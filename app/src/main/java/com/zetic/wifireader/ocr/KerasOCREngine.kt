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

class KerasOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "KerasOCREngine"
        private const val MIN_CONFIDENCE = 0.5f

        // Model file
        private const val MODEL_FILE = "keras_text_recognition.tflite"

        // Model parameters (these will need to be determined from the actual model)
        private const val INPUT_HEIGHT = 64  // Common for OCR models
        private const val INPUT_WIDTH = 256  // Common for OCR models
        private const val INPUT_CHANNELS = 1 // Grayscale
    }

    private var interpreter: Interpreter? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[KerasOCR] Initializing Keras OCR Engine...")

        try {
            // Load model
            Log.d(TAG, "[KerasOCR] Loading model: $MODEL_FILE")
            val modelBuffer = loadModelFile(MODEL_FILE)

            if (modelBuffer != null) {
                try {
                    interpreter = Interpreter(modelBuffer)
                    isInitialized = true

                    // Log model info
                    val inputTensor = interpreter!!.getInputTensor(0)
                    val outputTensor = interpreter!!.getOutputTensor(0)

                    Log.i(TAG, "[KerasOCR] ✅ Keras OCR initialized successfully")
                    Log.d(TAG, "[KerasOCR] Input tensor shape: ${inputTensor.shape().contentToString()}")
                    Log.d(TAG, "[KerasOCR] Output tensor shape: ${outputTensor.shape().contentToString()}")
                    Log.d(TAG, "[KerasOCR] Input data type: ${inputTensor.dataType()}")
                    Log.d(TAG, "[KerasOCR] Output data type: ${outputTensor.dataType()}")

                    true
                } catch (e: Exception) {
                    Log.e(TAG, "[KerasOCR] ❌ Model loading failed: ${e.message}", e)
                    false
                }
            } else {
                Log.w(TAG, "[KerasOCR] ⚠️ Model file not found: $MODEL_FILE")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[KerasOCR] ❌ Failed to initialize: ${e.message}", e)
            false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        Log.i(TAG, "[KerasOCR] 📄 Starting text extraction...")
        Log.d(TAG, "[KerasOCR] Input bitmap: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "[KerasOCR] Bounding box: $boundingBox")

        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "[KerasOCR] ❌ Keras OCR not initialized")
            return@withContext emptyList()
        }

        try {
            // Crop bitmap if bounding box is provided
            val targetBitmap = if (boundingBox != null) {
                Log.d(TAG, "[KerasOCR] 🔲 Cropping bitmap to bounding box...")
                cropBitmap(bitmap, boundingBox)
            } else {
                Log.d(TAG, "[KerasOCR] 📸 Using full bitmap")
                bitmap
            }

            Log.d(TAG, "[KerasOCR] Target bitmap: ${targetBitmap.width}x${targetBitmap.height}")

            // Get actual input tensor shape from the model
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val actualHeight = inputShape[1]
            val actualWidth = inputShape[2]
            val actualChannels = inputShape[3]

            Log.d(TAG, "[KerasOCR] Model expects: ${actualHeight}x${actualWidth}x${actualChannels}")

            // Preprocess image for Keras OCR
            val inputBuffer = preprocessImage(targetBitmap, actualHeight, actualWidth, actualChannels)

            // Get output tensor info
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputSize = outputShape.fold(1) { acc, dim -> acc * dim }

            Log.d(TAG, "[KerasOCR] Output shape: ${outputShape.contentToString()}, size: $outputSize")

            // Create output buffer
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            Log.d(TAG, "[KerasOCR] 🔍 Running Keras OCR inference...")
            interpreter!!.run(inputBuffer, outputBuffer)

            // Process output (this depends on the model's output format)
            val text = processOutput(outputBuffer, outputShape)

            Log.i(TAG, "[KerasOCR] 📝 Detected text: '$text'")

            if (text.isNotEmpty()) {
                // Create a text region covering the full image
                val textRegion = TextRegion(
                    text = text,
                    confidence = MIN_CONFIDENCE, // Keras OCR doesn't typically provide confidence scores
                    boundingBox = boundingBox ?: BoundingBox(0f, 0f, targetBitmap.width.toFloat(), targetBitmap.height.toFloat())
                )

                Log.i(TAG, "[KerasOCR] ✅ Text extraction completed: 1 region found")
                listOf(textRegion)
            } else {
                Log.w(TAG, "[KerasOCR] No text detected")
                emptyList()
            }

        } catch (e: Exception) {
            Log.e(TAG, "[KerasOCR] ❌ Text extraction failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun preprocessImage(bitmap: Bitmap, height: Int, width: Int, channels: Int): ByteBuffer {
        // Convert to grayscale if needed
        val grayscaleBitmap = if (channels == 1) {
            convertToGrayscale(bitmap)
        } else {
            bitmap
        }

        // Resize to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, width, height, true)

        // Create input buffer
        val inputSize = height * width * channels * 4 // 4 bytes per float
        val inputBuffer = ByteBuffer.allocateDirect(inputSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Fill buffer with normalized pixel values
        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            if (channels == 1) {
                // Grayscale: normalize to [0, 1]
                val gray = (pixel and 0xFF) / 255.0f
                inputBuffer.putFloat(gray)
            } else {
                // RGB: normalize to [0, 1]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
        }

        return inputBuffer
    }

    private fun processOutput(outputBuffer: ByteBuffer, outputShape: IntArray): String {
        outputBuffer.rewind()

        try {
            // Expected shape: [batch_size, sequence_length, num_classes]
            // For KerasOCR: [1, 48, 37] where 37 = 26 letters + 10 digits + 1 blank
            if (outputShape.size != 3) {
                Log.e(TAG, "[KerasOCR] Unexpected output shape: ${outputShape.contentToString()}")
                return ""
            }

            val batchSize = outputShape[0]
            val sequenceLength = outputShape[1]
            val numClasses = outputShape[2]

            Log.d(TAG, "[KerasOCR] Processing CTC output: batch=$batchSize, seq=$sequenceLength, classes=$numClasses")

            // Character set for KerasOCR model - determined from actual model output
            // Based on observed indices: A=10, E=14, P=25, etc.
            // This appears to be: 0-9 + a-z + BLANK
            val charset = "0123456789abcdefghijklmnopqrstuvwxyz "

            // Read probabilities for first batch (index 0)
            val probabilities = Array(sequenceLength) { FloatArray(numClasses) }

            for (t in 0 until sequenceLength) {
                for (c in 0 until numClasses) {
                    probabilities[t][c] = outputBuffer.getFloat()
                }
            }

            // CTC Greedy decoding: take most likely character at each timestep
            val decoded = StringBuilder()
            var previousChar = -1 // Track previous character to avoid repetitions

            for (t in 0 until sequenceLength) {
                // Find the character with highest probability at this timestep
                var maxProb = Float.NEGATIVE_INFINITY
                var bestChar = -1

                for (c in 0 until numClasses) {
                    if (probabilities[t][c] > maxProb) {
                        maxProb = probabilities[t][c]
                        bestChar = c
                    }
                }

                // CTC decoding rules:
                // 1. Skip blank character (usually last index)
                // 2. Skip repeated characters (unless separated by blank)
                if (bestChar != numClasses - 1 && bestChar != previousChar && bestChar < charset.length) {
                    val char = charset[bestChar].uppercaseChar()
                    decoded.append(char)
                    Log.v(TAG, "[KerasOCR] Timestep $t: char='$char' (index=$bestChar, prob=$maxProb)")
                }

                previousChar = bestChar
            }

            val result = decoded.toString().trim()
            Log.d(TAG, "[KerasOCR] CTC decoded result: '$result'")

            return result
        } catch (e: Exception) {
            Log.e(TAG, "[KerasOCR] CTC decoding failed: ${e.message}", e)
            return ""
        }
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
                Log.w(TAG, "[KerasOCR] Invalid crop dimensions, using original bitmap")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "[KerasOCR] Failed to crop bitmap: ${e.message}")
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

            Log.d(TAG, "[KerasOCR] Loading model: $modelFilename (${declaredLength} bytes)")
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        } catch (e: IOException) {
            Log.e(TAG, "[KerasOCR] Failed to load model file: $modelFilename - ${e.message}")
            null
        }
    }

    override fun release() {
        Log.d(TAG, "[KerasOCR] OCR engine released")
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}