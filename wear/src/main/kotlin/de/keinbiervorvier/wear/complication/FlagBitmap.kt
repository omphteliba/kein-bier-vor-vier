package de.keinbiervorvier.wear.complication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

private const val SIZE_PX = 96

/**
 * Draws a flag emoji into a square bitmap, so it keeps its colours where
 * watch faces would tint text.
 */
fun renderFlag(flag: String): Bitmap {
    val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = SIZE_PX * 0.8f
        textAlign = Paint.Align.CENTER
    }
    val metrics = paint.fontMetrics
    val baseline = SIZE_PX / 2f - (metrics.ascent + metrics.descent) / 2f
    Canvas(bitmap).drawText(flag, SIZE_PX / 2f, baseline, paint)
    return bitmap
}
