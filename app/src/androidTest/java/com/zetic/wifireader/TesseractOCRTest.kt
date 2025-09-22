package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zetic.wifireader.ocr.TesseractOCREngine
import com.zetic.wifireader.model.BoundingBox
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TesseractOCRTest {

    private lateinit var context: Context
    private lateinit var tesseractEngine: TesseractOCREngine

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        tesseractEngine = TesseractOCREngine(context)
    }

    @After
    fun teardown() {
        tesseractEngine.release()
    }

    @Test
    fun testTesseractInitialization() = runBlocking {
        // Test if Tesseract can initialize with downloaded language data
        val initialized = tesseractEngine.initialize()

        assertTrue("Tesseract should initialize successfully", initialized)
    }

    @Test
    fun testSimpleTextRecognition() = runBlocking {
        // Initialize Tesseract
        val initialized = tesseractEngine.initialize()
        assertTrue("Tesseract should initialize", initialized)

        // Create a simple bitmap with text
        val bitmap = createTextBitmap("Hello World", 200, 100)

        // Extract text (no bounding box - tests full image OCR)
        val textRegions = tesseractEngine.extractText(bitmap)

        // Verify results
        assertTrue("Should detect at least one text region", textRegions.isNotEmpty())

        val detectedText = textRegions.first().text.trim()
        println("Detected text: '$detectedText'")

        // Check if text contains expected words (Tesseract may not be perfect)
        assertTrue(
            "Detected text should contain 'Hello' or 'World'",
            detectedText.contains("Hello", ignoreCase = true) ||
            detectedText.contains("World", ignoreCase = true) ||
            detectedText.contains("Hello World", ignoreCase = true)
        )
    }

    @Test
    fun testWiFiCredentialText() = runBlocking {
        // Initialize Tesseract
        val initialized = tesseractEngine.initialize()
        assertTrue("Tesseract should initialize", initialized)

        // Create bitmap with WiFi credential format
        val wifiText = "Network Name (SSID): MyWiFi_5G\nNetwork Key (Password): SecurePass123!"
        val bitmap = createTextBitmap(wifiText, 400, 200)

        // Extract text (no bounding box - tests full image OCR)
        val textRegions = tesseractEngine.extractText(bitmap)

        // Verify results
        assertTrue("Should detect text regions", textRegions.isNotEmpty())

        val detectedText = textRegions.joinToString(" ") { it.text }
        println("WiFi text detected: '$detectedText'")

        // Check for key components (flexible matching due to OCR limitations)
        val hasNetworkKeywords = detectedText.contains("Network", ignoreCase = true) ||
                                detectedText.contains("SSID", ignoreCase = true) ||
                                detectedText.contains("Password", ignoreCase = true)

        assertTrue("Should detect network-related keywords", hasNetworkKeywords)

        // Check confidence scores
        textRegions.forEach { region ->
            assertTrue("Confidence should be between 0 and 1", region.confidence in 0.0f..1.0f)
            println("Text: '${region.text}', Confidence: ${region.confidence}")
        }
    }

    @Test
    fun testMultilingualSupport() = runBlocking {
        // Initialize Tesseract
        val initialized = tesseractEngine.initialize()
        assertTrue("Tesseract should initialize", initialized)

        // Test different languages
        val testTexts = listOf(
            "English Text" to "eng",
            "Texto Español" to "spa",
            "Texte Français" to "fra",
            "Deutscher Text" to "deu"
        )

        for ((text, language) in testTexts) {
            val bitmap = createTextBitmap(text, 300, 100)
            val textRegions = tesseractEngine.extractText(bitmap, null)

            assertTrue("Should detect text for $language", textRegions.isNotEmpty())

            val detectedText = textRegions.first().text
            println("$language detected: '$detectedText'")

            // Check if at least some characters are detected
            assertTrue("Should detect some text for $language", detectedText.isNotBlank())
        }
    }

    @Test
    fun testBoundingBoxExtraction() = runBlocking {
        // Initialize Tesseract
        val initialized = tesseractEngine.initialize()
        assertTrue("Tesseract should initialize", initialized)

        // Create bitmap with text
        val bitmap = createTextBitmap("Test Text", 300, 150)

        // Test with bounding box
        val boundingBox = BoundingBox(50f, 25f, 200f, 100f) // Crop center area
        val textRegions = tesseractEngine.extractText(bitmap, boundingBox)

        // Verify results
        assertTrue("Should detect text with bounding box", textRegions.isNotEmpty())

        val region = textRegions.first()
        println("Bounded text: '${region.text}', Confidence: ${region.confidence}")

        // Verify bounding box is used
        assertNotNull("Should have bounding box", region.boundingBox)
    }

    @Test
    fun testPerformance() = runBlocking {
        // Initialize Tesseract
        val initialized = tesseractEngine.initialize()
        assertTrue("Tesseract should initialize", initialized)

        val bitmap = createTextBitmap("Performance Test", 200, 100)

        // Test multiple extractions to check performance
        val startTime = System.currentTimeMillis()

        repeat(3) {
            val textRegions = tesseractEngine.extractText(bitmap, null)
            assertTrue("Should consistently detect text", textRegions.isNotEmpty())
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        println("3 OCR operations took: ${totalTime}ms")

        // Should complete within reasonable time (adjust based on device)
        assertTrue("OCR should complete within 10 seconds", totalTime < 10000)
    }

    private fun createTextBitmap(text: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill with white background
        canvas.drawColor(Color.WHITE)

        // Draw text with settings optimized for Tesseract
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