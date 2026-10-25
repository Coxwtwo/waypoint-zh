package app.waypoint.ui.methods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.UCard
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** Progress tracker: a goal split into counted tasks, each bumped up or down from Today. */
@Composable
fun TrackerBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.TRACKER)
    val container = LocalContext.current.container
    val tasks = w.nodesOf(goal.id, NodeKind.TASK).filter { it.title.isNotBlank() }
        .sortedWith(compareBy<Node>({ it.startDay ?: Long.MAX_VALUE }, { it.dueDay ?: Long.MAX_VALUE }, { it.slot }))
    var editTask by remember { mutableStateOf<Node?>(null) }

    SectionTitle(stringResource(R.string.tracker_tasks), action = stringResource(R.string.add), onAction = {
        editTask = Node(goalId = goal.id, kind = NodeKind.TASK.name, slot = tasks.size)
    })
    UCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        if (tasks.isEmpty()) {
            Text(stringResource(R.string.tracker_empty), style = UType.Sub, modifier = Modifier.padding(6.dp))
        }
        tasks.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(color = U.c.line)
            TaskRow(t, w, color) { editTask = t }
        }
    }

    editTask?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.tracker_new_task else R.string.tracker_edit_task),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.tracker_task_label),
                titleHint = stringResource(R.string.tracker_task_hint),
                metric = true,
                increment = true,
                schedule = true,
                period = true,
            ),
            onDismiss = { editTask = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
}

@Composable
private fun TaskRow(t: Node, w: World, color: Color, onEdit: () -> Unit) {
    val current = w.trackerCurrent(t)
    val f = w.metricFraction(t.start, t.target, current)
    Column(Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onEdit).padding(vertical = 10.dp, horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.title, style = UType.Body, modifier = Modifier.weight(1f))
            Text("${(f * 100).toInt()}%", style = UType.Number.copy(color = color))
        }
        Spacer(Modifier.height(6.dp))
        Bar(f, color = color)
        Spacer(Modifier.height(4.dp))
        Row {
            Text(withUnit(current, t.unit), style = UType.Small.copy(color = color))
            Spacer(Modifier.weight(1f))
            Text(withUnit(t.target, t.unit), style = UType.Small)
        }
        val days = periodDays(t)
        if (days != null) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.tracker_days, days), style = UType.Small)
        }
    }
}

private fun periodDays(t: Node): Long? =
    if (t.startDay != null && t.dueDay != null) t.dueDay!! - t.startDay!! + 1 else null
