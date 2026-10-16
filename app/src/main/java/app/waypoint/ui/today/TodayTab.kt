package app.waypoint.ui.today

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.count
import app.waypoint.data.record
import app.waypoint.data.tick
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.TodayItem
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.Tab
import app.waypoint.ui.WData
import app.waypoint.ui.clockTime
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.MethodBadge
import app.waypoint.ui.components.RoundIcon
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.formatNumber
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.daysLeftLabel
import app.waypoint.ui.greeting
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import app.waypoint.ui.todayTitle
import java.time.LocalTime

@Composable
fun TodayTab(data: WData, go: Go, onTab: (Tab) -> Unit) {
    val w = data.world
    val items = w.todayItems
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(todayTitle(data.today), style = UType.Small)
                Spacer(Modifier.height(4.dp))
                val hello = greeting(LocalTime.now().hour)
                Text(if (data.settings.name.isBlank()) hello else "$hello, ${data.settings.name}", style = UType.Title)
            }
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            if (w.active.isEmpty()) {
                Spacer(Modifier.height(28.dp))
                EmptyState(stringResource(if (w.queued.isEmpty()) R.string.today_no_goals else R.string.today_only_queued))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton(stringResource(R.string.goal_new), { go.newGoal() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                    if (w.queued.isNotEmpty()) SoftButton(stringResource(R.string.today_open_queue), { onTab(Tab.GOALS) }, Modifier.fillMaxWidth())
                }
                return@Column
            }

            Spacer(Modifier.height(14.dp))
            SkyCard(items, data)

            ReviewNudge(data, go)

            SectionTitle(stringResource(R.string.today_course))
            if (items.isEmpty()) {
                UCard {
                    Text(stringResource(R.string.today_free), style = UType.Body)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.today_free_text), style = UType.Muted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { StepRow(it, data, go) }
                }
            }

            BackupCourse(data, go)
            Deadlines(data, go)
        }
    }
}

/** Today's steps as a small constellation: every finished step is a lit star. */
@Composable
private fun SkyCard(items: List<TodayItem>, data: WData) {
    val done = items.count { it.done }
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_sky).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (items.isEmpty()) stringResource(R.string.today_sky_clear)
                    else stringResource(R.string.today_sky_lit, done, items.size),
                    style = UType.Heading,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        items.isEmpty() -> stringResource(R.string.today_sky_clear_text)
                        done == items.size -> stringResource(R.string.today_sky_all)
                        done == 0 -> stringResource(R.string.today_sky_none)
                        else -> stringResource(R.string.today_sky_some)
                    },
                    style = UType.Muted,
                )
            }
            Spacer(Modifier.width(10.dp))
            val n = items.size.coerceIn(3, 12)
            Constellation(
                seed = data.day.toInt(),
                levels = List(n) { i -> if (i < items.size && items[i].done) 1f else 0f }.sortedDescending(),
                color = U.c.accent,
                modifier = Modifier.size(width = 120.dp, height = 84.dp),
                starSize = 6.dp,
            )
        }
    }
}

/** One step for today. Counted lead measures get a +1, kaizen gets today's number; everything else is a tick. */
@Composable
private fun StepRow(item: TodayItem, data: WData, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val n = item.node
    val g = item.goal
    val m = Method.of(g.method)
    val hue = U.c.hue(m)
    val w = data.world
    val isKaizen = m == Method.KAIZEN && n.kind == NodeKind.ACTION.name
    val isLead = n.kind == NodeKind.LEAD.name
    var askValue by remember { mutableStateOf(false) }

    UCard(onClick = { go.goal(g.id) }, padding = androidx.compose.foundation.layout.PaddingValues(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLead) {
                Spacer(Modifier.width(6.dp))
                RoundIcon(Icons.Rounded.Add, color = hue, size = 34.dp) {
                    if (data.settings.haptics) Haptics.tick(context)
                    container.write { count(n, data.day, 1) }
                }
                Spacer(Modifier.width(6.dp))
            } else {
                StarCheck(item.done, {
                    if (isKaizen && !item.done) askValue = true
                    else {
                        if (!item.done && data.settings.haptics) Haptics.star(context)
                        container.write { tick(n, data.day) }
                    }
                }, Modifier.padding(4.dp), color = hue, size = 38.dp, label = n.title)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title, style = UType.Strong.copy(fontSize = UType.Body.fontSize, color = if (item.done) U.c.muted else U.c.ink),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(m)
                    Spacer(Modifier.width(6.dp))
                    // A pact or kaizen step is usually named like its goal; don't say it twice.
                    if (g.title != n.title) Text(g.title, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                when {
                    isLead -> Text("${item.weekCount}/${item.weekTarget}", style = UType.Strong.copy(color = if (item.weekCount >= item.weekTarget) hue else U.c.muted))
                    isKaizen -> Text(
                        if (item.done) formatNumber(item.value) else w.kaizenTargetRounded(g).toString(),
                        style = UType.Strong.copy(color = hue),
                    )
                    item.weekTarget > 0 -> Text("${item.weekCount}/${item.weekTarget}", style = UType.Small)
                    n.remindAt >= 0 -> Text(clockTime(n.remindAt), style = UType.Small)
                }
                val streak = w.streak(n)
                if (streak >= 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(13.dp), tint = U.c.accent)
                        Text(streak.toString(), style = UType.Small.copy(color = U.c.accent))
                    }
                }
            }
        }
    }

    if (askValue) {
        ValueDialog(
            title = n.title,
            initial = w.kaizenTargetRounded(g).toString(),
            unit = g.unit,
            onDismiss = { askValue = false },
        ) { v ->
            if (data.settings.haptics) Haptics.star(context)
            container.write {
                record(n, data.day, v)
                upsertGoal(g.copy(currentValue = v))
            }
        }
    }
}

