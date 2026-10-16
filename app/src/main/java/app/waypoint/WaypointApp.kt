package app.waypoint

import android.app.Application
import android.content.Context
import app.waypoint.data.DemoSeed
import app.waypoint.data.SettingsStore
import app.waypoint.data.WaypointDao
import app.waypoint.data.WaypointDatabase
import app.waypoint.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = WaypointDatabase.create(context)
    val dao: WaypointDao get() = db.dao()
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend WaypointDao.() -> Unit) {
        scope.launch {
            dao.block()
            changed()
        }
    }

    /** Re-arm reminders and redraw the home-screen widget. */
    suspend fun changed() {
        Alarms.reschedule(context)
        app.waypoint.widget.CourseWidget.refresh(context)
    }
}

class WaypointApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container.dao, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as WaypointApp).container
