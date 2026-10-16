package app.waypoint.ui.tools

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Idea
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.ConfirmDialog
import app.waypoint.ui.components.EmptyState
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.navStar
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private const val MAX = 25
private const val TOP = 5

@Composable
fun BuffettScreen(data: WData, go: Go) {
    val tool = Method.BUFFETT
    val hue = U.c.hue(tool)
    val context = LocalContext.current
    val container = context.container
    val items = data.world.ideas(tool).sortedWith(compareBy({ it.sort }, { it.id }))
    val top = items.filter { it.chosen }
    val rest = items.filterNot { it.chosen }
    var editing by remember { mutableStateOf<Idea?>(null) }
    val full = stringResource(R.string.buf_top_full)

    fun toggle(i: Idea) {
        if (!i.chosen && top.size >= TOP) {
            Toast.makeText(context, full, Toast.LENGTH_SHORT).show()
            return
        }
        container.write { upsertIdea(i.copy(chosen = !i.chosen)) }
    }

    ToolScaffold(tool, go) {
        Hint(stringResource(R.string.buf_intro))
        Spacer(Modifier.height(10.dp))
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.buf_written, items.size, MAX), style = UType.Strong, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.buf_chosen, top.size, TOP), style = UType.Small.copy(color = hue))
            }
            Spacer(Modifier.height(8.dp))
            Bar(items.size / MAX.toFloat(), color = hue)
        }
        if (items.size < MAX) {
            Spacer(Modifier.height(12.dp))
            SoftButton(stringResource(R.string.buf_add), {
                editing = Idea(tool = tool.name, title = "", sort = (items.maxOfOrNull { it.sort } ?: 0) + 1, at = System.currentTimeMillis())
            }, Modifier.fillMaxWidth(), color = hue, icon = Icons.Rounded.Add)
        }

        if (items.isEmpty()) {
            EmptyState(stringResource(R.string.buf_empty))
            return@ToolScaffold
        }

        SectionTitle(stringResource(R.string.buf_top))
        if (top.isEmpty()) Hint(stringResource(R.string.buf_top_empty))
        else UCard(padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            top.forEachIndexed { idx, i ->
                if (idx > 0) HorizontalDivider(color = U.c.line)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StarToggle(true, hue) { toggle(i) }
                    Spacer(Modifier.width(8.dp))
                    Text(i.title, style = UType.Strong, modifier = Modifier.weight(1f).clickable { editing = i })
                    if (i.goalId != 0L && data.world.goal(i.goalId) != null) GoalMark(i.goalId, go, hue)
                    else TextButton(onClick = { go.newGoal(title = i.title, idea = i.id) }) {
                        Text(stringResource(R.string.tool_make_goal), color = hue, style = UType.Button)
                    }
                }
            }
        }

        if (rest.isNotEmpty()) {
            SectionTitle(if (top.size >= TOP) stringResource(R.string.buf_avoid) else stringResource(R.string.buf_list))
            Hint(stringResource(if (top.size >= TOP) R.string.buf_avoid_text else R.string.buf_list_text))
            UCard(padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                rest.forEachIndexed { idx, i ->
                    if (idx > 0) HorizontalDivider(color = U.c.line)
                    val avoid = top.size >= TOP
                    Row(Modifier.fillMaxWidth().clickable { editing = i }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (avoid) {
                            Icon(Icons.Rounded.Block, stringResource(R.string.buf_avoid), Modifier.padding(6.dp).size(20.dp), tint = U.c.faint)
                        } else StarToggle(false, hue) { toggle(i) }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            i.title,
                            style = if (avoid) UType.Body.copy(color = U.c.faint, textDecoration = TextDecoration.LineThrough) else UType.Body,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    editing?.let { i ->
        BuffettEditor(i, hue, chosenCount = top.size, onDismiss = { editing = null }, onToggle = { toggle(i) })
    }
}

@Composable
private fun StarToggle(on: Boolean, hue: Color, onClick: () -> Unit) {
    val dim = U.c.faint
    Canvas(Modifier.size(32.dp).clickable(onClick = onClick).padding(4.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        if (on) navStar(c, size.minDimension * 0.4f, hue, 1f)
        else drawCircle(dim, size.minDimension * 0.3f, c, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun BuffettEditor(item: Idea, hue: Color, chosenCount: Int, onDismiss: () -> Unit, onToggle: () -> Unit) {
    val container = LocalContext.current.container
    var title by remember { mutableStateOf(item.title) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(stringResource(if (item.id == 0L) R.string.buf_new else R.string.buf_edit), style = UType.Heading) },
        text = {
            androidx.compose.foundation.layout.Column {
                UTextField(title, { title = it.take(120) }, placeholder = stringResource(R.string.buf_hint))
                if (item.id != 0L && (item.chosen || chosenCount < TOP)) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onToggle(); onDismiss() }) {
                        Text(stringResource(if (item.chosen) R.string.buf_unchoose else R.string.buf_choose), color = hue, style = UType.Button)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val saved = item.copy(title = title.trim())
                container.write { upsertIdea(saved) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = hue, style = UType.Button) }
        },
        dismissButton = {
            Row {
                if (item.id != 0L) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = U.c.danger, style = UType.Button) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
            }
        },
    )
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.buf_delete), stringResource(R.string.buf_delete_text), stringResource(R.string.delete),
            onConfirm = { container.write { deleteIdea(item) }; onDismiss() }, onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}
