package app.waypoint.ui.methods

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.WaypointDao
import app.waypoint.data.record
import app.waypoint.data.setField
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.formatNumber
import app.waypoint.ui.components.navStar
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.Manrope
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlin.math.ceil
import kotlin.math.ln

private val Rates = listOf(0.5, 1.0, 2.0, 5.0)

/** Creates the one practice node a kaizen or pact goal logs against, if it isn't there yet. */
internal suspend fun WaypointDao.ensurePractice(goal: Goal) {
    if (nodesOf(goal.id).none { it.kind == NodeKind.ACTION.name }) {
        insertNode(Node(goalId = goal.id, kind = NodeKind.ACTION.name, title = goal.title, days = World.EVERY_DAY))
    }
}

/** 1 % a day: today's number grows from an easy base, compounding; you log what you actually did. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaizenBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.KAIZEN)
    val context = LocalContext.current
    val container = context.container
    val practice = w.nodesOf(goal.id, NodeKind.ACTION).firstOrNull()
    LaunchedEffect(goal.id, practice == null) { if (practice == null) container.write { ensurePractice(goal) } }

    if (goal.startValue <= 0.0) {
        KaizenSetup(goal, color)
        return
    }
    if (practice == null) return

    val rate = w.field(goal.id, "kaizen.rate").toDoubleOrNull() ?: 1.0
    val target = w.kaizenTargetRounded(goal)
    val todayLog = w.log(practice.id, w.day)
    val logs = w.logsOf(practice.id).filter { it.value > 0 }
    var logDay by remember { mutableStateOf<Long?>(null) }
    var editBase by remember { mutableStateOf(false) }
    val dayN = (w.day - goal.startDay + 1).coerceAtLeast(1)

    // Today's number.
    UCard(border = color.copy(alpha = 0.4f)) {
        Text(stringResource(R.string.kz_today).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(target.toString(), style = UType.Big.copy(color = color))
            if (goal.unit.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(goal.unit, style = UType.Sub, modifier = Modifier.padding(bottom = 9.dp))
            }
            Spacer(Modifier.weight(1f))
            if (todayLog != null && todayLog.value >= target) {
                Canvas(Modifier.size(34.dp).padding(bottom = 6.dp)) {
                    navStar(Offset(size.width / 2, size.height / 2), size.minDimension * 0.45f, color, 1f)
                }
            }
        }
        Text(stringResource(R.string.kz_day_rate, dayN, formatNumber(rate)), style = UType.Muted)
        Spacer(Modifier.height(14.dp))
        if (todayLog != null) {
            Text(stringResource(R.string.kz_done_today, withUnit(todayLog.value, goal.unit)), style = UType.Strong.copy(color = if (todayLog.value >= target) color else U.c.ink))
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.kz_change), { logDay = w.day }, Modifier.fillMaxWidth(), color = color)
        } else {
            PrimaryButton(stringResource(R.string.kz_log), { logDay = w.day }, Modifier.fillMaxWidth(), color = color)
        }
        if (w.day - 1 >= goal.startDay && w.log(practice.id, w.day - 1) == null) {
            TextButton(onClick = { logDay = w.day - 1 }) { Text(stringResource(R.string.kz_log_yesterday), color = color, style = UType.Button) }
        }
    }
    Spacer(Modifier.height(10.dp))
    val doubling = if (rate > 0) ceil(ln(2.0) / ln(1 + rate / 100)).toInt() else 0
    Text(pluralStringResource(R.plurals.kz_compound, doubling, formatNumber(rate), doubling), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))

    SectionTitle(stringResource(R.string.kz_curve))
    UCard { KaizenChart(goal, w, practice, color) }

    SectionTitle(stringResource(R.string.kz_stats))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat(stringResource(R.string.kz_stat_days), logs.size.toString(), Modifier.weight(1f))
        Stat(stringResource(R.string.kz_stat_best), logs.maxOfOrNull { it.value }?.let { formatNumber(it) } ?: "–", Modifier.weight(1f))
        Stat(stringResource(R.string.kz_stat_streak), w.streak(practice).toString(), Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    UCard {
        Text(
            stringResource(R.string.kz_projection, withUnit(w.kaizenTargetRounded(goal, w.day + 30).toDouble(), goal.unit)),
            style = UType.Body,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.kz_projection_far, withUnit(w.kaizenTargetRounded(goal, w.day + 90).toDouble(), goal.unit)),
            style = UType.Muted,
        )
    }

    SectionTitle(stringResource(R.string.kz_step))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rates.forEach { r ->
            Chip(stringResource(R.string.kz_rate_chip, formatNumber(r)), rate == r, { container.write { setField(goal.id, "kaizen.rate", formatNumber(r)) } }, color = color)
        }
    }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { editBase = true }) {
        Text(stringResource(R.string.kz_base, withUnit(goal.startValue, goal.unit)), color = color, style = UType.Button)
    }

    logDay?.let { d ->
        val existing = w.log(practice.id, d)
        NumberPrompt(
            title = stringResource(if (d == w.day) R.string.kz_log_title else R.string.kz_log_title_yesterday),
            initial = existing?.value ?: w.kaizenTargetRounded(goal, d).toDouble(),
            unit = goal.unit,
            color = color,
            onDismiss = { logDay = null },
            text = stringResource(R.string.kz_log_text, withUnit(w.kaizenTargetRounded(goal, d).toDouble(), goal.unit)),
            onClear = if (existing != null) ({
                container.write {
                    record(practice, d, null)
                    val latest = logsAfterChange(practice, goal)
                    upsertGoal(goal.copy(currentValue = latest ?: goal.startValue))
                }
            }) else null,
        ) { v ->
            Haptics.star(context)
            container.write {
                record(practice, d, v)
                upsertGoal(goal.copy(currentValue = logsAfterChange(practice, goal) ?: v))
            }
        }
    }
    if (editBase) {
        NumberPrompt(stringResource(R.string.kz_base_title), goal.startValue, goal.unit, color, { editBase = false }, text = stringResource(R.string.kz_base_text)) { v ->
            if (v > 0) container.write { upsertGoal(goal.copy(startValue = v)) }
        }
    }
}

/** The value of the most recent log, read back after a write. */
private suspend fun WaypointDao.logsAfterChange(practice: Node, goal: Goal): Double? =
    logs().filter { it.nodeId == practice.id && it.goalId == goal.id }.maxByOrNull { it.day }?.value

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    UCard(modifier) {
        Text(value, style = UType.Number)
        Spacer(Modifier.height(2.dp))
        Text(label, style = UType.Small, maxLines = 2)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KaizenSetup(goal: Goal, color: Color) {
    val container = LocalContext.current.container
    var base by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(goal.unit) }
    var rate by remember { mutableStateOf(1.0) }
    val b = parseNumber(base)
    UCard(border = color.copy(alpha = 0.4f)) {
        Text(stringResource(R.string.kz_setup_title), style = UType.Heading)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.kz_setup_text), style = UType.Sub)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UTextField(base, { base = it.take(10) }, Modifier.weight(1f), label = stringResource(R.string.kz_setup_base), number = true)
            UTextField(unit, { unit = it.take(16) }, Modifier.weight(1f), label = stringResource(R.string.kz_setup_unit), placeholder = stringResource(R.string.kz_setup_unit_hint))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.kz_step), style = UType.Small)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Rates.forEach { r -> Chip(stringResource(R.string.kz_rate_chip, formatNumber(r)), rate == r, { rate = r }, color = color) }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            stringResource(R.string.kz_setup_go),
            {
                container.write {
                    upsertGoal(goal.copy(startValue = b!!, currentValue = b, unit = unit.trim()))
                    setField(goal.id, "kaizen.rate", formatNumber(rate))
                    ensurePractice(goal)
                }
            },
            Modifier.fillMaxWidth(),
            enabled = b != null && b > 0,
            color = color,
        )
    }
}

