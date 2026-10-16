package app.waypoint.ui.lenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.clockTime
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** Wish, outcome, obstacle, plan: one step at a time, ending with if-then plans. */
@Composable
fun WoopLens(goal: Goal, data: WData, go: Go) {
    val color = U.c.hue(Method.WOOP)
    val container = LocalContext.current.container
    val wish = rememberField(goal.id, "woop.wish", data, initial = goal.title)
    val outcome = rememberField(goal.id, "woop.outcome", data)
    val obstacle = rememberField(goal.id, "woop.obstacle", data)
    val plans = data.world.nodesOf(goal.id, NodeKind.PLAN)
    var step by rememberSaveable {
        mutableIntStateOf(
            when {
                data.world.field(goal.id, "woop.wish").isBlank() -> 0
                outcome.text.isBlank() -> 1
                obstacle.text.isBlank() -> 2
                else -> 3
            }
        )
    }
    var editing by remember { mutableStateOf<Node?>(null) }
    val letters = listOf(R.string.woop_w_letter, R.string.woop_o_letter, R.string.woop_o_letter, R.string.woop_p_letter).map { stringResource(it) }

    LensScaffold(Method.WOOP, goal, go) {
        StepTabs(letters, step, listOf(wish.text.isNotBlank(), outcome.text.isNotBlank(), obstacle.text.isNotBlank(), plans.isNotEmpty()), color) { step = it }
        Spacer(Modifier.height(20.dp))
        when (step) {
            0 -> {
                Question(letters[0], stringResource(R.string.woop_wish), stringResource(R.string.woop_wish_hint), color)
                Spacer(Modifier.height(14.dp))
                UTextField(wish.text, { wish.set(it.take(300)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 2)
                Example(stringResource(R.string.woop_wish_example))
            }
            1 -> {
                Question(letters[1], stringResource(R.string.woop_outcome), stringResource(R.string.woop_outcome_hint), color)
                Spacer(Modifier.height(14.dp))
                Tip(stringResource(R.string.woop_outcome_pause), color)
                Spacer(Modifier.height(14.dp))
                UTextField(outcome.text, { outcome.set(it.take(500)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 3)
                Example(stringResource(R.string.woop_outcome_example))
            }
            2 -> {
                Question(letters[2], stringResource(R.string.woop_obstacle), stringResource(R.string.woop_obstacle_hint), color)
                Spacer(Modifier.height(14.dp))
                Tip(stringResource(R.string.woop_obstacle_pause), color)
                Spacer(Modifier.height(14.dp))
                UTextField(obstacle.text, { obstacle.set(it.take(500)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 3)
                Example(stringResource(R.string.woop_obstacle_example))
            }
            else -> {
                Question(letters[3], stringResource(R.string.woop_plan), stringResource(R.string.woop_plan_hint), color)
                Spacer(Modifier.height(14.dp))
                if (obstacle.text.isNotBlank()) {
                    UCard {
                        Text(stringResource(R.string.woop_your_obstacle), style = UType.Small.copy(color = color))
                        Spacer(Modifier.height(4.dp))
                        Text(obstacle.text.trim(), style = UType.Body)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                plans.forEach { p ->
                    PlanCard(p, color) { editing = p }
                    Spacer(Modifier.height(10.dp))
                }
                SoftButton(
                    stringResource(if (plans.isEmpty()) R.string.woop_add_first_plan else R.string.woop_add_plan),
                    { editing = Node(goalId = goal.id, kind = NodeKind.PLAN.name, title = if (plans.isEmpty()) obstacle.text.trim() else "", slot = plans.size) },
                    Modifier.fillMaxWidth(), color = color, icon = Icons.Rounded.Add,
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.lens_backup_course), style = UType.Muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) SoftButton(stringResource(R.string.back), { step-- }, Modifier.weight(1f), color = color)
            if (step < 3) PrimaryButton(stringResource(R.string.next), { step++ }, Modifier.weight(1f), color = color)
            else PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.weight(1f), color = color)
        }

        if (plans.isNotEmpty() && step < 3) {
            SectionTitle(stringResource(R.string.woop_plans))
            plans.forEach { p ->
                PlanCard(p, color) { editing = p }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    editing?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.woop_new_plan else R.string.woop_edit_plan),
            node = n,
            spec = NodeSpec(
                titleLabel = stringResource(R.string.woop_if),
                titleHint = stringResource(R.string.woop_if_hint),
                noteLabel = stringResource(R.string.woop_then),
                noteHint = stringResource(R.string.woop_then_hint),
                remind = true,
            ),
            onDismiss = { editing = null },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved -> container.write { if (saved.id == 0L) insertNode(saved) else upsertNode(saved) } }
    }
}

/** "If … → then …", the shape every if-then plan is shown in. */
@Composable
internal fun PlanCard(p: Node, color: Color, ifLabel: Int = R.string.woop_if_short, thenLabel: Int = R.string.woop_then_short, onClick: () -> Unit) {
    UCard(onClick = onClick, border = color.copy(alpha = 0.35f)) {
        Text(stringResource(ifLabel), style = UType.Small.copy(color = color))
        Text(p.title, style = UType.Strong)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(thenLabel), style = UType.Small.copy(color = color))
        if (p.note.isNotBlank()) Text(p.note, style = UType.Body)
        else Text(stringResource(R.string.woop_then_missing), style = UType.Muted)
        if (p.remindAt >= 0) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.lens_reminder_at, clockTime(p.remindAt)), style = UType.Small)
        }
    }
}
