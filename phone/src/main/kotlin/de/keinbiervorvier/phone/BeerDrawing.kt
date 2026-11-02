package de.keinbiervorvier.phone

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * Paints a glass of beer behind the content: cream above [foamAt], a crown of
 * foam bubbles on the line, beer below with slowly rising carbonation.
 */
@Composable
fun Modifier.beerGlass(foamAt: Dp): Modifier {
    val rise by rememberInfiniteTransition(label = "bubbles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 9_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rise",
    )
    return drawBehind {
        val foamY = foamAt.toPx()
        drawRect(Cream, size = Size(size.width, foamY))
        drawRect(
            Brush.verticalGradient(listOf(Amber, DarkAmber), startY = foamY, endY = size.height),
            topLeft = Offset(0f, foamY),
            size = Size(size.width, size.height - foamY),
        )
        drawCarbonation(foamY, rise)
        drawFoam(foamY)
    }
}

/** A row of overlapping white bubbles along [y], like the watch face's foam. */
private fun DrawScope.drawFoam(y: Float) {
    val radii = listOf(16f, 21f, 14f, 23f, 17f, 19f, 13f, 22f).map { it.dp.toPx() }
    var x = -8.dp.toPx()
    var i = 0
    while (x < size.width + 24.dp.toPx()) {
        val r = radii[i % radii.size]
        drawCircle(Color.White, radius = r, center = Offset(x, y + r * 0.15f))
        x += r * 1.45f
        i++
    }
    drawRect(Color.White, topLeft = Offset(0f, y), size = Size(size.width, 10.dp.toPx()))
}

/** Small bubbles drifting up through the beer; [phase] runs 0..1 and loops. */
private fun DrawScope.drawCarbonation(foamY: Float, phase: Float) {
    val depth = size.height - foamY
    // x position (fraction of width), start offset, speed, radius in dp
    val bubbles = listOf(
        floatArrayOf(0.08f, 0.00f, 1.0f, 3f), floatArrayOf(0.17f, 0.55f, 1.6f, 2f),
        floatArrayOf(0.29f, 0.20f, 1.2f, 4f), floatArrayOf(0.44f, 0.75f, 2.0f, 2.5f),
        floatArrayOf(0.58f, 0.35f, 1.4f, 3.5f), floatArrayOf(0.71f, 0.90f, 1.0f, 2f),
        floatArrayOf(0.83f, 0.10f, 1.8f, 3f), floatArrayOf(0.93f, 0.62f, 1.3f, 2.5f),
    )
    bubbles.forEach { (fx, offset, speed, r) ->
        val progress = (phase * speed + offset) % 1f
        val y = size.height - progress * depth
        val wobble = sin((progress * 12f + fx * 20f).toDouble()).toFloat() * 3.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.35f), radius = r.dp.toPx(), center = Offset(fx * size.width + wobble, y))
    }
}

/** The brand mark: a tiny dial where every number is [digit], showing the real [time]. */
@Composable
fun MiniDial(digit: Int, time: LocalTime, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val digitStyle = TextStyle(color = Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(Cream, radius, center)
        // Beer in the lower half, with a foam line on top.
        clipRect(top = center.y) {
            drawCircle(Brush.verticalGradient(listOf(Amber, DarkAmber), startY = center.y, endY = size.height), radius, center)
        }
        clipRect(top = center.y - 1.5.dp.toPx(), bottom = center.y + 1.5.dp.toPx()) {
            drawCircle(Color.White, radius, center)
        }
        drawCircle(Ink, radius - 1.dp.toPx(), center, style = Stroke(2.dp.toPx()))

        val text = measurer.measure(digit.toString(), digitStyle)
        repeat(12) { i ->
            val angle = Math.toRadians(i * 30.0)
            val p = Offset(
                center.x + (radius * 0.72f) * sin(angle).toFloat(),
                center.y - (radius * 0.72f) * cos(angle).toFloat(),
            )
            drawText(text, topLeft = Offset(p.x - text.size.width / 2f, p.y - text.size.height / 2f))
        }

        val minutes = time.minute + time.second / 60f
        val hours = (time.hour % 12) + minutes / 60f
        rotate(hours * 30f, center) {
            drawLine(Ink, center, Offset(center.x, center.y - radius * 0.42f), 2.5.dp.toPx(), StrokeCap.Round)
        }
        rotate(minutes * 6f, center) {
            drawLine(Ink, center, Offset(center.x, center.y - radius * 0.62f), 1.8.dp.toPx(), StrokeCap.Round)
        }
        drawCircle(BeerRed, 2.dp.toPx(), center)
    }
}
