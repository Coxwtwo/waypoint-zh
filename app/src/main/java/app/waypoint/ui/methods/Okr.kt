package app.waypoint.ui.methods

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.record
import app.waypoint.data.setField
import app.waypoint.data.tick
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.Counter
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.formatNumber
import app.waypoint.ui.components.parseNumber
import app.waypoint.ui.dateShort
import app.waypoint.ui.daysLeftLabel
import app.waypoint.ui.scheduleLabel
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** A score from 0 to 1 the OKR way: 0.7 and up is a win for an ambitious objective. */
@Composable
private fun scoreColor(score: Float, hue: Color): Color = when {
    score >= 0.7f -> U.c.good
    score >= 0.4f -> hue
    else -> U.c.muted
}

private fun score(f: Float): String = "%.1f".format(java.util.Locale.ROOT, f)

@Composable
fun OkrBody(goal: Goal, data: WData, go: Go) {
    val container = LocalContext.current.container
    val world = data.world
    val hue = U.c.hue(Method.OKR)
    val krs = world.nodesOf(goal.id, NodeKind.KR).sortedWith(compareBy({ it.slot }, { it.id }))
    val scored = krs.filter { it.title.isNotBlank() }
    val total = if (scored.isEmpty()) 0f else scored.map { world.metricFraction(it.start, it.target, it.current) }.average().toFloat()
    var editKr by remember { mutableStateOf<Node?>(null) }
    var editInitiative by remember { mutableStateOf<Node?>(null) }
    var checkIn by remember { mutableStateOf<Node?>(null) }

    Spacer(Modifier.height(16.dp))
    QuarterCard(goal, world, hue)
    Spacer(Modifier.height(10.dp))

    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.okr_score).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (scored.isEmpty()) "—" else score(total),
                    style = UType.Big.copy(color = if (scored.isEmpty()) U.c.faint else scoreColor(total, hue)),
                )
            }
            Text(
                stringResource(
                    when {
                        scored.isEmpty() -> R.string.okr_score_none
                        total >= 0.7f -> R.string.okr_score_good
                        total >= 0.4f -> R.string.okr_score_mid
                        else -> R.string.okr_score_low
                    }
                ),
                style = UType.Muted,
                modifier = Modifier.weight(1.2f),
            )
        }
        Spacer(Modifier.height(12.dp))
        val conf = world.field(goal.id, "okr.confidence").toIntOrNull() ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.okr_confidence), style = UType.Strong)
                Text(stringResource(R.string.okr_confidence_text), style = UType.Small)
            }
            Counter(if (conf == 0) 5 else conf, { v -> container.write { setField(goal.id, "okr.confidence", v.toString()) } }, min = 1, max = 10, color = hue, suffix = "/10")
        }
    }

    SectionTitle(
        stringResource(R.string.okr_krs),
        action = stringResource(R.string.add),
        onAction = { editKr = Node(goalId = goal.id, kind = NodeKind.KR.name, slot = (krs.maxOfOrNull { it.slot } ?: -1) + 1) },
    )
    if (krs.isEmpty()) {
        EmptyState(stringResource(R.string.okr_empty), stringResource(R.string.okr_add_kr), onAction = {
            editKr = Node(goalId = goal.id, kind = NodeKind.KR.name)
        })
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        krs.forEach { kr ->
            KrCard(
                kr, world, hue,
                onEdit = { editKr = kr },
                onCheckIn = { checkIn = kr },
                onInitiative = { editInitiative = it ?: Node(goalId = goal.id, parentId = kr.id, kind = NodeKind.ACTION.name, days = World.EVERY_DAY) },
                onTick = { n -> container.write { tick(n, world.day) } },
            )
        }
    }
    if (krs.size > 5) {
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.okr_too_many), style = UType.Small.copy(color = hue))
    }
    if (krs.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.okr_review_hint), style = UType.Small)
    }

    editKr?.let { kr ->
        NodeEditor(
            heading = stringResource(if (kr.id == 0L) R.string.okr_new_kr else R.string.okr_kr),
            node = kr,
            spec = NodeSpec(titleLabel = stringResource(R.string.okr_kr), titleHint = stringResource(R.string.okr_kr_hint), metric = true),
            onDismiss = { editKr = null },
            onDelete = if (kr.id == 0L) null else ({ container.write { deleteNode(kr.id) } }),
            color = hue,
        ) { n -> container.write { upsertNode(n) } }
    }
    editInitiative?.let { n0 ->
        NodeEditor(
            heading = stringResource(if (n0.id == 0L) R.string.okr_new_initiative else R.string.okr_initiative),
            node = n0,
            spec = NodeSpec(titleLabel = stringResource(R.string.okr_initiative), titleHint = stringResource(R.string.okr_initiative_hint), schedule = true, due = true, remind = true),
            onDismiss = { editInitiative = null },
            onDelete = if (n0.id == 0L) null else ({ container.write { deleteNode(n0.id) } }),
            color = hue,
        ) { n -> container.write { upsertNode(n) } }
    }
    checkIn?.let { kr -> CheckInDialog(kr, hue, { checkIn = null }) { v -> container.write { upsertNode(kr.copy(current = v)); record(kr, world.day, v) } } }
}

