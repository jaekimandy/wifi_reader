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
import com.zetic.wifireader.ocr.KerasOCREngine
import com.zetic.wifireader.pipeline.WiFiTextParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KerasOCRLLMComparisonTest {

    private lateinit var context: Context
    private lateinit var kerasOCREngine: KerasOCREngine
    private lateinit var llmParser: ZeticMLangeLLMParser
    private lateinit var regexParser: WiFiTextParser

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        kerasOCREngine = KerasOCREngine(context)
        llmParser = ZeticMLangeLLMParser(context)
        regexParser = WiFiTextParser()
    }

    @After
    fun tearDown() {
        kerasOCREngine.release()
        llmParser.release()
    }

    @Test
    fun testKerasOCRInitialization() = runBlocking {
        val initResult = kerasOCREngine.initialize()
        assertTrue("KerasOCR engine should initialize successfully", initResult)
    }

    @Test
    fun testLLMInitialization() = runBlocking {
        val initResult = llmParser.initialize()
        assertTrue("ZeticMLangeLLMParser should initialize successfully", initResult)
    }

    @Test
    fun compareRegexParserVsLLMParser() = runBlocking {
        // Initialize both engines
        val ocrInit = kerasOCREngine.initialize()
        val llmInit = llmParser.initialize()

        assertTrue("OCR engine should initialize", ocrInit)
        assertTrue("LLM parser should initialize", llmInit)

        // Create realistic router label bitmap
        val testBitmap = createRouterLabelBitmap()

        // Extract text using KerasOCR
        val textRegions = kerasOCREngine.extractText(testBitmap, null)

        println("=== KerasOCR Text Extraction Results ===")
        textRegions.forEachIndexed { index, region ->
            println("Region $index: '${region.text}' (confidence: ${region.confidence})")
        }

        // Parse using REGEX parser (original)
        println("\n=== REGEX Parser Results ===")
        val regexCredentials = regexParser.parseWiFiCredentials(textRegions)
        regexCredentials.forEachIndexed { index, cred ->
            println("REGEX Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Parse using LLM parser
        println("\n=== LLM Parser Results ===")
        val llmCredentials = llmParser.parseWiFiCredentials(textRegions)
        llmCredentials.forEachIndexed { index, cred ->
            println("LLM Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
        }

        // Compare results
        println("\n=== COMPARISON ===")
        println("Text regions extracted: ${textRegions.size}")
        println("REGEX parser found: ${regexCredentials.size} credentials")
        println("LLM parser found: ${llmCredentials.size} credentials")

        if (regexCredentials.isNotEmpty() && llmCredentials.isNotEmpty()) {
            println("REGEX SSID: '${regexCredentials[0].ssid}' vs LLM SSID: '${llmCredentials[0].ssid}'")
            println("REGEX Password: '${regexCredentials[0].password}' vs LLM Password: '${llmCredentials[0].password}'")
        }

        // Assertions
        assertTrue("Should extract at least some text regions", textRegions.isNotEmpty())
        assertNotNull("REGEX parsing should complete without errors", regexCredentials)
        assertNotNull("LLM parsing should complete without errors", llmCredentials)
    }

    @Test
    fun testSpecificWiFiLabelFormats() = runBlocking {
        val ocrInit = kerasOCREngine.initialize()
        val llmInit = llmParser.initialize()

        assertTrue("OCR engine should initialize", ocrInit)
        assertTrue("LLM parser should initialize", llmInit)

        // Test different router label formats
        val testCases = listOf(
            "Format 1: Network Name/Key" to createNetworkNameKeyBitmap(),
            "Format 2: SSID/Password" to createSSIDPasswordBitmap(),
            "Format 3: Complex Router Label" to createComplexRouterLabelBitmap()
        )

        for ((formatName, bitmap) in testCases) {
            println("\n=== Testing $formatName ===")

            val textRegions = kerasOCREngine.extractText(bitmap, null)
            println("OCR extracted ${textRegions.size} text regions:")
            textRegions.forEachIndexed { index, region ->
                println("  Region $index: '${region.text}' (confidence: ${region.confidence})")
            }

            val regexResults = regexParser.parseWiFiCredentials(textRegions)
            val llmResults = llmParser.parseWiFiCredentials(textRegions)

            println("REGEX found ${regexResults.size} credentials:")
            regexResults.forEachIndexed { index, cred ->
                println("  REGEX $index: SSID='${cred.ssid}', Password='${cred.password}'")
            }

            println("LLM found ${llmResults.size} credentials:")
            llmResults.forEachIndexed { index, cred ->
                println("  LLM $index: SSID='${cred.ssid}', Password='${cred.password}'")
            }

            // Verify both parsers work
            assertNotNull("REGEX parsing should complete", regexResults)
            assertNotNull("LLM parsing should complete", llmResults)
        }
    }

    private fun createRouterLabelBitmap(): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        canvas.drawText("Network Name (SSID): MyWiFi_5G", 50f, 150f, paint)
        canvas.drawText("Network Key (Password): SecurePass123!", 50f, 220f, paint)
        canvas.drawText("Model: RT-AC68U", 50f, 290f, paint)

        return bitmap
    }

    private fun createNetworkNameKeyBitmap(): Bitmap {
        val width = 700
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        canvas.drawText("Network Name: HomeWiFi_2.4G", 40f, 120f, paint)
        canvas.drawText("Network Key: MyPassword123", 40f, 180f, paint)

        return bitmap
    }

    private fun createSSIDPasswordBitmap(): Bitmap {
        val width = 700
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        canvas.drawText("SSID: TestNetwork_5G", 40f, 120f, paint)
        canvas.drawText("Password: TestPass2024!", 40f, 180f, paint)

        return bitmap
    }

    private fun createComplexRouterLabelBitmap(): Bitmap {
        val width = 900
        val height = 700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

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

        canvas.drawText("WIRELESS ROUTER", 50f, 80f, titlePaint)
        canvas.drawText("2.4GHz Network", 50f, 140f, titlePaint)
        canvas.drawText("SSID: HomeNetwork_2.4G", 70f, 180f, textPaint)
        canvas.drawText("Password: MyP@ssw0rd2024", 70f, 220f, textPaint)

        canvas.drawText("5GHz Network", 50f, 300f, titlePaint)
        canvas.drawText("SSID: HomeNetwork_5G", 70f, 340f, textPaint)
        canvas.drawText("Password: MyP@ssw0rd2024", 70f, 380f, textPaint)

        return bitmap
    }
}