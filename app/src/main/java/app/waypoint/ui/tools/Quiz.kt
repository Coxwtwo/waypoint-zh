package app.waypoint.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.SoftButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.dimStar
import app.waypoint.ui.components.navStar
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodName
import app.waypoint.ui.methodTagline
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

private class Question(val title: Int, val answers: List<Int>)

private val questions = listOf(
    Question(R.string.quiz_q1, listOf(R.string.quiz_q1_dream, R.string.quiz_q1_number, R.string.quiz_q1_habit, R.string.quiz_q1_project, R.string.quiz_q1_unsure)),
    Question(R.string.quiz_q2, listOf(R.string.quiz_q2_yes, R.string.quiz_q2_no)),
    Question(R.string.quiz_q3, listOf(R.string.quiz_q3_weeks, R.string.quiz_q3_quarter, R.string.quiz_q3_year)),
    Question(R.string.quiz_q4, listOf(R.string.quiz_q4_focus, R.string.quiz_q4_delay, R.string.quiz_q4_give_up, R.string.quiz_q4_overwhelm, R.string.quiz_q4_start)),
    Question(R.string.quiz_q5, listOf(R.string.quiz_q5_lot, R.string.quiz_q5_little)),
)

private fun factorText(f: Factor): Int = when (f) {
    Factor.KIND_DREAM -> R.string.quiz_why_dream
    Factor.KIND_NUMBER -> R.string.quiz_why_number
    Factor.KIND_HABIT -> R.string.quiz_why_habit
    Factor.KIND_PROJECT -> R.string.quiz_why_project
    Factor.KIND_UNSURE -> R.string.quiz_why_unsure
    Factor.MEASURABLE -> R.string.quiz_why_measurable
    Factor.NOT_MEASURABLE -> R.string.quiz_why_not_measurable
    Factor.WEEKS -> R.string.quiz_why_weeks
    Factor.QUARTER -> R.string.quiz_why_quarter
    Factor.YEAR -> R.string.quiz_why_year
    Factor.FOCUS -> R.string.quiz_why_focus
    Factor.DELAY -> R.string.quiz_why_delay
    Factor.GIVE_UP -> R.string.quiz_why_give_up
    Factor.OVERWHELM -> R.string.quiz_why_overwhelm
    Factor.START -> R.string.quiz_why_start
    Factor.STRUCTURED -> R.string.quiz_why_structured
    Factor.LIGHT -> R.string.quiz_why_light
}

private fun lensText(m: Method): Int = when (m) {
    Method.WOOP -> R.string.quiz_lens_woop
    Method.SMART -> R.string.quiz_lens_smart
    Method.PREMORTEM -> R.string.quiz_lens_premortem
    else -> R.string.quiz_lens_whys
}

@Composable
fun QuizScreen(data: WData, go: Go) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var a0 by rememberSaveable { mutableIntStateOf(-1) }
    var a1 by rememberSaveable { mutableIntStateOf(-1) }
    var a2 by rememberSaveable { mutableIntStateOf(-1) }
    var a3 by rememberSaveable { mutableIntStateOf(-1) }
    var a4 by rememberSaveable { mutableIntStateOf(-1) }
    val answers = listOf(a0, a1, a2, a3, a4)
    fun set(i: Int, v: Int) = when (i) { 0 -> a0 = v; 1 -> a1 = v; 2 -> a2 = v; 3 -> a3 = v; else -> a4 = v }
    val back = { if (step > 0) step-- else go.back() }
    BackHandler(enabled = step > 0) { step-- }

    ToolScaffold(null, go, title = stringResource(R.string.quiz_title), onBack = { back() }) {
        StarProgress(step, questions.size)
        Spacer(Modifier.height(18.dp))
        if (step < questions.size) {
            val q = questions[step]
            Text(stringResource(R.string.quiz_step, step + 1, questions.size), style = UType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(q.title), style = UType.Title)
            Spacer(Modifier.height(18.dp))
            q.answers.forEachIndexed { i, res ->
                val on = answers[step] == i
                val accent = U.c.accent
                Row(
                    Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .clip(U.RMd)
                        .border(1.dp, if (on) U.c.accent else U.c.line, U.RMd)
                        .clickable { set(step, i); step++ }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(res), style = UType.Strong, modifier = Modifier.weight(1f))
                    Canvas(Modifier.size(18.dp)) {
                        val c = Offset(size.width / 2, size.height / 2)
                        if (on) navStar(c, size.minDimension / 2, accent, 1f)
                    }
                }
            }
        } else {
            val rec = Quiz.recommend(
                QuizAnswers(
                    kind = GoalKind.entries[a0.coerceAtLeast(0)],
                    measurable = a1 == 0,
                    horizon = Horizon.entries[a2.coerceAtLeast(0)],
                    blocker = Blocker.entries[a3.coerceAtLeast(0)],
                    structured = a4 == 0,
                ),
            )
            Result(rec, go) { step = 0 }
        }
    }
}

