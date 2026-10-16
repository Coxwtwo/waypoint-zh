package app.waypoint.ui.methods

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Node
import app.waypoint.data.setDone
import app.waypoint.data.tick
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import app.waypoint.ui.Go
import app.waypoint.ui.Haptics
import app.waypoint.ui.WData
import app.waypoint.ui.captureInto
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.NodeEditor
import app.waypoint.ui.components.NodeSpec
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.Sky
import app.waypoint.ui.components.StarCheck
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.dimStar
import app.waypoint.ui.components.navStar
import app.waypoint.ui.scheduleLabel
import app.waypoint.ui.shareImage
import app.waypoint.ui.theme.Manrope
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import app.waypoint.ui.theme.Unbounded
import kotlinx.coroutines.launch

/** Cells 0..8 of a 3×3 block, and which of them hold the eight satellites (everything but the centre). */
private val RING = listOf(0, 1, 2, 3, 5, 6, 7, 8)

/** What the user asked to edit: a pillar by slot, or an action of a pillar by slot. */
private sealed interface MandalaEdit {
    data class Pillar(val slot: Int) : MandalaEdit
    data class Action(val pillarSlot: Int, val slot: Int) : MandalaEdit
}

/** Everything the chart draws, gathered once per recomposition. */
private class MandalaModel(goal: Goal, world: World) {
    val pillars: Map<Int, Node> = world.nodesOf(goal.id, NodeKind.PILLAR).filter { it.parentId == 0L }.associateBy { it.slot }
    val actions: Map<Int, Map<Int, Node>> = pillars.mapValues { (_, p) -> world.children(p.id).filter { it.kind == NodeKind.ACTION.name }.associateBy { it.slot } }
    fun action(pillar: Int, slot: Int): Node? = actions[pillar]?.get(slot)
    fun lit(pillar: Int): Int = actions[pillar]?.values?.count { it.done } ?: 0
    val litTotal: Int = (0 until 8).sumOf { lit(it) }
    val named: Boolean = pillars.values.any { it.title.isNotBlank() }
}

