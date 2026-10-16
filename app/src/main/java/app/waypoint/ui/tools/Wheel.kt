package app.waypoint.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.WheelCheck
import app.waypoint.domain.Area
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.areaIcon
import app.waypoint.ui.areaName
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.navStar
import app.waypoint.ui.components.onCircle
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

@Composable
fun WheelScreen(data: WData, go: Go) {
    val tool = Method.WHEEL
    val hue = U.c.hue(tool)
    val container = LocalContext.current.container
    val checks = data.world.wheel
    val last = data.world.lastWheel
    val savedToday = last?.day == data.day

    val values = remember(last?.day, last?.scores) { mutableStateListOf<Int>().apply { addAll(last?.values ?: List(8) { 5 }) } }
    // The outline to compare with: the check before today's, or the last one; a tap in the history picks another.
    val defaultCompare = if (savedToday) checks.filter { it.day < data.day }.maxByOrNull { it.day } else last
    var compareDay by remember { mutableStateOf<Long?>(null) }
    val compare = compareDay?.let { d -> checks.firstOrNull { it.day == d } } ?: defaultCompare
    val changed = last == null || values.toList() != last.values

    ToolScaffold(tool, go) {
        Hint(stringResource(R.string.wheel_intro))
        Spacer(Modifier.height(8.dp))
        WheelChart(values.toList(), compare?.values, hue, Modifier.fillMaxWidth().aspectRatio(1f))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            Text(stringResource(R.string.wheel_average, "%.1f".format(values.average())), style = UType.Strong.copy(color = hue))
        }
        if (compare != null) {
            Text(
                stringResource(R.string.wheel_comparing, dateShort(compare.day), "%.1f".format(compare.values.average())),
                style = UType.Small,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        SectionTitle(stringResource(R.string.wheel_rate))
        UCard {
            Area.entries.forEachIndexed { i, a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(areaIcon(a), null, Modifier.size(20.dp), tint = hue)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        ValueSlider(areaName(a), values[i], 1..10, hue, { values[i] = it })
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            stringResource(if (savedToday && !changed) R.string.wheel_saved else R.string.wheel_save),
            {
                container.write { upsertWheel(WheelCheck.of(data.day, values.toList(), System.currentTimeMillis())) }
            },
            Modifier.fillMaxWidth(),
            enabled = !savedToday || changed,
            color = hue,
        )

        val lowest = Area.entries.withIndex().sortedBy { values[it.index] }.take(2)
        SectionTitle(stringResource(R.string.wheel_lowest))
        Hint(stringResource(R.string.wheel_lowest_text))
        lowest.forEach { (i, a) ->
            UCard(Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(areaIcon(a), null, Modifier.size(22.dp), tint = hue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(areaName(a), style = UType.Strong)
                        Text(stringResource(R.string.wheel_score, values[i]), style = UType.Muted)
                    }
                    SoftButton(stringResource(R.string.wheel_goal_here), { go.newGoal(area = a.ordinal) }, color = hue)
                }
            }
        }

        if (checks.isNotEmpty()) {
            SectionTitle(stringResource(R.string.wheel_history))
            UCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
                checks.sortedByDescending { it.day }.forEachIndexed { idx, c ->
                    if (idx > 0) HorizontalDivider(color = U.c.line)
                    val on = compare?.day == c.day
                    Row(
                        Modifier.fillMaxWidth().clickable { compareDay = c.day }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniWheel(c.values, if (on) hue else U.c.muted, Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(dateShort(c.day), style = UType.Body, modifier = Modifier.weight(1f))
                        Text("%.1f".format(c.values.average()), style = UType.Strong.copy(color = if (on) hue else U.c.ink))
                    }
                }
            }
            Hint(stringResource(R.string.wheel_history_hint))
        }
    }
}

/** Eight sectors filled to their score, rings for 1..10, the comparison drawn as an outline and area icons around. */
@Composable
private fun WheelChart(values: List<Int>, compare: List<Int>?, hue: Color, modifier: Modifier) {
    val line = U.c.line
    val muted = U.c.muted
    val ink = U.c.ink
    BoxWithConstraints(modifier) {
        val side = minOf(maxWidth, maxHeight)
        val iconSize = 22.dp
        val ringOuter = side / 2 - iconSize - 10.dp
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val rMax = ringOuter.toPx()
            val n = 8
            val sweep = 360f / n
            fun box(r: Float) = Pair(Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
            values.forEachIndexed { i, v ->
                val r = rMax * v / 10f
                val (tl, s) = box(r)
                // Sector i is centred on the direction of area i, starting from the top.
                drawArc(hue.copy(alpha = 0.18f + 0.04f * v), -90f - sweep / 2 + i * sweep, sweep, true, tl, s)
            }
            for (k in 1..10) {
                val strong = k == 5 || k == 10
                drawCircle(line.copy(alpha = if (strong) 1f else 0.55f), rMax * k / 10f, c, style = Stroke(if (strong) 1.2.dp.toPx() else 0.6.dp.toPx()))
            }
            for (i in 0 until n) {
                val a = Math.toRadians((-90.0 - sweep / 2 + i * sweep))
                drawLine(line, c, Offset(c.x + (rMax * kotlin.math.cos(a)).toFloat(), c.y + (rMax * kotlin.math.sin(a)).toFloat()), 0.8.dp.toPx())
            }
            compare?.forEachIndexed { i, v ->
                val r = rMax * v / 10f
                val (tl, s) = box(r)
                drawArc(muted, -90f - sweep / 2 + i * sweep, sweep, false, tl, s, style = Stroke(1.6.dp.toPx()))
            }
            values.forEachIndexed { i, v ->
                navStar(onCircle(c, rMax * v / 10f, i, n), 4.dp.toPx(), hue, 0.9f)
            }
            drawCircle(ink.copy(alpha = 0.15f), 2.dp.toPx(), c)
        }
        Area.entries.forEachIndexed { i, a ->
            val p = onCircle(Offset(0f, 0f), (ringOuter + iconSize / 2 + 6.dp).value, i, 8)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(p.x.dp, p.y.dp)
                    .size(iconSize + 6.dp)
                    .clip(CircleShape)
                    .background(U.c.surface)
                    .border(1.dp, hue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(areaIcon(a), areaName(a), Modifier.size(iconSize - 6.dp), tint = hue)
            }
        }
    }
}

/** A thumbnail of a past check for the history list. */
@Composable
private fun MiniWheel(values: List<Int>, color: Color, modifier: Modifier) {
    val line = U.c.line
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val rMax = size.minDimension / 2
        drawCircle(line, rMax, c, style = Stroke(0.8.dp.toPx()))
        values.forEachIndexed { i, v ->
            val r = rMax * v / 10f
            drawArc(color.copy(alpha = 0.55f), -90f - 22.5f + i * 45f, 45f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
        }
    }
}
