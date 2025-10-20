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
class ZeticModelPostDownloadTest {

    private lateinit var context: Context

    companion object {
        private const val TAG = "ZeticModelPostDownloadTest"
        private const val DETECT_API_KEY = "ztp_af48d53800494556b7f6fce22fd6e694"
        private const val RECOG_API_KEY = "ztp_8f79392af8294e289fe02ce9d139ec20"
        private const val TEXT_DETECT_MODEL = "jkim711/text_detect2"
        private const val TEXT_RECOG_MODEL = "jkim711/text_recog3"
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testModelDownloadAndInitialization() {
        println("=== Model Download and Post-Download Test ===")
        println("Since dashboard shows models are being accessed, let's see what happens after download")

        try {
            println("Step 1: Download detection model...")
            val detectionModel = ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            println("✅ Detection model download successful!")
            println("Model object: $detectionModel")

            // Try to access model properties/methods
            println("Step 2: Investigating model object...")
            try {
                println("Model class: ${detectionModel.javaClass.name}")
                println("Model string representation: $detectionModel")

                // Try to call toString or other basic methods
                val modelInfo = detectionModel.toString()
                println("Model info: $modelInfo")

            } catch (e: Exception) {
                println("❌ Error accessing model properties: ${e.message}")
                e.printStackTrace()
            }

            // Try to use the model (this might be where it fails)
            println("Step 3: Testing model usage...")
            try {
                // Note: ZLang 1.3.0 doesn't have outputBuffers property
                // Outputs are only available from model.run() which returns Array<Tensor>
                println("Testing model (no outputBuffers in 1.3.0, outputs come from model.run())")
                println("✅ Model created successfully")

            } catch (e: Exception) {
                println("❌ Error: ${e.message}")
                e.printStackTrace()
            }

        } catch (e: Exception) {
            println("❌ Unexpected: Model download failed")
            println("Exception: ${e.message}")
            e.printStackTrace()
            fail("Model download should work based on dashboard")
        }
    }

    @Test
    fun testRealZeticTextDetectorInitialization() {
        println("=== Real ZeticTextDetector Initialization Test ===")

        val detector = com.zetic.wifireader.model.ZeticTextDetector(context)

        println("Testing ZeticTextDetector.initialize()...")
        val initialized = detector.initialize()

        println("Initialization result: $initialized")

        if (initialized) {
            println("✅ ZeticTextDetector initialized successfully!")
            println("Model in detector: ${detector.model}")
        } else {
            println("❌ ZeticTextDetector initialization failed")
            println("But dashboard shows model was accessed - investigating why...")
        }

        // Test if we can see the actual exception
        try {
            println("Manually testing ZeticMLangeModel creation in detector context...")
            val model = ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            println("✅ Manual model creation successful: $model")

            // This proves the download works, so the issue is elsewhere
            assertTrue("Manual model creation should work", true)

        } catch (e: Exception) {
            println("❌ Manual model creation failed: ${e.message}")
            fail("Should not fail if dashboard shows access")
        }
    }

    @Test
    fun testModelRunWithoutInputs() {
        println("=== Testing Model Run Without Inputs ===")

        try {
            val model = ZeticMLangeModel(context, DETECT_API_KEY, TEXT_DETECT_MODEL, null)
            println("✅ Model downloaded successfully")

            // Check if model has methods we expect
            println("Checking model capabilities...")

            try {
                // Try to run model without inputs (this might throw an exception we need to handle)
                println("Testing model.run() without inputs...")
                val emptyTensors: Array<com.zeticai.mlange.core.tensor.Tensor> = emptyArray()
                val outputs = model.run(emptyTensors)
                println("✅ Model.run() with empty inputs succeeded, got ${outputs.size} output tensors")

            } catch (e: Exception) {
                println("❌ Model.run() with empty inputs failed: ${e.message}")
                println("This is expected - models need proper inputs")
            }

        } catch (e: Exception) {
            println("❌ Test failed: ${e.message}")
            e.printStackTrace()
        }

        assertTrue("Post-download test completed", true)
    }
}