@Composable
fun MandalaBody(goal: Goal, data: WData, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val world = data.world
    val hue = U.c.hue(Method.MANDALA)
    val m = MandalaModel(goal, world)
    var selected by rememberSaveable(goal.id) { mutableStateOf(m.pillars.keys.minOrNull() ?: 0) }
    var edit by remember { mutableStateOf<MandalaEdit?>(null) }
    var share by remember { mutableStateOf(false) }

    fun light(n: Node, on: Boolean) {
        if (on && data.settings.haptics) Haptics.star(context)
        container.write { setDone(n, on) }
    }

    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.mandala_map).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.mandala_lit_total, m.litTotal), style = UType.Strong.copy(color = hue))
        }
        IconButton(onClick = { share = true }) {
            Icon(Icons.Rounded.IosShare, stringResource(R.string.mandala_share), tint = U.c.muted)
        }
    }

    if (!m.named) {
        UCard(border = hue.copy(alpha = 0.35f)) {
            Text(stringResource(R.string.mandala_hint_title), style = UType.Strong)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.mandala_hint), style = UType.Muted)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.mandala_how),
                style = UType.Small.copy(color = hue),
                modifier = Modifier.clip(U.RSm).clickable { go.method(Method.MANDALA) }.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    MandalaGrid(
        goal = goal,
        m = m,
        world = world,
        hue = hue,
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
        onCell = { block, cell ->
            when {
                block == 4 && cell == 4 -> Unit
                block == 4 -> {
                    val p = RING.indexOf(cell)
                    selected = p
                    if (m.pillars[p]?.title.isNullOrBlank()) edit = MandalaEdit.Pillar(p)
                }
                else -> {
                    val p = RING.indexOf(block)
                    if (selected != p) selected = p
                    else if (cell == 4 || m.pillars[p]?.title.isNullOrBlank()) edit = MandalaEdit.Pillar(p)
                    else edit = MandalaEdit.Action(p, RING.indexOf(cell))
                }
            }
        },
    )
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.mandala_tap_hint), style = UType.Small, modifier = Modifier.padding(horizontal = 4.dp))

    PillarDetail(
        pillarSlot = selected,
        m = m,
        world = world,
        hue = hue,
        onEditPillar = { edit = MandalaEdit.Pillar(selected) },
        onEditAction = { edit = MandalaEdit.Action(selected, it) },
        onLight = ::light,
        onToday = { n -> container.write { upsertNode(n.copy(dueDay = world.day)) } },
        onTick = { n -> container.write { tick(n, world.day) } },
    )

    when (val e = edit) {
        is MandalaEdit.Pillar -> {
            val existing = m.pillars[e.slot]
            NodeEditor(
                heading = stringResource(R.string.mandala_pillar_n, e.slot + 1),
                node = existing ?: Node(goalId = goal.id, kind = NodeKind.PILLAR.name, slot = e.slot),
                spec = NodeSpec(titleLabel = stringResource(R.string.mandala_pillar), titleHint = stringResource(R.string.mandala_pillar_hint)),
                onDismiss = { edit = null },
                onDelete = existing?.let { p -> { container.write { deleteNode(p.id) } } },
                color = hue,
            ) { n -> container.write { upsertNode(n) } }
        }
        is MandalaEdit.Action -> {
            val pillar = m.pillars[e.pillarSlot]
            if (pillar != null) {
                val existing = m.action(e.pillarSlot, e.slot)
                NodeEditor(
                    heading = if (existing == null) stringResource(R.string.mandala_new_action) else pillar.title,
                    node = existing ?: Node(goalId = goal.id, parentId = pillar.id, kind = NodeKind.ACTION.name, slot = e.slot),
                    spec = NodeSpec(
                        titleLabel = stringResource(R.string.mandala_action),
                        titleHint = stringResource(R.string.mandala_action_hint),
                        schedule = true,
                        due = true,
                        remind = true,
                    ),
                    onDismiss = { edit = null },
                    onDelete = existing?.let { a -> { container.write { deleteNode(a.id) } } },
                    color = hue,
                ) { n -> container.write { upsertNode(n) } }
            } else LaunchedEffect(e) { edit = MandalaEdit.Pillar(e.pillarSlot) }
        }
        null -> Unit
    }

    if (share) MandalaShareDialog(goal, m, world, hue) { share = false }
}

