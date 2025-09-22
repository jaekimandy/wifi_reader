package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class SaveWorkingImageTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun saveWorkingWiFiTestImage() {
        println("=== SAVING WORKING WIFI TEST IMAGE ===")

        // Create the exact same bitmap that worked in MLKitOCRTest
        val bitmap = createWiFiTestBitmap()

        // Save to external storage in a tests directory
        val externalDir = context.getExternalFilesDir(null)
        val testsDir = File(externalDir?.parentFile, "tests")
        if (!testsDir.exists()) {
            testsDir.mkdirs()
        }

        val file = File(testsDir, "working_wifi_test_image.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        println("✅ Saved working WiFi test image to: ${file.absolutePath}")
        println("📸 Image size: ${bitmap.width}x${bitmap.height}")
        println("📁 You can find this image at: ${file.absolutePath}")
    }

    private fun createWiFiTestBitmap(): Bitmap {
        // Exact same parameters as in MLKitOCRTest.kt that worked successfully
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Black text paint - exact same settings that worked
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Draw WiFi credentials - exact same text and positions that ML Kit detected
        canvas.drawText("SSID: TestNetwork", 50f, 100f, paint)
        canvas.drawText("Password: password123", 50f, 200f, paint)
        canvas.drawText("Security: WPA2", 50f, 300f, paint)

        return bitmap
    }
}