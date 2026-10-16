package app.waypoint.ui.lenses

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.UCard
import app.waypoint.ui.components.UTextField
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** Five times "why does that matter?", each step quoting the last answer, down to the reason that keeps you going. */
@Composable
fun WhysLens(goal: Goal, data: WData, go: Go) {
    val color = U.c.hue(Method.WHYS)
    val container = LocalContext.current.container
    val answers = (1..5).map { rememberField(goal.id, "whys.$it", data) }
    val deepest = answers.lastOrNull { it.text.isNotBlank() }?.text?.trim().orEmpty()
    val depth = answers.indexOfLast { it.text.isNotBlank() } + 1

    LensScaffold(Method.WHYS, goal, go) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarMeter(depth, 5, color)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.whys_depth, depth), style = UType.Small)
        }
        Spacer(Modifier.height(20.dp))
        answers.forEachIndexed { i, a ->
            // Each question opens once the one before it has an answer.
            if (i > 0 && answers[i - 1].text.isBlank()) return@forEachIndexed
            if (i > 0) {
                Row(verticalAlignment = Alignment.Top) {
                    Spacer(Modifier.width(54.dp))
                    Text("«" + answers[i - 1].text.trim() + "»", style = UType.Muted.copy(fontStyle = FontStyle.Italic))
                }
                Spacer(Modifier.height(10.dp))
            }
            Question(
                (i + 1).toString(),
                if (i == 0) stringResource(R.string.whys_first, goal.title) else stringResource(R.string.whys_next),
                if (i == 0) stringResource(R.string.whys_first_hint) else "",
                color,
            )
            Spacer(Modifier.height(10.dp))
            UTextField(a.text, { a.set(it.take(400)) }, placeholder = stringResource(R.string.whys_because), minLines = 2)
            Spacer(Modifier.height(22.dp))
        }

        if (depth >= 3) {
            UCard(border = color.copy(alpha = 0.5f)) {
                Text(stringResource(R.string.whys_deep).uppercase(), style = UType.Eyebrow.copy(color = color))
                Spacer(Modifier.height(8.dp))
                Text(deepest, style = UType.Heading)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.whys_deep_text), style = UType.Muted)
                Spacer(Modifier.height(14.dp))
                if (goal.why.trim() == deepest) {
                    Text(stringResource(R.string.whys_on_card), style = UType.Small.copy(color = color))
                } else {
                    PrimaryButton(
                        stringResource(R.string.whys_put_on_card),
                        { container.write { upsertGoal(goal.copy(why = deepest)) } },
                        Modifier.fillMaxWidth(), color = color,
                    )
                }
            }
        } else if (depth > 0) {
            Text(stringResource(R.string.whys_keep_going), style = UType.Muted)
        }
    }
}