/**
 * The target curve over the last four weeks and the next week (dashed ahead of today),
 * with each logged day as a star when it met the target and a dot when it didn't.
 */
@Composable
private fun KaizenChart(goal: Goal, w: World, practice: Node, color: Color) {
    val d0 = maxOf(goal.startDay, w.day - 27)
    val d1 = w.day + 7
    val logs = w.logsOf(practice.id).filter { it.day in d0..w.day }
    val targets = (d0..d1).map { w.kaizenTarget(goal, it) }
    val maxY = (listOf(targets.max()) + logs.map { it.value }).max().coerceAtLeast(1.0) * 1.12
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontFamily = Manrope, fontSize = 10.sp, color = U.c.faint)
    val line = U.c.line
    val ink = U.c.muted
    val startLabel = dateShort(d0)
    val endLabel = dateShort(d1)
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val left = 34.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val top = 6.dp.toPx()
        val span = (d1 - d0).coerceAtLeast(1).toFloat()
        fun x(d: Long) = left + (size.width - left - 6.dp.toPx()) * ((d - d0) / span)
        fun y(v: Double) = bottom - (bottom - top) * (v / maxY).toFloat()

        // Grid: zero, half and the top value.
        listOf(0.0, maxY / 2, maxY / 1.12).forEach { v ->
            drawLine(line, Offset(left, y(v)), Offset(size.width, y(v)), 1.dp.toPx())
            val t = measurer.measure(formatNumber(kotlin.math.round(v)), label)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), y(v) - t.size.height / 2))
        }
        // Target: solid up to today, dashed ahead.
        val past = Path()
        val ahead = Path()
        (d0..d1).forEachIndexed { i, d ->
            val p = Offset(x(d), y(targets[i]))
            if (d <= w.day) { if (i == 0) past.moveTo(p.x, p.y) else past.lineTo(p.x, p.y) }
            if (d >= w.day) { if (d == w.day) ahead.moveTo(p.x, p.y) else ahead.lineTo(p.x, p.y) }
        }
        drawPath(past, color.copy(alpha = 0.55f), style = Stroke(2.dp.toPx()))
        drawPath(ahead, color.copy(alpha = 0.4f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        // Today marker.
        drawLine(ink.copy(alpha = 0.4f), Offset(x(w.day), top), Offset(x(w.day), bottom), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        // What was done.
        logs.forEach { l ->
            val p = Offset(x(l.day), y(l.value))
            if (l.value >= w.kaizenTargetRounded(goal, l.day)) navStar(p, 5.dp.toPx(), color, 0.9f)
            else drawCircle(ink, 3.dp.toPx(), p)
        }
        val a = measurer.measure(startLabel, label)
        drawText(a, topLeft = Offset(left, bottom + 4.dp.toPx()))
        val b = measurer.measure(endLabel, label)
        drawText(b, topLeft = Offset(size.width - b.size.width, bottom + 4.dp.toPx()))
    }
}