@Composable
private fun QuarterCard(goal: Goal, world: World, hue: Color) {
    val container = LocalContext.current.container
    val end = goal.endDay
    UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.okr_quarter).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(4.dp))
        if (end == null) {
            Text(stringResource(R.string.okr_no_end), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            SoftButton(stringResource(R.string.okr_set_end), { container.write { upsertGoal(goal.copy(endDay = goal.startDay + 90)) } }, color = hue)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.okr_range, dateShort(goal.startDay), dateShort(end)), style = UType.Strong, modifier = Modifier.weight(1f))
                Text(daysLeftLabel(end - world.day), style = UType.Small.copy(color = hue))
            }
            Spacer(Modifier.height(8.dp))
            val span = (end - goal.startDay).coerceAtLeast(1)
            Bar(((world.day - goal.startDay).toFloat() / span).coerceIn(0f, 1f), color = hue.copy(alpha = 0.6f), height = 4.dp)
        }
    }
}

@Composable
private fun KrCard(kr: Node, world: World, hue: Color, onEdit: () -> Unit, onCheckIn: () -> Unit, onInitiative: (Node?) -> Unit, onTick: (Node) -> Unit) {
    val f = world.metricFraction(kr.start, kr.target, kr.current)
    val initiatives = world.children(kr.id).filter { it.kind == NodeKind.ACTION.name }
    UCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.Top) {
            Text(kr.title.ifBlank { "…" }, style = UType.Strong, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(score(f), style = UType.Number.copy(fontSize = 18.sp, color = scoreColor(f, hue)))
        }
        Spacer(Modifier.height(8.dp))
        Bar(f, color = hue)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val unit = if (kr.unit.isBlank()) "" else " " + kr.unit
            Text(
                stringResource(R.string.okr_numbers, formatNumber(kr.start), formatNumber(kr.current) + unit, formatNumber(kr.target) + unit),
                style = UType.Small, modifier = Modifier.weight(1f),
            )
            Sparkline(world.logsOf(kr.id).map { it.value }, kr.start, kr.target, hue, Modifier.width(64.dp).height(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.okr_check_in),
                style = UType.Small.copy(color = U.c.onAccent, fontSize = 12.sp),
                modifier = Modifier.clip(CircleShape).background(hue).clickable(onClick = onCheckIn).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            world.logsOf(kr.id).maxByOrNull { it.day }?.let {
                Text(stringResource(R.string.okr_last_check, dateShort(it.day)), style = UType.Small)
            }
        }
        if (initiatives.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.okr_initiatives).uppercase(), style = UType.Eyebrow)
        }
        initiatives.forEach { n ->
            val goal = world.goal(n.goalId)
            val due = goal != null && world.dueOn(n, goal, world.day)
            val checked = if (n.repeats) world.log(n.id, world.day) != null else n.done
            Row(Modifier.fillMaxWidth().clip(U.RSm).clickable { onInitiative(n) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (due || !n.repeats) StarCheck(checked, { onTick(n) }, color = hue, size = 30.dp)
                else Spacer(Modifier.width(30.dp))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(n.title, style = UType.Body.copy(fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(scheduleLabel(n.days, n.perWeek), style = UType.Small)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(U.RSm).clickable { onInitiative(null) }.padding(top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = hue)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.okr_add_initiative), style = UType.Small.copy(color = hue))
        }
    }
}

/** The check-in history of a key result, scaled between its start and target. */
@Composable
private fun Sparkline(values: List<Double>, start: Double, target: Double, color: Color, modifier: Modifier) {
    if (values.size < 2) return
    Canvas(modifier) {
        val lo = minOf(start, target, values.min())
        val hi = maxOf(start, target, values.max())
        val span = (hi - lo).takeIf { it > 0 } ?: 1.0
        val down = target < start
        val pts = values.mapIndexed { i, v ->
            val t = ((v - lo) / span).toFloat().let { if (down) 1 - it else it }
            Offset(size.width * i / (values.size - 1), size.height * (1 - t))
        }
        val p = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(p, color.copy(alpha = 0.8f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 2.5.dp.toPx(), pts.last())
    }
}

@Composable
private fun CheckInDialog(kr: Node, hue: Color, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatNumber(kr.current)) }
    val v = parseNumber(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(stringResource(R.string.okr_check_in_title), style = UType.Heading) },
        text = {
            Column {
                Text(kr.title, style = UType.Muted)
                Spacer(Modifier.height(12.dp))
                UTextField(text, { text = it.take(12) }, label = stringResource(R.string.okr_now), number = true, suffix = kr.unit.ifBlank { null })
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.okr_target_is, formatNumber(kr.target) + if (kr.unit.isBlank()) "" else " " + kr.unit), style = UType.Small)
            }
        },
        confirmButton = {
            TextButton(onClick = { v?.let(onSave); onDismiss() }, enabled = v != null) { Text(stringResource(R.string.save), color = hue, style = UType.Button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}
