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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.setField
import app.waypoint.data.tick
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.ConfirmDialog
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.dateShort
import app.waypoint.ui.scheduleLabel
import app.waypoint.ui.theme.Manrope
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 85 % of planned tactics done is the 12-week year's mark of an excellent week. */
private const val GOOD = 0.85f

private fun pct(f: Float): String = "${(f * 100).toInt()}%"

@Composable
fun Weeks12Body(goal: Goal, data: WData, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val world = data.world
    val hue = U.c.hue(Method.WEEKS12)
    val started = goal.startDay <= world.day
    val current = world.week12(goal)
    val end = goal.endDay ?: (goal.startDay + 83)
    var selected by rememberSaveable(goal.id) { mutableIntStateOf(current.coerceAtMost(12)) }
    var edit by remember { mutableStateOf<Node?>(null) }
    var vision by remember { mutableStateOf(false) }
    var renew by remember { mutableStateOf(false) }
    val tactics = world.nodesOf(goal.id, NodeKind.TACTIC)

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                !started -> stringResource(R.string.w12_starts, dateShort(goal.startDay))
                current >= 13 -> stringResource(R.string.w12_review_week)
                else -> stringResource(R.string.w12_week_of, current)
            },
            style = UType.Heading, modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.w12_ends, dateShort(end)), style = UType.Small)
    }
    Spacer(Modifier.height(12.dp))
    WeekStrip(goal, world, hue, current = if (started) current else 0, selected = selected) { selected = it }
    Spacer(Modifier.height(8.dp))
    val scores = (1..minOf(current, 12)).mapNotNull { world.weekScore(goal, it) }
    Row {
        Text(stringResource(R.string.w12_goal85), style = UType.Small, modifier = Modifier.weight(1f))
        if (started && scores.isNotEmpty()) {
            Text(stringResource(R.string.w12_average, pct(scores.average().toFloat())), style = UType.Small.copy(color = hue))
        }
    }

    if (started && current >= 13) {
        Spacer(Modifier.height(14.dp))
        UCard(border = hue.copy(alpha = 0.45f)) {
            Text(stringResource(R.string.w12_over_title), style = UType.Heading)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.w12_over_text), style = UType.Muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton(stringResource(R.string.w12_review), { go.review() }, color = hue)
                SoftButton(stringResource(R.string.w12_new_year), { renew = true }, color = hue)
            }
        }
    }

    val week = selected.coerceIn(1, 12)
    val isCurrent = started && week == current
    val weekTactics = tactics.filter { it.week == 0 || it.week == week }
    SectionTitle(
        if (isCurrent) stringResource(R.string.w12_this_week) else stringResource(R.string.node_week_n, week),
        action = stringResource(R.string.add),
        onAction = { edit = Node(goalId = goal.id, kind = NodeKind.TACTIC.name, week = week) },
    )
    UCard {
        val s = world.weekScore(goal, week)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(s?.let { pct(it) } ?: "—", style = UType.Big.copy(color = when {
                s == null -> U.c.faint
                s >= GOOD -> U.c.good
                else -> hue
            }))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(
                    when {
                        s == null -> R.string.w12_score_none
                        s >= GOOD -> R.string.w12_score_good
                        s >= 0.6f -> R.string.w12_score_mid
                        else -> R.string.w12_score_low
                    }
                ),
                style = UType.Muted, modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        val ws = goal.startDay + (week - 1) * 7L
        Text(stringResource(R.string.w12_dates, dateShort(ws), dateShort(ws + 6)), style = UType.Small)
        Spacer(Modifier.height(8.dp))
        if (weekTactics.isEmpty()) {
            EmptyState(
                stringResource(if (tactics.isEmpty()) R.string.w12_empty else R.string.w12_no_tactics),
                stringResource(R.string.w12_add_tactic),
                onAction = { edit = Node(goalId = goal.id, kind = NodeKind.TACTIC.name, week = week) },
            )
        }
        weekTactics.forEach { t -> TacticRow(t, goal, world, hue, week, isCurrent, { edit = t }) { container.write { tick(t, world.day) } } }
        if (weekTactics.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(U.RSm).clickable { edit = Node(goalId = goal.id, kind = NodeKind.TACTIC.name, week = week) }.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = hue)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.w12_add_tactic), style = UType.Small.copy(color = hue))
            }
        }
    }

    SectionTitle(stringResource(R.string.w12_vision))
    val v = world.field(goal.id, "w12.vision")
    UCard(onClick = { vision = true }) {
        Text(v.ifBlank { stringResource(R.string.w12_vision_hint) }, style = if (v.isBlank()) UType.Muted else UType.Body)
    }

    edit?.let { t ->
        NodeEditor(
            heading = stringResource(if (t.id == 0L) R.string.w12_new_tactic else R.string.w12_tactic),
            node = t,
            spec = NodeSpec(titleLabel = stringResource(R.string.w12_tactic), titleHint = stringResource(R.string.w12_tactic_hint), schedule = true, due = true, remind = true, weeks = true),
            onDismiss = { edit = null },
            onDelete = if (t.id == 0L) null else ({ container.write { deleteNode(t.id) } }),
            color = hue,
        ) { n -> container.write { upsertNode(n) } }
    }
    if (vision) VisionDialog(v, hue, { vision = false }) { text -> container.write { setField(goal.id, "w12.vision", text) } }
    if (renew) {
        ConfirmDialog(
            stringResource(R.string.w12_new_year), stringResource(R.string.w12_new_year_text), stringResource(R.string.w12_new_year_go),
            onConfirm = {
                container.scope.launch {
                    val dao = container.dao
                    val now = System.currentTimeMillis()
                    val today = World.epochDay(now)
                    val id = dao.insertGoal(goal.copy(id = 0, startDay = today, endDay = today + 83, createdAt = now, closedAt = null, note = "", status = Goal.ACTIVE))
                    dao.insertNodes(tactics.map { it.copy(id = 0, goalId = id, done = false, doneAt = null, dueDay = null) })
                    world.field(goal.id, "w12.vision").takeIf { it.isNotBlank() }?.let { dao.setField(id, "w12.vision", it) }
                    dao.upsertGoal(goal.copy(status = Goal.DONE, closedAt = now))
                    container.changed()
                    withContext(Dispatchers.Main) { go.openCreated(id) }
                }
            },
            onDismiss = { renew = false },
        )
    }
}

