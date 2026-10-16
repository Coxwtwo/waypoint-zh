package app.waypoint.ui.lenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.DateDialog
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.dateShort
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private class SmartStep(val key: String, val letter: Int, val title: Int, val hint: Int, val example: Int)

private val steps = listOf(
    SmartStep("smart.s", R.string.smart_s_letter, R.string.smart_s, R.string.smart_s_hint, R.string.smart_s_example),
    SmartStep("smart.m", R.string.smart_m_letter, R.string.smart_m, R.string.smart_m_hint, R.string.smart_m_example),
    SmartStep("smart.a", R.string.smart_a_letter, R.string.smart_a, R.string.smart_a_hint, R.string.smart_a_example),
    SmartStep("smart.r", R.string.smart_r_letter, R.string.smart_r, R.string.smart_r_hint, R.string.smart_r_example),
    SmartStep("smart.t", R.string.smart_t_letter, R.string.smart_t, R.string.smart_t_hint, R.string.smart_t_example),
)

/** Five questions that turn a wish into a goal, then one clear sentence that can become the goal's title. */
@Composable
fun SmartLens(goal: Goal, data: WData, go: Go) {
    val color = U.c.hue(Method.SMART)
    val container = LocalContext.current.container
    val answers = steps.map { rememberField(goal.id, it.key, data) }
    val wording = rememberField(goal.id, "smart.wording", data, initial = goal.title)
    var step by rememberSaveable { mutableIntStateOf(answers.indexOfFirst { it.text.isBlank() }.let { if (it < 0) 5 else it }) }
    var pickDeadline by remember { mutableStateOf(false) }
    val answered = answers.count { it.text.isNotBlank() }

    LensScaffold(Method.SMART, goal, go) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarMeter(answered, 5, color)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.smart_meter, answered), style = UType.Small)
        }
        Spacer(Modifier.height(16.dp))
        StepTabs(
            steps.map { stringResource(it.letter) } + "✦",
            step,
            answers.map { it.text.isNotBlank() } + (data.world.field(goal.id, "smart.wording").isNotBlank()),
            color,
        ) { step = it }
        Spacer(Modifier.height(20.dp))

        if (step < 5) {
            val s = steps[step]
            val a = answers[step]
            Question(stringResource(s.letter), stringResource(s.title), stringResource(s.hint), color)
            Spacer(Modifier.height(14.dp))
            UTextField(a.text, { a.set(it.take(400)) }, placeholder = stringResource(R.string.lens_answer_hint), minLines = 3)
            Example(stringResource(s.example))
            if (step == 4) {
                Spacer(Modifier.height(14.dp))
                UCard {
                    Text(stringResource(R.string.smart_deadline), style = UType.Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(goal.endDay?.let { dateShort(it) } ?: stringResource(R.string.smart_no_deadline), style = UType.Strong)
                    Spacer(Modifier.height(10.dp))
                    SoftButton(
                        stringResource(if (goal.endDay == null) R.string.smart_set_deadline else R.string.smart_change_deadline),
                        { pickDeadline = true }, color = color,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) SoftButton(stringResource(R.string.back), { step-- }, Modifier.weight(1f), color = color)
                PrimaryButton(stringResource(R.string.next), { step++ }, Modifier.weight(1f), color = color)
            }
        } else {
            Question("✦", stringResource(R.string.smart_wording), stringResource(R.string.smart_wording_hint), color)
            Spacer(Modifier.height(14.dp))
            if (answered > 0) {
                UCard {
                    steps.forEachIndexed { i, s ->
                        val t = answers[i].text
                        if (t.isNotBlank()) {
                            Text(stringResource(s.title), style = UType.Small.copy(color = color))
                            Text(t.trim(), style = UType.Muted)
                            if (i < 4) Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            UTextField(wording.text, { wording.set(it.take(200)) }, placeholder = stringResource(R.string.smart_wording_placeholder), minLines = 2)
            Example(stringResource(R.string.smart_wording_example))
            Spacer(Modifier.height(20.dp))
            val clean = wording.text.trim()
            PrimaryButton(
                stringResource(R.string.smart_use_title),
                { container.write { upsertGoal(goal.copy(title = clean)) } },
                Modifier.fillMaxWidth(),
                enabled = clean.isNotEmpty() && clean != goal.title,
                color = color,
            )
            if (clean.isNotEmpty() && clean == goal.title) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.smart_is_title), style = UType.Small, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.back), { step = 4 }, Modifier.fillMaxWidth(), color = color)
        }
    }

    if (pickDeadline) {
        DateDialog(goal.endDay, { pickDeadline = false }, minDay = data.day) { d ->
            container.write { upsertGoal(goal.copy(endDay = d)) }
        }
    }
}
