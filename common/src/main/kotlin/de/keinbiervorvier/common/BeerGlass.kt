package de.keinbiervorvier.common

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Paints a glass of beer behind the content: [top] above [foamAtPx] (cream on
 * the phone, dark on the watch), a crown of foam bubbles on the line, beer below
 * with slowly rising carbonation. [bubbleScale] shrinks the foam for small
 * screens. The bubbles stand still when the user has switched animations off.
 */
@Composable
fun Modifier.beerGlass(foamAtPx: Float, top: Color = Cream, bubbleScale: Float = 1f): Modifier {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val rising by rememberInfiniteTransition(label = "bubbles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 9_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rise",
    )
    val rise = if (animationsOff) 0.3f else rising
    return drawBehind {
        val foamY = foamAtPx
        drawRect(top, size = Size(size.width, foamY))
        drawRect(
            Brush.verticalGradient(listOf(Amber, DarkAmber), startY = foamY, endY = size.height),
            topLeft = Offset(0f, foamY),
            size = Size(size.width, size.height - foamY),
        )
        drawCarbonation(foamY, rise)
        drawFoam(foamY, bubbleScale)
    }
}

/** A row of overlapping white bubbles along [y], like the watch face's foam. */
private fun DrawScope.drawFoam(y: Float, scale: Float) {
    val radii = listOf(16f, 21f, 14f, 23f, 17f, 19f, 13f, 22f).map { it.dp.toPx() * scale }
    var x = -8.dp.toPx()
    var i = 0
    while (x < size.width + 24.dp.toPx()) {
        val r = radii[i % radii.size]
        drawCircle(Color.White, radius = r, center = Offset(x, y + r * 0.15f))
        x += r * 1.45f
        i++
    }
    drawRect(Color.White, topLeft = Offset(0f, y), size = Size(size.width, 10.dp.toPx() * scale))
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

/** The flag on a round white beer mat. */
@Composable
fun FlagCoaster(flag: String, size: Dp = 92.dp, flagSize: TextUnit = 50.sp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(size / 15, CircleShape)
            .background(Color.White, CircleShape)
            .border(size / 30, Foam, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(flag, style = TextStyle(fontSize = flagSize))
    }
}
