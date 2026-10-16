package app.waypoint.ui.lenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.setDone
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.RoundIcon
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.dateShort
import app.waypoint.ui.scheduleLabel
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/**
 * Goal, reality, options, will. Options are OPTION nodes (done = chosen); the will is ACTION nodes,
 * each hanging under the option it came from, so they can be told apart from the goal's other actions.
 */
@Composable
fun GrowLens(goal: Goal, data: WData, go: Go) {
    val color = U.c.hue(Method.GROW)
    val container = LocalContext.current.container
    val g = rememberField(goal.id, "grow.goal", data)
    val reality = rememberField(goal.id, "grow.reality", data)
    val options = data.world.nodesOf(goal.id, NodeKind.OPTION)
    val optionIds = options.map { it.id }.toSet()
    val actions = data.world.nodesOf(goal.id, NodeKind.ACTION).filter { it.parentId in optionIds }
    var step by rememberSaveable {
        mutableIntStateOf(
            when {
                g.text.isBlank() -> 0
                reality.text.isBlank() -> 1
                options.none { it.done } -> 2
                else -> 3
            }
        )
    }
    var newOption by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Node?>(null) }
    /** For a brand-new action typed in the W step: the option it should be filed under, created on save. */
    var freeAction by remember { mutableStateOf(false) }
    val letters = listOf(R.string.grow_g_letter, R.string.grow_r_letter, R.string.grow_o_letter, R.string.grow_w_letter).map { stringResource(it) }

    fun addOption() {
        val t = newOption.trim()
        if (t.isEmpty()) return
        container.write { insertNode(Node(goalId = goal.id, kind = NodeKind.OPTION.name, title = t, slot = options.size)) }
        newOption = ""
    }

    LensScaffold(Method.GROW, goal, go) {
        StepTabs(letters, step, listOf(g.text.isNotBlank(), reality.text.isNotBlank(), options.isNotEmpty(), actions.isNotEmpty()), color) { step = it }
        Spacer(Modifier.height(20.dp))
        when (step) {
            0 -> {
                Question(letters[0], stringResource(R.string.grow_goal), stringResource(R.string.grow_goal_hint), color)
                Spacer(Modifier.height(14.dp))
                UTextField(g.text, { g.set(it.take(400)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 3)
                Example(stringResource(R.string.grow_goal_example))
            }
            1 -> {
                Question(letters[1], stringResource(R.string.grow_reality), stringResource(R.string.grow_reality_hint), color)
                Spacer(Modifier.height(14.dp))
                Tip(stringResource(R.string.grow_reality_prompts), color)
                Spacer(Modifier.height(14.dp))
                UTextField(reality.text, { reality.set(it.take(800)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 4)
            }
            2 -> {
                Question(letters[2], stringResource(R.string.grow_options), stringResource(R.string.grow_options_hint), color)
                Spacer(Modifier.height(14.dp))
                options.forEach { o ->
                    OptionRow(o, color, onToggle = { container.write { setDone(o, !o.done) } }, onDelete = { container.write { deleteNode(o.id) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UTextField(newOption, { newOption = it.take(160) }, Modifier.weight(1f), placeholder = stringResource(R.string.grow_option_hint))
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Rounded.Add, enabled = newOption.isNotBlank(), color = color, size = 44.dp) { addOption() }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (options.isEmpty()) stringResource(R.string.grow_options_empty) else stringResource(R.string.grow_options_choose),
                    style = UType.Muted,
                )
            }
            else -> {
                Question(letters[3], stringResource(R.string.grow_will), stringResource(R.string.grow_will_hint), color)
                Spacer(Modifier.height(14.dp))
                val chosen = options.filter { it.done }
                if (chosen.isEmpty() && actions.isEmpty()) Text(stringResource(R.string.grow_will_empty), style = UType.Muted)
                chosen.forEach { o ->
                    val mine = actions.filter { it.parentId == o.id }
                    UCard(border = color.copy(alpha = 0.35f)) {
                        Text(o.title, style = UType.Strong)
                        mine.forEach { a ->
                            Spacer(Modifier.height(8.dp))
                            ActionLine(a) { editing = a }
                        }
                        Spacer(Modifier.height(10.dp))
                        SoftButton(
                            stringResource(if (mine.isEmpty()) R.string.grow_to_action else R.string.grow_another_action),
                            { editing = Node(goalId = goal.id, parentId = o.id, kind = NodeKind.ACTION.name, title = if (mine.isEmpty()) o.title else "") },
                            color = color,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // Actions whose option was un-chosen stay visible, so nothing silently disappears.
                actions.filter { a -> chosen.none { it.id == a.parentId } }.forEach { a ->
                    UCard { ActionLine(a) { editing = a } }
                    Spacer(Modifier.height(10.dp))
                }
                SoftButton(
                    stringResource(R.string.grow_new_action), { freeAction = true; editing = Node(goalId = goal.id, kind = NodeKind.ACTION.name) },
                    Modifier.fillMaxWidth(), color = color, icon = Icons.Rounded.Add,
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.grow_on_today), style = UType.Muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) SoftButton(stringResource(R.string.back), { step-- }, Modifier.weight(1f), color = color)
            if (step < 3) PrimaryButton(stringResource(R.string.next), { step++ }, Modifier.weight(1f), color = color)
            else PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.weight(1f), color = color)
        }
    }

    editing?.let { n ->
        NodeEditor(
            heading = stringResource(if (n.id == 0L) R.string.grow_new_step else R.string.grow_edit_step),
            node = n,
            spec = NodeSpec(titleLabel = stringResource(R.string.grow_step_label), titleHint = stringResource(R.string.grow_step_hint), schedule = true, due = true, remind = true),
            onDismiss = { editing = null; freeAction = false },
            onDelete = if (n.id != 0L) ({ container.write { deleteNode(n.id) } }) else null,
            color = color,
        ) { saved ->
            val free = freeAction
            container.write {
                when {
                    saved.id != 0L -> upsertNode(saved)
                    free -> {
                        // A step that came straight from the will: file it under a chosen option of the same name.
                        val option = insertNode(Node(goalId = goal.id, kind = NodeKind.OPTION.name, title = saved.title, slot = options.size, done = true, doneAt = System.currentTimeMillis()))
                        insertNode(saved.copy(parentId = option))
                    }
                    else -> insertNode(saved)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(o: Node, color: androidx.compose.ui.graphics.Color, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        StarCheck(o.done, onToggle, color = color, label = stringResource(R.string.grow_choose))
        Spacer(Modifier.width(10.dp))
        Text(o.title, style = if (o.done) UType.Strong else UType.Body.copy(color = U.c.muted), modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint) }
    }
}

@Composable
private fun ActionLine(a: Node, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text("→ " + a.title, style = UType.Body)
        val whenText = if (!a.repeats && a.dueDay != null) dateShort(a.dueDay) else scheduleLabel(a.days, a.perWeek)
        Text(whenText, style = UType.Small)
    }
}

