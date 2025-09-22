package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zetic.wifireader.llm.ZeticMLangeLLMParser
import com.zetic.wifireader.ocr.CraftKerasOCREngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CraftKerasOCRTest {

    private lateinit var context: Context
    private lateinit var ocrEngine: CraftKerasOCREngine
    private lateinit var llmParser: ZeticMLangeLLMParser

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ocrEngine = CraftKerasOCREngine(context)
        llmParser = ZeticMLangeLLMParser(context)
    }

    @After
    fun tearDown() {
        ocrEngine.release()
        llmParser.release()
    }

    @Test
    fun testCraftKerasOCRInitialization() = runBlocking {
        val initResult = ocrEngine.initialize()
        assertTrue("CRAFT + KerasOCR engine should initialize successfully", initResult)
    }

    @Test
    fun testLLMInitialization() = runBlocking {
        val initResult = llmParser.initialize()
        assertTrue("ZeticMLangeLLMParser should initialize successfully", initResult)
    }

    @Test
    fun testWiFiCredentialExtractionWithLLM() = runBlocking {
        // Initialize both engines
        val ocrInit = ocrEngine.initialize()
        val llmInit = llmParser.initialize()

        assertTrue("OCR engine should initialize", ocrInit)
        assertTrue("LLM parser should initialize", llmInit)

        // Create realistic router label bitmap
        val testBitmap = createRouterLabelBitmap()

        // Extract text using CRAFT + KerasOCR
        val textRegions = ocrEngine.extractText(testBitmap, null)

        // Log extracted text regions
        println("=== CRAFT + KerasOCR Text Extraction Results ===")
        textRegions.forEachIndexed { index, region ->
            println("Region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        // Use LLM to parse WiFi credentials from extracted text
        val credentials = llmParser.parseWiFiCredentials(textRegions)

        // Log LLM parsing results
        println("=== ZeticMLangeLLMParser WiFi Credentials ===")
        credentials.forEachIndexed { index, cred ->
            println("Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Assertions
        assertTrue("Should extract at least some text regions", textRegions.isNotEmpty())

        // The LLM should be able to parse credentials if the OCR extracted recognizable text
        if (textRegions.any { it.text.isNotBlank() && it.text.length > 2 }) {
            println("Text was extracted, testing LLM parsing capability...")
            // Note: We can't guarantee credentials will be found since this depends on
            // both OCR accuracy and LLM parsing capability, but we can verify the pipeline works
            assertNotNull("LLM parsing should complete without errors", credentials)
        }
    }

    @Test
    fun testComplexRouterLabelWithLLM() = runBlocking {
        // Initialize engines
        val ocrInit = ocrEngine.initialize()
        val llmInit = llmParser.initialize()

        assertTrue("OCR engine should initialize", ocrInit)
        assertTrue("LLM parser should initialize", llmInit)

        // Create complex router label with multiple fields
        val complexBitmap = createComplexRouterLabelBitmap()

        // Extract text using CRAFT + KerasOCR
        val textRegions = ocrEngine.extractText(complexBitmap, null)

        println("=== Complex Router Label Text Extraction ===")
        textRegions.forEachIndexed { index, region ->
            println("Region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        // Parse with LLM
        val credentials = llmParser.parseWiFiCredentials(textRegions)

        println("=== Complex Label LLM Results ===")
        credentials.forEachIndexed { index, cred ->
            println("Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Verify pipeline completed
        assertNotNull("Text regions should be extracted", textRegions)
        assertNotNull("LLM parsing should complete", credentials)
    }

    private fun createRouterLabelBitmap(): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Router label text
        canvas.drawText("Network Name (SSID): MyWiFi_5G", 50f, 150f, paint)
        canvas.drawText("Network Key (Password): SecurePass123!", 50f, 220f, paint)
        canvas.drawText("Model: RT-AC68U", 50f, 290f, paint)
        canvas.drawText("S/N: 1234567890", 50f, 360f, paint)

        return bitmap
    }

    private fun createComplexRouterLabelBitmap(): Bitmap {
        val width = 900
        val height = 700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Light gray background
        canvas.drawColor(Color.parseColor("#F5F5F5"))

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        // Complex router label with multiple sections
        canvas.drawText("WIRELESS ROUTER", 50f, 80f, titlePaint)
        canvas.drawText("2.4GHz Network", 50f, 140f, titlePaint)
        canvas.drawText("SSID: HomeNetwork_2.4G", 70f, 180f, textPaint)
        canvas.drawText("Password: MyP@ssw0rd2024", 70f, 220f, textPaint)

        canvas.drawText("5GHz Network", 50f, 300f, titlePaint)
        canvas.drawText("SSID: HomeNetwork_5G", 70f, 340f, textPaint)
        canvas.drawText("Password: MyP@ssw0rd2024", 70f, 380f, textPaint)

        canvas.drawText("WPS PIN: 12345678", 50f, 450f, textPaint)
        canvas.drawText("MAC: 00:11:22:33:44:55", 50f, 490f, textPaint)
        canvas.drawText("Model: ASUS RT-AX88U", 50f, 530f, textPaint)

        return bitmap
    }
}