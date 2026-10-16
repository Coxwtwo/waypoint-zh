package app.waypoint.ui.compass

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.waypoint.R
import app.waypoint.domain.Method
import app.waypoint.ui.components.UCard
import app.waypoint.ui.methodAbout
import app.waypoint.ui.methodHow
import app.waypoint.ui.methodIcon
import app.waypoint.ui.methodName
import app.waypoint.ui.methodOrigin
import app.waypoint.ui.theme.U
import app.waypoint.ui.theme.UType

/** The method's steps, numbered: they are a real sequence. */
@Composable
fun MethodSteps(m: Method) {
    val hue = U.c.hue(m)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        methodHow(m).forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).border(1.dp, hue.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = UType.Small.copy(color = hue)) }
                Spacer(Modifier.width(12.dp))
                Text(step, style = UType.Body, modifier = Modifier.weight(1f).padding(top = 1.dp))
            }
        }
    }
}

/**
 * "How this method works": what it is, its steps and where it comes from, folded to one line when [open] is false.
 * Shown where a method is chosen and on the goal it drives, so nobody has to guess what to do.
 */
@Composable
fun MethodGuide(m: Method, open: Boolean = false, modifier: Modifier = Modifier, showName: Boolean = false) {
    val hue = U.c.hue(m)
    var expanded by rememberSaveable(m) { mutableStateOf(open) }
    UCard(modifier.animateContentSize(), onClick = { expanded = !expanded }, border = hue.copy(alpha = 0.3f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(methodIcon(m), null, Modifier.size(18.dp), tint = hue)
            Spacer(Modifier.width(10.dp))
            Text(
                if (showName) methodName(m) else stringResource(R.string.guide_title),
                style = UType.Strong.copy(color = hue), modifier = Modifier.weight(1f),
            )
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = U.c.faint)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(methodAbout(m), style = UType.Body)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.guide_steps).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(10.dp))
            MethodSteps(m)
            Spacer(Modifier.height(14.dp))
            Text(methodOrigin(m), style = UType.Muted.copy(fontStyle = FontStyle.Italic))
        }
    }
}
