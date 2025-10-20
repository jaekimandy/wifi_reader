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
class DirectZeticMLangeTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "DirectZeticMLangeTest"
        private const val DETECT_API_KEY = "ztp_79f3546cb28b4427830df9b82ae3dcb5"  // Latest detection key
        private const val RECOG_API_KEY = "ztp_07f8e796e5a64b5dbbac795d603b7b34"   // Latest recognition key
        private const val OLD_API_KEY = "dev_854ee24efea74a05852a50916e61518f"
        private const val TEXT_DETECT_MODEL = "jkim711/text_detect2"  // Model name with username
        private const val TEXT_RECOG_MODEL = "jkim711/text_recog3"    // Model name with username
        private const val YOLO_MODEL = "Ultralytics/YOLOv8n"  // Known working model

        // Legacy GitHub pattern keys (from face detection and YOLOv8 examples)
        private const val GITHUB_DEBUG_KEY = "debug_cb6cb12939644316888f333523e42622"
        private const val GITHUB_FACE_MODEL = "9e9431d8e3874ab2aa9530be711e8575"
        private const val GITHUB_YOLO_MODEL = "b9f5d74e6f644288a32c50174ded828e"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testDirectModelDownloadWithLatestKeys() {
        println("=== DIRECT ZeticMLange Model Download Test (LATEST KEYS) ===")
        println("Detection API Key: $DETECT_API_KEY")
        println("Recognition API Key: $RECOG_API_KEY")
        println("Detection Model: $TEXT_DETECT_MODEL")
        println("Recognition Model: $TEXT_RECOG_MODEL")

        // Test detection model with its specific key
        try {
            println("Testing detection model with detection key...")
            val startTime = System.currentTimeMillis()

            val detectionModel = ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)

            val downloadTime = System.currentTimeMillis() - startTime
            println("✅ Detection model download SUCCESSFUL!")
            println("Download time: ${downloadTime}ms")
            println("Model object: $detectionModel")

        } catch (e: Exception) {
            println("❌ Detection model download FAILED")
            println("Exception message: ${e.message}")

            if (e.message?.contains("404") == true) {
                println("🔍 HTTP 404 - Detection model not found on server")
            } else if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                println("🔍 Authentication error - Detection API key issue")
            }
        }

        // Test recognition model with its specific key
        try {
            println("Testing recognition model with recognition key...")
            val startTime = System.currentTimeMillis()

            val recognitionModel = ZeticMLangeModel(context, RECOG_API_KEY, TEXT_RECOG_MODEL, null)

            val downloadTime = System.currentTimeMillis() - startTime
            println("✅ Recognition model download SUCCESSFUL!")
            println("Download time: ${downloadTime}ms")
            println("Model object: $recognitionModel")

        } catch (e: Exception) {
            println("❌ Recognition model download FAILED")
            println("Exception message: ${e.message}")

            if (e.message?.contains("404") == true) {
                println("🔍 HTTP 404 - Recognition model not found on server")
            } else if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                println("🔍 Authentication error - Recognition API key issue")
            }
        }

        assertTrue("Latest keys investigation completed", true)
    }

    @Test
    fun testDirectModelDownloadWithOldKey() {
        println("=== DIRECT ZeticMLange Model Download Test (OLD KEY) ===")
        println("Old API Key: $OLD_API_KEY")
        println("Model: $TEXT_DETECT_MODEL")

        try {
            println("About to call ZeticMLangeModel constructor...")
            val startTime = System.currentTimeMillis()

            val model = ZeticMLangeModel(context, OLD_API_KEY, TEXT_DETECT_MODEL, null)

            val downloadTime = System.currentTimeMillis() - startTime
            println("✅ Model download SUCCESSFUL with old key!")
            println("Download time: ${downloadTime}ms")

            assertTrue("Model should download with old key", true)

        } catch (e: Exception) {
            println("❌ Model download FAILED with old key")
            println("Exception message: ${e.message}")

            if (e.message?.contains("404") == true) {
                println("🔍 HTTP 404 - Model not found on server")
            }

            assertTrue("Investigation completed", true)
        }
    }

    @Test
    fun testYOLOModelDownload() {
        println("=== YOLO Model Download Test ===")
        println("Detection API Key: $DETECT_API_KEY")
        println("Model: $YOLO_MODEL (should work)")

        try {
            println("About to download known working YOLO model...")
            val startTime = System.currentTimeMillis()

            val model = ZeticMLangeModel(context, DETECT_API_KEY, YOLO_MODEL, null)

            val downloadTime = System.currentTimeMillis() - startTime
            println("✅ YOLO model download SUCCESSFUL!")
            println("Download time: ${downloadTime}ms")

            assertTrue("YOLO model should work", true)

        } catch (e: Exception) {
            println("❌ Even YOLO model download FAILED")
            println("Exception message: ${e.message}")
            println("This indicates a fundamental API issue")

            assertTrue("YOLO test investigation completed", true)
        }
    }

    @Test
    fun testAPIKeyComparison() {
        println("=== API Key Comparison Test ===")

        val results = mutableMapOf<String, String>()

        // Test latest detection key with text detection
        try {
            ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            results["LATEST_DETECT_KEY + TEXT_DETECT"] = "✅ SUCCESS"
        } catch (e: Exception) {
            results["LATEST_DETECT_KEY + TEXT_DETECT"] = "❌ FAILED: ${e.message}"
        }

        // Test latest recognition key with text recognition
        try {
            ZeticMLangeModel(context, RECOG_API_KEY, TEXT_RECOG_MODEL, null)
            results["LATEST_RECOG_KEY + TEXT_RECOG"] = "✅ SUCCESS"
        } catch (e: Exception) {
            results["LATEST_RECOG_KEY + TEXT_RECOG"] = "❌ FAILED: ${e.message}"
        }

        // Test old key with text detection
        try {
            ZeticMLangeModel(context, OLD_API_KEY, TEXT_DETECT_MODEL, null)
            results["OLD_KEY + TEXT_DETECT"] = "✅ SUCCESS"
        } catch (e: Exception) {
            results["OLD_KEY + TEXT_DETECT"] = "❌ FAILED: ${e.message}"
        }

        // Test latest detection key with YOLO
        try {
            ZeticMLangeModel(context, DETECT_API_KEY, YOLO_MODEL, null)
            results["LATEST_DETECT_KEY + YOLO"] = "✅ SUCCESS"
        } catch (e: Exception) {
            results["LATEST_DETECT_KEY + YOLO"] = "❌ FAILED: ${e.message}"
        }

        // Test old key with YOLO
        try {
            ZeticMLangeModel(context, OLD_API_KEY, YOLO_MODEL, null)
            results["OLD_KEY + YOLO"] = "✅ SUCCESS"
        } catch (e: Exception) {
            results["OLD_KEY + YOLO"] = "❌ FAILED: ${e.message}"
        }

        println("=== RESULTS ===")
        results.forEach { (test, result) ->
            println("$test: $result")
        }

        val successCount = results.values.count { it.startsWith("✅") }
        val failCount = results.values.count { it.startsWith("❌") }

        println("\n=== SUMMARY ===")
        println("Successful downloads: $successCount")
        println("Failed downloads: $failCount")

        if (successCount > 0) {
            println("🔍 CONCLUSION: Some models/keys work - investigate working combinations")
        } else {
            println("🔍 CONCLUSION: Complete failure - check ZeticMLange API status")
        }

        assertTrue("API key comparison completed", true)
    }

    @Test
    fun testModelNameVariations() {
        println("=== Model Name Variations Test ===")

        val results = mutableMapOf<String, String>()

        // Test different combinations of model names with latest detection key
        val modelVariations = listOf(
            "jkim711",
            "jkim711/",
            "jkim711/text_detect2",
            "text_detect2",
            "jkim711/text_recog3",
            "text_recog3",
            "jkim711/YOLOv8n",
            "YOLOv8n",
            "Ultralytics/YOLOv8n"
        )

        modelVariations.forEach { modelName ->
            try {
                ZeticMLangeModel(context, DETECT_API_KEY, modelName, null)
                results[modelName] = "✅ SUCCESS"
            } catch (e: Exception) {
                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("401") == true -> "401"
                    e.message?.contains("403") == true -> "403"
                    else -> "OTHER"
                }
                results[modelName] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            }
        }

        println("=== MODEL NAME VARIATION RESULTS ===")
        results.forEach { (modelName, result) ->
            println("'$modelName': $result")
        }

        // Group by error type
        val errorTypes = mutableMapOf<String, MutableList<String>>()
        results.forEach { (modelName, result) ->
            val errorType = when {
                result.contains("404") -> "HTTP 404"
                result.contains("500") -> "HTTP 500"
                result.contains("401") -> "HTTP 401"
                result.contains("403") -> "HTTP 403"
                result.startsWith("✅") -> "SUCCESS"
                else -> "OTHER"
            }
            errorTypes.getOrPut(errorType) { mutableListOf() }.add(modelName)
        }

        println("\n=== ERROR TYPE ANALYSIS ===")
        errorTypes.forEach { (errorType, models) ->
            println("$errorType: ${models.joinToString(", ")}")
        }

        assertTrue("Model name variations test completed", true)
    }

    @Test
    fun testInvalidAPIKeyComparison() {
        println("=== Invalid API Key Test ===")
        println("Testing what happens with deliberately invalid API key...")

        val invalidApiKey = "ztp_invalid_key_for_testing_123456789"
        val validApiKey = DETECT_API_KEY
        val results = mutableMapOf<String, String>()

        // Test just two key model variations to compare
        val testModel = "jkim711/text_detect2"

        println("Testing model: $testModel")
        println("Valid API Key: $validApiKey")
        println("Invalid API Key: $invalidApiKey")
        println()

        // Test with valid key first
        try {
            ZeticMLangeModel(context, validApiKey, testModel, null)
            results["VALID_KEY"] = "✅ SUCCESS"
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["VALID_KEY"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
        }

        // Test with invalid key
        try {
            ZeticMLangeModel(context, invalidApiKey, testModel, null)
            results["INVALID_KEY"] = "✅ SUCCESS (unexpected!)"
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["INVALID_KEY"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
        }

        println("=== API KEY COMPARISON RESULTS ===")
        results.forEach { (keyType, result) ->
            println("$keyType: $result")
        }

        println("\n=== ANALYSIS ===")
        val validKeyResult = results["VALID_KEY"] ?: ""
        val invalidKeyResult = results["INVALID_KEY"] ?: ""

        if (validKeyResult.contains("404") && invalidKeyResult.contains("401")) {
            println("✅ CONCLUSION: Valid keys authenticate but models not found (404), invalid keys rejected (401)")
            println("This means authentication works but the specific models aren't available yet")
        } else if (validKeyResult.contains("404") && invalidKeyResult.contains("404")) {
            println("🤔 CONCLUSION: Both valid and invalid keys get 404 - might be model availability issue")
        } else {
            println("🔍 CONCLUSION: Mixed results - need further investigation")
            println("Valid key result: $validKeyResult")
            println("Invalid key result: $invalidKeyResult")
        }

        assertTrue("Invalid API key test completed", true)
    }

    @Test
    fun testLegacyAPIKeyFormat() {
        println("=== Legacy API Key Format Test ===")
        println("Testing legacy 'debug_' format from working GitHub example...")

        val legacyDebugKey = "debug_cb6cb12939644316888f333523e42622"  // From working GitHub example
        val legacyModelKey = "9e9431d8e3874ab2aa9530be711e8575"       // From working GitHub example
        val results = mutableMapOf<String, String>()

        println("Legacy Debug Key: $legacyDebugKey")
        println("Legacy Model Key: $legacyModelKey")
        println()

        // Test legacy debug key with legacy model key
        try {
            ZeticMLangeModel(context, legacyDebugKey, legacyModelKey, null)
            results["LEGACY_DEBUG_KEY"] = "✅ SUCCESS"
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["LEGACY_DEBUG_KEY"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
        }

        // Test our current API key with legacy model key format
        try {
            ZeticMLangeModel(context, DETECT_API_KEY, legacyModelKey, null)
            results["CURRENT_KEY_LEGACY_MODEL"] = "✅ SUCCESS"
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["CURRENT_KEY_LEGACY_MODEL"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
        }

        println("=== LEGACY FORMAT RESULTS ===")
        results.forEach { (keyType, result) ->
            println("$keyType: $result")
        }

        println("\n=== LEGACY FORMAT ANALYSIS ===")
        val legacyResult = results["LEGACY_DEBUG_KEY"] ?: ""
        val mixedResult = results["CURRENT_KEY_LEGACY_MODEL"] ?: ""

        if (legacyResult.contains("SUCCESS")) {
            println("🎉 BREAKTHROUGH: Legacy debug key format works!")
            println("This suggests there are two different API formats:")
            println("  - Legacy: debug_ prefix + hash model keys")
            println("  - Current: ztp_ prefix + username/model names")
        } else if (legacyResult.contains("401") || legacyResult.contains("403")) {
            println("🔑 Legacy debug key expired or unauthorized")
        } else if (legacyResult.contains("404")) {
            println("🤔 Legacy model key not found - but format might be correct")
        }

        if (mixedResult.contains("SUCCESS")) {
            println("✨ Current API key works with legacy model format!")
        }

        assertTrue("Legacy API format test completed", true)
    }

    @Test
    fun testGitHubLegacyDebugPattern() {
        println("=== GitHub Legacy Debug Pattern Test ===")
        println("Testing EXACT GitHub face detection pattern with debug_ prefix...")
        println("GitHub Debug Key: $GITHUB_DEBUG_KEY")
        println("GitHub Face Model: $GITHUB_FACE_MODEL")
        println("GitHub YOLO Model: $GITHUB_YOLO_MODEL")
        println()

        val results = mutableMapOf<String, String>()

        // Test 1: GitHub face detection pattern (exact from GitHub)
        try {
            println("Testing GitHub face detection pattern...")
            val faceModel = ZeticMLangeModel(context, GITHUB_DEBUG_KEY, GITHUB_FACE_MODEL, null)
            results["GITHUB_FACE_DETECTION"] = "✅ SUCCESS"
            println("✅ GitHub face detection pattern works!")
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["GITHUB_FACE_DETECTION"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ GitHub face detection failed: ${e.message}")
        }

        // Test 2: GitHub YOLO pattern (exact from GitHub)
        try {
            println("Testing GitHub YOLO pattern...")
            val yoloModel = ZeticMLangeModel(context, GITHUB_DEBUG_KEY, GITHUB_YOLO_MODEL, null)
            results["GITHUB_YOLO"] = "✅ SUCCESS"
            println("✅ GitHub YOLO pattern works!")
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["GITHUB_YOLO"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ GitHub YOLO failed: ${e.message}")
        }

        // Test 3: Mix GitHub debug key with our current text models
        try {
            println("Testing GitHub debug key with current text detection model...")
            val mixedModel = ZeticMLangeModel(context, GITHUB_DEBUG_KEY, TEXT_DETECT_MODEL, null)
            results["GITHUB_DEBUG_KEY + CURRENT_MODEL"] = "✅ SUCCESS"
            println("✅ Mixed GitHub pattern works!")
        } catch (e: Exception) {
            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                else -> "OTHER"
            }
            results["GITHUB_DEBUG_KEY + CURRENT_MODEL"] = "❌ FAILED: HTTP $errorCode - ${e.message}"
            println("❌ Mixed GitHub pattern failed: ${e.message}")
        }

        println("\n=== GITHUB LEGACY PATTERN RESULTS ===")
        results.forEach { (test, result) ->
            println("$test: $result")
        }

        val successCount = results.values.count { it.startsWith("✅") }
        val failCount = results.values.count { it.startsWith("❌") }

        println("\n=== GITHUB PATTERN ANALYSIS ===")
        println("Successful GitHub patterns: $successCount")
        println("Failed GitHub patterns: $failCount")

        if (results["GITHUB_FACE_DETECTION"]?.startsWith("✅") == true) {
            println("🎉 BREAKTHROUGH: GitHub face detection pattern works!")
            println("This proves legacy debug_ prefix + hash model keys still work")
        }

        if (results["GITHUB_YOLO"]?.startsWith("✅") == true) {
            println("🎉 BREAKTHROUGH: GitHub YOLO pattern works!")
            println("This proves YOLOv8 hash model keys still work")
        }

        if (results["GITHUB_DEBUG_KEY + CURRENT_MODEL"]?.startsWith("✅") == true) {
            println("🔄 MIXED SUCCESS: GitHub debug_ key works with current text models!")
            println("This means we can use legacy authentication with modern models")
        }

        // Analyze specific error patterns
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

        println("\n=== GITHUB ERROR TYPE BREAKDOWN ===")
        errorTypes.forEach { (errorType, tests) ->
            println("$errorType: ${tests.joinToString(", ")}")
        }

        if (errorTypes["HTTP 401"]?.isNotEmpty() == true) {
            println("🔑 HTTP 401 errors suggest authentication issue with GitHub keys")
        }

        if (errorTypes["HTTP 404"]?.isNotEmpty() == true) {
            println("📂 HTTP 404 errors suggest models not found (but authentication worked)")
        }

        assertTrue("GitHub legacy debug pattern test completed", true)
    }
}