/** How many did you do? For kaizen-style steps. */
@Composable
fun ValueDialog(title: String, initial: String, unit: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(title, style = UType.Heading) },
        text = {
            Column {
                Text(stringResource(R.string.today_how_many), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                UTextField(text, { text = it.take(10) }, number = true, suffix = unit.ifBlank { null })
            }
        },
        confirmButton = {
            TextButton(onClick = { parseNumber(text)?.let { onSave(it) }; onDismiss() }, enabled = parseNumber(text) != null) {
                Text(stringResource(R.string.save), color = U.c.accent, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

/** If-then plans, one at a time: the backup course for when things go off track. */
@Composable
private fun BackupCourse(data: WData, go: Go) {
    val plans = data.world.plans
    if (plans.isEmpty()) return
    var shift by remember { mutableIntStateOf(0) }
    val (plan, goal) = plans[Math.floorMod(data.day.toInt() + shift, plans.size)]
    SectionTitle(stringResource(R.string.today_backup))
    UCard(onClick = { go.goal(goal.id) }, border = U.c.hue(Method.WOOP).copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_if, plan.title), style = UType.Body)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.today_then, plan.note), style = UType.Strong.copy(color = U.c.hue(Method.WOOP)))
                Spacer(Modifier.height(8.dp))
                Text(goal.title, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (plans.size > 1) {
                IconButton(onClick = { shift++ }) { Icon(Icons.Rounded.Refresh, stringResource(R.string.more), tint = U.c.faint) }
            }
        }
    }
}

/** On review day (and after it until the week is reviewed), a nudge to look back. */
@Composable
private fun ReviewNudge(data: WData, go: Go) {
    val w = data.world
    val s = data.settings
    if (!s.reviewOn) return
    val dow = data.today.dayOfWeek.value
    if (dow < s.reviewDay || w.review(w.thisWeek) != null) return
    Spacer(Modifier.height(12.dp))
    UCard(onClick = { go.review() }, border = U.c.accent.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_review), style = UType.Strong.copy(color = U.c.accent))
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.today_review_text), style = UType.Muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.accent)
        }
    }
}

/** Goals whose deadline is within a week. */
@Composable
private fun Deadlines(data: WData, go: Go) {
    val w = data.world
    val soon = w.active.mapNotNull { g -> w.daysLeft(g)?.let { g to it } }.filter { it.second in -30..7 }.sortedBy { it.second }
    if (soon.isEmpty()) return
    SectionTitle(stringResource(R.string.today_deadlines))
    UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        soon.forEach { (g, left) -> DeadlineRow(g, left, data) { go.goal(g.id) } }
    }
}

@Composable
private fun DeadlineRow(g: Goal, left: Long, data: WData, onClick: () -> Unit) {
    val p = data.world.progress(g)
    Row(
        Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(g.title, style = UType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(daysLeftLabel(left), style = UType.Small.copy(color = if (left < 0) U.c.danger else U.c.faint))
        }
        Text("${(p.fraction * 100).toInt()}%", style = UType.Strong.copy(color = U.c.hue(Method.of(g.method))))
    }
}
