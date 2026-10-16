package app.waypoint.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.waypoint.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** False when the user turned animations off in system settings. */
fun motionOn(): Boolean = ValueAnimator.areAnimatorsEnabled()

/** The night sky behind every screen: a gradient from the Milky Way's glow down to deep blue, with twinkling stars. */
@Composable
fun Sky(modifier: Modifier = Modifier, stars: Int = 90, content: @Composable BoxScope.() -> Unit) {
    val c = U.c
    val field = remember(stars) {
        val r = Random(7)
        List(stars) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * (if (r.nextFloat() > 0.9f) 1.6f else 0.8f), r.nextFloat() * 6.28f) }
    }
    val animate = motionOn()
    val t by if (animate) rememberInfiniteTransition(label = "sky").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "twinkle",
    ) else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(
        modifier.background(Brush.verticalGradient(0f to c.bgTop, 0.55f to c.bg, 1f to c.bg)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val base = if (c.dark) 0.75f else 0.5f
            field.forEach { s ->
                val a = base * (0.55f + 0.45f * sin(t + s[3]))
                // Fewer stars low on the screen, like light pollution near the horizon.
                val y = s[1] * size.height
                val fade = 1f - (y / size.height) * 0.55f
                drawCircle(c.star.copy(alpha = (a * fade).coerceIn(0f, 1f)), s[2].dp.toPx(), Offset(s[0] * size.width, y))
            }
        }
        content()
    }
}

/** A four-point navigator's star with a soft halo; [glow] 0..1 sets how bright it shines. */
fun DrawScope.navStar(center: Offset, radius: Float, color: Color, glow: Float = 1f) {
    if (glow > 0.05f) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.45f * glow), Color.Transparent), center, radius * 2.6f),
            radius * 2.6f, center,
        )
    }
    val r = radius
    val w = radius * 0.28f
    val p = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x + w * 0.35f, center.y - w * 0.35f, center.x + r, center.y)
        quadraticTo(center.x + w * 0.35f, center.y + w * 0.35f, center.x, center.y + r)
        quadraticTo(center.x - w * 0.35f, center.y + w * 0.35f, center.x - r, center.y)
        quadraticTo(center.x - w * 0.35f, center.y - w * 0.35f, center.x, center.y - r)
        close()
    }
    drawPath(p, color.copy(alpha = (0.35f + 0.65f * glow).coerceIn(0f, 1f)))
    drawCircle(Color.White.copy(alpha = 0.8f * glow), r * 0.16f, center)
}

/** An unlit star: a small dim dot with a thin ring. */
fun DrawScope.dimStar(center: Offset, radius: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.35f), radius * 0.32f, center)
    drawCircle(color.copy(alpha = 0.18f), radius * 0.75f, center, style = Stroke(radius * 0.08f))
}

/** Points of a goal's constellation: always the same shape for the same seed. */
fun constellation(seed: Int, count: Int): List<Offset> {
    val r = Random(seed * 31 + count)
    val pts = mutableListOf<Offset>()
    var guard = 0
    while (pts.size < count && guard++ < 2000) {
        val p = Offset(0.08f + r.nextFloat() * 0.84f, 0.12f + r.nextFloat() * 0.76f)
        if (pts.all { hypot(it.x - p.x, it.y - p.y) > 0.6f / kotlin.math.sqrt(count.toFloat() + 1) }) pts += p
    }
    // Order as a path: start at the leftmost star and keep stepping to the nearest unvisited one.
    val left = pts.toMutableList()
    val path = mutableListOf(left.minBy { it.x }.also { left.remove(it) })
    while (left.isNotEmpty()) {
        val last = path.last()
        val next = left.minBy { hypot(it.x - last.x, it.y - last.y) }
        left.remove(next); path += next
    }
    return path
}

/**
 * A goal drawn as a constellation: one star per [levels] entry, brightness 0..1 each,
 * joined in order. Unlit stars show as dim dots, so the shape is visible from the start.
 */
@Composable
fun Constellation(seed: Int, levels: List<Float>, color: Color, modifier: Modifier = Modifier, starSize: Dp = 7.dp) {
    val pts = remember(seed, levels.size) { constellation(seed, levels.size.coerceAtLeast(1)) }
    val dim = U.c.star
    Canvas(modifier) {
        val at = pts.map { Offset(it.x * size.width, it.y * size.height) }
        for (i in 1 until at.size) {
            val lit = minOf(levels.getOrElse(i - 1) { 0f }, levels.getOrElse(i) { 0f })
            drawLine(color.copy(alpha = 0.12f + 0.35f * lit), at[i - 1], at[i], strokeWidth = 1.dp.toPx())
        }
        val r = starSize.toPx()
        at.forEachIndexed { i, p ->
            val l = levels.getOrElse(i) { 0f }
            if (l <= 0.02f) dimStar(p, r, dim) else navStar(p, r * (0.6f + 0.5f * l), color, l)
        }
    }
}

/** Evenly spread brightness for [lit] of [total] stars, brightest first, for goals measured by a single fraction. */
fun levelsOf(fraction: Float, total: Int = 8): List<Float> {
    val f = fraction.coerceIn(0f, 1f) * total
    return List(total) { i -> (f - i).coerceIn(0f, 1f) }
}

/** A round tick for a step: a dim ring before, a shining star after. */
@Composable
fun StarCheck(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.accent, size: Dp = 30.dp, label: String = "") {
    val dim = U.c.faint
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { if (label.isNotEmpty()) contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension * 0.36f
            if (checked) navStar(c, r, color, 1f)
            else drawCircle(dim, r * 0.8f, c, style = Stroke(1.6.dp.toPx()))
        }
    }
}

/** Points on a circle, for wheels and dials; angle 0 is at the top, going clockwise. */
fun onCircle(center: Offset, radius: Float, i: Int, n: Int): Offset {
    val a = -PI / 2 + 2 * PI * i / n
    return Offset(center.x + (radius * cos(a)).toFloat(), center.y + (radius * sin(a)).toFloat())
}
