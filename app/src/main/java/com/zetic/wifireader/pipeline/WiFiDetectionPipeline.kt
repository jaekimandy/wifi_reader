package com.zetic.wifireader.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zetic.wifireader.ml.ZeticMLangeDetector
import com.zetic.wifireader.model.DetectionResult
import com.zetic.wifireader.model.TextRegion
import com.zetic.wifireader.model.WiFiCredentials
import com.zetic.wifireader.ocr.PaddleOCREngine
import com.zetic.wifireader.ocr.OCREngine
import com.zetic.wifireader.llm.ZeticMLangeLLMParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WiFiDetectionPipeline(private val context: Context) {

    private val zeticDetector = ZeticMLangeDetector(context)
    private val ocrEngine: OCREngine = PaddleOCREngine(context)
    private val llmParser = ZeticMLangeLLMParser(context)

    companion object {
        private const val TAG = "WiFiDetectionPipeline"
    }

    fun getOCREngine(): OCREngine = ocrEngine

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "🚀 Initializing WiFi Detection Pipeline...")
        try {
            Log.d(TAG, "📊 Initializing Zetic detector...")
            val detectorInit = zeticDetector.initialize()
            Log.d(TAG, "Zetic detector init: $detectorInit")

            Log.d(TAG, "📄 Initializing OCR engine...")
            val ocrInit = ocrEngine.initialize()
            Log.d(TAG, "OCR engine init: $ocrInit")

            Log.d(TAG, "🤖 Initializing LLM parser...")
            val llmInit = llmParser.initialize()
            Log.d(TAG, "LLM parser init: $llmInit")

            if (detectorInit && ocrInit && llmInit) {
                Log.i(TAG, "✅ WiFi detection pipeline initialized successfully")
                true
            } else {
                Log.e(TAG, "❌ Failed to initialize detection pipeline - Detector: $detectorInit, OCR: $ocrInit, LLM: $llmInit")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Pipeline initialization failed: ${e.message}", e)
            false
        }
    }

    suspend fun detectWiFiCredentials(bitmap: Bitmap): List<WiFiCredentials> = withContext(Dispatchers.Default) {
        Log.i(TAG, "🔍 Starting WiFi credential detection...")
        Log.d(TAG, "Input image: ${bitmap.width}x${bitmap.height}")

        try {
            // Step 1: Detect router labels using Zetic.MLange YOLOv8
            Log.d(TAG, "📊 Step 1: Running Zetic YOLO detection...")
            val routerDetections = zeticDetector.detectRouterLabels(bitmap)
            Log.i(TAG, "✅ Detected ${routerDetections.size} router labels")

            if (routerDetections.isEmpty()) {
                Log.w(TAG, "❌ No router labels detected - returning empty list")
                return@withContext emptyList()
            }

            val allCredentials = mutableListOf<WiFiCredentials>()

            // Step 2: Extract text from each detected router label
            Log.d(TAG, "📄 Step 2: Extracting text from detected regions...")
            for ((index, detection) in routerDetections.withIndex()) {
                Log.d(TAG, "Processing detection $index: ${detection.boundingBox} (confidence: ${detection.confidence})")

                val textRegions = ocrEngine.extractText(bitmap, detection.boundingBox)
                Log.d(TAG, "OCR extracted ${textRegions.size} text regions from detection $index")

                textRegions.forEachIndexed { textIndex, region ->
                    Log.v(TAG, "Text region $textIndex: '${region.text}' (confidence: ${region.confidence})")
                }

                // Step 3: Parse text to find WiFi credentials using LLM
                Log.d(TAG, "🤖 Step 3: Parsing text for WiFi credentials using LLM...")
                val credentials = llmParser.parseWiFiCredentials(textRegions)
                Log.d(TAG, "LLM parsed ${credentials.size} credentials from detection $index")

                credentials.forEachIndexed { credIndex, cred ->
                    Log.d(TAG, "Credential $credIndex: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
                }

                allCredentials.addAll(credentials)
            }

            // Step 4: Remove duplicates and sort by confidence
            Log.d(TAG, "🔧 Step 4: Processing ${allCredentials.size} total credentials...")
            val uniqueCredentials = removeDuplicateCredentials(allCredentials)
            Log.i(TAG, "✅ Found ${uniqueCredentials.size} unique WiFi credentials")

            uniqueCredentials.forEachIndexed { index, cred ->
                Log.i(TAG, "Final credential $index: SSID='${cred.ssid}', Password='${cred.password}', Confidence=${cred.confidence}")
            }

            uniqueCredentials

        } catch (e: Exception) {
            Log.e(TAG, "❌ WiFi detection failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun removeDuplicateCredentials(credentials: List<WiFiCredentials>): List<WiFiCredentials> {
        return credentials
            .groupBy { it.ssid }
            .map { (_, group) ->
                // Keep the credential with highest confidence for each SSID
                group.maxByOrNull { it.confidence } ?: group.first()
            }
            .sortedByDescending { it.confidence }
    }

    fun release() {
        zeticDetector.release()
        ocrEngine.release()
        llmParser.release()
    }
}

class WiFiTextParser {

    companion object {
        private const val TAG = "WiFiTextParser"

        // Common patterns for WiFi credentials on router labels
        private val SSID_PATTERNS = listOf(
            Regex("""(?i)network\s+name\s*\(ssid\)[:=\s]*([^\s,\n]+)"""),
            Regex("""(?i)(?:ssid|network\s*name|wifi\s*name)[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)network[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)wifi[:=\s]*([^\s\n]+)""")
        )

        private val PASSWORD_PATTERNS = listOf(
            Regex("""(?i)network\s+key\s*\(password\)[:=\s]*([^\s,\n!]+)"""),
            Regex("""(?i)(?:password|pwd|pass|key|passphrase)[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)wpa\s*key[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)security\s*key[:=\s]*([^\s\n]+)"""),
            Regex("""(?i)access\s*key[:=\s]*([^\s\n]+)""")
        )

        // Patterns for common router label formats
        private val ROUTER_LABEL_PATTERNS = listOf(
            // Format: "Network Name (SSID): MyWiFi_5G" and "Network Key (Password): SecurePass123!"
            Regex("""(?i)network\s+name\s*\(ssid\)[:=\s]*([^\s,\n]+).*?network\s+key\s*\(password\)[:=\s]*([^\s,\n!]+)"""),
            // Format: "SSID: NetworkName, Password: Password123"
            Regex("""(?i)ssid[:=\s]*([^\s,\n]+).*?password[:=\s]*([^\s,\n]+)"""),
            // Format: "Network: NetworkName, Key: Password123"
            Regex("""(?i)network[:=\s]*([^\s,\n]+).*?key[:=\s]*([^\s,\n]+)"""),
            // Format: "WiFi: NetworkName, PWD: Password123"
            Regex("""(?i)wifi[:=\s]*([^\s,\n]+).*?pwd[:=\s]*([^\s,\n]+)""")
        )
    }

    fun parseWiFiCredentials(textRegions: List<TextRegion>): List<WiFiCredentials> {
        val credentials = mutableListOf<WiFiCredentials>()

        // Combine all text for comprehensive parsing
        val combinedText = textRegions.joinToString(" ") { it.text }
        Log.i(TAG, "[WiFiParser] 🔍 Parsing combined text: '$combinedText'")
        Log.d(TAG, "[WiFiParser] Number of text regions: ${textRegions.size}")

        // Try to find complete SSID/Password pairs first
        Log.d(TAG, "[WiFiParser] Trying ${ROUTER_LABEL_PATTERNS.size} router label patterns...")
        for ((patternIndex, pattern) in ROUTER_LABEL_PATTERNS.withIndex()) {
            Log.v(TAG, "[WiFiParser] Testing pattern $patternIndex: ${pattern.pattern}")
            val matches = pattern.findAll(combinedText)
            for (match in matches) {
                Log.d(TAG, "[WiFiParser] Pattern $patternIndex matched: ${match.value}")
                if (match.groupValues.size >= 3) {
                    val ssid = cleanText(match.groupValues[1])
                    val password = cleanText(match.groupValues[2])
                    Log.d(TAG, "[WiFiParser] Extracted SSID='$ssid', Password='$password'")

                    if (isValidSSID(ssid) && isValidPassword(password)) {
                        credentials.add(
                            WiFiCredentials(
                                ssid = ssid,
                                password = password,
                                confidence = calculateConfidence(textRegions)
                            )
                        )
                        Log.i(TAG, "[WiFiParser] ✅ Valid credential found: SSID='$ssid'")
                    } else {
                        Log.d(TAG, "[WiFiParser] ❌ Invalid credential: SSID valid=${isValidSSID(ssid)}, Password valid=${isValidPassword(password)}")
                    }
                }
            }
        }

        // If no complete pairs found, try to find individual SSID and password
        if (credentials.isEmpty()) {
            Log.d(TAG, "[WiFiParser] No complete pairs found, trying individual SSID/password search...")
            val ssid = findSSID(combinedText)
            val password = findPassword(combinedText)
            Log.d(TAG, "[WiFiParser] Individual search: SSID='$ssid', Password='$password'")

            if (ssid.isNotEmpty() && password.isNotEmpty()) {
                credentials.add(
                    WiFiCredentials(
                        ssid = ssid,
                        password = password,
                        confidence = calculateConfidence(textRegions) * 0.8f // Lower confidence for separate matches
                    )
                )
                Log.i(TAG, "[WiFiParser] ✅ Individual credential found: SSID='$ssid'")
            } else {
                Log.d(TAG, "[WiFiParser] ❌ No individual SSID/password found")
            }
        }

        Log.i(TAG, "[WiFiParser] 📝 Final result: ${credentials.size} credentials found")
        return credentials
    }

    private fun findSSID(text: String): String {
        for (pattern in SSID_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                val ssid = cleanText(match.groupValues[1])
                if (isValidSSID(ssid)) {
                    return ssid
                }
            }
        }
        return ""
    }

    private fun findPassword(text: String): String {
        for (pattern in PASSWORD_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                val password = cleanText(match.groupValues[1])
                if (isValidPassword(password)) {
                    return password
                }
            }
        }
        return ""
    }

    private fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("""[^\w\-._@#$%&*+=!?/\\:]"""), "")
            .replace(Regex("""\s+"""), "")
    }

    private fun isValidSSID(ssid: String): Boolean {
        return ssid.length in 1..32 &&
               ssid.isNotBlank() &&
               !ssid.matches(Regex("""^\d+$""")) && // Not just numbers
               !ssid.contains(Regex("""[<>"]""")) // No invalid characters
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
               password.isNotBlank() &&
               !password.matches(Regex("""^\d{1,7}$""")) && // Not just short numbers
               !password.contains(Regex("""[<>"]""")) // No invalid characters
    }

    private fun calculateConfidence(textRegions: List<TextRegion>): Float {
        if (textRegions.isEmpty()) return 0f

        val avgConfidence = textRegions.map { it.confidence }.average().toFloat()

        // Boost confidence if we have multiple consistent text regions
        val consistencyBonus = if (textRegions.size > 1) 0.1f else 0f

        return minOf(1.0f, avgConfidence + consistencyBonus)
    }
}