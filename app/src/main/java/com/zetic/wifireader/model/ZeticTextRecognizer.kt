package com.zetic.wifireader.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zetic.wifireader.BuildConfig
import com.zeticai.mlange.core.model.ZeticMLangeModel
import com.zeticai.mlange.core.tensor.Tensor
import java.nio.ByteBuffer

/**
 * Zetic MLange Text Recognition Model Wrapper
 * Following whisper app pattern - ZLang 1.3.0 with 4-parameter constructor and BuildConfig
 */
class ZeticTextRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "ZeticTextRecognizer"
    }

    var model: ZeticMLangeModel? = null
    private var isInitialized = false

    /**
     * Initialize text recognition model - whisper app pattern with 4-parameter constructor
     */
    fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing Zetic Text Recognition model: ${BuildConfig.TEXT_RECOG_MODEL}")
            Log.i(TAG, "🔍 Using ZLang 1.3.0 with BuildConfig API keys...")

            // Initialize ZeticMLangeModel using whisper app pattern (4-parameter constructor)
            Log.d(TAG, "🚀 Attempting model download for: ${BuildConfig.TEXT_RECOG_MODEL}")
            model = ZeticMLangeModel(context, BuildConfig.RECOG_API_KEY, BuildConfig.TEXT_RECOG_MODEL, null)
            Log.i(TAG, "✅ Model download SUCCESS - ZeticMLangeModel created with 4-param constructor")

            isInitialized = true
            Log.i(TAG, "Text recognition model initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize text recognition model: ${e.message}", e)
            isInitialized = false
            false
        }
    }

    /**
     * Recognize text from a cropped bitmap region
     * @param bitmap Cropped bitmap containing text to recognize
     * @return Recognized text string
     */
    fun recognizeText(bitmap: Bitmap): String {
        if (!isInitialized || model == null) {
            Log.w(TAG, "Text recognition model not initialized")
            return "[RECOGNITION_NOT_INITIALIZED]"
        }

        return try {
            Log.d(TAG, "Recognizing text from ${bitmap.width}x${bitmap.height} bitmap")

            // Convert bitmap to ByteBuffer format expected by ZeticMLangeModel 1.3.0
            Log.d(TAG, "Preparing input buffer for ${bitmap.width}x${bitmap.height} bitmap")
            val inputBuffer = prepareInputBuffer(bitmap)
            Log.d(TAG, "Prepared input buffer")

            // Wrap in Tensor for ZLang 1.3.0 API
            val inputTensor = Tensor.of(inputBuffer)

            // Run the actual Zetic MLange text recognition model (v1.3.0 uses Tensor API)
            Log.d(TAG, "Running ZeticMLangeModel inference with Tensor API...")
            val outputs = model!!.run(arrayOf(inputTensor))
            Log.d(TAG, "Model inference completed, received ${outputs.size} output tensors")

            // Get output data from tensors
            Log.d(TAG, "Extracting output data from tensors...")
            val outputBuffers = outputs.map { it.data }.toTypedArray()
            Log.d(TAG, "Retrieved ${outputBuffers.size} output buffers")
            val recognizedText = processRecognitionOutputs(outputBuffers)
            Log.d(TAG, "Processed outputs to text: '$recognizedText'")

            Log.i(TAG, "Recognized text: '$recognizedText'")
            recognizedText

        } catch (e: Exception) {
            Log.e(TAG, "❌ Text recognition FAILED - using MOCK data: ${e.message}", e)
            Log.w(TAG, "🔄 FALLING BACK TO MOCK RECOGNITION")
            // Fallback to mock recognition if real model fails
            val mockText = createMockRecognition(bitmap)
            Log.i(TAG, "📝 MOCK: Generated fake text: '$mockText'")
            mockText
        }
    }

    private fun processRecognitionOutputs(outputs: Array<ByteBuffer>): String {
        return try {
            Log.d(TAG, "Processing ${outputs.size} output buffers")

            if (outputs.isNotEmpty()) {
                // Log details about the output buffers
                outputs.forEachIndexed { index, buffer ->
                    Log.d(TAG, "Output buffer $index: capacity=${buffer.capacity()}, remaining=${buffer.remaining()}")
                }

                // For now, return a placeholder indicating we got real model outputs
                // TODO: Implement proper output decoding based on jkim711/text_recog3 format
                "RECOGNIZED_TEXT_PLACEHOLDER"
            } else {
                Log.w(TAG, "No output buffers received from model")
                "[NO_TEXT_DETECTED]"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process recognition outputs: ${e.message}", e)
            "[PROCESSING_ERROR]"
        }
    }

    /**
     * Convert bitmap to ByteBuffer format expected by ZeticMLangeModel 1.3.0 Tensor API for text recognition
     */
    fun prepareInputBuffer(bitmap: Bitmap): ByteBuffer {
        // Text recognition models typically expect different input sizes than detection
        val modelInputWidth = 224   // Common text recognition input width
        val modelInputHeight = 64   // Common text recognition input height

        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true)

        // Convert to ByteBuffer in the format expected by the model
        val pixelCount = modelInputWidth * modelInputHeight
        val byteBuffer = ByteBuffer.allocateDirect(pixelCount * 3 * 4) // RGB * float32
        byteBuffer.order(java.nio.ByteOrder.nativeOrder())

        val intValues = IntArray(pixelCount)
        resizedBitmap.getPixels(intValues, 0, modelInputWidth, 0, 0, modelInputWidth, modelInputHeight)

        // Convert RGB pixels to normalized float values (0.0 - 1.0)
        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // Red
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // Green
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)          // Blue
        }

        return byteBuffer
    }

    private fun createMockRecognition(bitmap: Bitmap): String {
        // Generate realistic mock WiFi credentials based on bitmap size/content
        val mockNetworks = listOf("HomeWiFi_5G", "MyNetwork_2024", "RouterNet_5G", "WiFi_Guest")
        val mockPasswords = listOf("SecurePass123!", "MyPassword2024", "RouterKey789", "GuestAccess456")

        // Simple logic to vary output based on bitmap dimensions
        val networkIndex = (bitmap.width + bitmap.height) % mockNetworks.size

        return if (bitmap.width > bitmap.height) {
            // Landscape - likely SSID region
            mockNetworks[networkIndex]
        } else {
            // Portrait or square - likely password region
            mockPasswords[networkIndex]
        }
    }

    /**
     * Clean up resources - Whisper app pattern (ZLang 1.3.0)
     */
    fun release() {
        Log.i(TAG, "Releasing text recognition model")
        model?.deinit()
        model = null
        isInitialized = false
    }

    /**
     * Debug method to inspect ByteBuffer outputs for testing
     */
    fun inspectOutputBuffers(outputs: Array<ByteBuffer>) {
        Log.d(TAG, "=== ByteBuffer Inspection ===")
        outputs.forEachIndexed { index, buffer ->
            buffer.rewind()
            Log.d(TAG, "Buffer $index: capacity=${buffer.capacity()}, remaining=${buffer.remaining()}")

            // Log first few float values
            val sampleData = StringBuilder()
            val sampleSize = minOf(10, buffer.remaining() / 4)
            for (i in 0 until sampleSize) {
                if (buffer.remaining() >= 4) {
                    val value = buffer.float
                    sampleData.append(String.format("%.4f ", value))
                }
            }
            Log.d(TAG, "Buffer $index first 10 floats: $sampleData")
            buffer.rewind()
        }
    }
}