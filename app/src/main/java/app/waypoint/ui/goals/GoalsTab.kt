package app.waypoint.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.MethodBadge
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.levelsOf
import app.waypoint.ui.daysLeftLabel
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

@Composable
fun GoalsTab(data: WData, go: Go) {
    val w = data.world
    val limit = data.settings.focusLimit
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.goals_title), style = UType.Display)
                if (w.active.isNotEmpty()) {
                    Text(
                        if (limit > 0) stringResource(R.string.goals_focus, w.active.size, limit)
                        else pluralStringResource(R.plurals.goals, w.active.size, w.active.size),
                        style = UType.Muted,
                    )
                }
            }
            IconButton(onClick = { go.newGoal() }) { Icon(Icons.Rounded.Add, stringResource(R.string.goal_new), tint = U.c.accent) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            if (w.active.isEmpty() && w.queued.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                EmptyState(stringResource(R.string.goals_empty))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton(stringResource(R.string.goal_new), { go.newGoal() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                    SoftButton(stringResource(R.string.goals_quiz), { go.quiz() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Quiz)
                    SoftButton(stringResource(R.string.goals_unsure), { go.tool(Method.WHEEL) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Explore)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    w.active.forEach { GoalCard(it, data) { go.goal(it.id) } }
                }
                if (w.queued.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.goals_queue))
                    Text(stringResource(R.string.goals_queue_text), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        w.queued.forEach { QueuedCard(it, data, go) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SoftButton(stringResource(R.string.goal_new), { go.newGoal() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
        }
    }
}

/** Brightness of a goal's eight stars: per pillar for a mandala, evenly from the progress otherwise. */
fun goalLevels(goal: Goal, data: WData): List<Float> {
    val w = data.world
    if (Method.of(goal.method) == Method.MANDALA) {
        val pillars = w.nodesOf(goal.id, NodeKind.PILLAR).associateBy { it.slot }
        return List(8) { slot ->
            val p = pillars[slot] ?: return@List 0f
            w.children(p.id).count { it.done } / 8f
        }
    }
    return levelsOf(w.progress(goal).fraction, 8)
}

/** A goal on the list: its constellation, title, method and how far it has come. */
@Composable
fun GoalCard(goal: Goal, data: WData, onClick: () -> Unit) {
    val w = data.world
    val m = Method.of(goal.method)
    val hue = U.c.hue(m)
    val p = w.progress(goal)
    val todayCount = w.todayItems.count { it.goal.id == goal.id }
    val todayDone = w.todayItems.count { it.goal.id == goal.id && it.done }
    UCard(onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Constellation(goal.seed, goalLevels(goal, data), hue, Modifier.size(width = 84.dp, height = 64.dp), starSize = 5.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(m)
                    Spacer(Modifier.width(8.dp))
                    w.daysLeft(goal)?.let { Text(daysLeftLabel(it), style = UType.Small, maxLines = 1) }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${(p.fraction * 100).toInt()}%", style = UType.Number.copy(fontSize = 17.sp, color = hue))
        }
        Spacer(Modifier.height(10.dp))
        Bar(p.fraction, color = hue, height = 4.dp)
        if (todayCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.goal_today_steps, todayDone, todayCount), style = UType.Small)
        }
    }
}

@Composable
private fun QueuedCard(goal: Goal, data: WData, go: Go) {
    val container = LocalContext.current.container
    val m = Method.of(goal.method)
    UCard(onClick = { go.goal(goal.id) }, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = UType.Strong.copy(color = U.c.muted), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                MethodBadge(m)
            }
            if (!data.world.overLimit(data.settings.focusLimit)) {
                TextButton(onClick = {
                    container.write { upsertGoal(goal.copy(status = Goal.ACTIVE, startDay = maxOf(goal.startDay, data.day))) }
                }) { Text(stringResource(R.string.goal_start_now), style = UType.Button, color = U.c.accent) }
            }
        }
    }
}
