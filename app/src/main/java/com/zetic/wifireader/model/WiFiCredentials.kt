package com.zetic.wifireader.model

data class WiFiCredentials(
    val ssid: String,
    val password: String,
    val confidence: Float = 0.0f
)

data class DetectionResult(
    val boundingBox: BoundingBox,
    val confidence: Float,
    val classId: Int
)

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class TextRegion(
    val boundingBox: BoundingBox,
    val text: String,
    val confidence: Float
)