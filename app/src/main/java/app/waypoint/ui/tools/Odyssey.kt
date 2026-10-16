package app.waypoint.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Idea
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Bar
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.ProgressRing
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private val lifeTitles = listOf(R.string.ody_life1, R.string.ody_life2, R.string.ody_life3)
private val lifePrompts = listOf(R.string.ody_life1_text, R.string.ody_life2_text, R.string.ody_life3_text)
private val gaugeNames = listOf(R.string.ody_resources, R.string.ody_like, R.string.ody_confidence, R.string.ody_coherence)

private fun Idea.gauges() = listOf(a, b, c, d)

@Composable
fun OdysseyScreen(data: WData, go: Go) {
    val tool = Method.ODYSSEY
    val hue = U.c.hue(tool)
    val lives = data.world.ideas(tool)
    var editing by remember { mutableStateOf<Idea?>(null) }

    ToolScaffold(tool, go) {
        Hint(stringResource(R.string.ody_intro))
        (0..2).forEach { slot ->
            val life = lives.firstOrNull { it.slot == slot }
            SectionTitle(stringResource(R.string.ody_life_n, slot + 1) + " · " + stringResource(lifeTitles[slot]))
            UCard {
                if (life == null) {
                    Text(stringResource(lifePrompts[slot]), style = UType.Sub)
                    Spacer(Modifier.height(12.dp))
                    SoftButton(stringResource(R.string.ody_sketch), {
                        editing = Idea(tool = tool.name, slot = slot, title = "", a = 50, b = 50, c = 50, d = 50, at = System.currentTimeMillis())
                    }, color = hue)
                } else {
                    Text(life.title, style = UType.Heading)
                    if (life.text.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(life.text, style = UType.Body)
                    }
                    Spacer(Modifier.height(12.dp))
                    life.gauges().forEachIndexed { i, v ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(stringResource(gaugeNames[i]), style = UType.Muted, modifier = Modifier.width(110.dp))
                            Bar(v / 100f, Modifier.weight(1f), color = hue)
                            Text(v.toString(), style = UType.Small, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { editing = life }) { Text(stringResource(R.string.edit), color = hue, style = UType.Button) }
                        Spacer(Modifier.weight(1f))
                        if (life.goalId != 0L && data.world.goal(life.goalId) != null) GoalMark(life.goalId, go, hue)
                        else TextButton(onClick = { go.newGoal(title = life.title, idea = life.id) }) {
                            Text(stringResource(R.string.tool_make_goal), color = hue, style = UType.Button)
                        }
                    }
                }
            }
        }

        val filled = (0..2).mapNotNull { s -> lives.firstOrNull { it.slot == s } }
        if (filled.size >= 2) {
            SectionTitle(stringResource(R.string.ody_compare))
            UCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { slot ->
                        val life = lives.firstOrNull { it.slot == slot }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.ody_life_n, slot + 1), style = UType.Small.copy(color = hue))
                            Spacer(Modifier.height(8.dp))
                            if (life == null) Text("—", style = UType.Muted)
                            else life.gauges().forEachIndexed { i, v ->
                                ProgressRing(v / 100f, Modifier.size(44.dp), stroke = 4.dp, color = hue) {
                                    Text(v.toString(), style = UType.Small.copy(color = U.c.ink))
                                }
                                Text(stringResource(gaugeNames[i]), style = UType.Small, textAlign = TextAlign.Center, maxLines = 1)
                                Spacer(Modifier.height(6.dp))
                            }
                            if (life != null) {
                                Text(stringResource(R.string.ody_total, life.gauges().sum() / 4), style = UType.Strong.copy(color = hue))
                            }
                        }
                    }
                }
            }
            val best = filled.maxBy { it.gauges().sum() }
            Spacer(Modifier.height(8.dp))
            Hint(stringResource(R.string.ody_compare_hint, best.slot + 1))
        }
    }

    editing?.let { life ->
        LifeEditor(life, hue, onDismiss = { editing = null })
    }
}

@Composable
private fun LifeEditor(life: Idea, hue: Color, onDismiss: () -> Unit) {
    val container = LocalContext.current.container
    var title by remember { mutableStateOf(life.title) }
    var text by remember { mutableStateOf(life.text) }
    val g = remember { life.gauges().map { mutableIntStateOf(it) } }
    var confirmDelete by remember { mutableStateOf(false) }
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
            Text(stringResource(R.string.ody_life_n, life.slot + 1) + " · " + stringResource(lifeTitles[life.slot]), style = UType.Heading)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(lifePrompts[life.slot]), style = UType.Muted)
            Spacer(Modifier.height(14.dp))
            UTextField(title, { title = it.take(80) }, label = stringResource(R.string.ody_headline), placeholder = stringResource(R.string.ody_headline_hint))
            Spacer(Modifier.height(10.dp))
            UTextField(text, { text = it.take(1000) }, label = stringResource(R.string.ody_story), placeholder = stringResource(R.string.ody_story_hint), minLines = 4)
            Spacer(Modifier.height(12.dp))
            gaugeNames.forEachIndexed { i, n -> ValueSlider(stringResource(n), g[i].intValue, 0..100, hue, { g[i].intValue = it }) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (life.id != 0L) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = U.c.danger, style = UType.Button) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
                Spacer(Modifier.width(4.dp))
                PrimaryButton(stringResource(R.string.save), {
                    val saved = life.copy(title = title.trim(), text = text.trim(), a = g[0].intValue, b = g[1].intValue, c = g[2].intValue, d = g[3].intValue)
                    container.write { upsertIdea(saved) }
                    onDismiss()
                }, enabled = title.isNotBlank(), color = hue)
            }
        }
    }
    if (confirmDelete) {
        app.waypoint.ui.components.ConfirmDialog(
            stringResource(R.string.ody_delete), stringResource(R.string.ody_delete_text), stringResource(R.string.delete),
            onConfirm = { container.write { deleteIdea(life) }; onDismiss() }, onDismiss = { confirmDelete = false }, danger = true,
        )
    }
}
