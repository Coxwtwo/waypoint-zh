package app.waypoint.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Review
import app.waypoint.domain.Method
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.MethodBadge
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.dateOf
import app.waypoint.ui.dateShort
import app.waypoint.ui.goals.goalLevels
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.delay

/** Steps finished in the week starting [weekStart], for one goal or (goalId = null) for all. */
fun stepsDone(w: World, weekStart: Long, goalId: Long? = null): Int {
    val range = weekStart until weekStart + 7
    val logs = w.logs.count { it.day in range && (goalId == null || it.goalId == goalId) && it.value > 0 }
    val oneOffs = w.nodes.count { n ->
        !n.repeats && n.done && (goalId == null || n.goalId == goalId) && w.isDoable(n) &&
            n.doneAt?.let { World.epochDay(it) in range } == true
    }
    return logs + oneOffs
}

/** Looking back: the weekly review, the wheel, reached goals as a wall of constellations, the ones let go. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalTab(data: WData, go: Go) {
    val w = data.world
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Text(stringResource(R.string.journal_title), style = UType.Display)
            Text(stringResource(R.string.journal_sub), style = UType.Muted)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Spacer(Modifier.height(14.dp))
            val thisWeek = stepsDone(w, w.thisWeek)
            val lastWeek = stepsDone(w, w.thisWeek - 7)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(stringResource(R.string.journal_week_steps), thisWeek.toString(), Modifier.weight(1f))
                Stat(stringResource(R.string.journal_last_week), lastWeek.toString(), Modifier.weight(1f))
                Stat(stringResource(R.string.journal_reached), w.reached.size.toString(), Modifier.weight(1f))
            }

            SectionTitle(stringResource(R.string.journal_reviews))
            val reviewed = w.review(w.thisWeek) != null
            UCard(onClick = { go.review() }, border = if (reviewed) U.c.line else U.c.accent.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (reviewed) R.string.journal_review_edit else R.string.journal_review_new), style = UType.Strong.copy(color = if (reviewed) U.c.ink else U.c.accent))
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.journal_review_text), style = UType.Muted)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                }
            }
            val past = w.reviews.filter { it.day != w.thisWeek }.take(12)
            if (past.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { past.forEach { PastReview(it) } }
            }

            SectionTitle(stringResource(R.string.m_wheel), action = stringResource(R.string.open)) { go.tool(Method.WHEEL) }
            val last = w.lastWheel
            UCard(onClick = { go.tool(Method.WHEEL) }) {
                if (last == null) {
                    Text(stringResource(R.string.journal_wheel_none), style = UType.Muted)
                } else {
                    Text(stringResource(R.string.journal_wheel_last, dateShort(last.day), "%.1f".format(last.values.average())), style = UType.Body)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        last.values.forEach { v ->
                            Column(Modifier.weight(1f)) { Bar(v / 10f, color = U.c.hue(Method.WHEEL), height = 5.dp) }
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.journal_wall))
            if (w.reached.isEmpty()) {
                Text(stringResource(R.string.journal_wall_empty), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                    w.reached.forEach { g -> WallTile(g, data, Modifier.weight(1f)) { go.goal(g.id) } }
                    if (w.reached.size % 2 == 1) Spacer(Modifier.weight(1f))
                }
            }

            val dropped = w.closed.filter { it.status == Goal.DROPPED }
            if (dropped.isNotEmpty()) {
                SectionTitle(stringResource(R.string.journal_dropped))
                UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    dropped.forEach { g ->
                        Column(Modifier.fillMaxWidth().clip(U.RSm).clickable { go.goal(g.id) }.padding(vertical = 10.dp)) {
                            Text(g.title, style = UType.Body.copy(color = U.c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (g.note.isNotBlank()) Text(g.note, style = UType.Small.copy(fontStyle = FontStyle.Italic), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    UCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Text(value, style = UType.Number)
        Spacer(Modifier.height(2.dp))
        Text(label, style = UType.Small, maxLines = 2)
    }
}

@Composable
private fun WallTile(g: Goal, data: WData, modifier: Modifier, onClick: () -> Unit) {
    val hue = U.c.hue(Method.of(g.method))
    UCard(modifier, onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Constellation(g.seed, List(goalLevels(g, data).size) { 1f }, hue, Modifier.fillMaxWidth().height(70.dp), starSize = 5.dp)
        Spacer(Modifier.height(6.dp))
        Text(g.title, style = UType.Small.copy(color = U.c.ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
        g.closedAt?.let { Text(dateOf(it), style = UType.Small) }
    }
}

@Composable
private fun PastReview(r: Review) {
    var open by remember { mutableStateOf(false) }
    UCard(onClick = { open = !open }, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.review_week_of, dateShort(r.day)), style = UType.Strong, modifier = Modifier.weight(1f))
            if (r.mood > 0) Row { repeat(r.mood) { Text("✦", style = UType.Small.copy(color = U.c.accent)) } }
        }
        if (open) {
            listOf(R.string.review_went to r.went, R.string.review_blocked to r.blocked, R.string.review_change to r.change).forEach { (q, a) ->
                if (a.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(q), style = UType.Small)
                    Text(a, style = UType.Body)
                }
            }
        } else {
            val first = listOf(r.went, r.change, r.blocked).firstOrNull { it.isNotBlank() }
            if (first != null) Text(first, style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The weekly review: how each goal went this week, then three questions and how the week felt.
 * Before the review day it covers last week, if that one wasn't reviewed.
 */
