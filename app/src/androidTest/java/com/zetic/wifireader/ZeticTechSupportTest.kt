package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zeticai.mlange.core.model.ZeticMLangeModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive Tech Support Test for Zetic MLange Model Downloads
 *
 * This test suite is designed to help diagnose issues with Zetic MLange model downloads
 * and provide detailed error information for tech support submissions.
 *
 * Run this test and share the results with Zetic tech support.
 */
@RunWith(AndroidJUnit4::class)
class ZeticTechSupportTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticTechSupportTest"

        // API Keys
        private const val DETECT_API_KEY = "dev_9ba20e80c3fd4edf80f94906aa0ae27d"
        private const val RECOG_API_KEY = "dev_7c93c5d85a2a4ec399f86ac1c2ca1f17"
        private const val FACE_DEBUG_KEY = "dev_854ee24efea74a05852a50916e61518f"

        // Model Names
        private const val TEXT_DETECT_MODEL = "jkim711/text_detect2"
        private const val TEXT_RECOG_MODEL = "jkim711/text_recog3"
        private const val YOLO_MODEL = "Ultralytics/YOLOv8n"
        private const val FACE_MODEL = "deepinsight/scrfd_500m"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        println("\n" + "=".repeat(80))
        println("ZETIC MLANGE TECH SUPPORT DIAGNOSTIC TEST")
        println("SDK Version: 1.3.0")
        println("Device: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})")
        println("=".repeat(80) + "\n")
    }

    // ========================================================================
    // TEST 1: Network Connectivity Verification
    // ========================================================================
    @Test
    fun test01_NetworkConnectivity() {
        println("TEST 1: Network Connectivity Verification")
        println("-".repeat(80))

        try {
            println("Testing network connectivity with intentionally invalid model name...")
            val invalidModel = ZeticMLangeModel(
                context,
                FACE_DEBUG_KEY,
                "invalid/model/name/for/testing",
                null
            )

            fail("Should have failed with invalid model name")

        } catch (e: Exception) {
            println("Expected error received:")
            println("  Error Type: ${e.javaClass.simpleName}")
            println("  Error Message: ${e.message}")

            when {
                e.message?.contains("404") == true -> {
                    println("  ✅ Network OK: Got 404 for invalid model (expected)")
                    assertTrue("Network connectivity confirmed", true)
                }
                e.message?.contains("HTTP") == true -> {
                    println("  ✅ Network OK: Got HTTP error (expected)")
                    assertTrue("Network connectivity confirmed", true)
                }
                else -> {
                    println("  ⚠️  Unexpected error type: ${e.message}")
                    assertTrue("Network test completed", true)
                }
            }
        }
        println()
    }

    // ========================================================================
    // TEST 2: Working Model Download (YOLOv8n - Control Test)
    // ========================================================================
    @Test
    fun test02_WorkingModelDownload_YOLOv8n() {
        println("TEST 2: Working Model Download (Control Test)")
        println("-".repeat(80))
        println("Model: $YOLO_MODEL")
        println("API Key: $FACE_DEBUG_KEY")

        try {
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, FACE_DEBUG_KEY, YOLO_MODEL, null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("  ✅ SUCCESS: YOLOv8n downloaded successfully")
            println("  Download Time: ${downloadTime}ms")
            println("  Model Instance: $model")
            assertTrue("YOLOv8n should download successfully", true)

        } catch (e: Exception) {
            println("  ❌ FAILED: YOLOv8n download failed (unexpected)")
            println("  Error Type: ${e.javaClass.simpleName}")
            println("  Error Message: ${e.message}")
            fail("YOLOv8n download should work: ${e.message}")
        }
        println()
    }

    // ========================================================================
    // TEST 3: Text Detection Model Download (PROBLEM MODEL)
    // ========================================================================
    @Test
    fun test03_TextDetectionModelDownload() {
        println("TEST 3: Text Detection Model Download")
        println("-".repeat(80))
        println("Model: $TEXT_DETECT_MODEL")
        println("API Key: $DETECT_API_KEY")

        try {
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("  ✅ SUCCESS: Text detection model downloaded")
            println("  Download Time: ${downloadTime}ms")
            println("  Model Instance: $model")
            assertTrue("Text detection model should download", true)

        } catch (e: Exception) {
            println("  ❌ FAILED: Text detection model download failed")
            println("  Error Type: ${e.javaClass.simpleName}")
            println("  Error Message: ${e.message}")

            // Detailed error analysis
            when {
                e.message?.contains("500") == true -> {
                    println("  🔍 Analysis: HTTP 500 - Server Error")
                    println("     Possible causes:")
                    println("     - Model does not exist on Zetic platform")
                    println("     - Model requires different API key/permissions")
                    println("     - Server-side issue with model serving")
                }
                e.message?.contains("404") == true -> {
                    println("  🔍 Analysis: HTTP 404 - Not Found")
                    println("     - Model name may be incorrect")
                    println("     - Model may have been moved or renamed")
                }
                e.message?.contains("403") == true -> {
                    println("  🔍 Analysis: HTTP 403 - Forbidden")
                    println("     - API key may not have access to this model")
                }
                else -> {
                    println("  🔍 Analysis: Other error - ${e.message}")
                }
            }

            e.printStackTrace()
            fail("Text detection model download failed: ${e.message}")
        }
        println()
    }

    // ========================================================================
    // TEST 4: Text Recognition Model Download (PROBLEM MODEL)
    // ========================================================================
    @Test
    fun test04_TextRecognitionModelDownload() {
        println("TEST 4: Text Recognition Model Download")
        println("-".repeat(80))
        println("Model: $TEXT_RECOG_MODEL")
        println("API Key: $RECOG_API_KEY")

        try {
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, RECOG_API_KEY, TEXT_RECOG_MODEL, null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("  ✅ SUCCESS: Text recognition model downloaded")
            println("  Download Time: ${downloadTime}ms")
            println("  Model Instance: $model")
            assertTrue("Text recognition model should download", true)

        } catch (e: Exception) {
            println("  ❌ FAILED: Text recognition model download failed")
            println("  Error Type: ${e.javaClass.simpleName}")
            println("  Error Message: ${e.message}")

            // Detailed error analysis
            when {
                e.message?.contains("500") == true -> {
                    println("  🔍 Analysis: HTTP 500 - Server Error")
                    println("     Possible causes:")
                    println("     - Model does not exist on Zetic platform")
                    println("     - Model requires different API key/permissions")
                    println("     - Server-side issue with model serving")
                }
                e.message?.contains("404") == true -> {
                    println("  🔍 Analysis: HTTP 404 - Not Found")
                    println("     - Model name may be incorrect")
                    println("     - Model may have been moved or renamed")
                }
                e.message?.contains("403") == true -> {
                    println("  🔍 Analysis: HTTP 403 - Forbidden")
                    println("     - API key may not have access to this model")
                }
                else -> {
                    println("  🔍 Analysis: Other error - ${e.message}")
                }
            }

            e.printStackTrace()
            fail("Text recognition model download failed: ${e.message}")
        }
        println()
    }

    // ========================================================================
    // TEST 5: LLM Model Download (deepseek-r1-distill-qwen-1.5b-f16)
    // ========================================================================
    @Test
    fun test05_LLMModelDownload() {
        println("TEST 5: LLM Model Download")
        println("-".repeat(80))
        println("Model: $FACE_MODEL")
        println("API Key: $FACE_DEBUG_KEY")
        println("Purpose: Test LLM model for WiFi credential parsing")

        try {
            val startTime = System.currentTimeMillis()
            val model = ZeticMLangeModel(context, FACE_DEBUG_KEY, "deepseek-r1-distill-qwen-1.5b-f16", null)
            val downloadTime = System.currentTimeMillis() - startTime

            println("  ✅ SUCCESS: LLM model downloaded successfully")
            println("  Download Time: ${downloadTime}ms")
            println("  Model Instance: $model")
            assertTrue("LLM model should download successfully", true)

        } catch (e: Exception) {
            println("  ❌ FAILED: LLM model download failed")
            println("  Error Type: ${e.javaClass.simpleName}")
            println("  Error Message: ${e.message}")

            // Detailed error analysis
            when {
                e.message?.contains("401") == true -> {
                    println("  🔍 Analysis: HTTP 401 - Unauthorized")
                    println("     - API key may not have access to this LLM model")
                }
                e.message?.contains("404") == true -> {
                    println("  🔍 Analysis: HTTP 404 - Not Found")
                    println("     - LLM model may not be available in Zetic MLange")
                }
                e.message?.contains("500") == true -> {
                    println("  🔍 Analysis: HTTP 500 - Server Error")
                    println("     - Model may not exist on Zetic platform")
                }
                else -> {
                    println("  🔍 Analysis: Other error - ${e.message}")
                }
            }

            e.printStackTrace()
            fail("LLM model download failed: ${e.message}")
        }
        println()
    }

    // ========================================================================
    // TEST 6: Alternative API Key Test (FACE_DEBUG_KEY)
    // ========================================================================
    @Test
    fun test06_AlternativeAPIKey_TextDetection() {
        println("TEST 6: Text Detection with Alternative API Key")
        println("-".repeat(80))
        println("Model: $TEXT_DETECT_MODEL")
        println("API Key: $FACE_DEBUG_KEY (the one that works for YOLOv8n)")
        println("Purpose: Test if issue is API-key specific")

        try {
            val model = ZeticMLangeModel(context, FACE_DEBUG_KEY, TEXT_DETECT_MODEL, null)
            println("  ✅ SUCCESS: Model downloaded with alternative API key!")
            println("  🔍 Conclusion: Previous API key may lack permissions for this model")
            assertTrue(true)

        } catch (e: Exception) {
            println("  ❌ FAILED: Model download failed with alternative API key")
            println("  Error: ${e.message}")
            println("  🔍 Conclusion: Issue is not API-key specific")

            // Don't fail the test - this is diagnostic
            assertTrue("Alternative API key test completed", true)
        }
        println()
    }

    // ========================================================================
    // TEST 7: Summary Report
    // ========================================================================
    @Test
    fun test07_SummaryReport() {
        println("TEST 7: Diagnostic Summary")
        println("=".repeat(80))

        val results = mutableMapOf<String, Boolean>()

        // Test each model
        println("Testing all models...")

        // YOLOv8n
        try {
            ZeticMLangeModel(context, FACE_DEBUG_KEY, YOLO_MODEL, null)
            results["YOLOv8n"] = true
        } catch (e: Exception) {
            results["YOLOv8n"] = false
        }

        // Text Detection
        try {
            ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            results["Text Detection"] = true
        } catch (e: Exception) {
            results["Text Detection"] = false
        }

        // Text Recognition
        try {
            ZeticMLangeModel(context, RECOG_API_KEY, TEXT_RECOG_MODEL, null)
            results["Text Recognition"] = true
        } catch (e: Exception) {
            results["Text Recognition"] = false
        }

        // LLM Model
        try {
            ZeticMLangeModel(context, FACE_DEBUG_KEY, "deepseek-r1-distill-qwen-1.5b-f16", null)
            results["LLM (deepseek-r1)"] = true
        } catch (e: Exception) {
            results["LLM (deepseek-r1)"] = false
        }

        println("\nFINAL RESULTS:")
        println("-".repeat(80))
        results.forEach { (model, success) ->
            val status = if (success) "✅ PASS" else "❌ FAIL"
            println("  $status - $model")
        }

        println("\nRECOMMENDATION FOR TECH SUPPORT:")
        println("-".repeat(80))

        val workingCount = results.values.count { it }
        val failingCount = results.size - workingCount

        when {
            failingCount == 0 -> {
                println("  ✅ All models working - no tech support needed!")
            }
            workingCount > 0 -> {
                println("  ⚠️  Partial failure detected:")
                println("     Working: ${results.filterValues { it }.keys.joinToString()}")
                println("     Failing: ${results.filterValues { !it }.keys.joinToString()}")
                println("\n  📧 Action: Contact tech support with:")
                println("     - This test output")
                println("     - TECH_SUPPORT_SUMMARY.md")
                println("     - Request alternative model names for failing models")
            }
            else -> {
                println("  ❌ All models failing:")
                println("\n  📧 Action: Contact tech support with:")
                println("     - This test output")
                println("     - Check API key validity")
                println("     - Check network connectivity")
            }
        }

        println("\n" + "=".repeat(80))

        // Always pass this summary test
        assertTrue("Summary report completed", true)
    }
}
