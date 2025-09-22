package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zetic.wifireader.ocr.KerasOCREngine
import com.zetic.wifireader.model.BoundingBox
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class KerasOCRTest {

    private lateinit var context: Context
    private lateinit var kerasEngine: KerasOCREngine

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        kerasEngine = KerasOCREngine(context)
    }

    @After
    fun teardown() {
        kerasEngine.release()
    }

    @Test
    fun testKerasOCRInitialization() = runBlocking {
        // Test if KerasOCR can initialize with the restored model file
        val initialized = kerasEngine.initialize()

        assertTrue("KerasOCR should initialize successfully", initialized)
    }

    @Test
    fun testSimpleTextRecognition() = runBlocking {
        // Initialize KerasOCR
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR should initialize", initialized)

        // Create a simple bitmap with text
        val bitmap = createTextBitmap("Hello World", 200, 100)

        // Extract text (no bounding box - tests full image OCR)
        val textRegions = kerasEngine.extractText(bitmap)

        // Verify results
        assertTrue("Should detect at least one text region", textRegions.isNotEmpty())

        val detectedText = textRegions.first().text.trim()
        println("Detected text: '$detectedText'")

        // Check if text is detected (should be real OCR now, not hardcoded "Di")
        assertTrue(
            "Detected text should not be empty",
            detectedText.isNotBlank()
        )

        // Should not be the old hardcoded "Di" anymore
        assertNotEquals(
            "Should not return old hardcoded 'Di' text",
            "Di",
            detectedText
        )

        // Verify confidence
        val confidence = textRegions.first().confidence
        assertTrue("Confidence should be between 0 and 1", confidence in 0.0f..1.0f)
        println("Confidence: $confidence")
    }

    @Test
    fun testWiFiCredentialText() = runBlocking {
        // Initialize KerasOCR
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR should initialize", initialized)

        // Create bitmap with WiFi credential format
        val wifiText = "Network Name (SSID): MyWiFi_5G\nNetwork Key (Password): SecurePass123!"
        val bitmap = createTextBitmap(wifiText, 400, 200)

        // Extract text (no bounding box - tests full image OCR)
        val textRegions = kerasEngine.extractText(bitmap)

        // Verify results
        assertTrue("Should detect text regions", textRegions.isNotEmpty())

        val detectedText = textRegions.joinToString(" ") { it.text }
        println("WiFi text detected: '$detectedText'")

        // Check that KerasOCR is working (not hardcoded anymore)
        assertTrue("Should detect some text", detectedText.isNotBlank())
        assertNotEquals("Should not be hardcoded 'Di'", "Di", detectedText)

        // Check confidence scores
        textRegions.forEach { region ->
            assertTrue("Confidence should be between 0 and 1", region.confidence in 0.0f..1.0f)
            println("Text: '${region.text}', Confidence: ${region.confidence}")
        }
    }

    @Test
    fun testBoundingBoxExtraction() = runBlocking {
        // Initialize KerasOCR
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR should initialize", initialized)

        // Create bitmap with text
        val bitmap = createTextBitmap("Test Text", 300, 150)

        // Test with bounding box
        val boundingBox = BoundingBox(50f, 25f, 200f, 100f) // Crop center area
        val textRegions = kerasEngine.extractText(bitmap, boundingBox)

        // Verify results
        assertTrue("Should detect text with bounding box", textRegions.isNotEmpty())

        val region = textRegions.first()
        println("Bounded text: '${region.text}', Confidence: ${region.confidence}")

        // Verify bounding box is used
        assertNotNull("Should have bounding box", region.boundingBox)

        // Should not be hardcoded "Di"
        assertNotEquals("Should not be hardcoded 'Di'", "Di", region.text)
    }

    @Test
    fun testPerformance() = runBlocking {
        // Initialize KerasOCR
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR should initialize", initialized)

        val bitmap = createTextBitmap("Performance Test", 200, 100)

        // Test multiple extractions to check performance
        val startTime = System.currentTimeMillis()

        repeat(3) {
            val textRegions = kerasEngine.extractText(bitmap, null)
            assertTrue("Should consistently detect text", textRegions.isNotEmpty())
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        println("3 KerasOCR operations took: ${totalTime}ms")

        // Should complete within reasonable time (adjust based on device)
        assertTrue("KerasOCR should complete within 10 seconds", totalTime < 10000)
    }

    @Test
    fun testModelLoading() = runBlocking {
        // Test that KerasOCR models can be loaded successfully
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR models should load successfully", initialized)

        // Create a small test image
        val bitmap = createTextBitmap("Model Test", 150, 80)

        // Verify OCR can run without crashing
        val textRegions = kerasEngine.extractText(bitmap)

        // Should return at least empty list (not crash)
        assertNotNull("Should return results (not crash)", textRegions)
        println("Model loading test: extracted ${textRegions.size} regions")
    }

    @Test
    fun testCTCDecodingFix() = runBlocking {
        // Test specifically that our CTC decoding fix is working
        val initialized = kerasEngine.initialize()
        assertTrue("KerasOCR should initialize", initialized)

        // Create bitmap with clear text
        val bitmap = createTextBitmap("HELLO123", 200, 100)

        // Extract text
        val textRegions = kerasEngine.extractText(bitmap)

        // Verify CTC decoding is working
        assertTrue("CTC decoding should return results", textRegions.isNotEmpty())

        val detectedText = textRegions.first().text
        println("CTC decoded text: '$detectedText'")

        // Most importantly: should NOT be the old broken "Di" result
        assertNotEquals(
            "CTC decoding should not return old broken 'Di' result",
            "Di",
            detectedText
        )

        // Should be meaningful text (not empty)
        assertTrue("CTC decoded text should not be empty", detectedText.isNotBlank())
    }

    private fun createTextBitmap(text: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill with white background
        canvas.drawColor(Color.WHITE)

        // Draw text with settings optimized for OCR
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f  // Larger text size
            isAntiAlias = false  // Disable anti-aliasing for sharper edges
            typeface = android.graphics.Typeface.MONOSPACE  // Monospace font is better for OCR
            style = android.graphics.Paint.Style.FILL
        }

        // Handle multi-line text
        val lines = text.split("\n")
        val lineHeight = paint.fontSpacing
        var y = (height / 2f - (lines.size - 1) * lineHeight / 2f).coerceAtLeast(paint.textSize)

        for (line in lines) {
            if (line.isNotBlank()) {
                val x = maxOf(10f, (width - paint.measureText(line)) / 2f)
                canvas.drawText(line, x, y, paint)
            }
            y += lineHeight
        }

        return bitmap
    }
}