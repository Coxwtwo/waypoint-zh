package app.waypoint.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.waypoint.MainActivity
import app.waypoint.R
import app.waypoint.container
import app.waypoint.domain.World
import app.waypoint.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's course on the home screen: a row of stars, lit for finished steps, and the next open ones. */
class CourseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CourseWidget::class.java))
            if (ids.isEmpty()) return
            val dao = context.container.dao
            val w = World(dao.goals(), dao.nodes(), dao.logs(), dao.entries(), dao.fields(), dao.reviews(), dao.wheel(), dao.ideas(), LocalDate.now())
            val items = w.todayItems
            val done = items.count { it.done }
            val open = items.filter { !it.done }
            val views = RemoteViews(context.packageName, R.layout.widget_course)
            views.setTextViewText(R.id.label, context.getString(R.string.widget_name))
            views.setTextViewText(R.id.count, if (items.isEmpty()) "" else "$done/${items.size}")
            views.setTextViewText(R.id.stars, items.sortedByDescending { it.done }.take(12).joinToString(" ") { if (it.done) "✦" else "·" })
            views.setTextViewText(
                R.id.text,
                when {
                    w.active.isEmpty() -> context.getString(R.string.widget_no_goals)
                    items.isEmpty() -> context.getString(R.string.today_free)
                    open.isEmpty() -> context.getString(R.string.today_sky_all)
                    else -> open.take(3).joinToString("\n") { "✧  " + it.node.title }
                },
            )
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Alarms.EXTRA_ROUTE, "today"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
