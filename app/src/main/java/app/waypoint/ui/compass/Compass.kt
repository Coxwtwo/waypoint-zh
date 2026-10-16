package app.waypoint.ui.compass

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.waypoint.domain.Method
import app.waypoint.ui.Go
import app.waypoint.ui.WData

@Composable
fun CompassTab(data: WData, go: Go) { Text("Compass") }

@Composable
fun MethodScreen(m: Method, data: WData, go: Go) { Text(m.name) }