/** The 9×9 chart: the centre block holds the goal and its pillars, each outer block one pillar and its eight actions. */
@Composable
private fun MandalaGrid(
    goal: Goal,
    m: MandalaModel,
    world: World,
    hue: Color,
    selected: Int?,
    modifier: Modifier = Modifier,
    poster: Boolean = false,
    onCell: ((block: Int, cell: Int) -> Unit)? = null,
) {
    val blockGap = 5.dp
    val cellGap = 2.dp
    Column(modifier.aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(blockGap)) {
        for (br in 0 until 3) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(blockGap)) {
                for (bc in 0 until 3) {
                    val block = br * 3 + bc
                    val p = if (block == 4) -1 else RING.indexOf(block)
                    val isSel = !poster && selected != null && p == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (block == 4) hue.copy(alpha = 0.07f) else U.c.surface.copy(alpha = 0.55f))
                            .border(if (isSel) 1.5.dp else 1.dp, if (isSel) hue.copy(alpha = 0.8f) else U.c.line.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                            .padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(cellGap),
                    ) {
                        for (cr in 0 until 3) {
                            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                                for (cc in 0 until 3) {
                                    val cell = cr * 3 + cc
                                    Box(Modifier.weight(1f).fillMaxSize()) {
                                        MandalaCellFor(goal, m, world, hue, block, cell, onCell)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MandalaCellFor(goal: Goal, m: MandalaModel, world: World, hue: Color, block: Int, cell: Int, onCell: ((Int, Int) -> Unit)?) {
    val click = onCell?.let { { it(block, cell) } }
    when {
        block == 4 && cell == 4 -> GoalCell(goal.title, hue, click)
        block == 4 -> {
            val p = RING.indexOf(cell)
            PillarCell(m.pillars[p]?.title.orEmpty(), m.lit(p), hue, click)
        }
        cell == 4 -> {
            val p = RING.indexOf(block)
            PillarCell(m.pillars[p]?.title.orEmpty(), m.lit(p), hue, click)
        }
        else -> {
            val p = RING.indexOf(block)
            val a = m.action(p, RING.indexOf(cell))
            val level = when {
                a == null || a.title.isBlank() -> -1f
                a.done -> 1f
                a.repeats -> (world.consistency(a) * 0.7f).coerceAtMost(0.7f)
                else -> 0f
            }
            ActionCell(a?.title.orEmpty(), level, a?.done == true, hue, click)
        }
    }
}

@Composable
private fun GoalCell(title: String, hue: Color, onClick: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.radialGradient(listOf(hue, hue.copy(alpha = 0.75f))))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        CellText(title, U.c.onAccent, bold = true, display = true)
    }
}

@Composable
private fun PillarCell(title: String, lit: Int, hue: Color, onClick: (() -> Unit)?) {
    val named = title.isNotBlank()
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(if (named) hue.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, hue.copy(alpha = if (named) 0.55f else 0.25f), RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (named) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CellText(title, U.c.ink, bold = true)
                Text("$lit/8", fontFamily = Manrope, fontSize = 5.5.sp, lineHeight = 6.sp, color = hue, maxLines = 1)
            }
        } else {
            Icon(Icons.Rounded.Add, null, Modifier.size(10.dp), tint = hue.copy(alpha = 0.5f))
        }
    }
}

/** level: -1 empty slot, 0 written but unlit, 0..0.7 a habit's glow, 1 lit. */
@Composable
private fun ActionCell(title: String, level: Float, lit: Boolean, hue: Color, onClick: (() -> Unit)?) {
    val dim = U.c.star
    val shown by animateFloatAsState(level.coerceAtLeast(0f), tween(700), label = "glow")
    // A flare when a star has just been lit: grows and fades.
    val flare = remember { Animatable(0f) }
    var wasLit by remember { mutableStateOf(lit) }
    LaunchedEffect(lit) {
        if (lit && !wasLit) {
            flare.snapTo(1f)
            flare.animateTo(0f, tween(900))
        }
        wasLit = lit
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, if (level < 0f) U.c.line.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(5.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (level >= 0f) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height * 0.32f)
                val r = size.minDimension * 0.17f
                if (flare.value > 0f) {
                    drawCircle(Brush.radialGradient(listOf(hue.copy(alpha = 0.6f * flare.value), Color.Transparent), c, size.minDimension * (0.4f + 0.5f * (1 - flare.value))), size.minDimension, c)
                }
                if (shown <= 0.02f) dimStar(c, r, dim) else navStar(c, r * (0.7f + 0.35f * shown), hue, shown)
            }
            Box(Modifier.fillMaxSize().padding(start = 1.dp, end = 1.dp, bottom = 1.dp), contentAlignment = Alignment.BottomCenter) {
                CellText(title, if (lit) U.c.ink else U.c.muted, maxLines = 2)
            }
        }
    }
}

@Composable
private fun CellText(text: String, color: Color, bold: Boolean = false, display: Boolean = false, maxLines: Int = 3) {
    Text(
        text,
        fontFamily = if (display) Unbounded else Manrope,
        fontWeight = if (bold) FontWeight(700) else FontWeight(600),
        fontSize = if (display) 6.sp else 6.2.sp,
        lineHeight = 6.8.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The zoomed-in pillar under the chart: its eight actions as rows. */
@Composable
private fun PillarDetail(
    pillarSlot: Int,
    m: MandalaModel,
    world: World,
    hue: Color,
    onEditPillar: () -> Unit,
    onEditAction: (Int) -> Unit,
    onLight: (Node, Boolean) -> Unit,
    onToday: (Node) -> Unit,
    onTick: (Node) -> Unit,
) {
    val pillar = m.pillars[pillarSlot]
    SectionTitle(stringResource(R.string.mandala_pillar_n, pillarSlot + 1))
    if (pillar == null || pillar.title.isBlank()) {
        EmptyState(stringResource(R.string.mandala_pillar_first), stringResource(R.string.mandala_name_pillar), onEditPillar)
        return
    }
    UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pillar.title, style = UType.Heading)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.mandala_pillar_lit, m.lit(pillarSlot)), style = UType.Small.copy(color = hue))
            }
            IconButton(onClick = onEditPillar) { Icon(Icons.Rounded.Edit, stringResource(R.string.mandala_rename), tint = U.c.muted, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(4.dp))
        (0 until 8).forEach { slot ->
            val a = m.action(pillarSlot, slot)
            if (a == null || a.title.isBlank()) AddActionRow(hue) { onEditAction(slot) }
            else ActionRow(a, world, hue, { onEditAction(slot) }, onLight, onToday, onTick)
        }
    }
}

