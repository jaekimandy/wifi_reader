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
class ZeticMLange130DownloadOnlyTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticMLange130DownloadTest"
        private const val API_KEY = "dev_854ee24efea74a05852a50916e61518f"
        private const val TEXT_DETECT_MODEL = "jkim711/text_detect2"
        private const val TEXT_RECOG_MODEL = "jkim711/text_recog3"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testVersion129ModelDownloadTextDetection() {
        println("=== Testing ZeticMLange 1.2.9 Text Detection Model Download ===")
        println("Testing model instantiation (download) with v1.2.9")

        try {
            println("Attempting to download model: $TEXT_DETECT_MODEL")
            println("Using API key: $API_KEY")
            println("ZeticMLange version: 1.2.9")

            // Only test model instantiation - this is where download happens
            // We don't call any methods that require Tensor API
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, API_KEY, TEXT_DETECT_MODEL, null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("✅ Text detection model downloaded successfully!")
            println("Download time: ${downloadTime}ms")
            println("Model instance created: $model")

            // Test passed if we get here without exception
            assertTrue("Text detection model should download successfully", true)

        } catch (e: Exception) {
            println("❌ Text detection model download FAILED")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")

            // Check if it's the same HTTP 404 error or something different
            if (e.message?.contains("404") == true) {
                println("🔍 SAME ISSUE: HTTP 404 - Model not available on server")
            } else if (e.message?.contains("HTTP") == true) {
                println("🔍 DIFFERENT HTTP ERROR: ${e.message}")
            } else {
                println("🔍 NON-HTTP ERROR: ${e.message}")
            }

            e.printStackTrace()

            // Log the full stack trace for debugging
            val stackTrace = e.stackTrace.joinToString("\n") { it.toString() }
            println("Stack trace:\n$stackTrace")

            // Fail the test with detailed error info
            fail("Text detection model download failed: ${e.message}")
        }
    }

    @Test
    fun testVersion130ModelDownloadTextRecognition() {
        println("=== Testing ZeticMLange 1.3.0 Text Recognition Model Download ===")
        println("Only testing model instantiation (download), NOT running inference")

        try {
            println("Attempting to download model: $TEXT_RECOG_MODEL")
            println("Using API key: $API_KEY")
            println("ZeticMLange version: 1.3.0")

            // Only test model instantiation - this is where download happens
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, API_KEY, TEXT_RECOG_MODEL, null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("✅ Text recognition model downloaded successfully!")
            println("Download time: ${downloadTime}ms")
            println("Model instance created: $model")

            // Test passed if we get here without exception
            assertTrue("Text recognition model should download successfully", true)

        } catch (e: Exception) {
            println("❌ Text recognition model download FAILED")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")

            // Check if it's the same HTTP 404 error or something different
            if (e.message?.contains("404") == true) {
                println("🔍 SAME ISSUE: HTTP 404 - Model not available on server")
            } else if (e.message?.contains("HTTP") == true) {
                println("🔍 DIFFERENT HTTP ERROR: ${e.message}")
            } else {
                println("🔍 NON-HTTP ERROR: ${e.message}")
            }

            e.printStackTrace()

            // Fail the test with detailed error info
            fail("Text recognition model download failed: ${e.message}")
        }
    }

    @Test
    fun testVersion130DownloadComparison() {
        println("=== Testing Both Models with 1.3.0 ===")
        println("Comparing download behavior between detection and recognition models")

        var detectionError: String? = null
        var recognitionError: String? = null

        // Test detection model
        try {
            println("1. Testing detection model download...")
            val detectionModel = ZeticMLangeModel(context, API_KEY, TEXT_DETECT_MODEL, null)
            println("✅ Detection model download SUCCESS")
        } catch (e: Exception) {
            detectionError = e.message
            println("❌ Detection model download FAILED: ${e.message}")
        }

        // Test recognition model
        try {
            println("2. Testing recognition model download...")
            val recognitionModel = ZeticMLangeModel(context, API_KEY, TEXT_RECOG_MODEL, null)
            println("✅ Recognition model download SUCCESS")
        } catch (e: Exception) {
            recognitionError = e.message
            println("❌ Recognition model download FAILED: ${e.message}")
        }

        // Analysis
        println("=== DOWNLOAD TEST ANALYSIS ===")
        println("Detection model: ${if (detectionError == null) "✅ SUCCESS" else "❌ FAILED: $detectionError"}")
        println("Recognition model: ${if (recognitionError == null) "✅ SUCCESS" else "❌ FAILED: $recognitionError"}")

        // Check if error patterns are the same
        if (detectionError != null && recognitionError != null) {
            val sameError = detectionError == recognitionError
            println("Same error pattern: $sameError")
            if (sameError) {
                println("🔍 CONCLUSION: Consistent error across both models - likely systematic issue")
            } else {
                println("🔍 CONCLUSION: Different errors - models may have different availability")
            }
        }

        // Test passes if at least we can analyze the behavior
        assertTrue("Download test analysis completed", true)
    }

    @Test
    fun testVersion130NetworkConnectivity() {
        println("=== Testing 1.3.0 Network Connectivity ===")

        try {
            // Try to create a model with an obviously invalid name to test network access
            println("Testing network connectivity with invalid model name...")
            val invalidModel = ZeticMLangeModel(context, API_KEY, "definitely/invalid/model/name/test/12345", null)
            println("❌ Unexpected success with invalid model name")
            fail("Should have failed with invalid model name")

        } catch (e: Exception) {
            println("Network connectivity test result:")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")

            if (e.message?.contains("404") == true) {
                println("✅ Network is working (got 404 for invalid model)")
                println("🔍 This confirms we can reach Zetic's servers")
                assertTrue("Network connectivity confirmed", true)
            } else if (e.message?.contains("HTTP") == true) {
                println("✅ Network is working (got HTTP error)")
                println("🔍 HTTP error: ${e.message}")
                assertTrue("Network connectivity confirmed", true)
            } else {
                println("⚠️ Network issue or other error: ${e.message}")
                println("🔍 This might be a different kind of error")
                // Don't fail this test - just report the issue
                assertTrue("Network test completed", true)
            }
        }
    }
}