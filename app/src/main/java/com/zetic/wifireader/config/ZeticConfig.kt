package com.zetic.wifireader.config

object ZeticConfig {

    // Zetic MLange Configuration
    // Get these values from https://mlange.zetic.ai/

    /**
     * Personal Key from Zetic MLange portal
     * Using the working key from Zetic MLange apps repository
     * Source: https://mlange.zetic.ai/p/Ultralytics/YOLOv8n
     */
    const val PERSONAL_KEY = "dev_854ee24efea74a05852a50916e61518f"

    /**
     * Model configuration for YOLOv8 Detection
     * Using the working Ultralytics YOLOv8n model for initial testing
     * Can be changed to custom router detection model later
     */
    object RouterDetection {
        const val MODEL_NAME = "Ultralytics/YOLOv8n"
        const val INPUT_SIZE = 640
        const val NUM_CLASSES = 1 // router label class
        const val CONFIDENCE_THRESHOLD = 0.5f
        const val IOU_THRESHOLD = 0.4f

        // Model input/output specifications
        const val INPUT_CHANNELS = 3 // RGB
        const val OUTPUT_BOXES = 8400 // YOLOv8 default
        const val OUTPUT_FEATURES = 5 // 4 bbox coords + 1 class confidence
    }

    /**
     * Performance settings
     */
    object Performance {
        const val DETECTION_THROTTLE_MS = 2000L // Process every 2 seconds
        const val USE_GPU_ACCELERATION = true
        const val MAX_DETECTIONS_PER_FRAME = 5
    }

    /**
     * Validation function to check if configuration is complete
     */
    fun isConfigured(): Boolean {
        return PERSONAL_KEY != "YOUR_PERSONAL_KEY_HERE" && PERSONAL_KEY.isNotBlank()
    }

    /**
     * Get configuration status message
     */
    fun getConfigurationMessage(): String {
        return if (isConfigured()) {
            "Zetic MLange configured successfully"
        } else {
            "Please set your PERSONAL_KEY in ZeticConfig.kt. Get it from https://mlange.zetic.ai/"
        }
    }
}