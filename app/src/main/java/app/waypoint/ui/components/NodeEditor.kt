package app.waypoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.waypoint.R
import app.waypoint.data.Node
import app.waypoint.domain.World
import app.waypoint.ui.clockTime
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** Which parts of a [Node] an editor shows. Labels are already-translated strings. */
class NodeSpec(
    val titleLabel: String,
    val titleHint: String = "",
    /** Shows a second text field (the "then" of a plan, a description) when set. */
    val noteLabel: String? = null,
    val noteHint: String = "",
    /** Once / on weekdays / N times a week. */
    val schedule: Boolean = false,
    /** A due date for one-offs. */
    val due: Boolean = false,
    /** Start, target and unit, e.g. for a key result. */
    val metric: Boolean = false,
    /** A per-tap increment (progress tracker). */
    val increment: Boolean = false,
    /** A start-and-end date window (progress tracker). */
    val period: Boolean = false,
    val remind: Boolean = false,
    /** 12-week year: which week (0 = every week). */
    val weeks: Boolean = false,
    /** Frequency choices offered when [schedule] is on. */
    val allowOnce: Boolean = true,
)

private enum class Mode { ONCE, DAYS, WEEKLY }

/**
 * One editor for every kind of step, so a mandala cell, a key result and a 12-week tactic feel the same to edit.
 * [onDelete] null hides the delete button.
 */