/** Twelve weeks as bars of their execution score, a dashed 85 % line, and the review week at the end. */
@Composable
private fun WeekStrip(goal: Goal, world: World, hue: Color, current: Int, selected: Int, onSelect: (Int) -> Unit) {
    val line = U.c.faint
    Box(Modifier.fillMaxWidth().height(84.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..13).forEach { w ->
                val s = if (w <= 12 && current > 0 && w <= current) world.weekScore(goal, w) else null
                val isSel = w == selected
                val isNow = w == current
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSel) hue.copy(alpha = 0.12f) else Color.Transparent)
                        .border(1.dp, if (isNow) hue.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(7.dp))
                        .clickable(enabled = w <= 12) { onSelect(w) }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (w == 13) {
                            Text("★", fontSize = 12.sp, color = if (current >= 13) hue else U.c.faint)
                        } else {
                            Box(Modifier.fillMaxWidth(0.62f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(U.c.line.copy(alpha = 0.5f)))
                            if (s != null && s > 0f) {
                                Box(
                                    Modifier.fillMaxWidth(0.62f).fillMaxHeight(s.coerceIn(0.04f, 1f)).clip(RoundedCornerShape(3.dp))
                                        .background(if (s >= GOOD) U.c.good else hue.copy(alpha = if (w == current) 1f else 0.75f)),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (w == 13) "13" else w.toString(),
                        fontFamily = Manrope, fontSize = 10.sp,
                        color = when {
                            isNow -> hue
                            isSel -> U.c.ink
                            else -> U.c.faint
                        },
                        textAlign = TextAlign.Center, maxLines = 1,
                    )
                }
            }
        }
        // The 85 % line, drawn across the bar area (bars sit above a 4 dp padding and the ~16 dp labels).
        Canvas(Modifier.fillMaxSize().padding(top = 4.dp, bottom = 23.dp)) {
            val y = size.height * (1 - GOOD)
            drawLine(line.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width * 12f / 13f, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
    }
}

@Composable
private fun TacticRow(t: Node, goal: Goal, world: World, hue: Color, week: Int, isCurrent: Boolean, onEdit: () -> Unit, onTick: () -> Unit) {
    val ws = goal.startDay + (week - 1) * 7L
    Row(Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onEdit).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val dueToday = isCurrent && world.dueOn(t, goal, world.day)
        when {
            !t.repeats -> StarCheck(t.done, onTick, color = hue, size = 32.dp)
            dueToday -> StarCheck(world.log(t.id, world.day) != null, onTick, color = hue, size = 32.dp)
            else -> Spacer(Modifier.width(32.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = UType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                add(scheduleLabel(t.days, t.perWeek))
                if (t.week == 0) add(stringResource(R.string.node_every_week))
            }.joinToString(" · ")
            Text(meta, style = UType.Small)
        }
        if (t.repeats) {
            val planned = if (t.days != 0) (ws..ws + 6).count { world.planned(t, it) } else t.perWeek
            val done = if (t.days != 0) (ws..ws + 6).count { world.log(t.id, it) != null } else minOf(t.perWeek, world.weekCount(t.id, ws))
            Text("$done/$planned", style = UType.Small.copy(color = if (planned > 0 && done >= planned) U.c.good else U.c.muted))
        }
    }
}

@Composable
private fun VisionDialog(initial: String, hue: Color, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(stringResource(R.string.w12_vision), style = UType.Heading) },
        text = {
            Column {
                Text(stringResource(R.string.w12_vision_text), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                UTextField(text, { text = it.take(600) }, placeholder = stringResource(R.string.w12_vision_hint), minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text); onDismiss() }) { Text(stringResource(R.string.save), color = hue, style = UType.Button) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}
