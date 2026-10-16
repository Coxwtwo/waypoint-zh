package app.waypoint.ui.methods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.domain.World
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.DateDialog
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.formatNumber
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/*
 * Small pieces shared by the 4DX, kaizen, pact, ladder and milestone bodies:
 * number and text prompts, the inline "set a number" card and the start → now → target card.
 */

/** "12 kg", or just "12" without a unit. */
internal fun withUnit(v: Double, unit: String): String = if (unit.isBlank()) formatNumber(v) else formatNumber(v) + " " + unit

/** Asks for one number: today's result, a lag measure, a new base. */
@Composable
internal fun NumberPrompt(
    title: String,
    initial: Double?,
    unit: String,
    color: Color,
    onDismiss: () -> Unit,
    text: String? = null,
    onClear: (() -> Unit)? = null,
    onSave: (Double) -> Unit,
) {
    var value by remember { mutableStateOf(initial?.let { formatNumber(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(title, style = UType.Heading) },
        text = {
            Column {
                if (text != null) {
                    Text(text, style = UType.Sub)
                    Spacer(Modifier.height(10.dp))
                }
                UTextField(value, { value = it.take(12) }, number = true, suffix = unit.takeIf { it.isNotBlank() })
            }
        },
        confirmButton = {
            TextButton(enabled = parseNumber(value) != null, onClick = { parseNumber(value)?.let(onSave); onDismiss() }) {
                Text(stringResource(R.string.save), color = color, style = UType.Button)
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.remove), color = U.c.danger, style = UType.Button) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
            }
        },
    )
}

/** Asks for a line of text: a commitment, a new pact action. */
@Composable
internal fun TextPrompt(title: String, initial: String, placeholder: String, color: Color, onDismiss: () -> Unit, text: String? = null, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(title, style = UType.Heading) },
        text = {
            Column {
                if (text != null) {
                    Text(text, style = UType.Sub)
                    Spacer(Modifier.height(10.dp))
                }
                UTextField(value, { value = it.take(200) }, placeholder = placeholder, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()); onDismiss() }) { Text(stringResource(R.string.save), color = color, style = UType.Button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

/**
 * Inline setup of a goal's number: from, to, unit and, when [askDeadline], a date.
 * Saving also resets the current value to the start.
 */
@Composable
internal fun MetricSetup(
    goal: Goal,
    color: Color,
    title: String,
    text: String,
    askDeadline: Boolean,
    onDone: () -> Unit = {},
) {
    val container = LocalContext.current.container
    var from by remember { mutableStateOf(if (goal.hasMetric) formatNumber(goal.startValue) else "") }
    var to by remember { mutableStateOf(if (goal.hasMetric) formatNumber(goal.targetValue) else "") }
    var unit by remember { mutableStateOf(goal.unit) }
    var deadline by remember { mutableStateOf(goal.endDay) }
    var pick by remember { mutableStateOf(false) }
    val s = parseNumber(from)
    val t = parseNumber(to)
    UCard(border = color.copy(alpha = 0.4f)) {
        Text(title, style = UType.Heading)
        Spacer(Modifier.height(6.dp))
        Text(text, style = UType.Sub)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UTextField(from, { from = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.simple_from), number = true)
            UTextField(to, { to = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.simple_to), number = true)
            UTextField(unit, { unit = it.take(16) }, Modifier.weight(1f), label = stringResource(R.string.simple_unit))
        }
        if (askDeadline) {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().clip(U.RSm).clickable { pick = true }.padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.simple_deadline), style = UType.Body, modifier = Modifier.weight(1f))
                Text(deadline?.let { dateShort(it) } ?: stringResource(R.string.simple_no_deadline), style = UType.Strong.copy(color = color))
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            stringResource(R.string.save),
            {
                container.write {
                    upsertGoal(goal.copy(startValue = s!!, targetValue = t!!, currentValue = s, unit = unit.trim(), endDay = if (askDeadline) deadline else goal.endDay))
                }
                onDone()
            },
            Modifier.fillMaxWidth(),
            enabled = s != null && t != null && s != t,
            color = color,
        )
    }
    if (pick) DateDialog(deadline, { pick = false }, onClear = { deadline = null }, minDay = goal.startDay) { deadline = it }
}

/** Where the number stands: big current value, a bar from start to target, and an update button. */
@Composable
internal fun MetricCard(goal: Goal, world: World, color: Color, label: String, onUpdate: () -> Unit) {
    val f = world.metricFraction(goal.startValue, goal.targetValue, goal.currentValue)
    UCard {
        Text(label.uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatNumber(goal.currentValue), style = UType.Big.copy(color = color, fontSize = 40.sp))
            if (goal.unit.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(goal.unit, style = UType.Sub, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("${(f * 100).toInt()}%", style = UType.Number.copy(color = color, fontWeight = FontWeight(400)), modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Bar(f, color = color)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(withUnit(goal.startValue, goal.unit), style = UType.Small)
            Spacer(Modifier.weight(1f))
            Text(withUnit(goal.targetValue, goal.unit), style = UType.Small)
        }
        Spacer(Modifier.height(12.dp))
        SoftButton(stringResource(R.string.simple_update), onUpdate, Modifier.fillMaxWidth(), color = color)
    }
}
