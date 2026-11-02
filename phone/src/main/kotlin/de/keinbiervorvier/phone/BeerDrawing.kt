package de.keinbiervorvier.phone

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.keinbiervorvier.common.Amber
import de.keinbiervorvier.common.BeerRed
import de.keinbiervorvier.common.Cream
import de.keinbiervorvier.common.DarkAmber
import de.keinbiervorvier.common.Ink
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

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