@Composable
fun NodeEditor(
    heading: String,
    node: Node,
    spec: NodeSpec,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    color: Color = U.c.accent,
    onSave: (Node) -> Unit,
) {
    var title by remember { mutableStateOf(node.title) }
    var note by remember { mutableStateOf(node.note) }
    var mode by remember {
        mutableStateOf(
            when {
                node.days != 0 -> Mode.DAYS
                node.perWeek > 0 -> Mode.WEEKLY
                spec.allowOnce -> Mode.ONCE
                else -> Mode.DAYS
            }
        )
    }
    var days by remember { mutableIntStateOf(if (node.days != 0) node.days else World.EVERY_DAY) }
    var perWeek by remember { mutableIntStateOf(if (node.perWeek > 0) node.perWeek else 3) }
    var due by remember { mutableStateOf(node.dueDay) }
    var start by remember { mutableStateOf(if (node.start != 0.0 || node.target != 0.0) formatNumber(node.start) else "") }
    var target by remember { mutableStateOf(if (node.target != 0.0) formatNumber(node.target) else "") }
    var unit by remember { mutableStateOf(node.unit) }
    var increment by remember { mutableStateOf(if (node.increment != 0.0) formatNumber(node.increment) else "") }
    var startDay by remember { mutableStateOf(node.startDay) }
    var remindAt by remember { mutableIntStateOf(node.remindAt) }
    var week by remember { mutableIntStateOf(node.week) }
    var pickDate by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun result(): Node {
        val s = parseNumber(start) ?: 0.0
        val t = parseNumber(target) ?: 0.0
        return node.copy(
            title = title.trim(),
            note = note.trim(),
            days = if (spec.schedule && mode == Mode.DAYS) days.takeIf { it != 0 } ?: World.EVERY_DAY else if (spec.schedule) 0 else node.days,
            perWeek = if (spec.schedule && mode == Mode.WEEKLY) perWeek else if (spec.schedule) 0 else node.perWeek,
            dueDay = if (spec.period) due else if (spec.due && (!spec.schedule || mode == Mode.ONCE)) due else node.dueDay,
            start = if (spec.metric) s else node.start,
            target = if (spec.metric) t else node.target,
            // A new key result starts where it starts; later the check-ins move it.
            current = if (spec.metric && node.id == 0L) s else node.current,
            unit = if (spec.metric) unit.trim() else node.unit,
            increment = if (spec.increment) parseNumber(increment) ?: 0.0 else node.increment,
            startDay = if (spec.period) startDay else node.startDay,
            remindAt = if (spec.remind) remindAt else node.remindAt,
            week = if (spec.weeks) week else node.week,
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(U.RLg)
                .background(U.c.surface2)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(heading, style = UType.Heading)
            Spacer(Modifier.height(14.dp))
            UTextField(title, { title = it.take(120) }, label = spec.titleLabel, placeholder = spec.titleHint, minLines = 1)
            if (spec.noteLabel != null) {
                Spacer(Modifier.height(10.dp))
                UTextField(note, { note = it.take(400) }, label = spec.noteLabel, placeholder = spec.noteHint, minLines = 2)
            }
            if (spec.metric) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UTextField(start, { start = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.node_from), number = true)
                    UTextField(target, { target = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.node_to), number = true)
                    UTextField(unit, { unit = it.take(16) }, Modifier.weight(1f), label = stringResource(R.string.node_unit))
                }
            }
            if (spec.increment) {
                Spacer(Modifier.height(10.dp))
                UTextField(increment, { increment = it.take(12) }, label = stringResource(R.string.tracker_increment), number = true)
            }
            if (spec.schedule) {
                SectionTitle(stringResource(R.string.node_how_often))
                val modes = if (spec.allowOnce) Mode.entries else listOf(Mode.DAYS, Mode.WEEKLY)
                Segmented(modes, mode, {
                    stringResource(
                        when (it) {
                            Mode.ONCE -> R.string.node_once
                            Mode.DAYS -> R.string.node_days
                            Mode.WEEKLY -> R.string.node_weekly
                        }
                    )
                }, { mode = it })
                Spacer(Modifier.height(10.dp))
                when (mode) {
                    Mode.DAYS -> DaysPicker(days, { days = it }, color = color)
                    Mode.WEEKLY -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.node_times_week), style = UType.Body, modifier = Modifier.weight(1f))
                        Counter(perWeek, { perWeek = it }, min = 1, max = 21, color = color)
                    }
                    Mode.ONCE -> Unit
                }
            }
            if (spec.due && (!spec.schedule || mode == Mode.ONCE)) {
                Spacer(Modifier.height(6.dp))
                PickRow(stringResource(R.string.node_due), due?.let { dateShort(it) } ?: stringResource(R.string.node_no_date)) { pickDate = true }
            }
            if (spec.period) {
                Spacer(Modifier.height(6.dp))
                PickRow(stringResource(R.string.tracker_start_date), startDay?.let { dateShort(it) } ?: stringResource(R.string.node_no_date)) { pickStart = true }
                Spacer(Modifier.height(6.dp))
                PickRow(stringResource(R.string.tracker_end_date), due?.let { dateShort(it) } ?: stringResource(R.string.node_no_date)) { pickDate = true }
            }
            if (spec.weeks) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (week == 0) stringResource(R.string.node_every_week) else stringResource(R.string.node_week_n, week),
                        style = UType.Body, modifier = Modifier.weight(1f),
                    )
                    Counter(week, { week = it }, min = 0, max = 12, color = color)
                }
            }
            if (spec.remind) {
                PickRow(stringResource(R.string.node_remind), if (remindAt < 0) stringResource(R.string.node_no_remind) else clockTime(remindAt)) { pickTime = true }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = U.c.danger, style = UType.Button) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
                Spacer(Modifier.width(4.dp))
                PrimaryButton(stringResource(R.string.save), { onSave(result()); onDismiss() }, enabled = title.isNotBlank(), color = color)
            }
        }
    }

    if (pickDate) DateDialog(due, { pickDate = false }, onClear = { due = null }) { due = it }
    if (pickStart) DateDialog(startDay, { pickStart = false }, onClear = { startDay = null }) { startDay = it }
    if (pickTime) TimeDialog(remindAt, { pickTime = false }, onClear = { remindAt = -1 }) { remindAt = it }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            stringResource(R.string.delete_step), stringResource(R.string.delete_step_text), stringResource(R.string.delete),
            onConfirm = { onDelete(); onDismiss() }, onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}

@Composable
private fun PickRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        Text(value, style = UType.Strong.copy(color = U.c.accent))
    }
}
