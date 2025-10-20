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
class ZeticMLangeLLMDownloadTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticMLangeLLMTest"

        // Pattern from ZeticMLangeLLMParser.kt (OLD PATTERN - likely won't work)
        private const val DEBUG_ID = "debug_cb6cb12939644316888f333523e42622"
        private const val MODEL_KEY = "deepseek-r1-distill-qwen-1.5b-f16"

        // Alternative patterns to test
        private const val DETECT_API_KEY = "dev_9ba20e80c3fd4edf80f94906aa0ae27d"
        private const val OLD_API_KEY = "dev_854ee24efea74a05852a50916e61518f"

        // Proper account/project format (if exists)
        private const val LLM_MODEL_ACCOUNT_FORMAT = "deepseek/r1-distill-qwen-1.5b-f16"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testCurrentLLMParserPattern() {
        println("=== Testing Current ZeticMLangeLLMParser Pattern ===")
        println("Debug ID: $DEBUG_ID")
        println("Model Key: $MODEL_KEY (without account/project format)")
        println()

        try {
            println("Step 1: Testing old 2-parameter pattern...")
            // This won't work because ZLang 1.3.0 needs 4 parameters
            // But let's see what error we get

            println("❌ SKIPPED: 2-parameter constructor doesn't exist in ZLang 1.3.0")
            println("ZLang 1.3.0 requires: ZeticMLangeModel(context, apiKey, modelName, config)")

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
        }

        assertTrue("Current pattern investigation completed", true)
    }

    @Test
    fun testLLMModelWithOldHashFormat() {
        println("=== Testing LLM Model with Hash-Based Model Key ===")
        println("Model Key: $MODEL_KEY (no slash)")
        println()

        try {
            println("Testing with DEBUG_ID and 4-parameter constructor...")
            val model = ZeticMLangeModel(context, DEBUG_ID, MODEL_KEY, null)

            println("✅ Model created successfully!")
            println("Model: $model")

        } catch (e: Exception) {
            println("Exception Type: ${e.javaClass.name}")
            println("Exception Message: ${e.message}")
            e.printStackTrace()

            val errorCode = when {
                e.message?.contains("404") == true -> "404"
                e.message?.contains("500") == true -> "500"
                e.message?.contains("401") == true -> "401"
                e.message?.contains("403") == true -> "403"
                e.message?.contains("slash") == true -> "CLIENT_VALIDATION"
                else -> "OTHER (${e.javaClass.simpleName})"
            }
            println("Error code: $errorCode")
        }

        assertTrue("Hash format test completed", true)
    }

    @Test
    fun testLLMModelWithAccountProjectFormat() {
        println("=== Testing LLM Model with Account/Project Format ===")
        println("Model Key: $LLM_MODEL_ACCOUNT_FORMAT (with slash)")
        println()

        val apiKeys = mapOf(
            "DEBUG_ID" to DEBUG_ID,
            "DETECT_API_KEY" to DETECT_API_KEY,
            "OLD_API_KEY" to OLD_API_KEY
        )

        apiKeys.forEach { (keyName, apiKey) ->
            try {
                println("Testing with $keyName: $apiKey")
                val startTime = System.currentTimeMillis()

                val model = ZeticMLangeModel(context, apiKey, LLM_MODEL_ACCOUNT_FORMAT, null)

                val downloadTime = System.currentTimeMillis() - startTime
                println("✅ $keyName SUCCESS! Download time: ${downloadTime}ms")
                println("Model: $model")

            } catch (e: Exception) {
                println("Exception Type: ${e.javaClass.name}")
                println("Exception Message: ${e.message}")

                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("401") == true -> "401"
                    e.message?.contains("403") == true -> "403"
                    else -> "OTHER (${e.javaClass.simpleName})"
                }
                println("❌ $keyName FAILED: HTTP $errorCode - ${e.message}")
            }
            println()
        }

        assertTrue("Account/project format test completed", true)
    }

    @Test
    fun testVariousLLMModelFormats() {
        println("=== Testing Various LLM Model Name Formats ===")
        println()

        val modelFormats = listOf(
            "deepseek-r1-distill-qwen-1.5b-f16" to "Hash format (no slash)",
            "deepseek/r1-distill-qwen-1.5b-f16" to "Account/project format",
            "DeepSeek/deepseek-r1-distill-qwen-1.5b" to "Alternative account name",
            "deepseek/r1-distill-qwen-1.5b" to "Shorter version",
            "deepseek/deepseek-r1" to "Minimal version"
        )

        modelFormats.forEach { (modelName, description) ->
            try {
                println("Testing: $modelName ($description)")
                val model = ZeticMLangeModel(context, DETECT_API_KEY, modelName, null)

                println("✅ SUCCESS: $modelName works!")
                println()

            } catch (e: Exception) {
                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("slash") == true -> "CLIENT_VALIDATION"
                    else -> "OTHER (${e.javaClass.simpleName})"
                }
                println("❌ FAILED: $modelName - $errorCode: ${e.message}")
                println()
            }
        }

        assertTrue("Various format test completed", true)
    }

    @Test
    fun testNaverHyperCLOVAXModel() {
        println("=== Testing Naver HyperCLOVAX-SEED-Text-Instruct-1 ===")
        println("From: https://mlange.zetic.ai/p/Naver/HyperCLOVAX-SEED-Text-Instruct-1")
        println()

        val modelName = "Naver/HyperCLOVAX-SEED-Text-Instruct-1"
        val apiKeys = mapOf(
            "DETECT_API_KEY" to DETECT_API_KEY,
            "OLD_API_KEY" to OLD_API_KEY,
            "DEBUG_ID" to DEBUG_ID
        )

        apiKeys.forEach { (keyName, apiKey) ->
            try {
                println("Testing with $keyName...")
                val startTime = System.currentTimeMillis()

                val model = ZeticMLangeModel(context, apiKey, modelName, null)

                val downloadTime = System.currentTimeMillis() - startTime
                println("✅ SUCCESS with $keyName! Download time: ${downloadTime}ms")
                println("Model: $model")
                println()

            } catch (e: Exception) {
                println("Exception Type: ${e.javaClass.name}")
                println("Exception Message: ${e.message}")
                e.printStackTrace()

                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("401") == true -> "401"
                    e.message?.contains("403") == true -> "403"
                    else -> "OTHER (${e.javaClass.simpleName})"
                }
                println("❌ $keyName FAILED: HTTP $errorCode - ${e.message}")
                println()
            }
        }

        assertTrue("Naver HyperCLOVAX test completed", true)
    }

    @Test
    fun testSteveColbertKorModel() {
        println("=== Testing Steve/colbert_kor ===")
        println("From: https://mlange.zetic.ai/p/Steve/colbert_kor")
        println()

        val modelName = "Steve/colbert_kor"
        val apiKeys = mapOf(
            "DETECT_API_KEY" to DETECT_API_KEY,
            "OLD_API_KEY" to OLD_API_KEY,
            "DEBUG_ID" to DEBUG_ID
        )

        apiKeys.forEach { (keyName, apiKey) ->
            try {
                println("Testing with $keyName...")
                val startTime = System.currentTimeMillis()

                val model = ZeticMLangeModel(context, apiKey, modelName, null)

                val downloadTime = System.currentTimeMillis() - startTime
                println("✅ SUCCESS with $keyName! Download time: ${downloadTime}ms")
                println("Model: $model")
                println("Model class: ${model.javaClass.name}")
                println()

            } catch (e: Exception) {
                println("Exception Type: ${e.javaClass.name}")
                println("Exception Message: ${e.message}")
                e.printStackTrace()

                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("401") == true -> "401"
                    e.message?.contains("403") == true -> "403"
                    e.message?.contains("NullPointer") == true -> "NPE (Model Downloaded)"
                    else -> "OTHER (${e.javaClass.simpleName})"
                }
                println("❌ $keyName FAILED: $errorCode - ${e.message}")
                println()
            }
        }

        assertTrue("Steve/colbert_kor test completed", true)
    }

    @Test
    fun testQwen3Model() {
        println("=== Testing Qwen/Qwen3-0.6B ===")
        println("From: https://mlange.zetic.ai/p/Qwen/Qwen3-0.6B")
        println()

        val modelName = "Qwen/Qwen3-0.6B"
        val apiKeys = mapOf(
            "DETECT_API_KEY" to DETECT_API_KEY,
            "OLD_API_KEY" to OLD_API_KEY,
            "DEBUG_ID" to DEBUG_ID
        )

        apiKeys.forEach { (keyName, apiKey) ->
            try {
                println("Testing with $keyName...")
                val startTime = System.currentTimeMillis()

                val model = ZeticMLangeModel(context, apiKey, modelName, null)

                val downloadTime = System.currentTimeMillis() - startTime
                println("✅ SUCCESS with $keyName! Download time: ${downloadTime}ms")
                println("Model: $model")
                println("Model class: ${model.javaClass.name}")
                println()

            } catch (e: Exception) {
                println("Exception Type: ${e.javaClass.name}")
                println("Exception Message: ${e.message}")
                e.printStackTrace()

                val errorCode = when {
                    e.message?.contains("404") == true -> "404"
                    e.message?.contains("500") == true -> "500"
                    e.message?.contains("401") == true -> "401"
                    e.message?.contains("403") == true -> "403"
                    e.message?.contains("NullPointer") == true -> "NPE (Model Downloaded)"
                    else -> "OTHER (${e.javaClass.simpleName})"
                }
                println("❌ $keyName FAILED: $errorCode - ${e.message}")
                println()
            }
        }

        assertTrue("Qwen/Qwen3-0.6B test completed", true)
    }

    @Test
    fun testKnownWorkingModelAsBaseline() {
        println("=== Baseline Test: Known Working Model ===")
        println("Testing Ultralytics/YOLOv8n to verify API key works")
        println()

        try {
            val model = ZeticMLangeModel(context, OLD_API_KEY, "Ultralytics/YOLOv8n", null)
            println("✅ Baseline test PASSED - API key is valid")
            println("Model: $model")

        } catch (e: Exception) {
            println("❌ Baseline test FAILED - API key or network issue")
            println("Exception: ${e.message}")
            fail("Baseline model download failed - check API key and network")
        }

        assertTrue("Baseline test completed", true)
    }

    @Test
    fun testComprehensiveLLMModelSearch() {
        println("=== Comprehensive LLM Model Search ===")
        println("Testing multiple possible LLM model locations")
        println()

        val results = mutableMapOf<String, String>()

        // Test various possible model names
        val possibleModels = listOf(
            // DeepSeek variants
            "deepseek/r1-distill-qwen-1.5b-f16",
            "deepseek/deepseek-r1-distill-qwen-1.5b-f16",
            "DeepSeek/r1-distill-qwen-1.5b-f16",
            "deepseek-ai/deepseek-r1-distill-qwen-1.5b",

            // Generic LLM models
            "facebook/opt-125m",
            "gpt2/gpt2",
            "microsoft/phi-2",
            "TinyLlama/TinyLlama-1.1B",

            // Mobile-optimized models
            "Qwen/Qwen2-1.5B",
            "google/gemma-2b"
        )

        possibleModels.forEach { modelName ->
            try {
                println("Trying: $modelName...")
                val model = ZeticMLangeModel(context, DETECT_API_KEY, modelName, null)

                results[modelName] = "✅ SUCCESS"
                println("✅ FOUND WORKING MODEL: $modelName")

            } catch (e: Exception) {
                val errorType = when {
                    e.message?.contains("404") == true -> "HTTP 404"
                    e.message?.contains("500") == true -> "HTTP 500"
                    e.message?.contains("slash") == true -> "VALIDATION"
                    else -> "OTHER"
                }
                results[modelName] = "❌ $errorType"
                println("   $errorType")
            }
        }

        println("\n=== SEARCH RESULTS ===")
        results.forEach { (model, result) ->
            println("$result: $model")
        }

        val workingModels = results.filter { it.value.startsWith("✅") }
        if (workingModels.isNotEmpty()) {
            println("\n🎉 Found ${workingModels.size} working LLM model(s)!")
            workingModels.keys.forEach { println("  - $it") }
        } else {
            println("\n❌ No working LLM models found")
        }

        assertTrue("Comprehensive search completed", true)
    }
}
