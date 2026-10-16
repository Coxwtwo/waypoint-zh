package app.waypoint.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.setField
import app.waypoint.domain.Area
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.areaIcon
import app.waypoint.ui.areaName
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.DateDialog
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.formatNumber
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.dateShort
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodName
import app.waypoint.ui.methodTagline
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Default length of a goal's period for methods that have one, in days. */
private fun defaultLength(m: Method): Int? = when (m) {
    Method.OKR -> 91
    Method.WEEKS12 -> 84
    Method.PACT -> 30
    else -> null
}

/** What a method needs to know up front: a number to move, and whether it's required. */
private enum class MetricNeed { NONE, OPTIONAL, REQUIRED }

private fun metricNeed(m: Method): MetricNeed = when (m) {
    Method.FOURDX, Method.KAIZEN -> MetricNeed.REQUIRED
    Method.SIMPLE -> MetricNeed.OPTIONAL
    else -> MetricNeed.NONE
}

/**
 * New goal or edit: the method (fixed once created), the title, why, area, dates and, for methods that measure
 * a number, the number. Beyond the focus limit a new goal waits in the queue unless the user starts it anyway.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalFormScreen(id: Long, method: Method?, title: String, area: Int, idea: Long, data: WData, go: Go) {
    val container = LocalContext.current.container
    val existing = data.world.goal(id)
    val editing = existing != null
    var m by remember { mutableStateOf(existing?.let { Method.of(it.method) } ?: method) }
    var name by remember { mutableStateOf(existing?.title ?: title) }
    var why by remember { mutableStateOf(existing?.why.orEmpty()) }
    var areaIx by remember { mutableIntStateOf(existing?.area ?: area) }
    var start by remember { mutableStateOf(existing?.startDay ?: data.day) }
    var end by remember { mutableStateOf(existing?.endDay) }
    var endTouched by remember { mutableStateOf(editing) }
    var from by remember { mutableStateOf(existing?.takeIf { it.hasMetric || it.startValue != 0.0 }?.let { formatNumber(it.startValue) } ?: "") }
    var to by remember { mutableStateOf(existing?.takeIf { it.hasMetric }?.let { formatNumber(it.targetValue) } ?: "") }
    var unit by remember { mutableStateOf(existing?.unit.orEmpty()) }
    var rate by remember { mutableStateOf(existing?.let { data.world.field(it.id, "kaizen.rate") }?.takeIf { it.isNotBlank() } ?: "1") }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val over = !editing && data.world.overLimit(data.settings.focusLimit)
    var startNow by remember { mutableStateOf(!over) }

    val mm = m
    // A method's natural length fills the deadline until the user picks one.
    val effectiveEnd = if (!endTouched && mm != null) defaultLength(mm)?.let { start + it - 1 } else end
    val need = mm?.let { metricNeed(it) } ?: MetricNeed.NONE
    val metricOk = when (need) {
        MetricNeed.REQUIRED -> if (mm == Method.KAIZEN) (parseNumber(from) ?: 0.0) > 0 else parseNumber(from) != null && parseNumber(to) != null && parseNumber(from) != parseNumber(to)
        else -> true
    }
    val valid = mm != null && name.isNotBlank() && metricOk

    fun save() {
        val method0 = mm ?: return
        val f = parseNumber(from) ?: 0.0
        val t = if (method0 == Method.KAIZEN) parseNumber(to) ?: f else parseNumber(to) ?: f
        val now = System.currentTimeMillis()
        val base = existing ?: Goal(title = "", method = method0.name, startDay = start, createdAt = now, seed = Random.nextInt(1, 1_000_000), sort = data.world.goals.size)
        val g = base.copy(
            title = name.trim(),
            why = why.trim(),
            area = areaIx,
            startDay = start,
            endDay = effectiveEnd,
            unit = unit.trim(),
            startValue = if (need == MetricNeed.NONE) base.startValue else f,
            targetValue = if (need == MetricNeed.NONE) base.targetValue else t,
            currentValue = if (existing == null && need != MetricNeed.NONE) f else base.currentValue,
            status = if (existing == null) (if (startNow) Goal.ACTIVE else Goal.QUEUED) else base.status,
        )
        container.scope.launch {
            val dao = container.dao
            val gid = if (existing == null) dao.insertGoal(g) else { dao.upsertGoal(g); g.id }
            if (existing == null) {
                // The daily practice of a kaizen or a pact is a step from the first day, so Today shows it right away.
                if (method0 == Method.KAIZEN || method0 == Method.PACT) {
                    dao.insertNode(Node(goalId = gid, kind = NodeKind.ACTION.name, title = g.title, days = World.EVERY_DAY))
                }
                if (idea > 0) dao.ideas().firstOrNull { it.id == idea }?.let { dao.upsertIdea(it.copy(goalId = gid)) }
            }
            if (method0 == Method.KAIZEN) dao.setField(gid, "kaizen.rate", (parseNumber(rate) ?: 1.0).let { formatNumber(it) })
            container.changed()
            withContext(Dispatchers.Main) { if (existing == null) go.openCreated(gid) else go.back() }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (editing) R.string.goal_edit else R.string.goal_new), onBack = { go.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            if (!editing) {
                SectionTitle(stringResource(R.string.form_method))
                MethodPicker(mm) { m = it }
                TextButton(onClick = { go.quiz() }) {
                    Icon(Icons.Rounded.Quiz, null, Modifier.size(18.dp), tint = U.c.accent)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.goals_quiz), style = UType.Button, color = U.c.accent)
                }
            }

            SectionTitle(stringResource(if (mm == Method.PACT) R.string.form_title_pact else R.string.form_title))
            UTextField(
                name, { name = it.take(120) },
                placeholder = stringResource(
                    when (mm) {
                        Method.PACT -> R.string.form_title_pact_hint
                        Method.KAIZEN -> R.string.form_title_kaizen_hint
                        Method.FOURDX -> R.string.form_title_fourdx_hint
                        Method.OKR -> R.string.form_title_okr_hint
                        else -> R.string.form_title_hint
                    }
                ),
                minLines = 2,
            )

            if (need != MetricNeed.NONE && mm != null) {
                SectionTitle(stringResource(if (mm == Method.KAIZEN) R.string.form_kaizen else R.string.form_metric))
                Text(
                    stringResource(
                        when (mm) {
                            Method.KAIZEN -> R.string.form_kaizen_text
                            Method.FOURDX -> R.string.form_fourdx_text
                            else -> R.string.form_metric_text
                        }
                    ),
                    style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UTextField(from, { from = it.take(12) }, Modifier.weight(1f), label = stringResource(if (mm == Method.KAIZEN) R.string.form_kaizen_base else R.string.node_from), number = true)
                    if (mm == Method.KAIZEN) {
                        UTextField(rate, { rate = it.take(5) }, Modifier.weight(1f), label = stringResource(R.string.form_kaizen_rate), number = true, suffix = "%")
                    } else {
                        UTextField(to, { to = it.take(12) }, Modifier.weight(1f), label = stringResource(R.string.node_to), number = true)
                    }
                    UTextField(unit, { unit = it.take(16) }, Modifier.weight(1f), label = stringResource(R.string.node_unit))
                }
                if (mm == Method.KAIZEN) {
                    Spacer(Modifier.height(8.dp))
                    UTextField(to, { to = it.take(12) }, label = stringResource(R.string.form_kaizen_target), number = true)
                }
            }

            SectionTitle(stringResource(R.string.form_why))
            UTextField(why, { why = it.take(300) }, placeholder = stringResource(R.string.form_why_hint), minLines = 2)

            SectionTitle(stringResource(R.string.form_area))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Area.entries.forEach { a ->
                    Chip(areaName(a), areaIx == a.ordinal, { areaIx = if (areaIx == a.ordinal) -1 else a.ordinal }, icon = areaIcon(a))
                }
            }

            SectionTitle(stringResource(R.string.form_dates))
            DateRow(stringResource(R.string.form_start), dateShort(start)) { pickStart = true }
            DateRow(stringResource(R.string.form_end), effectiveEnd?.let { dateShort(it) } ?: stringResource(R.string.node_no_date)) { pickEnd = true }
            if (mm != null && defaultLength(mm) != null && !endTouched) {
                Text(stringResource(R.string.form_end_auto), style = UType.Small, modifier = Modifier.padding(start = 4.dp))
            }

            if (over) {
                SectionTitle(stringResource(R.string.form_focus))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.form_focus_start), style = UType.Strong)
                        Text(stringResource(R.string.form_focus_text, data.settings.focusLimit), style = UType.Muted)
                    }
                    Switch(startNow, { startNow = it }, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent, checkedThumbColor = U.c.onAccent))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            PrimaryButton(
                stringResource(if (editing) R.string.save else if (startNow) R.string.form_create else R.string.form_create_queued),
                ::save, Modifier.fillMaxWidth(), enabled = valid,
            )
        }
    }

    if (pickStart) DateDialog(start, { pickStart = false }) { start = it; if (end != null && end!! < it) end = null }
    if (pickEnd) DateDialog(effectiveEnd, { pickEnd = false }, onClear = { end = null; endTouched = true }, minDay = start) { end = it; endTouched = true }
}

/** The driving methods as cards; the chosen one shines in its colour. */
@Composable
private fun MethodPicker(selected: Method?, onPick: (Method) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Method.drivers.forEach { m ->
            val on = m == selected
            val hue = U.c.hue(m)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(U.RMd)
                    .background(if (on) hue.copy(alpha = 0.14f) else U.c.surface.copy(alpha = 0.7f))
                    .border(1.dp, if (on) hue.copy(alpha = 0.6f) else U.c.line, U.RMd)
                    .clickable { onPick(m) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(methodIcon(m), null, Modifier.size(22.dp), tint = hue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(methodName(m), style = UType.Strong.copy(color = if (on) hue else U.c.ink))
                    Text(methodTagline(m), style = UType.Muted)
                }
            }
            if (on) app.waypoint.ui.compass.MethodGuide(m, open = true)
        }
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        Text(value, style = UType.Strong.copy(color = U.c.accent))
    }
}
