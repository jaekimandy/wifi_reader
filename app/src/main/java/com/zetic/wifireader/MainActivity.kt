package com.zetic.wifireader

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zetic.wifireader.camera.CameraManager
import com.zetic.wifireader.model.WiFiCredentials
import com.zetic.wifireader.pipeline.WiFiDetectionPipeline
import com.zetic.wifireader.ui.OverlayView
import com.zetic.wifireader.ui.WiFiCredentialsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var flashButton: ImageButton
    private lateinit var snapshotButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var bottomPanel: LinearLayout
    private lateinit var credentialsRecyclerView: RecyclerView
    private lateinit var instructionOverlay: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var cameraManager: CameraManager
    private lateinit var detectionPipeline: WiFiDetectionPipeline
    private lateinit var credentialsAdapter: WiFiCredentialsAdapter

    private var isFlashOn = false
    private var detectionJob: Job? = null
    private var lastDetectionTime = 0L
    private val detectionThrottleMs = 2000L // Detect every 2 seconds

    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        initializeComponents()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        flashButton = findViewById(R.id.flashButton)
        snapshotButton = findViewById(R.id.snapshotButton)
        infoButton = findViewById(R.id.infoButton)
        statusText = findViewById(R.id.statusText)
        bottomPanel = findViewById(R.id.bottomPanel)
        credentialsRecyclerView = findViewById(R.id.credentialsRecyclerView)
        instructionOverlay = findViewById(R.id.instructionOverlay)
        progressBar = findViewById(R.id.progressBar)

        // Setup RecyclerView
        credentialsAdapter = WiFiCredentialsAdapter(this)
        credentialsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = credentialsAdapter
        }
    }

    private fun setupClickListeners() {
        flashButton.setOnClickListener {
            toggleFlash()
        }

        snapshotButton.setOnClickListener {
            takeManualSnapshot()
        }

        infoButton.setOnClickListener {
            showInfoDialog()
        }

        previewView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                cameraManager.focusOnPoint(event.x, event.y)
                overlayView.drawFocusRing(event.x, event.y)
            }
            true
        }

        instructionOverlay.setOnClickListener {
            instructionOverlay.visibility = View.GONE
        }
    }

    private fun initializeComponents() {
        cameraManager = CameraManager(this)
        detectionPipeline = WiFiDetectionPipeline(this)

        lifecycleScope.launch {
            try {
                showProgress(true)
                val initialized = detectionPipeline.initialize()
                showProgress(false)

                if (initialized) {
                    statusText.text = getString(R.string.scanning)
                } else {
                    statusText.text = "Initialization failed"
                    Toast.makeText(this@MainActivity, "Failed to initialize AI models", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showProgress(false)
                Log.e(TAG, "Initialization error: ${e.message}")
                statusText.text = "Initialization error"
            }
        }
    }

    private fun startCamera() {
        lifecycleScope.launch {
            try {
                val success = cameraManager.startCamera(
                    lifecycleOwner = this@MainActivity,
                    previewView = previewView,
                    onImageCaptured = { bitmap ->
                        processCameraFrame(bitmap)
                    }
                )

                if (success) {
                    updateFlashButton()
                    instructionOverlay.visibility = View.VISIBLE
                    Log.d(TAG, "Camera started successfully")
                } else {
                    statusText.text = "Camera initialization failed"
                    Toast.makeText(this@MainActivity, "Failed to start camera", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera start error: ${e.message}")
                statusText.text = "Camera error"
            }
        }
    }

    private fun processCameraFrame(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < detectionThrottleMs) {
            Log.v(TAG, "⏳ Detection throttled (${currentTime - lastDetectionTime}ms since last)")
            return // Throttle detection
        }

        Log.i(TAG, "📸 Processing camera frame - size: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "Detection interval: ${currentTime - lastDetectionTime}ms")

        lastDetectionTime = currentTime

        detectionJob?.cancel()
        detectionJob = lifecycleScope.launch {
            Log.i(TAG, "🚀 Starting detection job...")
            try {
                showProgress(true)
                statusText.text = getString(R.string.scanning)

                Log.d(TAG, "📊 Calling detection pipeline...")
                val startTime = System.currentTimeMillis()

                val credentials = withContext(Dispatchers.Default) {
                    detectionPipeline.detectWiFiCredentials(bitmap)
                }

                val processingTime = System.currentTimeMillis() - startTime
                Log.i(TAG, "⏱️ Detection completed in ${processingTime}ms")

                showProgress(false)

                if (credentials.isNotEmpty()) {
                    Log.i(TAG, "✅ Found ${credentials.size} WiFi credential(s)!")
                    credentials.forEachIndexed { index, cred ->
                        Log.d(TAG, "Credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
                    }
                    displayResults(credentials)
                    statusText.text = "Found ${credentials.size} WiFi credential(s)"
                } else {
                    Log.w(TAG, "❌ No credentials found in image")
                    hideResults()
                    statusText.text = getString(R.string.no_credentials_found)
                }

            } catch (e: Exception) {
                showProgress(false)
                Log.e(TAG, "❌ Detection error: ${e.message}", e)
                statusText.text = "Detection error"
            }
        }
    }

    private fun displayResults(credentials: List<WiFiCredentials>) {
        credentialsAdapter.submitList(credentials)
        bottomPanel.visibility = View.VISIBLE
        instructionOverlay.visibility = View.GONE

        // Optional: Add vibration feedback
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(100)
        } catch (e: Exception) {
            // Vibration not available
        }
    }

    private fun hideResults() {
        bottomPanel.visibility = View.GONE
        overlayView.clearDetections()
    }

    private fun toggleFlash() {
        if (cameraManager.isFlashAvailable()) {
            isFlashOn = cameraManager.toggleFlash()
            updateFlashButton()
        } else {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFlashButton() {
        val iconRes = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        flashButton.setImageResource(iconRes)
    }

    private fun showInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("WiFi Reader")
            .setMessage("Point your camera at a router label to automatically detect WiFi credentials. The app uses AI to detect router labels and extract SSID and password information.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun showPermissionDeniedMessage() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(getString(R.string.camera_permission_required))
            .setPositiveButton("Grant") { _, _ -> requestPermissions() }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun takeManualSnapshot() {
        val rawBitmap = cameraManager.getLastRawImage()
        if (rawBitmap == null) {
            Toast.makeText(this, "No camera image available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val timestamp = System.currentTimeMillis()

                // Save to public Pictures directory so you can view in gallery
                val publicPicturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val wifiReaderDir = File(publicPicturesDir, "WiFiReader")
                if (!wifiReaderDir.exists()) {
                    wifiReaderDir.mkdirs()
                }

                // Save raw camera image
                val rawFile = File(wifiReaderDir, "snapshot_raw_$timestamp.png")
                withContext(Dispatchers.IO) {
                    val rawOutput = FileOutputStream(rawFile)
                    rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, rawOutput)
                    rawOutput.flush()
                    rawOutput.close()
                }

                // Save enhanced image
                val enhancedBitmap = cameraManager.getLastEnhancedImage()
                var enhancedFile: File? = null
                if (enhancedBitmap != null) {
                    enhancedFile = File(wifiReaderDir, "snapshot_enhanced_$timestamp.png")
                    withContext(Dispatchers.IO) {
                        val enhancedOutput = FileOutputStream(enhancedFile)
                        enhancedBitmap.compress(Bitmap.CompressFormat.PNG, 100, enhancedOutput)
                        enhancedOutput.flush()
                        enhancedOutput.close()
                    }
                }

                // Test with OCR and show results - TEST BOTH PATHS
                Log.i(TAG, "🧪 Testing direct OCR call (JUnit path)...")
                val directTextRegions = detectionPipeline.getOCREngine().extractText(enhancedBitmap ?: rawBitmap)
                Log.i(TAG, "🧪 Direct OCR found ${directTextRegions.size} text regions")

                Log.i(TAG, "🧪 Testing detection pipeline call (main app path)...")
                val pipelineTextRegions = detectionPipeline.detectWiFiCredentials(enhancedBitmap ?: rawBitmap)
                Log.i(TAG, "🧪 Pipeline found ${pipelineTextRegions.size} WiFi credentials")

                val textRegions = directTextRegions

                val message = if (enhancedFile != null) {
                    "Snapshots saved:\n• Raw: ${rawFile.name}\n• Enhanced: ${enhancedFile.name}\n\nOCR detected ${textRegions.size} text regions"
                } else {
                    "Snapshot saved: ${rawFile.name}\n\nOCR detected ${textRegions.size} text regions"
                }

                Log.i(TAG, "📷 Manual snapshot - OCR found ${textRegions.size} text regions")
                textRegions.forEachIndexed { index, region ->
                    Log.i(TAG, "Text $index: '${region.text}' (confidence: ${region.confidence})")
                }

                // Notify media scanner so images appear in gallery
                val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = android.net.Uri.fromFile(rawFile)
                sendBroadcast(mediaScanIntent)

                if (enhancedFile != null) {
                    val enhancedScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    enhancedScanIntent.data = android.net.Uri.fromFile(enhancedFile)
                    sendBroadcast(enhancedScanIntent)
                }

                Toast.makeText(this@MainActivity, "$message\n\n📱 Check your Gallery app!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save manual snapshot: ${e.message}")
                Toast.makeText(this@MainActivity, "Failed to save snapshot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionJob?.cancel()
        cameraManager.release()
        detectionPipeline.release()
    }

    override fun onPause() {
        super.onPause()
        detectionJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            lastDetectionTime = 0L // Reset throttle when resuming
        }
    }
}