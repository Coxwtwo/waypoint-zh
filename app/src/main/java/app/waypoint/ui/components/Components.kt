package app.waypoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.waypoint.R
import app.waypoint.domain.Method
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A panel floating on the sky: a touch lighter than the night, with a fine edge. */
@Composable
fun UCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    border: Color = U.c.line,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(U.c.surface.copy(alpha = if (U.c.dark) 0.82f else 1f))
            .border(1.dp, border, U.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = UType.Eyebrow, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = UType.Small.copy(color = U.c.accent), modifier = Modifier.clip(U.RSm).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = U.c.accent, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = U.RMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = U.c.onAccent,
            disabledContainerColor = U.c.line,
            disabledContentColor = U.c.faint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button)
    }
}

/** Quiet, tinted button for secondary actions. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.accent, icon: ImageVector? = null) {
    Row(
        modifier
            .height(48.dp)
            .clip(U.RMd)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), U.RMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = U.c.accent, icon: ImageVector? = null) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = UType.Button.copy(fontSize = 14.sp)) },
        leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp)) } },
        shape = U.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = U.c.surface.copy(alpha = 0.7f),
            labelColor = U.c.muted,
            iconColor = U.c.muted,
            selectedContainerColor = color.copy(alpha = 0.16f),
            selectedLabelColor = color,
            selectedLeadingIconColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = U.c.line,
            selectedBorderColor = color.copy(alpha = 0.5f),
        ),
    )
}

/** Day / Week / Month style switch. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(U.RSm)
            .background(U.c.surface2)
            .border(1.dp, U.c.line, U.RSm)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedSm)
                    .background(if (on) U.c.accentSoft else Color.Transparent)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = UType.Button.copy(fontSize = 14.sp), color = if (on) U.c.accent else U.c.muted, maxLines = 1)
            }
        }
    }
}

private val RoundedSm = androidx.compose.foundation.shape.RoundedCornerShape(9.dp)

/** A glyph on a soft disc. */
@Composable
fun IconDisc(icon: ImageVector, size: Dp = 44.dp, color: Color = U.c.accent) {
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink)
            }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = UType.Heading.copy(fontSize = 17.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** A tappable row inside a card: icon, label, value and a chevron. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, tint: Color = U.c.accent, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = UType.Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = UType.Strong)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

/** Ring that fills toward a goal. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, stroke: Dp = 8.dp, color: Color = U.c.accent, track: Color = U.c.line, content: @Composable () -> Unit = {}) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arc = Size(size.width - w, size.height - w)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
            if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0.01f, 1f), false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = U.c.accent, track: Color = U.c.line, height: Dp = 6.dp) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(height).clip(CircleShape),
        color = color,
        trackColor = track,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Wall clock that ticks every [stepMs] while the screen is visible. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { visible = true; now = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    LaunchedEffect(visible, stepMs) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}

@Composable
fun UTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    number: Boolean = false,
    minLines: Int = 1,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = U.c.faint) },
        label = label?.let { { Text(it) } },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = U.RSm,
        suffix = suffix?.let { { Text(it, color = U.c.muted) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = U.c.surface,
            unfocusedContainerColor = U.c.surface,
            unfocusedBorderColor = U.c.line,
            focusedBorderColor = U.c.accent,
            cursorColor = U.c.accent,
            focusedLabelColor = U.c.accent,
            unfocusedLabelColor = U.c.muted,
        ),
    )
}

/** Parses what the user typed as a number, accepting a comma as the decimal mark. */
fun parseNumber(s: String): Double? = s.trim().replace(',', '.').replace(" ", "").toDoubleOrNull()

/** Shows 12.0 as "12" and 12.5 as "12.5". */
fun formatNumber(v: Double): String =
    if (v == Math.floor(v) && kotlin.math.abs(v) < 1e12) v.toLong().toString()
    else "%.2f".format(java.util.Locale.ROOT, v).trimEnd('0').trimEnd('.')

/** Minus, the number, plus. */
@Composable
fun Counter(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 0, max: Int = 999, color: Color = U.c.accent, suffix: String = "") {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.Rounded.Remove, enabled = value > min, color = color) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            value.toString() + suffix,
            style = UType.Number.copy(fontSize = 18.sp, textAlign = TextAlign.Center),
            modifier = Modifier.width(56.dp),
        )
        RoundIcon(Icons.Rounded.Add, enabled = value < max, color = color) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, enabled: Boolean = true, color: Color = U.c.accent, size: Dp = 36.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (enabled) 0.14f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(size * 0.55f), tint = if (enabled) color else U.c.faint) }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(title, style = UType.Heading) },
        text = { Text(text, style = UType.Sub) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) U.c.danger else U.c.accent, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

/** Clock-face picker; [minutes] after midnight in and out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minutes: Int, onDismiss: () -> Unit, onClear: (() -> Unit)? = null, onPicked: (Int) -> Unit) {
    val m = if (minutes < 0) 9 * 60 else minutes
    val state = rememberTimePickerState(m / 60, m % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.save), color = U.c.accent) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.remove), color = U.c.danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) }
            }
        },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
    )
}

/** Calendar picker for a day; [day] and the result are epoch days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(day: Long?, onDismiss: () -> Unit, onClear: (() -> Unit)? = null, minDay: Long? = null, onPicked: (Long) -> Unit) {
    val initial = day ?: LocalDate.now().toEpochDay()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDay == null || Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() >= minDay
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = U.c.accent) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.remove), color = U.c.danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) }
            }
        },
        colors = DatePickerDefaults.colors(containerColor = U.c.surface2),
    ) { DatePicker(state, showModeToggle = false) }
}

/** Seven weekday toggles, Monday first; [mask] has Monday at bit 0. */
@Composable
fun DaysPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, color: Color = U.c.accent) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until 7).forEach { i ->
            val on = mask and (1 shl i) != 0
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(U.RSm)
                    .background(if (on) color.copy(alpha = 0.18f) else U.c.surface)
                    .border(1.dp, if (on) color.copy(alpha = 0.6f) else U.c.line, U.RSm)
                    .clickable { onChange(mask xor (1 shl i)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(app.waypoint.ui.weekdayShort(i), style = UType.Small.copy(color = if (on) color else U.c.muted))
            }
        }
    }
}

/** A small pill naming a method, in its star colour. */
@Composable
fun MethodBadge(m: Method, modifier: Modifier = Modifier) {
    val color = U.c.hue(m)
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(methodIcon(m), null, Modifier.size(12.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(methodShort(m), style = UType.Small.copy(color = color, fontSize = 11.sp), maxLines = 1)
    }
}

/** A friendly blank state: a dim constellation, a line of text and an action. */
@Composable
fun EmptyState(text: String, action: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Constellation(seed = text.length, levels = List(5) { 0f }, color = U.c.accent, modifier = Modifier.width(160.dp).height(70.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 12.dp))
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            SoftButton(action, onAction)
        }
    }
}
