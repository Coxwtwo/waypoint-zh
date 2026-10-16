package app.waypoint.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Scoreboard
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.waypoint.R
import app.waypoint.domain.Area
import app.waypoint.domain.Group
import app.waypoint.domain.Method
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

private class MethodTexts(@StringRes val name: Int, @StringRes val short: Int, @StringRes val tagline: Int, @StringRes val about: Int, @StringRes val how: Int, @StringRes val origin: Int)

private fun texts(m: Method): MethodTexts = when (m) {
    Method.MANDALA -> MethodTexts(R.string.m_mandala, R.string.m_mandala_short, R.string.m_mandala_tag, R.string.m_mandala_about, R.string.m_mandala_how, R.string.m_mandala_origin)
    Method.OKR -> MethodTexts(R.string.m_okr, R.string.m_okr_short, R.string.m_okr_tag, R.string.m_okr_about, R.string.m_okr_how, R.string.m_okr_origin)
    Method.WEEKS12 -> MethodTexts(R.string.m_weeks12, R.string.m_weeks12_short, R.string.m_weeks12_tag, R.string.m_weeks12_about, R.string.m_weeks12_how, R.string.m_weeks12_origin)
    Method.FOURDX -> MethodTexts(R.string.m_fourdx, R.string.m_fourdx_short, R.string.m_fourdx_tag, R.string.m_fourdx_about, R.string.m_fourdx_how, R.string.m_fourdx_origin)
    Method.KAIZEN -> MethodTexts(R.string.m_kaizen, R.string.m_kaizen_short, R.string.m_kaizen_tag, R.string.m_kaizen_about, R.string.m_kaizen_how, R.string.m_kaizen_origin)
    Method.PACT -> MethodTexts(R.string.m_pact, R.string.m_pact_short, R.string.m_pact_tag, R.string.m_pact_about, R.string.m_pact_how, R.string.m_pact_origin)
    Method.LADDER -> MethodTexts(R.string.m_ladder, R.string.m_ladder_short, R.string.m_ladder_tag, R.string.m_ladder_about, R.string.m_ladder_how, R.string.m_ladder_origin)
    Method.SIMPLE -> MethodTexts(R.string.m_simple, R.string.m_simple_short, R.string.m_simple_tag, R.string.m_simple_about, R.string.m_simple_how, R.string.m_simple_origin)
    Method.SMART -> MethodTexts(R.string.m_smart, R.string.m_smart_short, R.string.m_smart_tag, R.string.m_smart_about, R.string.m_smart_how, R.string.m_smart_origin)
    Method.WOOP -> MethodTexts(R.string.m_woop, R.string.m_woop_short, R.string.m_woop_tag, R.string.m_woop_about, R.string.m_woop_how, R.string.m_woop_origin)
    Method.GROW -> MethodTexts(R.string.m_grow, R.string.m_grow_short, R.string.m_grow_tag, R.string.m_grow_about, R.string.m_grow_how, R.string.m_grow_origin)
    Method.WHYS -> MethodTexts(R.string.m_whys, R.string.m_whys_short, R.string.m_whys_tag, R.string.m_whys_about, R.string.m_whys_how, R.string.m_whys_origin)
    Method.PREMORTEM -> MethodTexts(R.string.m_premortem, R.string.m_premortem_short, R.string.m_premortem_tag, R.string.m_premortem_about, R.string.m_premortem_how, R.string.m_premortem_origin)
    Method.WHEEL -> MethodTexts(R.string.m_wheel, R.string.m_wheel_short, R.string.m_wheel_tag, R.string.m_wheel_about, R.string.m_wheel_how, R.string.m_wheel_origin)
    Method.ODYSSEY -> MethodTexts(R.string.m_odyssey, R.string.m_odyssey_short, R.string.m_odyssey_tag, R.string.m_odyssey_about, R.string.m_odyssey_how, R.string.m_odyssey_origin)
    Method.BUFFETT -> MethodTexts(R.string.m_buffett, R.string.m_buffett_short, R.string.m_buffett_tag, R.string.m_buffett_about, R.string.m_buffett_how, R.string.m_buffett_origin)
    Method.DREAMLINE -> MethodTexts(R.string.m_dreamline, R.string.m_dreamline_short, R.string.m_dreamline_tag, R.string.m_dreamline_about, R.string.m_dreamline_how, R.string.m_dreamline_origin)
}

@Composable fun methodName(m: Method): String = stringResource(texts(m).name)
@Composable fun methodShort(m: Method): String = stringResource(texts(m).short)
@Composable fun methodTagline(m: Method): String = stringResource(texts(m).tagline)
@Composable fun methodAbout(m: Method): String = stringResource(texts(m).about)
/** Steps of the method, one per line. */
@Composable fun methodHow(m: Method): List<String> = stringResource(texts(m).how).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
@Composable fun methodOrigin(m: Method): String = stringResource(texts(m).origin)

