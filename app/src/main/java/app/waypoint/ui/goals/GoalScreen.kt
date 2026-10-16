package app.waypoint.ui.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.domain.Area
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.areaIcon
import app.waypoint.ui.areaName
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.ConfirmDialog
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.MethodBadge
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.motionOn
import app.waypoint.ui.dateOf
import app.waypoint.ui.dateShort
import app.waypoint.ui.daysLeftLabel
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodShort
import app.waypoint.ui.methods.FourDxBody
import app.waypoint.ui.methods.KaizenBody
import app.waypoint.ui.methods.LadderBody
import app.waypoint.ui.methods.MandalaBody
import app.waypoint.ui.methods.OkrBody
import app.waypoint.ui.methods.PactBody
import app.waypoint.ui.methods.SimpleBody
import app.waypoint.ui.methods.Weeks12Body
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** One goal: its constellation and progress on top, the method's own screen below, then the lenses. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalScreen(id: Long, data: WData, go: Go) {
    val goal = data.world.goal(id)
    if (goal == null) {
        // Deleted while open.
        LaunchedEffect(Unit) { go.back() }
        return
    }
    val context = LocalContext.current
    val container = context.container
    val w = data.world
    val m = Method.of(goal.method)
    val hue = U.c.hue(m)
    val p = w.progress(goal)
    var menu by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar("", onBack = { go.back() }) {
            IconButton(onClick = { go.editGoal(goal.id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = U.c.muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = U.c.surface2) {
                    if (goal.status == Goal.ACTIVE || goal.status == Goal.QUEUED) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.goal_reached)) }, onClick = { menu = false; closing = Goal.DONE })
                        DropdownMenuItem(text = { Text(stringResource(R.string.goal_drop)) }, onClick = { menu = false; closing = Goal.DROPPED })
                    }
                    if (goal.status == Goal.ACTIVE) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.goal_to_queue)) }, onClick = {
                            menu = false
                            container.write { upsertGoal(goal.copy(status = Goal.QUEUED)) }
                        })
                    }
                    if (goal.status != Goal.ACTIVE) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.goal_make_active)) }, onClick = {
                            menu = false
                            container.write { upsertGoal(goal.copy(status = Goal.ACTIVE, closedAt = null, startDay = if (goal.status == Goal.QUEUED) maxOf(goal.startDay, data.day) else goal.startDay)) }
                        })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.method_about_short, methodShort(m))) }, onClick = { menu = false; go.method(m) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = U.c.danger) }, onClick = { menu = false; confirmDelete = true })
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 28.dp)) {
            Header(goal, data)
            when (goal.status) {
                Goal.QUEUED -> QueuedBanner(goal, data)
                Goal.DONE, Goal.DROPPED -> ClosedBanner(goal)
                else -> if (p.fraction >= 1f) ReachedBanner { closing = Goal.DONE }
            }
            Spacer(Modifier.height(8.dp))
            when (m) {
                Method.MANDALA -> MandalaBody(goal, data, go)
                Method.OKR -> OkrBody(goal, data, go)
                Method.WEEKS12 -> Weeks12Body(goal, data, go)
                Method.FOURDX -> FourDxBody(goal, data, go)
                Method.KAIZEN -> KaizenBody(goal, data, go)
                Method.PACT -> PactBody(goal, data, go)
                Method.LADDER -> LadderBody(goal, data, go)
                else -> SimpleBody(goal, data, go)
            }

            SectionTitle(stringResource(R.string.goal_lenses))
            Text(stringResource(R.string.goal_lenses_text), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            val used = w.lensesOf(goal.id)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Method.lenses.forEach { l ->
                    val on = l in used
                    Chip(methodShort(l), on, { go.lens(goal.id, l) }, color = U.c.hue(l), icon = if (on) methodIcon(l) else Icons.Rounded.Add)
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { go.method(m) }) {
                Icon(Icons.Rounded.Info, null, tint = U.c.faint)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.method_about_short, methodShort(m)), style = UType.Small)
            }
        }
    }

    closing?.let { status ->
        CloseDialog(status, onDismiss = { closing = null }) { note ->
            container.write { upsertGoal(goal.copy(status = status, closedAt = System.currentTimeMillis(), note = note)) }
            if (status == Goal.DONE) {
                if (data.settings.haptics) Haptics.star(context)
                celebrate = true
            }
        }
    }
    if (celebrate) Celebration(goal) { celebrate = false }
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.goal_delete), stringResource(R.string.goal_delete_text), stringResource(R.string.delete),
            onConfirm = { container.write { deleteGoal(goal.id) }; go.back() },
            onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(goal: Goal, data: WData) {
    val w = data.world
    val m = Method.of(goal.method)
    val hue = U.c.hue(m)
    val p = w.progress(goal)
    Column(Modifier.fillMaxWidth()) {
        Constellation(goal.seed, goalLevels(goal, data), hue, Modifier.fillMaxWidth().height(150.dp), starSize = 9.dp)
        Spacer(Modifier.height(4.dp))
        Text(goal.title, style = UType.Title)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            MethodBadge(m)
            Area.of(goal.area)?.let { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(areaIcon(a), null, Modifier.padding(end = 4.dp).height(14.dp), tint = U.c.muted)
                    Text(areaName(a), style = UType.Small.copy(color = U.c.muted))
                }
            }
            if (goal.status == Goal.ACTIVE) w.daysLeft(goal)?.let { Text("${daysLeftLabel(it)} · ${dateShort(goal.endDay!!)}", style = UType.Small) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${(p.fraction * 100).toInt()}", style = UType.Big.copy(color = hue))
            Text("%", style = UType.Heading.copy(color = hue), modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
            Spacer(Modifier.width(12.dp))
            if (p.total > 0) {
                Text(
                    stringResource(R.string.goal_progress_of, p.done, p.total),
                    style = UType.Muted, modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
        if (goal.why.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            UCard(border = hue.copy(alpha = 0.25f)) {
                Text(stringResource(R.string.goal_why).uppercase(), style = UType.Eyebrow.copy(color = hue))
                Spacer(Modifier.height(4.dp))
                Text(goal.why, style = UType.Body.copy(fontStyle = FontStyle.Italic))
            }
        }
    }
}

@Composable
private fun ReachedBanner(onReach: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    UCard(border = U.c.accent.copy(alpha = 0.5f)) {
        Text(stringResource(R.string.goal_full_title), style = UType.Strong.copy(color = U.c.accent))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.goal_full_text), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(stringResource(R.string.goal_reached), onReach, Modifier.fillMaxWidth())
    }
}

@Composable
private fun QueuedBanner(goal: Goal, data: WData) {
    val container = LocalContext.current.container
    Spacer(Modifier.height(12.dp))
    UCard {
        Text(stringResource(R.string.goal_queued_title), style = UType.Strong)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.goals_queue_text), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(stringResource(R.string.goal_start_now), {
            container.write { upsertGoal(goal.copy(status = Goal.ACTIVE, startDay = maxOf(goal.startDay, data.day))) }
        }, Modifier.fillMaxWidth())
        if (data.world.overLimit(data.settings.focusLimit)) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.goal_over_limit, data.settings.focusLimit), style = UType.Small)
        }
    }
}

