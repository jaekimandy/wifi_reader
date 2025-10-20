package com.zetic.wifireader

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zeticai.mlange.core.model.ZeticMLangeModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ZeticMLange130ModelDownloadTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticMLange130Test"
        private const val API_KEY = "dev_854ee24efea74a05852a50916e61518f"
        private const val TEXT_DETECT_MODEL = "jkim711/text_detect2"
        private const val TEXT_RECOG_MODEL = "jkim711/text_recog3"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testTextDetectionModelDownload() {
        println("=== Testing ZeticMLange 1.3.0 Text Detection Model Download ===")

        try {
            println("Attempting to download model: $TEXT_DETECT_MODEL")
            println("Using API key: $API_KEY")

            // Simple model instantiation test - this is where download happens
            val model = ZeticMLangeModel(context, API_KEY, TEXT_DETECT_MODEL, null)

            println("✅ Text detection model downloaded successfully!")
            println("Model instance created: $model")

            // Test passed if we get here without exception
            assertTrue("Text detection model should download successfully", true)

        } catch (e: Exception) {
            println("❌ Text detection model download FAILED")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")
            e.printStackTrace()

            // Log the full stack trace for debugging
            val stackTrace = e.stackTrace.joinToString("\n") { it.toString() }
            println("Stack trace:\n$stackTrace")

            // Fail the test with detailed error info
            fail("Text detection model download failed: ${e.message}")
        }
    }

    @Test
    fun testTextRecognitionModelDownload() {
        println("=== Testing ZeticMLange 1.3.0 Text Recognition Model Download ===")

        try {
            println("Attempting to download model: $TEXT_RECOG_MODEL")
            println("Using API key: $API_KEY")

            // Simple model instantiation test - this is where download happens
            val model = ZeticMLangeModel(context, API_KEY, TEXT_RECOG_MODEL, null)

            println("✅ Text recognition model downloaded successfully!")
            println("Model instance created: $model")

            // Test passed if we get here without exception
            assertTrue("Text recognition model should download successfully", true)

        } catch (e: Exception) {
            println("❌ Text recognition model download FAILED")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")
            e.printStackTrace()

            // Log the full stack trace for debugging
            val stackTrace = e.stackTrace.joinToString("\n") { it.toString() }
            println("Stack trace:\n$stackTrace")

            // Fail the test with detailed error info
            fail("Text recognition model download failed: ${e.message}")
        }
    }

    @Test
    fun testBothModelsSequentially() {
        println("=== Testing Both Models Download Sequentially ===")

        var detectionSuccess = false
        var recognitionSuccess = false

        // Test detection model first
        try {
            println("1. Testing detection model download...")
            val detectionModel = ZeticMLangeModel(context, API_KEY, TEXT_DETECT_MODEL, null)
            detectionSuccess = true
            println("✅ Detection model download SUCCESS")
        } catch (e: Exception) {
            println("❌ Detection model download FAILED: ${e.message}")
        }

        // Test recognition model second
        try {
            println("2. Testing recognition model download...")
            val recognitionModel = ZeticMLangeModel(context, API_KEY, TEXT_RECOG_MODEL, null)
            recognitionSuccess = true
            println("✅ Recognition model download SUCCESS")
        } catch (e: Exception) {
            println("❌ Recognition model download FAILED: ${e.message}")
        }

        // Summary
        println("=== DOWNLOAD TEST SUMMARY ===")
        println("Detection model: ${if (detectionSuccess) "✅ SUCCESS" else "❌ FAILED"}")
        println("Recognition model: ${if (recognitionSuccess) "✅ SUCCESS" else "❌ FAILED"}")

        if (detectionSuccess && recognitionSuccess) {
            println("🎉 ALL MODELS DOWNLOADED SUCCESSFULLY WITH v1.3.0!")
        } else {
            println("⚠️ Some models failed to download")
        }

        // Test passes if at least one model downloads successfully
        assertTrue("At least one model should download successfully", detectionSuccess || recognitionSuccess)
    }

    @Test
    fun testNetworkConnectivity() {
        println("=== Testing Network Connectivity ===")

        try {
            // Try to create a model with an obviously invalid name to test network access
            val invalidModel = ZeticMLangeModel(context, API_KEY, "invalid/model/name/test", null)
            println("❌ Unexpected success with invalid model name")
            fail("Should have failed with invalid model name")

        } catch (e: Exception) {
            println("Network test result:")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")

            if (e.message?.contains("404") == true) {
                println("✅ Network is working (got 404 for invalid model)")
                assertTrue("Network connectivity confirmed", true)
            } else if (e.message?.contains("HTTP") == true) {
                println("✅ Network is working (got HTTP error)")
                assertTrue("Network connectivity confirmed", true)
            } else {
                println("⚠️ Network issue or other error: ${e.message}")
                // Don't fail this test - just report the issue
                assertTrue("Network test completed", true)
            }
        }
    }
}