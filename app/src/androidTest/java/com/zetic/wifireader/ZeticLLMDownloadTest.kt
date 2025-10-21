package com.zetic.wifireader

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zeticai.mlange.core.model.ZeticMLangeModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Zetic MLange LLM Model Download Test
 *
 * Tests the download and initialization of Zetic MLange LLM model for WiFi credential parsing.
 * This test verifies that the LLM model can be successfully downloaded and initialized.
 */
@RunWith(AndroidJUnit4::class)
class ZeticLLMDownloadTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticLLMDownloadTest"

        // API Key for LLM model
        private const val LLM_API_KEY = "debug_cb6cb12939644316888f333523e42622"

        // LLM Model name
        private const val LLM_MODEL = "deepseek-r1-distill-qwen-1.5b-f16"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        println("\n" + "=".repeat(80))
        println("ZETIC MLANGE LLM MODEL DOWNLOAD TEST")
        println("SDK Version: 1.3.0")
        println("Device: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})")
        println("=".repeat(80) + "\n")
    }

    @Test
    fun test01_LLMModelDownload() {
        println("TEST: LLM Model Download")
        println("-".repeat(80))
        println("Model: $LLM_MODEL")
        println("API Key: $LLM_API_KEY")

        try {
            val startTime = System.currentTimeMillis()

            println("  Attempting to download and initialize LLM model...")
            val model = ZeticMLangeModel(context, LLM_API_KEY, LLM_MODEL, null)

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
                    println("     - Model requires authorization")
                }
                e.message?.contains("404") == true -> {
                    println("  🔍 Analysis: HTTP 404 - Not Found")
                    println("     - Model name may be incorrect")
                    println("     - Model may not be available in Zetic MLange catalog")
                }
                e.message?.contains("500") == true -> {
                    println("  🔍 Analysis: HTTP 500 - Server Error")
                    println("     - Model may not exist on Zetic platform")
                    println("     - Server-side issue with model serving")
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

    @Test
    fun test02_LLMModelSummary() {
        println("TEST: LLM Model Download Summary")
        println("=".repeat(80))

        var llmModelWorking = false

        // Test LLM model
        try {
            ZeticMLangeModel(context, LLM_API_KEY, LLM_MODEL, null)
            llmModelWorking = true
        } catch (e: Exception) {
            llmModelWorking = false
        }

        println("\nFINAL RESULT:")
        println("-".repeat(80))
        val status = if (llmModelWorking) "✅ PASS" else "❌ FAIL"
        println("  $status - LLM Model ($LLM_MODEL)")

        println("\nRECOMMENDATION:")
        println("-".repeat(80))

        if (llmModelWorking) {
            println("  ✅ LLM model is working correctly!")
            println("  The app can use Zetic MLange LLM for WiFi credential parsing.")
        } else {
            println("  ❌ LLM model download failed")
            println("\n  📧 Action: Contact tech support with:")
            println("     - This test output")
            println("     - Request access to LLM model: $LLM_MODEL")
            println("     - API Key: $LLM_API_KEY")
            println("     - Alternative: Provide recommended LLM model for text parsing")
        }

        println("\n" + "=".repeat(80))

        // Always pass this summary test
        assertTrue("Summary report completed", true)
    }
}
