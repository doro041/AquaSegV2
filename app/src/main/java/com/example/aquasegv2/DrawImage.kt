package com.example.aquasegv2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import androidx.core.content.ContextCompat

class DrawImages(private val context: Context) {

    private val boxColors = listOf(
        R.color.overlay_blue,
        R.color.overlay_red,
        R.color.overlay_green
    )

    // You can adjust these offset values to fix alignment issues
    private val xOffset = 0
    private val yOffset = -30

    fun invoke(results: List<SegmentationResult>): Bitmap {
        if (results.isEmpty()) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val width = results.first().mask[0].size
        val height = results.first().mask.size
        val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combined)

        results.forEach { result ->
            val colorIndex = result.box.cls % boxColors.size
            val colorResId = boxColors[colorIndex]
            drawMaskWithOffset(canvas, result, colorResId)
            drawBoundingBoxAndLabel(canvas, result, colorResId, width, height)
        }
        return combined
    }

    private fun drawMaskWithOffset(canvas: Canvas, result: SegmentationResult, colorResId: Int) {
        val width = canvas.width
        val height = canvas.height
        val mask = result.mask
        val maskHeight = mask.size
        val maskWidth = if (maskHeight > 0) mask[0].size else 0

        val paint = Paint().apply {
            color = applyTransparentOverlayColor(ContextCompat.getColor(context, colorResId))
        }

        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                if (mask[y][x] > 0) {
                    // Apply offset to fix alignment
                    val adjustedX = x + xOffset
                    val adjustedY = y + yOffset

                    if (adjustedX >= 0 && adjustedX < width && adjustedY >= 0 && adjustedY < height) {
                        canvas.drawPoint(adjustedX.toFloat(), adjustedY.toFloat(), paint)
                    }
                }
            }
        }
    }

    private fun drawBoundingBoxAndLabel(canvas: Canvas, result: SegmentationResult, colorResId: Int, width: Int, height: Int) {
        val box = result.box

        // Calculate box coordinates based on relative positions
        val left = (box.x1 * width).toInt().coerceIn(0, width - 1)
        val top = (box.y1 * height).toInt().coerceIn(0, height - 1)
        val right = (box.x2 * width).toInt().coerceIn(0, width - 1)
        val bottom = (box.y2 * height).toInt().coerceIn(0, height - 1)

        // Draw bounding box
        val boxPaint = Paint().apply {
            color = ContextCompat.getColor(context, colorResId)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), boxPaint)

        // Draw label
        val textBackgroundPaint = Paint().apply {
            color = ContextCompat.getColor(context, colorResId)
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 16f
            isAntiAlias = true
        }

        val clsName = result.box.clsName
        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(clsName, 0, clsName.length, bounds)

        val textWidth = bounds.width()
        val textHeight = bounds.height()
        val padding = 4

        // Draw label background
        canvas.drawRect(
            left.toFloat(),
            (top - textHeight - 2 * padding).coerceAtLeast(0).toFloat(),
            (left + textWidth + 2 * padding).toFloat(),
            top.toFloat(),
            textBackgroundPaint
        )

        // Draw label text
        canvas.drawText(
            clsName,
            left.toFloat() + padding,
            top.toFloat() - padding,
            textPaint
        )
    }

    private fun applyTransparentOverlayColor(color: Int): Int {
        val alpha = 48  // You can adjust transparency here (0-255)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return Color.argb(alpha, red, green, blue)
    }
}