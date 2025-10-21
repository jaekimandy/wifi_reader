package com.zetic.wifireader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zetic.wifireader.ocr.ZeticMLangeOCREngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class CameraSnapshotTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testSavedCameraSnapshots() = runBlocking {
        println("=== SAVED CAMERA SNAPSHOT OCR TEST ===")

        // Initialize Zetic MLange OCR Engine
        val ocrEngine = ZeticMLangeOCREngine(context)
        val initialized = ocrEngine.initialize()

        assertTrue("Zetic MLange OCR should initialize successfully", initialized)
        println("✅ Zetic MLange OCR initialized")

        // Look for saved camera snapshots in multiple locations
        val externalFilesDir = context.getExternalFilesDir(null)
        val appPicturesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val appWifiReaderDir = File(appPicturesDir, "WiFiReader")

        // Public Pictures directory (where MainActivity actually saves snapshots)
        val publicPicturesDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "WiFiReader")

        println("🔍 Checking directories:")
        println("  📁 External files: ${externalFilesDir?.absolutePath}")
        println("  📁 App Pictures: ${appPicturesDir?.absolutePath}")
        println("  📁 App WiFiReader: ${appWifiReaderDir.absolutePath}")
        println("  📁 Public WiFiReader: ${publicPicturesDir.absolutePath}")

        val allSnapshotFiles = mutableListOf<File>()

        // Check main external files directory (old automatic snapshots)
        externalFilesDir?.listFiles { file ->
            file.name.startsWith("camera_snapshot_") && file.name.endsWith(".png")
        }?.let { allSnapshotFiles.addAll(it) }

        // Check app WiFiReader directory (old location)
        if (appWifiReaderDir.exists()) {
            appWifiReaderDir.listFiles { file ->
                file.name.startsWith("snapshot_") && file.name.endsWith(".png")
            }?.let { allSnapshotFiles.addAll(it) }
        }

        // Check public WiFiReader directory (current location used by MainActivity)
        if (publicPicturesDir.exists()) {
            publicPicturesDir.listFiles { file ->
                file.name.startsWith("snapshot_") && file.name.endsWith(".png")
            }?.let { allSnapshotFiles.addAll(it) }
        }

        val snapshotFiles = allSnapshotFiles.sortedByDescending { it.lastModified() } // Most recent first

        println("📁 Found ${snapshotFiles.size} total snapshot files across all directories")

        if (snapshotFiles.isEmpty()) {
            println("❌ No saved snapshots found. Run the main app first to capture snapshots.")
            // Still run control test
            testControlImage(ocrEngine)
            ocrEngine.release()
            return@runBlocking
        }

        // Test the most recent snapshots (handle both old and new naming conventions)
        val rawSnapshots = snapshotFiles.filter {
            it.name.contains("_raw_") || it.name.contains("raw_")
        }.take(3)
        val enhancedSnapshots = snapshotFiles.filter {
            it.name.contains("_enhanced_") || it.name.contains("enhanced_")
        }.take(3)

        println("📸 Testing ${rawSnapshots.size} raw snapshots and ${enhancedSnapshots.size} enhanced snapshots")

        // Test raw snapshots
        rawSnapshots.forEachIndexed { index, file ->
            println("\n=== TESTING RAW SNAPSHOT ${index + 1}: ${file.name} ===")
            testSnapshotFile(ocrEngine, file, "Raw snapshot ${index + 1}")
        }

        // Test enhanced snapshots
        enhancedSnapshots.forEachIndexed { index, file ->
            println("\n=== TESTING ENHANCED SNAPSHOT ${index + 1}: ${file.name} ===")
            testSnapshotFile(ocrEngine, file, "Enhanced snapshot ${index + 1}")
        }

        // Control test
        testControlImage(ocrEngine)

        // Clean up
        ocrEngine.release()
        println("\n=== TEST COMPLETED ===")
    }

    private suspend fun testSnapshotFile(ocrEngine: ZeticMLangeOCREngine, file: File, description: String) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                println("❌ $description: Failed to load image from ${file.name}")
                return
            }

            println("📸 $description: ${bitmap.width}x${bitmap.height} (${file.length()} bytes)")

            val textRegions = ocrEngine.extractText(bitmap)
            println("🔍 $description detected ${textRegions.size} text regions")

            if (textRegions.isEmpty()) {
                println("✅ $description: NO TEXT DETECTED")
            } else {
                println("✅ $description: TEXT DETECTED!")
                textRegions.forEachIndexed { index, region ->
                    println("  Text $index: '${region.text}' (confidence: ${region.confidence})")
                }
            }

        } catch (e: Exception) {
            println("❌ $description: Error testing snapshot - ${e.message}")
        }
    }

    private suspend fun testControlImage(ocrEngine: ZeticMLangeOCREngine) {
        println("\n=== TESTING SYNTHETIC IMAGE (CONTROL) ===")
        val syntheticBitmap = createTestBitmap("HELLO CAMERA TEST")
        val syntheticTextRegions = ocrEngine.extractText(syntheticBitmap)
        println("🔍 Synthetic image detected ${syntheticTextRegions.size} text regions")

        syntheticTextRegions.forEachIndexed { index, region ->
            println("Synthetic text $index: '${region.text}' (confidence: ${region.confidence})")
        }

        if (syntheticTextRegions.isEmpty()) {
            println("✅ Synthetic image: No text detected")
        } else {
            println("✅ Synthetic image: TEXT DETECTED!")
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, filename: String) {
        try {
            val file = File(context.getExternalFilesDir(null), filename)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            println("💾 Saved snapshot: ${file.absolutePath}")
        } catch (e: Exception) {
            println("❌ Failed to save snapshot: ${e.message}")
        }
    }

    private fun createTestBitmap(text: String): Bitmap {
        val width = 800
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // White background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Black text
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Center the text
        val textWidth = paint.measureText(text)
        val x = (width - textWidth) / 2
        val y = height / 2 + paint.textSize / 3

        canvas.drawText(text, x, y, paint)

        return bitmap
    }
}