package app.waypoint.ui.methods

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.components.DateDialog
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.navStar
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private val RailWidth = 40.dp

/**
 * Reverse planning: the goal at the top, steps hanging below it by date, down to today.
 * Each new step answers "what has to be done just before the one above?".
 */
@Composable
fun LadderBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.LADDER)
    val context = LocalContext.current
    val container = context.container
    val steps = w.nodesOf(goal.id, NodeKind.STEP)
        .filter { it.title.isNotBlank() }
        .sortedWith(compareByDescending<Node> { it.dueDay ?: Long.MIN_VALUE }.thenByDescending { it.slot })
    val next = steps.lastOrNull { !it.done }
    val lowest = steps.lastOrNull()
    var editing by remember { mutableStateOf<Node?>(null) }
    var pickGoalDate by remember { mutableStateOf(false) }

    Text(stringResource(R.string.ladder_intro), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
    Spacer(Modifier.height(14.dp))

    // The summit.
    RungRow(color, isTop = true, isBottom = false, marker = {
        Canvas(Modifier.size(34.dp)) { navStar(Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, color, 1f) }
    }) {
        Column(Modifier.clip(U.RSm).clickable { pickGoalDate = true }.padding(vertical = 8.dp)) {
            Text(stringResource(R.string.ladder_summit).uppercase(), style = UType.Eyebrow.copy(color = color))
            Spacer(Modifier.height(2.dp))
            Text(goal.title, style = UType.Strong)
            Text(goal.endDay?.let { dateShort(it) } ?: stringResource(R.string.ladder_set_date), style = UType.Small.copy(color = if (goal.endDay == null) color else U.c.faint))
        }
    }

    steps.forEach { s ->
        val overdue = !s.done && s.dueDay != null && s.dueDay < w.day
        RungRow(color, isTop = false, isBottom = false, marker = {
            StarCheck(s.done, {
                if (!s.done) Haptics.star(context)
                container.write { tick(s, w.day) }
            }, color = color, size = 34.dp, label = s.title)
        }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(U.RSm)
                    .background(if (s == next) color.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { editing = s }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (s == next) Text(stringResource(R.string.ladder_next).uppercase(), style = UType.Eyebrow.copy(color = color))
                Text(s.title, style = UType.Body.copy(color = if (s.done) U.c.muted else U.c.ink))
                Text(
                    when {
                        s.dueDay == null -> stringResource(R.string.ladder_no_date)
                        overdue -> stringResource(R.string.ladder_overdue, dateShort(s.dueDay))
                        else -> dateShort(s.dueDay)
                    },
                    style = UType.Small.copy(color = if (overdue) U.c.danger else U.c.faint),
                )
            }
        }
    }

    // Ground level: today, and the question that adds the next rung down.
    RungRow(color, isTop = false, isBottom = true, marker = {
        Box(Modifier.size(12.dp).clip(CircleShape).background(U.c.faint))
    }) {
        Text(stringResource(R.string.ladder_today, dateShort(w.day)), style = UType.Small, modifier = Modifier.padding(vertical = 10.dp))
    }

    Spacer(Modifier.height(14.dp))
    UCard(border = color.copy(alpha = 0.35f)) {
        val above = lowest?.title ?: goal.title
        Text(stringResource(R.string.ladder_question, above), style = UType.Heading.copy(color = U.c.ink))
        val reached = lowest?.dueDay?.let { it <= w.day + 7 } == true
        if (reached) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.ladder_reached), style = UType.Muted)
        }
        Spacer(Modifier.height(12.dp))
        SoftButton(stringResource(R.string.ladder_add), {
            val from = lowest?.dueDay ?: goal.endDay
            val due = from?.let { maxOf(it - 7, w.day) } ?: (w.day + 7)
            editing = Node(goalId = goal.id, kind = NodeKind.STEP.name, dueDay = due, slot = steps.size)
        }, Modifier.fillMaxWidth(), color = color, icon = Icons.Rounded.Add)
    }

    editing?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.ladder_new else R.string.ladder_edit),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.ladder_step_label),
                titleHint = stringResource(R.string.ladder_step_hint),
                due = true,
                remind = true,
            ),
            onDismiss = { editing = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
    if (pickGoalDate) {
        DateDialog(goal.endDay, { pickGoalDate = false }, onClear = { container.write { upsertGoal(goal.copy(endDay = null)) } }, minDay = goal.startDay) { d ->
            container.write { upsertGoal(goal.copy(endDay = d)) }
        }
    }
}

/** One rung: the rail on the left (a line through a marker), the content on the right. */
@Composable
private fun RungRow(color: Color, isTop: Boolean, isBottom: Boolean, marker: @Composable () -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val mid = size.height / 2
                    val stroke = 1.5.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    if (!isTop) drawLine(color.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, mid), stroke)
                    if (!isBottom) drawLine(color.copy(alpha = 0.35f), Offset(x, mid), Offset(x, size.height), stroke)
                    if (isBottom) drawLine(color.copy(alpha = 0.2f), Offset(x - 10.dp.toPx(), mid), Offset(x + 10.dp.toPx(), mid), stroke, pathEffect = dash)
                },
            contentAlignment = Alignment.Center,
        ) { marker() }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}
