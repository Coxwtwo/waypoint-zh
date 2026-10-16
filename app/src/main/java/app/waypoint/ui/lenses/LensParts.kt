package app.waypoint.ui.lenses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.setField
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.components.dimStar
import app.waypoint.ui.components.navStar
import app.waypoint.ui.methodName
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.delay

/** The frame of every lens: top bar with an info button, the goal as context, a scrolling body. */
@Composable
internal fun LensScaffold(lens: Method, goal: Goal, go: Go, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(methodName(lens), onBack = { go.back() }) {
            IconButton(onClick = { go.method(lens) }) {
                Icon(Icons.Outlined.Info, stringResource(R.string.lens_about), tint = U.c.muted)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            Text(stringResource(R.string.lens_goal).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(4.dp))
            Text(goal.title, style = UType.Heading)
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

/** A text answer stored in a [app.waypoint.data.Field]; saves shortly after typing stops and when the screen closes. */
internal class FieldText(private val state: MutableState<String>, private val dirty: MutableState<Boolean>) {
    val text: String get() = state.value
    fun set(v: String) {
        state.value = v
        dirty.value = true
    }
}

/** [initial] fills an empty answer as a suggestion; it is only saved once the user edits it. */
@Composable
internal fun rememberField(goalId: Long, key: String, data: WData, initial: String = ""): FieldText {
    val stored = data.world.field(goalId, key)
    val state = rememberSaveable(goalId, key) { mutableStateOf(stored.ifEmpty { initial }) }
    val dirty = rememberSaveable(goalId, key) { mutableStateOf(false) }
    val container = LocalContext.current.container
    LaunchedEffect(state.value, dirty.value) {
        if (dirty.value && state.value.trim() != stored) {
            delay(500)
            container.write { setField(goalId, key, state.value) }
        }
    }
    val latest = rememberUpdatedState(state.value)
    DisposableEffect(goalId, key) {
        onDispose { if (dirty.value) container.write { setField(goalId, key, latest.value) } }
    }
    return FieldText(state, dirty)
}

/** Letter tabs for a guided method (S M A R T, G R O W…). [done] marks steps already answered. */
@Composable
internal fun StepTabs(letters: List<String>, selected: Int, done: List<Boolean>, color: Color, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        letters.forEachIndexed { i, l ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(U.RSm)
                    .background(if (on) color.copy(alpha = 0.18f) else U.c.surface.copy(alpha = 0.7f))
                    .border(1.dp, if (on) color.copy(alpha = 0.7f) else if (done.getOrElse(i) { false }) color.copy(alpha = 0.35f) else U.c.line, U.RSm)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    l,
                    style = UType.Heading.copy(
                        fontSize = 15.sp,
                        color = if (on || done.getOrElse(i) { false }) color else U.c.muted,
                    ),
                )
            }
        }
    }
}

/** A row of stars, [lit] of [total] shining: how much of the method is answered. */
@Composable
internal fun StarMeter(lit: Int, total: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val dim = U.c.star
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Canvas(Modifier.size(size)) {
                val c = Offset(this.size.width / 2, this.size.height / 2)
                val r = this.size.minDimension * 0.42f
                if (i < lit) navStar(c, r, color, 1f) else dimStar(c, r, dim)
            }
        }
    }
}

/** The big question of a step: a glowing letter, the question and a softer hint under it. */
@Composable
internal fun Question(letter: String, title: String, hint: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(letter, style = UType.Heading.copy(color = color, fontSize = 15.sp, textAlign = TextAlign.Center)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Heading)
            if (hint.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(hint, style = UType.Sub)
            }
        }
    }
}

/** "For example: …" in a quiet italic. */
@Composable
internal fun Example(text: String) {
    Text(stringResource(R.string.lens_example, text), style = UType.Muted.copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

/** A soft panel in the lens colour for a tip or a moment to pause. */
@Composable
internal fun Tip(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), U.RMd)
            .padding(14.dp),
    ) { Text(text, style = UType.Body.copy(fontSize = 14.sp, color = U.c.ink)) }
}
