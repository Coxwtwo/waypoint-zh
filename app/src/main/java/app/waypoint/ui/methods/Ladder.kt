package app.waypoint.ui.methods

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.waypoint.data.Goal
import app.waypoint.ui.Go
import app.waypoint.ui.WData

/** The body of a goal driven by this method, shown under the goal header. */
@Composable
fun LadderBody(goal: Goal, data: WData, go: Go) {
    Text("Ladder")
}
