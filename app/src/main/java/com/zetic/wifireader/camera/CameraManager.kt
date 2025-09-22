package com.zetic.wifireader.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastCapturedBitmap: Bitmap? = null

    companion object {
        private const val TAG = "CameraManager"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onImageCaptured: (Bitmap) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        try {
            val cameraProvider = getCameraProvider()
            this@CameraManager.cameraProvider = cameraProvider

            // Build preview use case
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Build image analysis use case optimized for quality and color
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080)) // Higher resolution for better clarity
                .setTargetRotation(previewView.display.rotation) // Match preview rotation
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // Force YUV format
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy, onImageCaptured)
                    }
                }

            // Build image capture use case for snapshots with optimized settings
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setTargetRotation(previewView.display.rotation) // Match preview rotation
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(95) // High quality JPEG
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer,
                    imageCapture
                )

                // Enable auto-focus for better text clarity
                setupAutoFocus()

                Log.d(TAG, "Camera started successfully with auto-focus enabled")
                true

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                false
            }

        } catch (exc: Exception) {
            Log.e(TAG, "Camera initialization failed", exc)
            false
        }
    }

    private fun setupAutoFocus() {
        val camera = this.camera ?: return

        try {
            val cameraControl = camera.cameraControl
            val cameraInfo = camera.cameraInfo

            // Check if auto-focus is available
            try {
                val focusState = cameraInfo.zoomState.value
                Log.d(TAG, "Camera focus capabilities available")
            } catch (e: Exception) {
                Log.w(TAG, "Focus state check failed, continuing with focus setup: ${e.message}")
            }

            // Enable continuous auto-focus with multiple focus points for better sharpness
            val meteringPointFactory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
            
            // Create multiple focus points for better overall sharpness
            val centerPoint = meteringPointFactory.createPoint(0.5f, 0.5f, 1.0f)
            val topLeftPoint = meteringPointFactory.createPoint(0.3f, 0.3f, 0.5f)
            val topRightPoint = meteringPointFactory.createPoint(0.7f, 0.3f, 0.5f)
            val bottomLeftPoint = meteringPointFactory.createPoint(0.3f, 0.7f, 0.5f)
            val bottomRightPoint = meteringPointFactory.createPoint(0.7f, 0.7f, 0.5f)

            val focusMeteringAction = FocusMeteringAction.Builder(centerPoint)
                .addPoint(topLeftPoint)
                .addPoint(topRightPoint) 
                .addPoint(bottomLeftPoint)
                .addPoint(bottomRightPoint)
                .disableAutoCancel() // Keep focus active
                .build()

            val result = cameraControl.startFocusAndMetering(focusMeteringAction)
            result.addListener({
                try {
                    if (result.get().isFocusSuccessful) {
                        Log.d(TAG, "Multi-point auto-focus successful")
                    } else {
                        Log.w(TAG, "Multi-point auto-focus failed, trying single point")
                        // Fallback to single point focus
                        val singlePointAction = FocusMeteringAction.Builder(centerPoint)
                            .disableAutoCancel()
                            .build()
                        cameraControl.startFocusAndMetering(singlePointAction)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Focus result check failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(context))

            Log.d(TAG, "Enhanced auto-focus enabled with multiple focus points")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup enhanced auto-focus: ${e.message}")
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).apply {
            addListener({
                continuation.resume(get())
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun processImage(imageProxy: ImageProxy, onImageCaptured: (Bitmap) -> Unit) {
        try {
            Log.d(TAG, "Processing image: ${imageProxy.width}x${imageProxy.height}, format=${imageProxy.format}")

            // Use reliable conversion method with fallback
            val rawBitmap = try {
                when (imageProxy.format) {
                    ImageFormat.YUV_420_888 -> {
                        convertYuv420ToNv21(imageProxy)
                    }
                    ImageFormat.JPEG -> {
                        val buffer = imageProxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: createEmptyBitmap()
                    }
                    else -> {
                        Log.w(TAG, "Unknown format ${imageProxy.format}, using YUV conversion")
                        convertYuv420ToNv21(imageProxy)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Primary conversion failed, trying fallback: ${e.message}")
                convertYuvFallback(imageProxy)
            }

            // Store raw bitmap for testing
            lastCapturedBitmap = rawBitmap

            // Enhance image for better OCR performance
            val enhancedBitmap = enhanceForOCR(rawBitmap)
            onImageCaptured(enhancedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }


    private fun convertYuv420ToNv21(imageProxy: ImageProxy): Bitmap {
        try {
            val image = imageProxy.image ?: return createEmptyBitmap()
            
            val planes = image.planes
            val yPlane = planes[0]
            val uPlane = planes[1] 
            val vPlane = planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val width = imageProxy.width
            val height = imageProxy.height

            Log.d(TAG, "YUV420 to NV21 - size: ${width}x${height}, Y:$ySize, U:$uSize, V:$vSize")

            // NV21 format: YYYYYYYY... VUVUVU...
            val nv21 = ByteArray(width * height + width * height / 2)

            // Copy Y plane directly
            yBuffer.rewind()
            if (yPlane.rowStride == width) {
                // No padding, copy directly
                yBuffer.get(nv21, 0, ySize)
            } else {
                // Handle row stride padding
                var pos = 0
                for (row in 0 until height) {
                    yBuffer.position(row * yPlane.rowStride)
                    yBuffer.get(nv21, pos, width)
                    pos += width
                }
            }

            // Handle UV planes - create proper NV21 interleaved format
            val uvPixelStride = uPlane.pixelStride
            val uvRowStride = uPlane.rowStride
            val uvHeight = height / 2

            var uvPos = width * height

            Log.d(TAG, "UV processing - pixelStride: $uvPixelStride, rowStride: $uvRowStride")

            // Reset buffer positions
            uBuffer.rewind()
            vBuffer.rewind()

            if (uvPixelStride == 1) {
                // Planar format - U and V are in separate continuous planes
                Log.d(TAG, "Processing planar UV format")
                
                if (uvRowStride == width / 2) {
                    // No padding, simple copy
                    for (i in 0 until (width * height / 4)) {
                        nv21[uvPos++] = vBuffer.get()
                        nv21[uvPos++] = uBuffer.get()
                    }
                } else {
                    // Handle row stride padding
                    for (row in 0 until uvHeight) {
                        vBuffer.position(row * uvRowStride)
                        uBuffer.position(row * uvRowStride)
                        
                        for (col in 0 until (width / 2)) {
                            nv21[uvPos++] = vBuffer.get()
                            nv21[uvPos++] = uBuffer.get()
                        }
                    }
                }
            } else {
                // Semi-planar format (UV interleaved) or other pixel stride
                Log.d(TAG, "Processing semi-planar UV format with stride $uvPixelStride")
                
                for (row in 0 until uvHeight) {
                    val rowStart = row * uvRowStride
                    
                    for (col in 0 until (width / 2)) {
                        val pixelStart = rowStart + col * uvPixelStride
                        
                        // Get U and V values with bounds checking
                        if (pixelStart < uBuffer.limit() && pixelStart < vBuffer.limit()) {
                            nv21[uvPos++] = vBuffer.get(pixelStart) // V first for NV21
                            nv21[uvPos++] = uBuffer.get(pixelStart) // U second
                        }
                    }
                }
            }

            // Convert directly to RGB bitmap for better color preservation
            val bitmap = convertNv21ToRgbBitmap(nv21, width, height)
            
            Log.d(TAG, "Successfully converted YUV420 to RGB bitmap: ${bitmap.width}x${bitmap.height}")
            return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

        } catch (e: Exception) {
            Log.e(TAG, "YUV420 to NV21 conversion failed: ${e.message}", e)
            return createEmptyBitmap()
        }
    }

    private fun convertNv21ToRgbBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
        try {
            val pixels = IntArray(width * height)
            val frameSize = width * height
            
            for (j in 0 until height) {
                for (i in 0 until width) {
                    val y = nv21[j * width + i].toInt() and 0xFF
                    
                    // Calculate UV indices for NV21 format (V, U interleaved)
                    val uvp = frameSize + (j shr 1) * width + (i and 1.inv())
                    if (uvp < nv21.size - 1) {
                        val v = (nv21[uvp].toInt() and 0xFF) - 128
                        val u = (nv21[uvp + 1].toInt() and 0xFF) - 128
                        
                        // YUV to RGB conversion with proper coefficients
                        val r = (y + 1.402 * v).toInt().coerceIn(0, 255)
                        val g = (y - 0.344 * u - 0.714 * v).toInt().coerceIn(0, 255)
                        val b = (y + 1.772 * u).toInt().coerceIn(0, 255)
                        
                        pixels[j * width + i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    } else {
                        // Fallback to grayscale if UV data is not available
                        pixels[j * width + i] = (0xFF shl 24) or (y shl 16) or (y shl 8) or y
                    }
                }
            }
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "NV21 to RGB conversion failed: ${e.message}")
            // Fallback to YuvImage method
            return try {
                val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)
                val jpegBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: createEmptyBitmap()
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback YuvImage conversion also failed: ${e2.message}")
                createEmptyBitmap()
            }
        }
    }

    private fun convertYuvFallback(imageProxy: ImageProxy): Bitmap {
        Log.d(TAG, "Using enhanced fallback YUV conversion")
        
        return try {
            // Try to use RenderScript or alternative for better quality conversion
            val image = imageProxy.image
            if (image != null) {
                // Attempt a simpler but more reliable conversion
                val planes = image.planes
                val yPlane = planes[0]
                val yBuffer = yPlane.buffer.duplicate()
                
                val width = imageProxy.width
                val height = imageProxy.height
                
                // Create enhanced grayscale with better contrast
                val yBytes = ByteArray(yBuffer.remaining())
                yBuffer.get(yBytes)
                
                val pixels = IntArray(width * height)
                for (i in yBytes.indices) {
                    var gray = yBytes[i].toInt() and 0xFF
                    
                    // Apply contrast enhancement even to grayscale
                    gray = when {
                        gray < 60 -> (gray * 0.5).toInt()
                        gray > 200 -> Math.min(255, (gray * 1.2).toInt())
                        else -> gray
                    }.coerceIn(0, 255)
                    
                    pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                }
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                
                return rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
            } else {
                return createEmptyBitmap()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced fallback conversion also failed: ${e.message}")
            createEmptyBitmap()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    private fun enhanceForOCR(bitmap: Bitmap): Bitmap {
        return try {
            // Create enhanced bitmap with same dimensions
            val enhanced = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

            // Process each pixel to enhance contrast while preserving color information
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Apply sharpening and contrast enhancement to each channel
                val enhancedR = enhanceChannel(r)
                val enhancedG = enhanceChannel(g)
                val enhancedB = enhanceChannel(b)

                // Create enhanced color pixel
                val enhancedPixel = (0xFF shl 24) or (enhancedR shl 16) or (enhancedG shl 8) or enhancedB
                pixels[i] = enhancedPixel
            }

            enhanced.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            Log.d(TAG, "Color image enhanced for OCR: ${bitmap.width}x${bitmap.height}")
            enhanced

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enhance image for OCR: ${e.message}")
            bitmap // Return original if enhancement fails
        }
    }

    private fun enhanceChannel(value: Int): Int {
        // Apply contrast enhancement with preserved color
        return when {
            value < 60 -> (value * 0.3).toInt()          // Darken dark areas
            value > 200 -> (255 - (255 - value) * 0.3).toInt()  // Brighten bright areas
            else -> {
                // Apply S-curve enhancement for mid-tones
                val normalized = value / 255.0
                val enhanced = Math.pow(normalized, 0.9) // Subtle gamma correction
                (enhanced * 255).toInt()
            }
        }.coerceIn(0, 255)
    }

    private fun createEmptyBitmap(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    fun toggleFlash(): Boolean {
        return try {
            val camera = this.camera ?: return false
            val currentState = camera.cameraInfo.torchState.value == TorchState.ON
            camera.cameraControl.enableTorch(!currentState)
            !currentState
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flash: ${e.message}")
            false
        }
    }

    fun focusOnPoint(x: Float, y: Float) {
        val camera = this.camera ?: return

        try {
            val meteringPointFactory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
            val focusPoint = meteringPointFactory.createPoint(x, y)

            val focusMeteringAction = FocusMeteringAction.Builder(focusPoint)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            camera.cameraControl.startFocusAndMetering(focusMeteringAction)
            Log.d(TAG, "Focus triggered at ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to focus at ($x, $y): ${e.message}")
        }
    }

    fun takeSnapshot(): Bitmap? {
        val snapshot = lastCapturedBitmap
        Log.d(TAG, "Snapshot taken: ${snapshot?.width}x${snapshot?.height}")
        return snapshot
    }

    fun getLastRawImage(): Bitmap? {
        return lastCapturedBitmap
    }

    fun getLastEnhancedImage(): Bitmap? {
        return lastCapturedBitmap?.let { enhanceForOCR(it) }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageAnalyzer = null
        imageCapture = null
        lastCapturedBitmap = null
    }

    fun release() {
        stopCamera()
        cameraExecutor.shutdown()
    }

    fun isFlashAvailable(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() == true
    }

    fun getCurrentZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
    }

    fun setZoomRatio(ratio: Float) {
        try {
            camera?.cameraControl?.setZoomRatio(ratio)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set zoom ratio: ${e.message}")
        }
    }
}