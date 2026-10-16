package app.waypoint.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.ui.WData
import app.waypoint.ui.components.Chip
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.IconDisc
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.components.motionOn
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType
import kotlinx.coroutines.launch

/** Where to go after the welcome: straight to a new goal, the quiz, the wheel of life, or just look around. */
enum class FirstStep { GOAL, QUIZ, WHEEL, LOOK }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(data: WData, onDone: (FirstStep) -> Unit) {
    val container = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(data.settings.name) }
    var focus by rememberSaveable { mutableIntStateOf(data.settings.focusLimit) }

    fun finish(step: FirstStep) {
        container.scope.launch {
            container.settings.update { it.copy(onboarded = true, name = name.trim(), focusLimit = focus) }
            container.changed()
        }
        onDone(step)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) }, label = "page") { p ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (p) {
                    0 -> Welcome()
                    1 -> {
                        Spacer(Modifier.height(56.dp))
                        Text(stringResource(R.string.ob_name), style = UType.Title)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.ob_name_text), style = UType.Sub)
                        Spacer(Modifier.height(20.dp))
                        UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.ob_name_hint))
                    }
                    2 -> {
                        Spacer(Modifier.height(56.dp))
                        Text(stringResource(R.string.ob_focus), style = UType.Title)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.ob_focus_text), style = UType.Sub)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { n -> Chip(n.toString(), focus == n, { focus = n }) }
                            Chip(stringResource(R.string.set_focus_off), focus == 0, { focus = 0 })
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob_focus_hint), style = UType.Small)
                    }
                    else -> {
                        Spacer(Modifier.height(56.dp))
                        Text(stringResource(R.string.ob_first), style = UType.Title)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.ob_first_text), style = UType.Sub)
                        Spacer(Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SoftButton(stringResource(R.string.ob_first_goal), { finish(FirstStep.GOAL) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Flag)
                            SoftButton(stringResource(R.string.goals_quiz), { finish(FirstStep.QUIZ) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Quiz)
                            SoftButton(stringResource(R.string.goals_unsure), { finish(FirstStep.WHEEL) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Explore)
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { finish(FirstStep.LOOK) }) { Text(stringResource(R.string.ob_first_look), color = U.c.muted, style = UType.Button) }
                    }
                }
            }
        }
        if (page < 3) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) TextButton(onClick = { page-- }) { Text(stringResource(R.string.back), color = U.c.muted, style = UType.Button) }
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(if (page == 0) R.string.ob_begin else R.string.next), { page++ }, Modifier.width(180.dp))
            }
        }
    }
}

@Composable
private fun Welcome() {
    val lit = remember { Animatable(if (motionOn()) 0f else 9f) }
    LaunchedEffect(Unit) { lit.animateTo(9f, tween(2200)) }
    Spacer(Modifier.height(36.dp))
    Constellation(2026, List(9) { i -> (lit.value - i).coerceIn(0f, 1f) }, U.c.accent, Modifier.fillMaxWidth().height(170.dp), starSize = 9.dp)
    Spacer(Modifier.height(16.dp))
    Text("Waypoint", style = UType.Display.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.ob_tagline), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(28.dp))
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Feature(Icons.Rounded.Explore, stringResource(R.string.ob_f1_title), stringResource(R.string.ob_f1_text))
        Feature(Icons.Rounded.AutoAwesome, stringResource(R.string.ob_f2_title), stringResource(R.string.ob_f2_text))
        Feature(Icons.Rounded.WbTwilight, stringResource(R.string.ob_f3_title), stringResource(R.string.ob_f3_text))
        Feature(Icons.Rounded.Visibility, stringResource(R.string.ob_f4_title), stringResource(R.string.ob_f4_text))
        Feature(Icons.Rounded.Lock, stringResource(R.string.ob_f5_title), stringResource(R.string.ob_f5_text))
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        IconDisc(icon, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(text, style = UType.Muted)
        }
    }
    Spacer(Modifier.size(0.dp))
}
