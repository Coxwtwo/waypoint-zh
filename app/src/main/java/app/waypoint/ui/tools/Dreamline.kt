package app.waypoint.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Idea
import app.waypoint.data.setField
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.ConfirmDialog
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.Segmented
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.locale
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.delay
import java.text.NumberFormat

private val columns = listOf(R.string.dream_having, R.string.dream_being, R.string.dream_doing)
private val columnHints = listOf(R.string.dream_having_hint, R.string.dream_being_hint, R.string.dream_doing_hint)

/** Ferriss's target monthly income: one-off costs spread over the timeline, plus monthly costs, plus a 30% buffer. */
internal fun targetMonthly(dreams: List<Idea>, months: Int): Double =
    (dreams.sumOf { it.amount } / months.coerceAtLeast(1) + dreams.sumOf { it.monthly }) * 1.3

@Composable
fun DreamlineScreen(data: WData, go: Go) {
    val tool = Method.DREAMLINE
    val hue = U.c.hue(tool)
    val container = LocalContext.current.container
    val dreams = data.world.ideas(tool)
    val months = data.world.field(0, "dream.months").toIntOrNull()?.takeIf { it == 6 || it == 12 } ?: 6
    val savedCurrency = data.world.field(0, "dream.currency")
    var currency by remember { mutableStateOf(savedCurrency) }
    LaunchedEffect(currency) {
        delay(400)
        if (currency.trim() != savedCurrency) container.write { setField(0, "dream.currency", currency.take(6)) }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Idea?>(null) }
    val loc = locale()
    val fmt = remember(loc) { NumberFormat.getNumberInstance(loc).apply { maximumFractionDigits = 0 } }
    fun money(v: Double) = fmt.format(v) + if (currency.isNotBlank()) " " + currency.trim() else ""

    val tmi = targetMonthly(dreams, months)

    ToolScaffold(tool, go) {
        Hint(stringResource(R.string.dream_intro))
        Spacer(Modifier.height(10.dp))
        UCard(border = hue.copy(alpha = 0.45f)) {
            Text(stringResource(R.string.dream_tmi).uppercase(), style = UType.Eyebrow.copy(color = hue))
            Spacer(Modifier.height(4.dp))
            Text(money(tmi), style = UType.Big.copy(color = hue))
            Text(stringResource(R.string.dream_tmi_text, months), style = UType.Muted)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dream_tdi), style = UType.Body, modifier = Modifier.weight(1f))
                Text(money(tmi / 30), style = UType.Strong.copy(color = hue))
            }
            Text(stringResource(R.string.dream_tdi_text), style = UType.Muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dream_timeline), style = UType.Body, modifier = Modifier.weight(1f))
                listOf(6, 12).forEach { m ->
                    Chip(stringResource(R.string.dream_months, m), months == m, { container.write { setField(0, "dream.months", m.toString()) } }, color = hue)
                }
            }
            Spacer(Modifier.height(6.dp))
            UTextField(currency, { currency = it.take(6) }, label = stringResource(R.string.dream_currency), placeholder = stringResource(R.string.dream_currency_hint))
        }

        Spacer(Modifier.height(18.dp))
        Segmented(listOf(0, 1, 2), tab, { stringResource(columns[it]) }, { tab = it })
        Spacer(Modifier.height(8.dp))
        Hint(stringResource(columnHints[tab]))
        val list = dreams.filter { it.slot == tab }
        if (list.isEmpty()) EmptyState(stringResource(R.string.dream_empty))
        else UCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            list.forEachIndexed { idx, d ->
                if (idx > 0) HorizontalDivider(color = U.c.line)
                Row(Modifier.fillMaxWidth().clickable { editing = d }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = UType.Strong)
                        val parts = buildList {
                            if (d.amount > 0) add(stringResource(R.string.dream_once_value, money(d.amount)))
                            if (d.monthly > 0) add(stringResource(R.string.dream_monthly_value, money(d.monthly)))
                        }
                        Text(if (parts.isEmpty()) stringResource(R.string.dream_free) else parts.joinToString(" · "), style = UType.Muted)
                    }
                    if (d.goalId != 0L && data.world.goal(d.goalId) != null) GoalMark(d.goalId, go, hue)
                    else TextButton(onClick = { go.newGoal(title = d.title, idea = d.id) }) {
                        Text(stringResource(R.string.tool_make_goal), color = hue, style = UType.Button)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftButton(stringResource(R.string.dream_add), {
            editing = Idea(tool = tool.name, slot = tab, title = "", at = System.currentTimeMillis())
        }, Modifier.fillMaxWidth(), color = hue, icon = Icons.Rounded.Add)

        if (dreams.isNotEmpty()) {
            SectionTitle(stringResource(R.string.dream_totals))
            UCard {
                TotalRow(stringResource(R.string.dream_total_once), money(dreams.sumOf { it.amount }))
                TotalRow(stringResource(R.string.dream_total_monthly), money(dreams.sumOf { it.monthly }))
            }
        }
    }

    editing?.let { d -> DreamEditor(d, hue, onDismiss = { editing = null }) }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        Text(value, style = UType.Strong)
    }
}

@Composable
private fun DreamEditor(d: Idea, hue: Color, onDismiss: () -> Unit) {
    val container = LocalContext.current.container
    var title by remember { mutableStateOf(d.title) }
    var once by remember { mutableStateOf(if (d.amount > 0) app.waypoint.ui.components.formatNumber(d.amount) else "") }
    var monthly by remember { mutableStateOf(if (d.monthly > 0) app.waypoint.ui.components.formatNumber(d.monthly) else "") }
    var slot by remember { mutableIntStateOf(d.slot) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(stringResource(if (d.id == 0L) R.string.dream_new else R.string.dream_edit), style = UType.Heading) },
        text = {
            Column {
                UTextField(title, { title = it.take(100) }, placeholder = stringResource(columnHints[slot]))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UTextField(once, { once = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.dream_once), number = true)
                    UTextField(monthly, { monthly = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.dream_monthly), number = true)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..2).forEach { s -> Chip(stringResource(columns[s]), slot == s, { slot = s }, color = hue) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val saved = d.copy(
                    title = title.trim(), slot = slot,
                    amount = (parseNumber(once) ?: 0.0).coerceAtLeast(0.0),
                    monthly = (parseNumber(monthly) ?: 0.0).coerceAtLeast(0.0),
                )
                container.write { upsertIdea(saved) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = hue, style = UType.Button) }
        },
        dismissButton = {
            Row {
                if (d.id != 0L) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = U.c.danger, style = UType.Button) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
            }
        },
    )
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.dream_delete), stringResource(R.string.dream_delete_text), stringResource(R.string.delete),
            onConfirm = { container.write { deleteIdea(d) }; onDismiss() }, onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}
