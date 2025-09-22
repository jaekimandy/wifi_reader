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
import kotlin.math.*

class CraftKerasOCREngine(private val context: Context) : OCREngine {

    companion object {
        private const val TAG = "CraftKerasOCREngine"
        private const val MIN_CONFIDENCE = 0.5f
        private const val TEXT_THRESHOLD = 0.7f
        private const val LINK_THRESHOLD = 0.4f

        // Model files
        private const val CRAFT_MODEL_FILE = "craft_text_detection.tflite"
        private const val KERAS_MODEL_FILE = "keras_text_recognition.tflite"

        // CRAFT model parameters (typical values for CRAFT)
        private const val CRAFT_INPUT_SIZE = 640  // CRAFT typically uses 640x640
        private const val CRAFT_OUTPUT_CHANNELS = 2  // text region and link region

        // KerasOCR model parameters
        private const val KERAS_INPUT_HEIGHT = 64
        private const val KERAS_INPUT_WIDTH = 256
        private const val KERAS_INPUT_CHANNELS = 1
    }

    private var craftInterpreter: Interpreter? = null
    private var kerasInterpreter: Interpreter? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[CraftKeras] Initializing CRAFT + KerasOCR Engine...")

        try {
            // Load CRAFT detection model
            Log.d(TAG, "[CraftKeras] Loading CRAFT model: $CRAFT_MODEL_FILE")
            val craftBuffer = loadModelFile(CRAFT_MODEL_FILE)

            // Load KerasOCR recognition model
            Log.d(TAG, "[CraftKeras] Loading KerasOCR model: $KERAS_MODEL_FILE")
            val kerasBuffer = loadModelFile(KERAS_MODEL_FILE)

            if (craftBuffer != null && kerasBuffer != null) {
                try {
                    craftInterpreter = Interpreter(craftBuffer)
                    kerasInterpreter = Interpreter(kerasBuffer)
                    isInitialized = true

                    // Log model info
                    val craftInputTensor = craftInterpreter!!.getInputTensor(0)
                    val craftOutputTensor = craftInterpreter!!.getOutputTensor(0)
                    val kerasInputTensor = kerasInterpreter!!.getInputTensor(0)
                    val kerasOutputTensor = kerasInterpreter!!.getOutputTensor(0)

                    Log.i(TAG, "[CraftKeras] ✅ CRAFT + KerasOCR initialized successfully")
                    Log.d(TAG, "[CraftKeras] CRAFT input: ${craftInputTensor.shape().contentToString()}")
                    Log.d(TAG, "[CraftKeras] CRAFT output: ${craftOutputTensor.shape().contentToString()}")
                    Log.d(TAG, "[CraftKeras] KerasOCR input: ${kerasInputTensor.shape().contentToString()}")
                    Log.d(TAG, "[CraftKeras] KerasOCR output: ${kerasOutputTensor.shape().contentToString()}")

                    true
                } catch (e: Exception) {
                    Log.e(TAG, "[CraftKeras] ❌ Model loading failed: ${e.message}", e)
                    false
                }
            } else {
                Log.w(TAG, "[CraftKeras] ⚠️ One or both model files not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CraftKeras] ❌ Failed to initialize: ${e.message}", e)
            false
        }
    }