@Composable
private fun ClosedBanner(goal: Goal) {
    val done = goal.status == Goal.DONE
    Spacer(Modifier.height(12.dp))
    UCard(border = if (done) U.c.accent.copy(alpha = 0.5f) else U.c.line) {
        Text(
            stringResource(if (done) R.string.goal_closed_done else R.string.goal_closed_dropped, goal.closedAt?.let { dateOf(it) } ?: ""),
            style = UType.Strong.copy(color = if (done) U.c.accent else U.c.muted),
        )
        if (goal.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(goal.note, style = UType.Body.copy(fontStyle = FontStyle.Italic))
        }
    }
}

/** Reached or let go: either way, one line about what this goal taught you. */
@Composable
private fun CloseDialog(status: Int, onDismiss: () -> Unit, onClose: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    val done = status == Goal.DONE
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(4.dp)) {
            UCard(padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(stringResource(if (done) R.string.close_done_title else R.string.close_drop_title), style = UType.Heading)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(if (done) R.string.close_done_text else R.string.close_drop_text), style = UType.Muted)
                Spacer(Modifier.height(12.dp))
                UTextField(note, { note = it.take(400) }, placeholder = stringResource(if (done) R.string.close_done_hint else R.string.close_drop_hint), minLines = 3)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
                    Spacer(Modifier.width(4.dp))
                    PrimaryButton(stringResource(if (done) R.string.goal_reached else R.string.goal_drop), { onClose(note.trim()); onDismiss() })
                }
            }
        }
    }
}

/** Every star of the constellation lights up, one after another. */
@Composable
private fun Celebration(goal: Goal, onDismiss: () -> Unit) {
    val hue = U.c.hue(Method.of(goal.method))
    val lit = remember { Animatable(if (motionOn()) 0f else 8f) }
    LaunchedEffect(Unit) { lit.animateTo(8f, tween(1600)) }
    Dialog(onDismissRequest = onDismiss) {
        UCard(padding = androidx.compose.foundation.layout.PaddingValues(22.dp), border = hue.copy(alpha = 0.5f)) {
            Constellation(goal.seed, List(8) { i -> (lit.value - i).coerceIn(0f, 1f) }, hue, Modifier.fillMaxWidth().height(170.dp), starSize = 10.dp)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.celebrate_title), style = UType.Title.copy(textAlign = TextAlign.Center, fontSize = 22.sp), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(goal.title, style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.celebrate_ok), onDismiss, Modifier.fillMaxWidth(), color = hue)
        }
    }
}
