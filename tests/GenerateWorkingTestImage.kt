package com.zetic.wifireader.tests

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

/**
 * Generates the exact test image that successfully worked with ML Kit OCR
 * This is the same image creation code from MLKitOCRTest.kt that detected:
 * - "SSID: TestNetwork"
 * - "Password: password123"
 * - "Security: WPA2"
 */
class GenerateWorkingTestImage {

    fun createAndSaveWorkingWiFiImage(context: Context): File {
        // Create the exact same bitmap that worked in MLKitOCRTest
        val bitmap = createWiFiTestBitmap()

        // Save to tests directory
        val testsDir = File(context.getExternalFilesDir(null)?.parent, "tests")
        if (!testsDir.exists()) {
            testsDir.mkdirs()
        }

        val file = File(testsDir, "working_wifi_test_image.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return file
    }

    private fun createWiFiTestBitmap(): Bitmap {
        // Exact same parameters as in MLKitOCRTest.kt
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Black text paint - exact same settings
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Draw WiFi credentials - exact same text and positions
        canvas.drawText("SSID: TestNetwork", 50f, 100f, paint)
        canvas.drawText("Password: password123", 50f, 200f, paint)
        canvas.drawText("Security: WPA2", 50f, 300f, paint)

        return bitmap
    }
}