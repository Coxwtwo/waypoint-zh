package app.waypoint.ui.methods

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Log
import app.waypoint.data.setField
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.dimStar
import app.waypoint.ui.components.navStar
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.delay

/** "Write 200 words" reads as "write 200 words" mid-sentence; acronyms like "TV" stay as they are. */
private fun inSentence(s: String): String =
    if (s.length > 1 && s[1].isLowerCase()) s.replaceFirstChar { it.lowercase() } else s

/** A pact's length in days: the goal's period, 30 by default. */
private fun pactLength(goal: Goal): Int = goal.endDay?.let { (it - goal.startDay + 1).toInt() }?.coerceAtLeast(1) ?: 30

/**
 * A tiny experiment: "I will … for 30 days". One star per day of the pact, lit on the days it was kept,
 * a note for each day, and at the end a decision: persist, pause or pivot.
 */
@Composable
fun PactBody(goal: Goal, data: WData, go: Go) {
    val w = data.world
    val color = U.c.hue(Method.PACT)
    val context = LocalContext.current
    val container = context.container
    val node = w.nodesOf(goal.id, NodeKind.ACTION).firstOrNull()
    LaunchedEffect(goal.id, node == null) { if (node == null) container.write { ensurePractice(goal) } }
    if (node == null) return

    val length = pactLength(goal)
    val end = goal.startDay + length - 1
    val kept = (goal.startDay..end).count { w.log(node.id, it) != null }
    val dayN = (w.day - goal.startDay + 1).toInt()
    val over = w.day > end
    var editAction by remember { mutableStateOf(false) }
    var pivot by remember { mutableStateOf(false) }

    // The pact itself, in one sentence.
    UCard(onClick = { editAction = true }, border = color.copy(alpha = 0.4f)) {
        Text(stringResource(R.string.pact_eyebrow).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.pact_sentence, inSentence(node.title), pluralStringResource(R.plurals.pact_days, length, length)),
            style = UType.Heading.copy(fontSize = 18.sp, lineHeight = 26.sp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                dayN < 1 -> stringResource(R.string.pact_starts, dateShort(goal.startDay))
                over -> pluralStringResource(R.plurals.pact_over_kept, length, kept, length)
                else -> stringResource(R.string.pact_day_of, dayN, length, kept)
            },
            style = UType.Muted,
        )
    }

    if (over && goal.status == Goal.ACTIVE) {
        Spacer(Modifier.height(12.dp))
        Decision(goal, kept, length, color, onPivot = { pivot = true })
    }

    // One star per day.
    SectionTitle(stringResource(R.string.pact_days_title))
    UCard {
        val rows = (0 until length).chunked(7)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { i ->
                        val d = goal.startDay + i
                        DayStar(
                            n = i + 1,
                            lit = w.log(node.id, d) != null,
                            today = d == w.day,
                            future = d > w.day,
                            color = color,
                            modifier = Modifier.weight(1f),
                        ) {
                            val on = w.log(node.id, d) == null
                            if (on) Haptics.star(context)
                            container.write {
                                if (log(node.id, d) != null) deleteLog(node.id, d)
                                else upsertLog(Log(node.id, goal.id, d, 1.0, System.currentTimeMillis()))
                            }
                        }
                    }
                    repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.pact_days_hint), style = UType.Small)
    }

    // Notes along the way.
    if (w.day in goal.startDay..end) {
        SectionTitle(stringResource(R.string.pact_note_today))
        TodayNote(goal, w, color)
    }
    val notes = w.fields
        .filter { it.goalId == goal.id && it.key.startsWith("pact.note.") && it.value.isNotBlank() }
        .mapNotNull { f -> f.key.removePrefix("pact.note.").toLongOrNull()?.let { it to f.value } }
        .filter { it.first != w.day }
        .sortedByDescending { it.first }
    if (notes.isNotEmpty()) {
        SectionTitle(stringResource(R.string.pact_notes))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { (d, text) ->
                UCard {
                    Text(stringResource(R.string.pact_note_day, (d - goal.startDay + 1).toInt(), dateShort(d)), style = UType.Small)
                    Spacer(Modifier.height(4.dp))
                    Text(text, style = UType.Body)
                }
            }
        }
    }

    if (editAction) {
        TextPrompt(
            stringResource(R.string.pact_edit_title), node.title, stringResource(R.string.pact_action_hint), color, { editAction = false },
            text = stringResource(R.string.pact_edit_text),
        ) { v -> if (v.isNotBlank()) container.write { upsertNode(node.copy(title = v)) } }
    }
    if (pivot) {
        TextPrompt(
            stringResource(R.string.pact_pivot_title), node.title, stringResource(R.string.pact_action_hint), color, { pivot = false },
            text = stringResource(R.string.pact_pivot_text),
        ) { v ->
            if (v.isNotBlank()) container.write {
                upsertNode(node.copy(title = v))
                upsertGoal(goal.copy(startDay = w.day, endDay = w.day + length - 1))
                setField(goal.id, "pact.decision", "pivot")
            }
        }
    }
}

