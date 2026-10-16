package app.waypoint.ui.methods

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.count
import app.waypoint.data.setField
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.RoundIcon
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.dimStar
import app.waypoint.ui.components.navStar
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** A lead measure's weekly target: the set count, or the number of planned weekdays. */
private fun leadTarget(n: Node): Int = if (n.perWeek > 0) n.perWeek else Integer.bitCount(n.days).coerceAtLeast(1)

/**
 * 4DX: one wildly important goal measured "from X to Y by when", one or two lead measures you control,
 * and a scoreboard that says each week whether you are winning.
 */
@Composable
fun FourDxBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.FOURDX)
    val context = LocalContext.current
    val container = context.container
    val leads = w.nodesOf(goal.id, NodeKind.LEAD).filter { it.title.isNotBlank() }
    var editing by remember { mutableStateOf<Node?>(null) }
    var updateLag by remember { mutableStateOf(false) }
    var editCommit by remember { mutableStateOf(false) }

    // 1. The wildly important goal and its lag measure.
    if (!goal.hasMetric) {
        MetricSetup(goal, color, stringResource(R.string.fdx_setup_title), stringResource(R.string.fdx_setup_text), askDeadline = true)
    } else {
        val from = withUnit(goal.startValue, goal.unit)
        val to = withUnit(goal.targetValue, goal.unit)
        Text(
            goal.endDay?.let { stringResource(R.string.fdx_wig_by, from, to, dateShort(it)) } ?: stringResource(R.string.fdx_wig, from, to),
            style = UType.Heading.copy(color = color),
        )
        Spacer(Modifier.height(12.dp))
        MetricCard(goal, w, color, stringResource(R.string.fdx_lag), onUpdate = { updateLag = true })
    }

    // 2. The scoreboard of lead measures.
    SectionTitle(stringResource(R.string.fdx_scoreboard), action = stringResource(R.string.add), onAction = {
        editing = Node(goalId = goal.id, kind = NodeKind.LEAD.name, perWeek = 3, slot = leads.size)
    })
    if (leads.isEmpty()) {
        UCard {
            EmptyState(stringResource(R.string.fdx_leads_empty), stringResource(R.string.fdx_add_lead), {
                editing = Node(goalId = goal.id, kind = NodeKind.LEAD.name, perWeek = 3)
            })
        }
    } else {
        val winning = leads.all { w.weekCount(it.id, w.thisWeek) >= leadTarget(it) }
        val daysLeft = (w.thisWeek + 6 - w.day + 1).toInt()
        WeekVerdict(winning, daysLeft, color)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            leads.forEach { n ->
                LeadRow(n, w, color, onEdit = { editing = n }) { delta ->
                    if (delta > 0) Haptics.tick(context)
                    container.write { count(n, w.day, delta) }
                }
            }
        }
        if (leads.size > 2) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.fdx_too_many), style = UType.Muted.copy(color = U.c.danger.copy(alpha = 0.85f)))
        }

        SectionTitle(stringResource(R.string.fdx_history))
        UCard { History(leads, w, color) }
    }

    // 3. This week's commitment.
    val key = "fdx.commit.${w.thisWeek}"
    val commit = w.field(goal.id, key)
    val lastCommit = w.field(goal.id, "fdx.commit.${w.thisWeek - 7}")
    SectionTitle(stringResource(R.string.fdx_commit_title))
    UCard(onClick = { editCommit = true }) {
        if (commit.isBlank()) Text(stringResource(R.string.fdx_commit_empty), style = UType.Sub)
        else Text(commit, style = UType.Body)
        if (lastCommit.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.fdx_commit_last, lastCommit), style = UType.Small)
        }
    }

    if (updateLag) {
        NumberPrompt(stringResource(R.string.fdx_lag_update), goal.currentValue, goal.unit, color, { updateLag = false }) { v ->
            container.write { upsertGoal(goal.copy(currentValue = v)) }
        }
    }
    if (editCommit) {
        TextPrompt(
            stringResource(R.string.fdx_commit_title), commit, stringResource(R.string.fdx_commit_hint), color, { editCommit = false },
            text = stringResource(R.string.fdx_commit_text),
        ) { v -> container.write { setField(goal.id, key, v) } }
    }
    editing?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.fdx_new_lead else R.string.fdx_edit_lead),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.fdx_lead_label),
                titleHint = stringResource(R.string.fdx_lead_hint),
                schedule = true,
                allowOnce = false,
                remind = true,
            ),
            onDismiss = { editing = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
}

