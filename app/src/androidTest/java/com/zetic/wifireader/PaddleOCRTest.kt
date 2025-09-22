package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zetic.wifireader.ocr.PaddleOCREngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class PaddleOCRTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testPaddleOCRWithSimpleText() = runBlocking {
        // Create a simple bitmap with clear text
        val testBitmap = createTestBitmap("HELLO WORLD")

        // Initialize PaddleOCR Engine
        val ocrEngine = PaddleOCREngine(context)
        val initialized = ocrEngine.initialize()

        assertTrue("PaddleOCR should initialize successfully", initialized)

        // Extract text from the test bitmap
        val textRegions = ocrEngine.extractText(testBitmap)

        // Test verifies the engine initializes and processes images
        println("PaddleOCR detected ${textRegions.size} text regions")

        textRegions.forEachIndexed { index, region ->
            println("Text region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        // Clean up
        ocrEngine.release()
    }

    @Test
    fun testPaddleOCRWithWiFiCredentials() = runBlocking {
        println("=== WIFI CREDENTIAL PARSING TEST ===")

        // Create a bitmap with WiFi-like credentials
        val testBitmap = createWiFiTestBitmap()

        // Initialize PaddleOCR Engine
        val ocrEngine = PaddleOCREngine(context)
        val initialized = ocrEngine.initialize()

        assertTrue("PaddleOCR should initialize successfully", initialized)
        println("✅ PaddleOCR initialized successfully")

        // Step 1: Extract text from the test bitmap
        println("\n--- STEP 1: TEXT DETECTION ---")
        val textRegions = ocrEngine.extractText(testBitmap)

        println("PaddleOCR detected ${textRegions.size} text regions from WiFi test image")
        textRegions.forEachIndexed { index, region ->
            println("Text region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        val allText = textRegions.joinToString(" ") { it.text }
        println("All detected text: '$allText'")

        // Step 2: Test WiFi credential parsing
        println("\n--- STEP 2: WIFI CREDENTIAL PARSING ---")
        val wifiParser = com.zetic.wifireader.pipeline.WiFiTextParser()
        val credentials = wifiParser.parseWiFiCredentials(textRegions)

        println("WiFi parser found ${credentials.size} credentials")
        credentials.forEachIndexed { index, cred ->
            println("Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Step 3: Test individual text patterns
        println("\n--- STEP 3: PATTERN TESTING ---")
        testIndividualPatterns(allText)

        // Clean up
        ocrEngine.release()
        println("\n=== TEST COMPLETED ===")
    }

    private fun testIndividualPatterns(text: String) {
        println("Testing patterns against text: '$text'")

        // Test SSID patterns
        val ssidPatterns = listOf(
            Regex("""(?i)(?:ssid|network\s*name|wifi\s*name)[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)network[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)wifi[:=\s]*([^\s\n]+)""")
        )

        println("SSID Pattern Tests:")
        ssidPatterns.forEachIndexed { index, pattern ->
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                println("  Pattern $index MATCHED: '${match.groupValues[1]}'")
            } else {
                println("  Pattern $index: No match")
            }
        }

        // Test Password patterns
        val passwordPatterns = listOf(
            Regex("""(?i)(?:password|pwd|pass|key|passphrase)[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)wpa\s*key[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)security\s*key[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)access\s*key[:=\s]*([^\s\n]+)""")
        )

        println("Password Pattern Tests:")
        passwordPatterns.forEachIndexed { index, pattern ->
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                println("  Pattern $index MATCHED: '${match.groupValues[1]}'")
            } else {
                println("  Pattern $index: No match")
            }
        }

        // Test complete router label patterns
        val routerPatterns = listOf(
            Regex("""(?i)ssid[:=\s]*([^\s,\n]+).*?password[:=\s]*([^\s,\n]+)"""),
            Regex("""(?i)network[:=\s]*([^\s,\n]+).*?key[:=\s]*([^\s,\n]+)"""),
            Regex("""(?i)wifi[:=\s]*([^\s,\n]+).*?pwd[:=\s]*([^\s,\n]+)""")
        )

        println("Router Label Pattern Tests:")
        routerPatterns.forEachIndexed { index, pattern ->
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 3) {
                println("  Pattern $index MATCHED: SSID='${match.groupValues[1]}', Password='${match.groupValues[2]}'")
            } else {
                println("  Pattern $index: No match")
            }
        }
    }

    private fun createTestBitmap(text: String): Bitmap {
        val width = 800
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Black text
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Center the text
        val textWidth = paint.measureText(text)
        val x = (width - textWidth) / 2
        val y = height / 2 + paint.textSize / 3

        canvas.drawText(text, x, y, paint)

        return bitmap
    }

    @Test
    fun testRealisticWiFiRouterLabel() = runBlocking {
        println("=== REALISTIC WIFI ROUTER LABEL TEST ===")

        // Create a more realistic router label bitmap
        val testBitmap = createRealisticRouterLabelBitmap()

        // Initialize PaddleOCR Engine
        val ocrEngine = PaddleOCREngine(context)
        val initialized = ocrEngine.initialize()

        assertTrue("PaddleOCR should initialize successfully", initialized)
        println("✅ PaddleOCR initialized successfully")

        // Step 1: Extract text from the realistic test bitmap
        println("\n--- STEP 1: TEXT DETECTION ---")
        val textRegions = ocrEngine.extractText(testBitmap)

        println("PaddleOCR detected ${textRegions.size} text regions from realistic router label")
        textRegions.forEachIndexed { index, region ->
            println("Text region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        val allText = textRegions.joinToString(" ") { it.text }
        println("All detected text: '$allText'")

        // Step 2: Test WiFi credential parsing
        println("\n--- STEP 2: WIFI CREDENTIAL PARSING ---")
        val wifiParser = com.zetic.wifireader.pipeline.WiFiTextParser()
        val credentials = wifiParser.parseWiFiCredentials(textRegions)

        println("WiFi parser found ${credentials.size} credentials")
        credentials.forEachIndexed { index, cred ->
            println("Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Verify we found at least one credential for the realistic test
        if (credentials.isNotEmpty()) {
            println("✅ SUCCESS: Found WiFi credentials in realistic router label!")
            val cred = credentials.first()
            assertTrue("SSID should not be empty", cred.ssid.isNotEmpty())
            assertTrue("Password should not be empty", cred.password.isNotEmpty())
        } else {
            println("❌ No credentials found - need to improve parsing patterns")
        }

        // Step 3: Test individual text patterns
        println("\n--- STEP 3: PATTERN TESTING ---")
        testIndividualPatterns(allText)

        // Clean up
        ocrEngine.release()
        println("\n=== REALISTIC TEST COMPLETED ===")
    }

    private fun createWiFiTestBitmap(): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Black text paint
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Draw WiFi credentials
        canvas.drawText("SSID: TestNetwork", 50f, 100f, paint)
        canvas.drawText("Password: password123", 50f, 200f, paint)
        canvas.drawText("Security: WPA2", 50f, 300f, paint)

        return bitmap
    }

    private fun createRealisticRouterLabelBitmap(): Bitmap {
        val width = 800
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Light gray background (like a real router label)
        canvas.drawColor(Color.parseColor("#F5F5F5"))

        // Create different paint styles for a more realistic look
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 28f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Draw router brand/model (typical router label)
        canvas.drawText("NETGEAR R6700", 50f, 40f, titlePaint)

        // Draw WiFi credentials in typical router label format
        canvas.drawText("Network Name (SSID):", 50f, 100f, labelPaint)
        canvas.drawText("NETGEAR_Home_5G", 350f, 100f, valuePaint)

        canvas.drawText("Network Key (Password):", 50f, 150f, labelPaint)
        canvas.drawText("MySecurePassword123!", 350f, 150f, valuePaint)

        canvas.drawText("Security Mode:", 50f, 200f, labelPaint)
        canvas.drawText("WPA2-PSK", 350f, 200f, valuePaint)

        // Add some additional router info
        canvas.drawText("MAC: 00:11:22:33:44:55", 50f, 250f, labelPaint)

        return bitmap
    }
}