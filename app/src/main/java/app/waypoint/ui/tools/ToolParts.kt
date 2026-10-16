package app.waypoint.ui.tools

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.methodName
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** The frame every tool shares: a top bar with the method's name and an info button, and a scrolling body. */
@Composable
internal fun ToolScaffold(
    tool: Method?,
    go: Go,
    title: String? = null,
    onBack: () -> Unit = { go.back() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(title ?: tool?.let { methodName(it) }.orEmpty(), onBack = onBack) {
            if (tool != null) IconButton(onClick = { go.method(tool) }) {
                Icon(Icons.Outlined.Info, stringResource(R.string.tool_about), tint = U.c.muted)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            content = content,
        )
    }
}

/** A labelled slider with its value on the right. */
@Composable
internal fun ValueSlider(
    label: String,
    value: Int,
    range: IntRange,
    color: Color,
    onChange: (Int) -> Unit,
    valueText: String = value.toString(),
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = UType.Body, modifier = Modifier.weight(1f))
            Text(valueText, style = UType.Strong.copy(color = color))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0).let { if (it > 30) 0 else it },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = U.c.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

/** "A goal already": shown on an idea that became a goal; tapping opens it. */
@Composable
internal fun GoalMark(goalId: Long, go: Go, color: Color) {
    Row(
        Modifier.clip(U.RSm).clickable { go.goal(goalId) }.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.tool_is_goal), style = UType.Small.copy(color = color))
    }
}

@Composable
internal fun Hint(text: String) {
    Text(text, style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
    Spacer(Modifier.height(4.dp))
}
