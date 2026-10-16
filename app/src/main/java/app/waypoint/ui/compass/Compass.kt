package app.waypoint.ui.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.waypoint.R
import app.waypoint.domain.Group
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData
import app.waypoint.ui.components.Constellation
import app.waypoint.ui.components.PrimaryButton
import app.waypoint.ui.components.SectionTitle
import app.waypoint.ui.components.TopBar
import app.waypoint.ui.components.UCard
import app.waypoint.ui.groupName
import app.waypoint.ui.groupText
import app.waypoint.ui.methodAbout
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodName
import app.waypoint.ui.methodOrigin
import app.waypoint.ui.methodTagline
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** Groups in the order of the road: choose, sharpen, drive, protect. */
private val roadOrder = listOf(Group.CHOOSE, Group.SHAPE, Group.DRIVE, Group.PROTECT)

/** Every method in the app, grouped by where it helps on the way to a goal, with the quiz on top. */
@Composable
fun CompassTab(data: WData, go: Go) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Text(stringResource(R.string.compass_title), style = UType.Display)
            Text(stringResource(R.string.compass_sub), style = UType.Muted)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Spacer(Modifier.height(14.dp))
            UCard(onClick = { go.quiz() }, border = U.c.accent.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Constellation(42, listOf(1f, 0.7f, 1f, 0.4f, 0.9f), U.c.accent, Modifier.size(width = 76.dp, height = 56.dp), starSize = 5.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.compass_quiz), style = UType.Strong.copy(color = U.c.accent))
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.compass_quiz_text), style = UType.Muted)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.accent)
                }
            }
            roadOrder.forEach { g ->
                SectionTitle(groupName(g))
                Text(groupText(g), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Method.entries.filter { it.group == g }.forEach { m ->
                        MethodRow(m) { if (m.tool) go.tool(m) else go.method(m) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodRow(m: Method, onClick: () -> Unit) {
    val hue = U.c.hue(m)
    UCard(onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(hue.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(methodIcon(m), null, Modifier.size(20.dp), tint = hue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(methodName(m), style = UType.Strong)
                Text(methodTagline(m), style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
        }
    }
}

/** What a method is, how to do it, where it comes from, and a way to start. */
@Composable
fun MethodScreen(m: Method, data: WData, go: Go) {
    val hue = U.c.hue(m)
    var pickGoal by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(groupName(m.group), onBack = { go.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Constellation(m.ordinal * 7 + 3, List(7) { i -> if (i % 3 == 1) 0.45f else 1f }, hue, Modifier.fillMaxWidth().height(120.dp), starSize = 8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(methodIcon(m), null, Modifier.size(24.dp), tint = hue)
                Spacer(Modifier.width(10.dp))
                Text(methodName(m), style = UType.Title)
            }
            Spacer(Modifier.height(6.dp))
            Text(methodTagline(m), style = UType.Sub.copy(color = hue))
            Spacer(Modifier.height(14.dp))
            Text(methodAbout(m), style = UType.Body)

            SectionTitle(stringResource(R.string.method_how))
            MethodSteps(m)

            SectionTitle(stringResource(R.string.method_origin))
            Text(methodOrigin(m), style = UType.Sub.copy(fontStyle = FontStyle.Italic))
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
            when {
                m.drives -> PrimaryButton(stringResource(R.string.method_start), { go.newGoal(method = m) }, Modifier.fillMaxWidth(), color = hue)
                m.tool -> PrimaryButton(stringResource(R.string.method_open_tool), { go.tool(m) }, Modifier.fillMaxWidth(), color = hue)
                else -> PrimaryButton(stringResource(R.string.method_use_lens), {
                    val active = data.world.active
                    if (active.size == 1) go.lens(active.first().id, m) else pickGoal = true
                }, Modifier.fillMaxWidth(), color = hue, enabled = data.world.active.isNotEmpty())
            }
            if (m.lens && data.world.active.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.method_lens_no_goals), style = UType.Small)
            }
        }
    }

    if (pickGoal) {
        Dialog(onDismissRequest = { pickGoal = false }) {
            UCard(padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                Text(stringResource(R.string.method_pick_goal), style = UType.Heading)
                Spacer(Modifier.height(10.dp))
                data.world.active.forEach { g ->
                    Text(
                        g.title, style = UType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clip(U.RSm).clickable { pickGoal = false; go.lens(g.id, m) }.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
