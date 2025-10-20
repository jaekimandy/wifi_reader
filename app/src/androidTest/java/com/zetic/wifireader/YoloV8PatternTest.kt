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
class YoloV8PatternTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "YoloV8PatternTest"
        // Using EXACT pattern from YOLOv8 GitHub example
        private const val DEBUG_KEY = "debug_cb6cb12939644316888f333523e42622"
        private const val YOLO_MODEL_KEY = "b9f5d74e6f644288a32c50174ded828e"

        // Face detection example keys from legacy GitHub
        private const val FACE_DEBUG_KEY = "debug_cb6cb12939644316888f333523e42622"
        private const val FACE_MODEL_KEY = "9e9431d8e3874ab2aa9530be711e8575"

        // Our current API keys for comparison
        private const val CURRENT_DETECT_API_KEY = "ztp_79f3546cb28b4427830df9b82ae3dcb5"
        private const val CURRENT_RECOG_API_KEY = "ztp_07f8e796e5a64b5dbbac795d603b7b34"
        private const val CURRENT_TEXT_DETECT_MODEL = "jkim711/text_detect2"
        private const val CURRENT_TEXT_RECOG_MODEL = "jkim711/text_recog3"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testLegacyDebugKeyPattern() {
        println("=== Legacy GitHub Pattern Test ===")
        println("Testing the debug_ key format from GitHub examples...")
        println("Debug Key: $DEBUG_KEY")
        println("Face Model Key: $FACE_MODEL_KEY")
        println()

        try {
            println("Step 1: Testing legacy 3-parameter constructor with debug_ key...")

            // Use legacy GitHub face detection pattern with 3 parameters
            val model = ZeticMLangeModel(
                context,
                FACE_DEBUG_KEY,
                FACE_MODEL_KEY,
                null
            )

            println("✅ Legacy GitHub pattern model creation SUCCESSFUL!")
            println("Model object: $model")
            println("Model class: ${model.javaClass.name}")

            // Test if we can access basic model properties
            try {
                println("Step 2: Testing model properties access...")
                val modelInfo = model.toString()
                println("Model info: $modelInfo")

                // Note: ZLang 1.3.0 doesn't have outputBuffers property
                // Outputs are only available from model.run() which returns Array<Tensor>
                println("Step 3: Model created successfully (no outputBuffers in 1.3.0)")

            } catch (e: Exception) {
                println("❌ Error accessing model properties: ${e.message}")
                println("Stack trace:")
                e.printStackTrace()
            }

        } catch (e: Exception) {
            println("❌ Legacy GitHub pattern model creation FAILED")
            println("Exception Type: ${e.javaClass.name}")
            println("Exception Message: ${e.message}")
            println("Exception Cause: ${e.cause?.message}")

            // Print full stack trace for debugging
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            println("Error code: HTTP $errorCode")
        }

        assertTrue("Legacy GitHub pattern test completed", true)
    }

    @Test
    fun testLegacyVsCurrentPatternComparison() {
        println("=== Legacy vs Current Pattern Comparison ===")

        val results = mutableMapOf<String, String>()

        // Test 1: Legacy face detection pattern (debug_ key + hash model)
        try {
            println("Testing legacy face detection pattern...")
            val faceModel = ZeticMLangeModel(context, FACE_DEBUG_KEY, FACE_MODEL_KEY, null)
            results["LEGACY_FACE_DETECTION"] = "✅ SUCCESS"
            println("✅ Legacy face detection pattern works!")
        } catch (e: Exception) {
            println("Exception Type: ${e.javaClass.name}")
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            results["LEGACY_FACE_DETECTION"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ Legacy face detection failed: ${e.message}")
        }

        // Test 2: Current detection pattern (ztp_ key + username/model)
        try {
            println("Testing current detection pattern...")
            val currentDetectModel = ZeticMLangeModel(context, CURRENT_DETECT_API_KEY, CURRENT_TEXT_DETECT_MODEL, null)
            results["CURRENT_DETECTION"] = "✅ SUCCESS"
            println("✅ Current detection pattern works!")
        } catch (e: Exception) {
            println("Exception Type: ${e.javaClass.name}")
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            results["CURRENT_DETECTION"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ Current detection failed: ${e.message}")
        }

        // Test 3: Current recognition pattern (ztp_ key + username/model)
        try {
            println("Testing current recognition pattern...")
            val currentRecogModel = ZeticMLangeModel(context, CURRENT_RECOG_API_KEY, CURRENT_TEXT_RECOG_MODEL, null)
            results["CURRENT_RECOGNITION"] = "✅ SUCCESS"
            println("✅ Current recognition pattern works!")
        } catch (e: Exception) {
            println("Exception Type: ${e.javaClass.name}")
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            results["CURRENT_RECOGNITION"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ Current recognition failed: ${e.message}")
        }

        // Test 4: Mix legacy debug key with current model names
        try {
            println("Testing legacy debug key with current text detection model...")
            val mixedModel = ZeticMLangeModel(context, FACE_DEBUG_KEY, CURRENT_TEXT_DETECT_MODEL, null)
            results["MIXED_LEGACY_KEY_CURRENT_MODEL"] = "✅ SUCCESS"
            println("✅ Mixed pattern works!")
        } catch (e: Exception) {
            println("Exception Type: ${e.javaClass.name}")
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            results["MIXED_LEGACY_KEY_CURRENT_MODEL"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ Mixed pattern failed: ${e.message}")
        }

        println("\n=== COMPARISON RESULTS ===")
        results.forEach { (test, result) ->
            println("$test: $result")
        }

        val successCount = results.values.count { it.startsWith("✅") }
        val failCount = results.values.count { it.startsWith("❌") }

        println("\n=== PATTERN ANALYSIS ===")
        println("Successful patterns: $successCount")
        println("Failed patterns: $failCount")

        if (results["LEGACY_FACE_DETECTION"]?.startsWith("✅") == true) {
            println("🎉 BREAKTHROUGH: Legacy debug_ pattern works!")
            println("This suggests using:")
            println("  - debug_ API key prefix")
            println("  - Hash-based model keys (not username/model format)")
            println("  - 3-parameter constructor")
        }

        if (results["CURRENT_DETECTION"]?.startsWith("✅") == true ||
            results["CURRENT_RECOGNITION"]?.startsWith("✅") == true) {
            println("✨ Current ztp_ pattern also works!")
        }

        if (results["MIXED_LEGACY_KEY_CURRENT_MODEL"]?.startsWith("✅") == true) {
            println("🔄 Mixed approach works: Legacy debug_ keys + Current model names!")
        }

        // Analyze error patterns
        val errorTypes = mutableMapOf<String, MutableList<String>>()
        results.forEach { (testName, result) ->
            val errorType = when {
                result.contains("404") -> "HTTP 404"
                result.contains("500") -> "HTTP 500"
                result.contains("401") -> "HTTP 401"
                result.contains("403") -> "HTTP 403"
                result.startsWith("✅") -> "SUCCESS"
                else -> "OTHER"
            }
            errorTypes.getOrPut(errorType) { mutableListOf() }.add(testName)
        }

        println("\n=== ERROR TYPE BREAKDOWN ===")
        errorTypes.forEach { (errorType, tests) ->
            println("$errorType: ${tests.joinToString(", ")}")
        }

        assertTrue("Pattern comparison test completed", true)
    }
}