@Composable
private fun DayStar(n: Int, lit: Boolean, today: Boolean, future: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val faint = U.c.faint
    Column(
        modifier.clip(U.RSm).clickable(enabled = !future, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth(0.8f).aspectRatio(1f)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * 0.34f
            when {
                lit -> navStar(c, r, color, 1f)
                future -> drawCircle(faint.copy(alpha = 0.35f), r * 0.18f, c)
                else -> dimStar(c, r, faint)
            }
            if (today) drawCircle(color.copy(alpha = 0.7f), size.minDimension * 0.46f, c, style = Stroke(1.5.dp.toPx()))
        }
        Text(n.toString(), style = UType.Small.copy(fontSize = 10.sp, textAlign = TextAlign.Center, color = if (lit) color else faint))
    }
}

/** Today's note, saved a moment after typing stops. */
@Composable
private fun TodayNote(goal: Goal, w: World, color: Color) {
    val container = LocalContext.current.container
    val key = "pact.note.${w.day}"
    val saved = w.field(goal.id, key)
    var text by remember(key) { mutableStateOf(saved) }
    LaunchedEffect(text) {
        delay(600)
        if (text.trim() != saved) container.write { setField(goal.id, key, text) }
    }
    UTextField(text, { text = it.take(500) }, placeholder = stringResource(R.string.pact_note_hint), minLines = 3)
}

/** After the last day: keep going, pause, or change the experiment. */
@Composable
private fun Decision(goal: Goal, kept: Int, length: Int, color: Color, onPivot: () -> Unit) {
    val container = LocalContext.current.container
    val pausedNote = pluralStringResource(R.plurals.pact_paused_note, length, kept, length)
    val days = pluralStringResource(R.plurals.pact_days, length, length)
    UCard(border = color.copy(alpha = 0.55f)) {
        Box(Modifier.fillMaxWidth().height(54.dp)) {
            Canvas(Modifier.fillMaxWidth().height(54.dp)) {
                val n = 5
                for (i in 0 until n) {
                    val c = Offset(size.width * (i + 0.5f) / n, size.height * (if (i % 2 == 0) 0.4f else 0.65f))
                    navStar(c, 7.dp.toPx(), color, if (i < (kept * n + length - 1) / length) 1f else 0.15f)
                }
            }
        }
        Text(stringResource(R.string.pact_end_title), style = UType.Heading)
        Spacer(Modifier.height(4.dp))
        Text(pluralStringResource(R.plurals.pact_end_text, length, kept, length), style = UType.Sub)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(stringResource(R.string.pact_persist, days), {
            container.write {
                upsertGoal(goal.copy(endDay = goal.startDay + length - 1 + length))
                setField(goal.id, "pact.decision", "persist")
            }
        }, Modifier.fillMaxWidth(), color = color)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(stringResource(R.string.pact_pause), {
                container.write {
                    setField(goal.id, "pact.decision", "pause")
                    upsertGoal(goal.copy(status = Goal.DONE, closedAt = System.currentTimeMillis(), note = goal.note.ifBlank { pausedNote }))
                }
            }, Modifier.weight(1f), color = color)
            SoftButton(stringResource(R.string.pact_pivot), onPivot, Modifier.weight(1f), color = color)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.pact_end_hint), style = UType.Small)
    }
}
