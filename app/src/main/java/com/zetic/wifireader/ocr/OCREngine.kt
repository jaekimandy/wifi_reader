package com.zetic.wifireader.ocr

import android.graphics.Bitmap
import com.zetic.wifireader.model.BoundingBox
import com.zetic.wifireader.model.TextRegion

interface OCREngine {
    suspend fun initialize(): Boolean
    suspend fun extractText(bitmap: Bitmap, boundingBox: BoundingBox? = null): List<TextRegion>
    fun release()
}