@Composable
private fun StarProgress(step: Int, total: Int) {
    val accent = U.c.accent
    val dim = U.c.star
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val gap = size.width / (total + 1)
        for (i in 0 until total) {
            val c = Offset(gap * (i + 1), size.height / 2)
            if (i > 0) drawLine(dim.copy(alpha = 0.2f), Offset(gap * i + 8.dp.toPx(), c.y), Offset(c.x - 8.dp.toPx(), c.y), 1.dp.toPx())
            if (i < step) navStar(c, 7.dp.toPx(), accent, 1f)
            else if (i == step) navStar(c, 6.dp.toPx(), accent, 0.4f)
            else dimStar(c, 7.dp.toPx(), dim)
        }
    }
}

@Composable
private fun Result(rec: Recommendation, go: Go, onRestart: () -> Unit) {
    if (rec.toolsFirst) {
        Text(stringResource(R.string.quiz_tools_title), style = UType.Title)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.quiz_tools_text), style = UType.Sub)
        Spacer(Modifier.height(14.dp))
        listOf(Method.WHEEL, Method.ODYSSEY, Method.BUFFETT).forEach { t ->
            MethodCard(t, null, go, primary = stringResource(R.string.open)) { go.tool(t) }
        }
        SectionTitle(stringResource(R.string.quiz_or_start))
    } else {
        Text(stringResource(R.string.quiz_result_title), style = UType.Title)
        Spacer(Modifier.height(14.dp))
    }
    rec.picks.forEachIndexed { i, p ->
        MethodCard(
            p.method,
            stringResource(R.string.quiz_because, stringResource(factorText(p.because))),
            go,
            primary = stringResource(R.string.quiz_start_with, methodName(p.method)),
            best = i == 0 && !rec.toolsFirst,
        ) { go.newGoal(method = p.method) }
    }
    SectionTitle(stringResource(R.string.quiz_lens_title))
    UCard(onClick = { go.method(rec.lens) }) {
        val hue = U.c.hue(rec.lens)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(methodIcon(rec.lens), null, Modifier.size(22.dp), tint = hue)
            Spacer(Modifier.width(10.dp))
            Text(methodName(rec.lens), style = UType.Strong.copy(color = hue))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(lensText(rec.lens)), style = UType.Sub)
    }
    Spacer(Modifier.height(16.dp))
    TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.quiz_again), color = U.c.muted, style = UType.Button)
    }
}

@Composable
private fun MethodCard(m: Method, reason: String?, go: Go, primary: String, best: Boolean = false, onPrimary: () -> Unit) {
    val hue = U.c.hue(m)
    UCard(Modifier.padding(vertical = 6.dp), border = if (best) hue.copy(alpha = 0.6f) else U.c.line) {
        if (best) {
            Text(stringResource(R.string.quiz_best).uppercase(), style = UType.Eyebrow.copy(color = hue))
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(methodIcon(m), null, Modifier.size(24.dp), tint = hue)
            Spacer(Modifier.width(10.dp))
            Text(methodName(m), style = UType.Heading)
        }
        Spacer(Modifier.height(6.dp))
        Text(methodTagline(m), style = UType.Body)
        if (reason != null) {
            Spacer(Modifier.height(4.dp))
            Text(reason, style = UType.Muted)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (best) PrimaryButton(primary, onPrimary, Modifier.weight(1f), color = hue)
            else SoftButton(primary, onPrimary, Modifier.weight(1f), color = hue)
            TextButton(onClick = { go.method(m) }) { Text(stringResource(R.string.quiz_about), color = U.c.muted, style = UType.Button) }
        }
    }
}
