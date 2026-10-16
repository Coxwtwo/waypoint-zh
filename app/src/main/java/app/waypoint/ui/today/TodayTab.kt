package app.waypoint.ui.today

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.waypoint.ui.Go
import app.waypoint.ui.Tab
import app.waypoint.ui.WData

@Composable
fun TodayTab(data: WData, go: Go, onTab: (Tab) -> Unit) { Text("Today") }
