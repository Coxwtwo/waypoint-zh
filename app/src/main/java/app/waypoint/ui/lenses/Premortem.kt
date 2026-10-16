package app.waypoint.ui.lenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private val prompts = listOf(
    R.string.pm_p_motivation, R.string.pm_p_time, R.string.pm_p_sick, R.string.pm_p_perfect,
    R.string.pm_p_life, R.string.pm_p_alone, R.string.pm_p_big, R.string.pm_p_money,
)

/** Imagine the goal failed, list why, and give every reason a way to prevent it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremortemLens(goal: Goal, data: WData, go: Go) {
    val color = U.c.hue(Method.PREMORTEM)
    val container = LocalContext.current.container
    val risks = data.world.nodesOf(goal.id, NodeKind.RISK)
    var editing by remember { mutableStateOf<Node?>(null) }
    fun new(title: String = "") = Node(goalId = goal.id, kind = NodeKind.RISK.name, title = title, slot = risks.size)

    LensScaffold(Method.PREMORTEM, goal, go) {
        val day = goal.endDay?.let { dateShort(it) }
        Tip(if (day != null) stringResource(R.string.pm_intro_date, day) else stringResource(R.string.pm_intro), color)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.pm_intro_hint), style = UType.Muted)

        SectionTitle(stringResource(R.string.pm_reasons))
        if (risks.isEmpty()) Text(stringResource(R.string.pm_empty), style = UType.Muted)
        risks.forEach { r ->
            PlanCard(r, color, ifLabel = R.string.pm_failed, thenLabel = R.string.pm_prevent_short) { editing = r }
            Spacer(Modifier.height(10.dp))
        }
        SoftButton(stringResource(R.string.pm_add), { editing = new() }, Modifier.fillMaxWidth(), color = color, icon = Icons.Rounded.Add)

        SectionTitle(stringResource(R.string.pm_common))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { p ->
                val t = stringResource(p)
                Chip(t, risks.any { it.title == t }, { if (risks.none { it.title == t }) editing = new(t) }, color = color)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.lens_backup_course), style = UType.Muted)
    }

    editing?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.pm_new else R.string.pm_edit),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.pm_failed_label),
                titleHint = stringResource(R.string.pm_failed_hint),
                noteLabel = stringResource(R.string.pm_prevent),
                noteHint = stringResource(R.string.pm_prevent_hint),
                remind = true,
            ),
            onDismiss = { editing = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
}
