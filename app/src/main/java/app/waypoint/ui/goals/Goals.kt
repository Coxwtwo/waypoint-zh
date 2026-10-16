package app.waypoint.ui.goals

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData

@Composable
fun GoalsTab(data: WData, go: Go) { Text("Goals") }

@Composable
fun GoalScreen(id: Long, data: WData, go: Go) { Text("Goal $id") }

@Composable
fun GoalFormScreen(id: Long, method: Method?, title: String, area: Int, idea: Long, data: WData, go: Go) { Text("Form") }