fun methodIcon(m: Method): ImageVector = when (m) {
    Method.MANDALA -> Icons.Rounded.GridView
    Method.OKR -> Icons.Rounded.TrackChanges
    Method.WEEKS12 -> Icons.Rounded.CalendarViewWeek
    Method.FOURDX -> Icons.Rounded.Scoreboard
    Method.KAIZEN -> Icons.AutoMirrored.Rounded.TrendingUp
    Method.PACT -> Icons.Rounded.Science
    Method.LADDER -> Icons.Rounded.Stairs
    Method.SIMPLE -> Icons.Rounded.Flag
    Method.SMART -> Icons.Rounded.CenterFocusStrong
    Method.WOOP -> Icons.Rounded.Lightbulb
    Method.GROW -> Icons.Rounded.Spa
    Method.WHYS -> Icons.Rounded.Psychology
    Method.PREMORTEM -> Icons.Rounded.Shield
    Method.WHEEL -> Icons.Rounded.DonutLarge
    Method.ODYSSEY -> Icons.Rounded.Explore
    Method.BUFFETT -> Icons.AutoMirrored.Rounded.FormatListBulleted
    Method.DREAMLINE -> Icons.Rounded.AutoAwesome
}

@Composable
fun groupName(g: Group): String = stringResource(
    when (g) {
        Group.CHOOSE -> R.string.group_choose
        Group.SHAPE -> R.string.group_shape
        Group.DRIVE -> R.string.group_drive
        Group.PROTECT -> R.string.group_protect
    }
)

@Composable
fun groupText(g: Group): String = stringResource(
    when (g) {
        Group.CHOOSE -> R.string.group_choose_text
        Group.SHAPE -> R.string.group_shape_text
        Group.DRIVE -> R.string.group_drive_text
        Group.PROTECT -> R.string.group_protect_text
    }
)

@Composable
fun areaName(a: Area): String = stringResource(
    when (a) {
        Area.HEALTH -> R.string.area_health
        Area.CAREER -> R.string.area_career
        Area.MONEY -> R.string.area_money
        Area.LOVE -> R.string.area_love
        Area.FAMILY -> R.string.area_family
        Area.GROWTH -> R.string.area_growth
        Area.FUN -> R.string.area_fun
        Area.HOME -> R.string.area_home
    }
)

fun areaIcon(a: Area): ImageVector = when (a) {
    Area.HEALTH -> Icons.Rounded.MonitorHeart
    Area.CAREER -> Icons.Rounded.Work
    Area.MONEY -> Icons.Rounded.Savings
    Area.LOVE -> Icons.Rounded.Favorite
    Area.FAMILY -> Icons.Rounded.Groups
    Area.GROWTH -> Icons.Rounded.School
    Area.FUN -> Icons.Rounded.Celebration
    Area.HOME -> Icons.Rounded.Home
}

@Composable
fun greeting(hour: Int): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.greet_morning
        in 12..17 -> R.string.greet_day
        in 18..22 -> R.string.greet_evening
        else -> R.string.greet_night
    }
)

@Composable
fun clockTime(minutes: Int): String =
    LocalTime.of((minutes / 60) % 24, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()))

/** "16 Oct" / «16 окт.»; adds the year when it isn't this year. */
@Composable
fun dateShort(day: Long): String {
    val d = LocalDate.ofEpochDay(day)
    val pattern = if (d.year == LocalDate.now().year) "d MMM" else "d MMM yyyy"
    return d.format(DateTimeFormatter.ofPattern(pattern, locale()))
}

@Composable
fun dateLong(day: LocalDate): String = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale()))

@Composable
fun dateOf(at: Long): String = dateShort(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay())

/** "Friday, 16 October". */
@Composable
fun todayTitle(day: LocalDate): String {
    val l = locale()
    val s = day.format(DateTimeFormatter.ofPattern(if (l.language == "ru") "EEEE, d MMMM" else "EEEE, d MMMM", l))
    return s.replaceFirstChar { it.titlecase(l) }
}

/** Monday = 0. */
@Composable
fun weekdayShort(i: Int): String =
    java.time.DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT_STANDALONE, locale()).replaceFirstChar { it.titlecase(locale()) }

/** "Every day", "Weekdays", "Mon, Wed, Fri" or "3× a week". */
@Composable
fun scheduleLabel(days: Int, perWeek: Int): String = when {
    days == 0b1111111 -> stringResource(R.string.sched_every_day)
    days == 0b0011111 -> stringResource(R.string.sched_weekdays)
    days == 0b1100000 -> stringResource(R.string.sched_weekends)
    days != 0 -> (0 until 7).filter { days and (1 shl it) != 0 }.map { weekdayShort(it) }.joinToString(", ")
    perWeek > 0 -> stringResource(R.string.sched_per_week, perWeek)
    else -> stringResource(R.string.sched_once)
}

/** "12 days left", "Due today", "3 days overdue". */
@Composable
fun daysLeftLabel(left: Long): String = when {
    left > 0 -> androidx.compose.ui.res.pluralStringResource(R.plurals.days_left, left.toInt().coerceAtMost(9999), left.toInt())
    left == 0L -> stringResource(R.string.due_today)
    else -> androidx.compose.ui.res.pluralStringResource(R.plurals.days_overdue, (-left).toInt().coerceAtMost(9999), (-left).toInt())
}

object Haptics {
    fun tick(context: Context) = vibrate(context, longArrayOf(0, 18), intArrayOf(0, 90))
    fun star(context: Context) = vibrate(context, longArrayOf(0, 20, 70, 40), intArrayOf(0, 110, 0, 200))

    private fun vibrate(context: Context, timings: LongArray, amps: IntArray) {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        if (v?.hasVibrator() != true) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, amps, -1)) }
    }
}