    override suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox?): List<TextRegion> = withContext(Dispatchers.Default) {
        Log.i(TAG, "[CraftKeras] 📄 Starting text extraction...")
        Log.d(TAG, "[CraftKeras] Input bitmap: ${bitmap.width}x${bitmap.height}")

        if (!isInitialized || craftInterpreter == null || kerasInterpreter == null) {
            Log.w(TAG, "[CraftKeras] ❌ Engine not initialized")
            return@withContext emptyList()
        }

        try {
            // Step 1: Use CRAFT to detect text regions
            val detectedRegions = detectTextRegions(bitmap, boundingBox)
            Log.d(TAG, "[CraftKeras] CRAFT detected ${detectedRegions.size} text regions")

            if (detectedRegions.isEmpty()) {
                Log.w(TAG, "[CraftKeras] No text regions detected by CRAFT")
                return@withContext emptyList()
            }

            // Step 2: Use KerasOCR to recognize text in each detected region
            val textRegions = mutableListOf<TextRegion>()

            for ((index, region) in detectedRegions.withIndex()) {
                Log.d(TAG, "[CraftKeras] Processing region $index: ${region}")

                try {
                    // Crop the detected region from the original bitmap
                    val croppedBitmap = cropBitmap(bitmap, region)

                    // Recognize text using KerasOCR
                    val recognizedText = recognizeText(croppedBitmap)

                    if (recognizedText.isNotEmpty()) {
                        textRegions.add(
                            TextRegion(
                                text = recognizedText,
                                confidence = MIN_CONFIDENCE, // Use minimum confidence for now
                                boundingBox = region
                            )
                        )
                        Log.d(TAG, "[CraftKeras] Region $index text: '$recognizedText'")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[CraftKeras] Failed to process region $index: ${e.message}")
                }
            }

            Log.i(TAG, "[CraftKeras] ✅ Extracted ${textRegions.size} text regions")
            textRegions

        } catch (e: Exception) {
            Log.e(TAG, "[CraftKeras] ❌ Text extraction failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun detectTextRegions(bitmap: Bitmap, boundingBox: BoundingBox?): List<BoundingBox> {
        // Crop bitmap if bounding box is provided
        val targetBitmap = if (boundingBox != null) {
            cropBitmap(bitmap, boundingBox)
        } else {
            bitmap
        }

        // Get actual input tensor shape from CRAFT model
        val inputTensor = craftInterpreter!!.getInputTensor(0)
        val inputShape = inputTensor.shape()
        val actualHeight = inputShape[1]
        val actualWidth = inputShape[2]
        val actualChannels = inputShape[3]

        Log.d(TAG, "[CraftKeras] CRAFT expects: ${actualHeight}x${actualWidth}x${actualChannels}")

        // Preprocess image for CRAFT
        val inputBuffer = preprocessImageForCraft(targetBitmap, actualHeight, actualWidth, actualChannels)

        // Get output tensor info
        val outputTensor = craftInterpreter!!.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputSize = outputShape.fold(1) { acc, dim -> acc * dim }

        // Create output buffer
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run CRAFT inference
        Log.d(TAG, "[CraftKeras] 🔍 Running CRAFT text detection...")
        craftInterpreter!!.run(inputBuffer, outputBuffer)

        // Process CRAFT output to extract bounding boxes
        return processCraftOutput(outputBuffer, outputShape, actualHeight, actualWidth, targetBitmap.width, targetBitmap.height)
    }

    private fun recognizeText(bitmap: Bitmap): String {
        // Preprocess image for KerasOCR
        val inputBuffer = preprocessImageForKeras(bitmap)

        // Get output tensor info
        val outputTensor = kerasInterpreter!!.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputSize = outputShape.fold(1) { acc, dim -> acc * dim }

        // Create output buffer
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run KerasOCR inference
        kerasInterpreter!!.run(inputBuffer, outputBuffer)

        // Process output to get text
        return processKerasOutput(outputBuffer, outputShape)
    }

    private fun preprocessImageForCraft(bitmap: Bitmap, height: Int, width: Int, channels: Int): ByteBuffer {
        // Resize to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        // Create input buffer
        val inputSize = height * width * channels * 4 // 4 bytes per float
        val inputBuffer = ByteBuffer.allocateDirect(inputSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Fill buffer with normalized pixel values
        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            // RGB: normalize to [0, 1]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            if (channels == 3) {
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            } else {
                // Convert to grayscale if needed
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                inputBuffer.putFloat(gray)
            }
        }

        return inputBuffer
    }

    private fun preprocessImageForKeras(bitmap: Bitmap): ByteBuffer {
        // Convert to grayscale
        val grayscaleBitmap = convertToGrayscale(bitmap)

        // Resize to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, KERAS_INPUT_WIDTH, KERAS_INPUT_HEIGHT, true)

        // Create input buffer
        val inputSize = KERAS_INPUT_HEIGHT * KERAS_INPUT_WIDTH * KERAS_INPUT_CHANNELS * 4
        val inputBuffer = ByteBuffer.allocateDirect(inputSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Fill buffer with normalized pixel values
        val pixels = IntArray(KERAS_INPUT_WIDTH * KERAS_INPUT_HEIGHT)
        resizedBitmap.getPixels(pixels, 0, KERAS_INPUT_WIDTH, 0, 0, KERAS_INPUT_WIDTH, KERAS_INPUT_HEIGHT)

        for (pixel in pixels) {
            // Grayscale: normalize to [0, 1]
            val gray = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(gray)
        }

        return inputBuffer
    }

    private fun processCraftOutput(outputBuffer: ByteBuffer, outputShape: IntArray, modelHeight: Int, modelWidth: Int, originalWidth: Int, originalHeight: Int): List<BoundingBox> {
        outputBuffer.rewind()

        try {
            // CRAFT output typically has shape [1, height, width, 2]
            // Channel 0: text region map, Channel 1: link region map
            val batchSize = outputShape[0]
            val outputHeight = outputShape[1]
            val outputWidth = outputShape[2]
            val outputChannels = outputShape[3]

            Log.d(TAG, "[CraftKeras] CRAFT output shape: [${batchSize}, ${outputHeight}, ${outputWidth}, ${outputChannels}]")

            val regions = mutableListOf<BoundingBox>()

            // Simple thresholding approach to find text regions
            // In a real implementation, you'd use more sophisticated post-processing
            val textScores = FloatArray(outputHeight * outputWidth)

            // Read text scores (channel 0)
            for (y in 0 until outputHeight) {
                for (x in 0 until outputWidth) {
                    val textScore = outputBuffer.getFloat()
                    val linkScore = outputBuffer.getFloat() // Skip link score for now
                    textScores[y * outputWidth + x] = textScore
                }
            }

            // Find regions with high text scores using simple connected components
            val visited = BooleanArray(outputHeight * outputWidth)

            for (y in 0 until outputHeight) {
                for (x in 0 until outputWidth) {
                    val idx = y * outputWidth + x
                    if (!visited[idx] && textScores[idx] > TEXT_THRESHOLD) {
                        val region = findConnectedRegion(textScores, visited, x, y, outputWidth, outputHeight)
                        if (region != null) {
                            // Scale region back to original image coordinates
                            val scaledRegion = BoundingBox(
                                x = region.x * originalWidth / outputWidth,
                                y = region.y * originalHeight / outputHeight,
                                width = region.width * originalWidth / outputWidth,
                                height = region.height * originalHeight / outputHeight
                            )
                            regions.add(scaledRegion)
                        }
                    }
                }
            }

            Log.d(TAG, "[CraftKeras] Found ${regions.size} text regions")
            return regions

        } catch (e: Exception) {
            Log.e(TAG, "[CraftKeras] CRAFT output processing failed: ${e.message}", e)
            return emptyList()
        }
    }

    private fun findConnectedRegion(scores: FloatArray, visited: BooleanArray, startX: Int, startY: Int, width: Int, height: Int): BoundingBox? {
        val stack = mutableListOf<Pair<Int, Int>>()
        stack.add(Pair(startX, startY))

        var minX = startX
        var maxX = startX
        var minY = startY
        var maxY = startY
        var pixelCount = 0

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeAt(stack.size - 1)
            val idx = y * width + x

            if (x < 0 || x >= width || y < 0 || y >= height || visited[idx] || scores[idx] < TEXT_THRESHOLD) {
                continue
            }

            visited[idx] = true
            pixelCount++

            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)

            // Add neighbors
            stack.add(Pair(x + 1, y))
            stack.add(Pair(x - 1, y))
            stack.add(Pair(x, y + 1))
            stack.add(Pair(x, y - 1))
        }

        // Filter out very small regions
        if (pixelCount < 10 || (maxX - minX) < 5 || (maxY - minY) < 5) {
            return null
        }

        return BoundingBox(
            x = minX.toFloat(),
            y = minY.toFloat(),
            width = (maxX - minX + 1).toFloat(),
            height = (maxY - minY + 1).toFloat()
        )
    }

    private fun processKerasOutput(outputBuffer: ByteBuffer, outputShape: IntArray): String {
        outputBuffer.rewind()

        try {
            // KerasOCR CTC output processing (same as original KerasOCR)
            if (outputShape.size != 3) {
                Log.e(TAG, "[CraftKeras] Unexpected KerasOCR output shape: ${outputShape.contentToString()}")
                return ""
            }

            val batchSize = outputShape[0]
            val sequenceLength = outputShape[1]
            val numClasses = outputShape[2]

            Log.d(TAG, "[CraftKeras] Processing KerasOCR CTC output: batch=$batchSize, seq=$sequenceLength, classes=$numClasses")

            val charset = "0123456789abcdefghijklmnopqrstuvwxyz "

            // Read probabilities for first batch
            val probabilities = Array(sequenceLength) { FloatArray(numClasses) }

            for (t in 0 until sequenceLength) {
                for (c in 0 until numClasses) {
                    probabilities[t][c] = outputBuffer.getFloat()
                }
            }

            // CTC Greedy decoding
            val decoded = StringBuilder()
            var previousChar = -1

            for (t in 0 until sequenceLength) {
                var maxProb = Float.NEGATIVE_INFINITY
                var bestChar = -1

                for (c in 0 until numClasses) {
                    if (probabilities[t][c] > maxProb) {
                        maxProb = probabilities[t][c]
                        bestChar = c
                    }
                }

                // CTC decoding rules: skip blank and repeated characters
                if (bestChar != numClasses - 1 && bestChar != previousChar && bestChar < charset.length) {
                    val char = charset[bestChar].uppercaseChar()
                    decoded.append(char)
                }

                previousChar = bestChar
            }

            val result = decoded.toString().trim()
            Log.d(TAG, "[CraftKeras] KerasOCR decoded result: '$result'")
            return result

        } catch (e: Exception) {
            Log.e(TAG, "[CraftKeras] KerasOCR decoding failed: ${e.message}", e)
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
                Log.w(TAG, "[CraftKeras] Invalid crop dimensions, using original bitmap")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CraftKeras] Failed to crop bitmap: ${e.message}")
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

            Log.d(TAG, "[CraftKeras] Loading model: $modelFilename (${declaredLength} bytes)")
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        } catch (e: IOException) {
            Log.e(TAG, "[CraftKeras] Failed to load model file: $modelFilename - ${e.message}")
            null
        }
    }

    override fun release() {
        Log.d(TAG, "[CraftKeras] OCR engine released")
        craftInterpreter?.close()
        kerasInterpreter?.close()
        craftInterpreter = null
        kerasInterpreter = null
        isInitialized = false
    }
}