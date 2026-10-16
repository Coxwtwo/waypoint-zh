package app.waypoint.ui.methods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.tick
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.dateShort
import app.waypoint.ui.scheduleLabel
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** A plain goal: an optional number, a few milestones and the habits that move it. */
@Composable
fun SimpleBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.SIMPLE)
    val context = LocalContext.current
    val container = context.container
    val milestones = w.nodesOf(goal.id, NodeKind.MILESTONE).filter { it.title.isNotBlank() }
        .sortedWith(compareBy<Node>({ it.done }, { it.dueDay ?: Long.MAX_VALUE }, { it.slot }))
    val habits = w.nodesOf(goal.id, NodeKind.ACTION).filter { it.title.isNotBlank() }
    var editMilestone by remember { mutableStateOf<Node?>(null) }
    var editHabit by remember { mutableStateOf<Node?>(null) }
    var update by remember { mutableStateOf(false) }
    var addNumber by remember { mutableStateOf(false) }

    // The number, if the goal has one.
    if (goal.hasMetric) {
        MetricCard(goal, w, color, stringResource(R.string.simple_number), onUpdate = { update = true })
    } else if (addNumber) {
        MetricSetup(goal, color, stringResource(R.string.simple_number_title), stringResource(R.string.simple_number_text), askDeadline = false, onDone = { addNumber = false })
    } else {
        TextButton(onClick = { addNumber = true }) { Text(stringResource(R.string.simple_add_number), color = color, style = UType.Button) }
    }

    // Milestones.
    SectionTitle(stringResource(R.string.simple_milestones), action = stringResource(R.string.add), onAction = {
        editMilestone = Node(goalId = goal.id, kind = NodeKind.MILESTONE.name, slot = milestones.size)
    })
    UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        if (milestones.isEmpty()) {
            Text(stringResource(R.string.simple_milestones_empty), style = UType.Sub, modifier = Modifier.padding(6.dp))
        }
        milestones.forEachIndexed { i, m ->
            if (i > 0) HorizontalDivider(color = U.c.line)
            val overdue = !m.done && m.dueDay != null && m.dueDay < w.day
            StepRow(
                checked = m.done,
                title = m.title,
                sub = when {
                    m.done -> m.doneAt?.let { stringResource(R.string.simple_reached_on, dateShort(World.epochDay(it))) } ?: stringResource(R.string.simple_reached)
                    m.dueDay == null -> null
                    overdue -> stringResource(R.string.simple_overdue, dateShort(m.dueDay))
                    else -> dateShort(m.dueDay)
                },
                subDanger = overdue,
                color = color,
                onCheck = {
                    if (!m.done) Haptics.star(context)
                    container.write { tick(m, w.day) }
                },
                onEdit = { editMilestone = m },
            )
        }
    }

    // Habits.
    SectionTitle(stringResource(R.string.simple_habits), action = stringResource(R.string.add), onAction = {
        editHabit = Node(goalId = goal.id, kind = NodeKind.ACTION.name, days = World.EVERY_DAY, slot = habits.size)
    })
    UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        if (habits.isEmpty()) {
            Text(stringResource(R.string.simple_habits_empty), style = UType.Sub, modifier = Modifier.padding(6.dp))
        }
        habits.forEachIndexed { i, h ->
            if (i > 0) HorizontalDivider(color = U.c.line)
            val done = w.log(h.id, w.day) != null
            val streak = w.streak(h)
            val schedule = scheduleLabel(h.days, h.perWeek)
            StepRow(
                checked = done,
                title = h.title,
                sub = if (streak > 0) stringResource(R.string.simple_streak, schedule, streak) else schedule,
                subDanger = false,
                color = color,
                onCheck = {
                    if (!done) Haptics.tick(context)
                    container.write { tick(h, w.day) }
                },
                onEdit = { editHabit = h },
            )
        }
    }

    if (update) {
        NumberPrompt(stringResource(R.string.simple_update_title), goal.currentValue, goal.unit, color, { update = false }) { v ->
            container.write { upsertGoal(goal.copy(currentValue = v)) }
        }
    }
    editMilestone?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.simple_new_milestone else R.string.simple_edit_milestone),
            node = n,
            spec = NodeSpec(titleLabel = stringResource(R.string.simple_milestone_label), titleHint = stringResource(R.string.simple_milestone_hint), due = true),
            onDismiss = { editMilestone = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
    editHabit?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.simple_new_habit else R.string.simple_edit_habit),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.simple_habit_label),
                titleHint = stringResource(R.string.simple_habit_hint),
                schedule = true,
                allowOnce = false,
                remind = true,
            ),
            onDismiss = { editHabit = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
}

@Composable
private fun StepRow(
    checked: Boolean,
    title: String,
    sub: String?,
    subDanger: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onCheck: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        StarCheck(checked, onCheck, color = color, size = 36.dp, label = title)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).clip(U.RSm).clickable(onClick = onEdit).padding(vertical = 8.dp, horizontal = 4.dp)) {
            Text(title, style = UType.Body.copy(color = if (checked) U.c.muted else U.c.ink))
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = UType.Small.copy(color = if (subDanger) U.c.danger else U.c.faint))
            }
        }
    }
}
