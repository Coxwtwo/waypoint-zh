package app.waypoint.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.waypoint.MainActivity
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Node
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The moments Waypoint comes by on its own: today's course in the morning, open steps in the evening,
 * the weekly review, and the reminders set on single steps and if-then plans.
 */
object Alarms {
    private const val CHANNEL = "course_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_MORNING = "app.waypoint.notify.MORNING"
    private const val ACTION_EVENING = "app.waypoint.notify.EVENING"
    private const val ACTION_REVIEW = "app.waypoint.notify.REVIEW"
    private const val ACTION_STEP = "app.waypoint.notify.STEP"

    private const val ID_MORNING = 1
    private const val ID_EVENING = 2
    private const val ID_REVIEW = 3

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_course), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_course_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Next [day] of the week (1 = Monday) at [minutes], strictly after now. */
    fun nextWeekday(day: Int, minutes: Int, now: LocalDateTime): LocalDateTime {
        var d = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(day))).atTime(minutes / 60, minutes % 60)
        if (!d.isAfter(now)) d = d.plusWeeks(1)
        return d
    }

    private suspend fun world(context: Context, day: LocalDate): World {
        val dao = context.container.dao
        return World(dao.goals(), dao.nodes(), dao.logs(), dao.entries(), dao.fields(), dao.reviews(), dao.wheel(), dao.ideas(), day)
    }

    /** Steps and plans with a reminder that should ring on [day]: open steps due that day, and every if-then plan. */
    private fun remindersOn(w: World, day: Long): List<Node> = w.active.flatMap { g ->
        w.nodesOf(g.id).filter { n ->
            n.remindAt >= 0 && when {
                n.kind == NodeKind.PLAN.name || n.kind == NodeKind.RISK.name -> true
                w.isDoable(n) -> w.dueOn(n, g, day) && (if (n.repeats) w.log(n.id, day) == null else !n.done)
                else -> false
            }
        }
    }

    /** The soonest step reminder after [now], with every node that rings at that minute. */
    fun nextStep(w: World, now: LocalDateTime): Pair<LocalDateTime, List<Node>>? {
        for (offset in 0..1) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            // Tomorrow's list can't know about tomorrow's ticks yet; it's re-checked when the alarm fires.
            val nodes = remindersOn(w, date.toEpochDay())
            val upcoming = nodes.map { it to date.atTime(it.remindAt / 60, it.remindAt % 60) }.filter { it.second.isAfter(now) }
            val first = upcoming.minByOrNull { it.second } ?: continue
            return first.second to upcoming.filter { it.second == first.second }.map { it.first }
        }
        return null
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        fun set(action: String, code: Int, at: LocalDateTime?) {
            val p = pi(context, action, code)
            // Reminders don't need to-the-second precision; this avoids the exact-alarm permission.
            if (at == null || !s.onboarded) am.cancel(p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), p)
        }
        set(ACTION_MORNING, 1, if (s.morningOn) nextAt(s.morningAt, now) else null)
        set(ACTION_EVENING, 2, if (s.eveningOn) nextAt(s.eveningAt, now) else null)
        set(ACTION_REVIEW, 3, if (s.reviewOn) nextWeekday(s.reviewDay, s.reviewAt, now) else null)
        set(ACTION_STEP, 4, nextStep(world(context, now.toLocalDate()), now)?.first)
    }

    suspend fun fire(context: Context, action: String) {
        val c = context.container
        val s = c.settings.now()
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val w = world(context, now.toLocalDate())
        val res = context.resources
        when (action) {
            ACTION_MORNING -> if (s.morningOn && w.todayItems.isNotEmpty()) {
                val items = w.todayItems
                val list = items.take(4).joinToString("\n") { "✦ " + it.node.title }
                notify(context, ID_MORNING, res.getQuantityString(R.plurals.notif_morning_title, items.size, items.size), list, "today")
            }
            ACTION_EVENING -> if (s.eveningOn) {
                val open = w.todayItems.filter { !it.done }
                if (open.isNotEmpty()) {
                    val list = open.take(4).joinToString("\n") { "✧ " + it.node.title }
                    notify(context, ID_EVENING, res.getQuantityString(R.plurals.notif_evening_title, open.size, open.size), list, "today")
                }
            }
            ACTION_REVIEW -> if (s.reviewOn && w.review(w.thisWeek) == null && w.active.isNotEmpty()) {
                notify(context, ID_REVIEW, context.getString(R.string.notif_review_title), context.getString(R.string.notif_review_text), "review")
            }
            ACTION_STEP -> {
                // Everything due this minute (or missed in the last ten, if the phone dozed).
                val due = remindersOn(w, w.day).filter {
                    val at = now.toLocalDate().atTime(it.remindAt / 60, it.remindAt % 60)
                    !at.isAfter(now) && at.isAfter(now.minusMinutes(10))
                }
                due.forEach { n ->
                    val goal = w.goal(n.goalId) ?: return@forEach
                    val plan = n.kind == NodeKind.PLAN.name || n.kind == NodeKind.RISK.name
                    val title = if (plan) context.getString(R.string.notif_plan_title, n.title) else n.title
                    val text = if (plan) context.getString(R.string.notif_plan_text, n.note) else goal.title
                    notify(context, (1000 + n.id % 100_000).toInt(), title, text, "goal/${goal.id}")
                }
            }
        }
    }

    private fun open(context: Context, code: Int, route: String) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, id: Int, title: String, text: String, route: String) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id, route))
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_MORNING, ACTION_EVENING, ACTION_REVIEW, ACTION_STEP)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Alarms.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context, action)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
