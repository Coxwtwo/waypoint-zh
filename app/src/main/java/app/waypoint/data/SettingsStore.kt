package app.waypoint.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** The night sky is the app's home; daylight is an option. */
    val theme: ThemeMode = ThemeMode.DARK,
    val onboarded: Boolean = false,
    /** Used in greetings; empty is fine. */
    val name: String = "",
    /** At most this many goals are active at once; 0 = no limit. New goals beyond it wait in the queue. */
    val focusLimit: Int = 3,
    /** Today's course in the morning. */
    val morningOn: Boolean = true,
    /** Minutes after midnight. */
    val morningAt: Int = 8 * 60 + 30,
    /** A nudge in the evening when today still has unchecked steps. */
    val eveningOn: Boolean = true,
    val eveningAt: Int = 20 * 60 + 30,
    /** The weekly review. */
    val reviewOn: Boolean = true,
    /** 1 = Monday … 7 = Sunday. */
    val reviewDay: Int = 7,
    val reviewAt: Int = 19 * 60,
    val haptics: Boolean = true,
    /** Ids of moments already celebrated (a goal reached, a pillar lit), so each is shown once. */
    val seen: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.DARK,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        focusLimit = this[FOCUS] ?: 3,
        morningOn = this[MORNING_ON] ?: true,
        morningAt = this[MORNING_AT] ?: (8 * 60 + 30),
        eveningOn = this[EVENING_ON] ?: true,
        eveningAt = this[EVENING_AT] ?: (20 * 60 + 30),
        reviewOn = this[REVIEW_ON] ?: true,
        reviewDay = this[REVIEW_DAY] ?: 7,
        reviewAt = this[REVIEW_AT] ?: (19 * 60),
        haptics = this[HAPTICS] ?: true,
        seen = this[SEEN] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[FOCUS] = s.focusLimit
            it[MORNING_ON] = s.morningOn
            it[MORNING_AT] = s.morningAt
            it[EVENING_ON] = s.eveningOn
            it[EVENING_AT] = s.eveningAt
            it[REVIEW_ON] = s.reviewOn
            it[REVIEW_DAY] = s.reviewDay
            it[REVIEW_AT] = s.reviewAt
            it[HAPTICS] = s.haptics
            it[SEEN] = s.seen
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val FOCUS = intPreferencesKey("focus_limit")
        val MORNING_ON = booleanPreferencesKey("morning_on")
        val MORNING_AT = intPreferencesKey("morning_at")
        val EVENING_ON = booleanPreferencesKey("evening_on")
        val EVENING_AT = intPreferencesKey("evening_at")
        val REVIEW_ON = booleanPreferencesKey("review_on")
        val REVIEW_DAY = intPreferencesKey("review_day")
        val REVIEW_AT = intPreferencesKey("review_at")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SEEN = stringSetPreferencesKey("seen")
    }
}
