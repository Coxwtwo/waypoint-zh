package app.waypoint.data

import app.waypoint.domain.World

/** Ticks a step for [day]: a repeating step gets a log, a one-off is finished. Tapping again undoes it. */
suspend fun WaypointDao.tick(n: Node, day: Long, now: Long = System.currentTimeMillis()) {
    if (n.repeats) {
        if (log(n.id, day) != null) deleteLog(n.id, day)
        else upsertLog(Log(n.id, n.goalId, day, 1.0, now))
    } else {
        val done = !n.done
        upsertNode(n.copy(done = done, doneAt = if (done) now else null, dueDay = n.dueDay ?: if (done) World.epochDay(now) else null))
    }
}

/** Adds [delta] to the day's count (4DX lead measures); a count of zero removes the log. */
suspend fun WaypointDao.count(n: Node, day: Long, delta: Int, now: Long = System.currentTimeMillis()) {
    val v = ((log(n.id, day)?.value ?: 0.0) + delta).coerceAtLeast(0.0)
    if (v == 0.0) deleteLog(n.id, day) else upsertLog(Log(n.id, n.goalId, day, v, now))
}

/** Records a number for the day (kaizen, a key-result check-in); null clears it. */
suspend fun WaypointDao.record(n: Node, day: Long, value: Double?, now: Long = System.currentTimeMillis()) {
    if (value == null) deleteLog(n.id, day) else upsertLog(Log(n.id, n.goalId, day, value, now))
}

/** Saves a free-text answer; a blank one is removed. */
suspend fun WaypointDao.setField(goalId: Long, key: String, value: String) {
    if (value.isBlank()) deleteField(goalId, key) else upsertField(Field(goalId, key, value.trim()))
}

/** Lights or dims a mandala cell (or chooses a GROW option). */
suspend fun WaypointDao.setDone(n: Node, done: Boolean, now: Long = System.currentTimeMillis()) {
    upsertNode(n.copy(done = done, doneAt = if (done) now else null))
}