@Composable
fun ReviewScreen(data: WData, go: Go) {
    val container = LocalContext.current.container
    val w = data.world
    val s = data.settings
    val week = remember {
        val last = w.thisWeek - 7
        if (data.today.dayOfWeek.value < s.reviewDay && w.review(last) == null && w.goals.any { it.startDay <= last + 6 }) last else w.thisWeek
    }
    val existing = w.review(week)
    var went by remember { mutableStateOf(existing?.went.orEmpty()) }
    var blocked by remember { mutableStateOf(existing?.blocked.orEmpty()) }
    var change by remember { mutableStateOf(existing?.change.orEmpty()) }
    var mood by remember { mutableIntStateOf(existing?.mood ?: 0) }
    var dirty by remember { mutableStateOf(false) }

    fun save() {
        container.write { upsertReview(Review(week, went.trim(), blocked.trim(), change.trim(), mood, System.currentTimeMillis())) }
    }
    // Saves as you type, so going back never loses an answer.
    LaunchedEffect(went, blocked, change, mood) {
        if (!dirty) return@LaunchedEffect
        delay(600)
        save()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.review_title), onBack = { if (dirty) save(); go.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.review_week_of, dateShort(week)), style = UType.Small, modifier = Modifier.padding(start = 4.dp))

            SectionTitle(stringResource(R.string.review_goals))
            if (w.active.isEmpty()) {
                Text(stringResource(R.string.review_no_goals), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                w.active.forEach { g ->
                    val m = Method.of(g.method)
                    val hue = U.c.hue(m)
                    val p = w.progress(g)
                    val n = stepsDone(w, week, g.id)
                    UCard(onClick = { go.goal(g.id) }, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MethodBadge(m)
                                    Spacer(Modifier.width(8.dp))
                                    Text(pluralStringResource(R.plurals.steps, n, n), style = UType.Small)
                                }
                            }
                            Text("${(p.fraction * 100).toInt()}%", style = UType.Strong.copy(color = hue))
                        }
                        reviewHint(m)?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(it), style = UType.Small.copy(color = hue))
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.review_mood))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (1..5).forEach { i ->
                    StarCheck(i <= mood, { mood = if (mood == i) 0 else i; dirty = true }, size = 44.dp, label = "$i")
                }
            }
            Text(
                stringResource(
                    when (mood) {
                        1 -> R.string.mood_1; 2 -> R.string.mood_2; 3 -> R.string.mood_3; 4 -> R.string.mood_4; 5 -> R.string.mood_5
                        else -> R.string.mood_0
                    }
                ),
                style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )

            SectionTitle(stringResource(R.string.review_went))
            UTextField(went, { went = it.take(600); dirty = true }, placeholder = stringResource(R.string.review_went_hint), minLines = 3)
            SectionTitle(stringResource(R.string.review_blocked))
            UTextField(blocked, { blocked = it.take(600); dirty = true }, placeholder = stringResource(R.string.review_blocked_hint), minLines = 3)
            SectionTitle(stringResource(R.string.review_change))
            UTextField(change, { change = it.take(600); dirty = true }, placeholder = stringResource(R.string.review_change_hint), minLines = 3)
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            PrimaryButton(stringResource(R.string.done), { save(); go.back() }, Modifier.fillMaxWidth())
        }
    }
}

/** What the method asks of you in a weekly review. */
private fun reviewHint(m: Method): Int? = when (m) {
    Method.OKR -> R.string.review_hint_okr
    Method.WEEKS12 -> R.string.review_hint_weeks12
    Method.FOURDX -> R.string.review_hint_fourdx
    Method.MANDALA -> R.string.review_hint_mandala
    Method.PACT -> R.string.review_hint_pact
    else -> null
}