@Composable
private fun ActionRow(a: Node, world: World, hue: Color, onEdit: () -> Unit, onLight: (Node, Boolean) -> Unit, onToday: (Node) -> Unit, onTick: (Node) -> Unit) {
    val light = stringResource(if (a.done) R.string.mandala_dim else R.string.mandala_light)
    Row(
        Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onEdit).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StarCheck(a.done, { onLight(a, !a.done) }, color = hue, size = 34.dp, label = light)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = UType.Body.copy(color = if (a.done) U.c.ink else U.c.ink.copy(alpha = 0.9f)), maxLines = 2, overflow = TextOverflow.Ellipsis)
            val streak = world.streak(a)
            val meta = buildList {
                add(scheduleLabel(a.days, a.perWeek))
                if (streak > 1) add(stringResource(R.string.mandala_streak, streak))
                if (a.done) add(stringResource(R.string.mandala_part_of_you))
            }.joinToString(" · ")
            Text(meta, style = UType.Small)
        }
        val goal = world.goal(a.goalId)
        when {
            a.repeats && goal != null && world.dueOn(a, goal, world.day) -> {
                val ticked = world.log(a.id, world.day) != null
                SmallPill(stringResource(R.string.mandala_today), ticked, hue) { onTick(a) }
            }
            !a.repeats && !a.done && a.dueDay != world.day -> {
                SmallPill(stringResource(R.string.take_today), false, hue) { onToday(a) }
            }
            !a.repeats && !a.done && a.dueDay == world.day -> {
                SmallPill(stringResource(R.string.mandala_on_today), true, hue, null)
            }
        }
    }
}

@Composable
private fun SmallPill(text: String, on: Boolean, hue: Color, onClick: (() -> Unit)?) {
    Text(
        (if (on) "✓ " else "") + text,
        style = UType.Small.copy(color = if (on) U.c.onAccent else hue, fontSize = 11.sp),
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (on) hue else hue.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun AddActionRow(hue: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).clip(CircleShape).border(1.dp, hue.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, Modifier.size(14.dp), tint = hue.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.mandala_add_action), style = UType.Muted.copy(color = U.c.faint))
    }
}

/** A clean poster of the chart, previewed in a dialog and shared as a picture. */
@Composable
private fun MandalaShareDialog(goal: Goal, m: MandalaModel, world: World, hue: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.mandala_share)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().clip(U.RLg).captureInto(layer)) {
                Sky(Modifier.fillMaxWidth(), stars = 60) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(stringResource(R.string.m_mandala).uppercase(), style = UType.Eyebrow.copy(color = hue))
                        Spacer(Modifier.height(6.dp))
                        Text(goal.title, style = UType.Title.copy(fontSize = 19.sp, lineHeight = 25.sp))
                        Spacer(Modifier.height(14.dp))
                        MandalaGrid(goal, m, world, hue, selected = null, modifier = Modifier.fillMaxWidth(), poster = true)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.mandala_lit_total, m.litTotal), style = UType.Small.copy(color = hue), modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.mandala_poster_by), style = UType.Small)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = U.c.ink, style = UType.Button) }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(title, {
                    scope.launch { runCatching { shareImage(context, layer, "mandala", title) } }
                }, color = hue, icon = Icons.Rounded.IosShare)
            }
        }
    }
}
