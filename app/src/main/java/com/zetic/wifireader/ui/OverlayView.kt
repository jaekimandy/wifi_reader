package com.zetic.wifireader.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.DetectionResult

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val backgroundPaint = Paint().apply {
        color = Color.argb(128, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private var detections = listOf<DetectionResult>()
    private var scaleFactorX = 1f
    private var scaleFactorY = 1f

    fun updateDetections(detections: List<DetectionResult>, imageWidth: Int, imageHeight: Int) {
        this.detections = detections

        // Calculate scale factors to map detection coordinates to view coordinates
        scaleFactorX = width.toFloat() / imageWidth
        scaleFactorY = height.toFloat() / imageHeight

        invalidate()
    }

    fun clearDetections() {
        detections = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (detection in detections) {
            drawDetection(canvas, detection)
        }
    }

    private fun drawDetection(canvas: Canvas, detection: DetectionResult) {
        val box = detection.boundingBox

        // Scale the bounding box coordinates to view coordinates
        val left = box.x * scaleFactorX
        val top = box.y * scaleFactorY
        val right = left + (box.width * scaleFactorX)
        val bottom = top + (box.height * scaleFactorY)

        val rect = RectF(left, top, right, bottom)

        // Draw semi-transparent background
        canvas.drawRect(rect, backgroundPaint)

        // Draw bounding box
        canvas.drawRect(rect, paint)

        // Draw confidence text
        val confidenceText = String.format("%.1f%%", detection.confidence * 100)
        val textBounds = Rect()
        textPaint.getTextBounds(confidenceText, 0, confidenceText.length, textBounds)

        val textX = left + 8f
        val textY = top - 8f

        canvas.drawText(confidenceText, textX, textY, textPaint)

        // Draw router label indicator
        val labelText = "Router Label"
        canvas.drawText(labelText, textX, textY + textBounds.height() + 8f, textPaint)
    }

    fun drawFocusRing(x: Float, y: Float) {
        // Draw focus ring at specified coordinates
        val focusPaint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        post {
            val canvas = Canvas()
            canvas.drawCircle(x, y, 50f, focusPaint)
            invalidate()
        }
    }
}