@Composable
private fun WeekVerdict(winning: Boolean, daysLeft: Int, color: Color) {
    val tint = if (winning) U.c.good else color
    Row(
        Modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.35f), U.RMd)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))
        Canvas(Modifier.size(28.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            if (winning) navStar(c, size.minDimension * 0.42f, tint, 1f) else dimStar(c, size.minDimension * 0.42f, tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(if (winning) R.string.fdx_winning else R.string.fdx_behind), style = UType.Heading.copy(color = tint))
            Text(pluralStringResource(R.plurals.fdx_days_left_week, daysLeft, daysLeft), style = UType.Small)
        }
        Spacer(Modifier.width(16.dp))
    }
}

/** One lead measure: this week's count against the target as a row of stars, with −1 and a big +1. */
@Composable
private fun LeadRow(n: Node, w: World, color: Color, onEdit: () -> Unit, onCount: (Int) -> Unit) {
    val target = leadTarget(n)
    val count = w.weekCount(n.id, w.thisWeek)
    val today = w.log(n.id, w.day)?.value?.toInt() ?: 0
    val met = count >= target
    UCard(border = if (met) color.copy(alpha = 0.45f) else U.c.line) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clip(U.RSm).clickable(onClick = onEdit)) {
                Text(n.title, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (today > 0) stringResource(R.string.fdx_today_n, today) else stringResource(R.string.fdx_tap_edit),
                    style = UType.Small,
                )
            }
            Text(
                stringResource(R.string.fdx_count_of, count, target),
                style = UType.Number.copy(color = if (met) color else U.c.ink, fontSize = 20.sp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (target <= 14) {
                Canvas(Modifier.weight(1f).height(22.dp)) {
                    val step = size.width / target
                    val r = minOf(step * 0.36f, size.height * 0.45f)
                    for (i in 0 until target) {
                        val c = Offset(step * (i + 0.5f), size.height / 2)
                        if (i < count) navStar(c, r, color, 1f) else dimStar(c, r, color)
                    }
                }
            } else {
                app.waypoint.ui.components.Bar(count.toFloat() / target, Modifier.weight(1f), color = color)
            }
            Spacer(Modifier.width(12.dp))
            RoundIcon(Icons.Rounded.Remove, enabled = today > 0, color = color) { onCount(-1) }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(color).clickable { onCount(1) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, stringResource(R.string.fdx_plus_one), Modifier.size(28.dp), tint = U.c.onAccent) }
        }
    }
}

/** The last eight weeks: how much of the lead targets each week reached, with the 100 % line. */
@Composable
private fun History(leads: List<Node>, w: World, color: Color) {
    val weeks = (7 downTo 0).map { w.thisWeek - it * 7L }
    val shares = weeks.map { ws ->
        leads.map { (w.weekCount(it.id, ws).toFloat() / leadTarget(it)).coerceIn(0f, 1f) }.average().toFloat()
    }
    val line = U.c.line
    val faint = U.c.faint
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val gap = 8.dp.toPx()
        val bw = (size.width - gap * (weeks.size - 1)) / weeks.size
        drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        shares.forEachIndexed { i, s ->
            val x = i * (bw + gap)
            drawRoundRect(faint.copy(alpha = 0.18f), Offset(x, 0f), Size(bw, size.height), CornerRadius(6.dp.toPx()))
            val h = size.height * s
            if (h > 0f) {
                val c = if (s >= 1f) color else color.copy(alpha = 0.5f)
                drawRoundRect(c, Offset(x, size.height - h), Size(bw, h), CornerRadius(6.dp.toPx()))
            }
            if (s >= 1f) navStar(Offset(x + bw / 2, 10.dp.toPx()), 5.dp.toPx(), Color.White, 0.6f)
        }
    }
    Spacer(Modifier.height(6.dp))
    Row {
        Text(dateShort(weeks.first()), style = UType.Small)
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.fdx_this_week), style = UType.Small.copy(textAlign = TextAlign.End))
    }
}
