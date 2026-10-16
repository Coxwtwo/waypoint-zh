package app.waypoint.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot): String = JSONObject()
        .put("app", "waypoint")
        .put("version", VERSION)
        .put("goals", JSONArray(s.goals.map {
            JSONObject().put("id", it.id).put("title", it.title).put("method", it.method).put("status", it.status).put("why", it.why)
                .put("area", it.area).put("startDay", it.startDay).put("endDay", it.endDay ?: JSONObject.NULL).put("unit", it.unit)
                .put("startValue", it.startValue).put("targetValue", it.targetValue).put("currentValue", it.currentValue)
                .put("seed", it.seed).put("createdAt", it.createdAt).put("closedAt", it.closedAt ?: JSONObject.NULL)
                .put("note", it.note).put("sort", it.sort)
        }))
        .put("nodes", JSONArray(s.nodes.map {
            JSONObject().put("id", it.id).put("goalId", it.goalId).put("parentId", it.parentId).put("kind", it.kind).put("slot", it.slot)
                .put("title", it.title).put("note", it.note).put("days", it.days).put("perWeek", it.perWeek).put("week", it.week)
                .put("dueDay", it.dueDay ?: JSONObject.NULL).put("start", it.start).put("target", it.target).put("current", it.current)
                .put("unit", it.unit).put("done", it.done).put("doneAt", it.doneAt ?: JSONObject.NULL).put("remindAt", it.remindAt)
        }))
        .put("logs", JSONArray(s.logs.map {
            JSONObject().put("nodeId", it.nodeId).put("goalId", it.goalId).put("day", it.day).put("value", it.value).put("at", it.at)
        }))
        .put("fields", JSONArray(s.fields.map { JSONObject().put("goalId", it.goalId).put("key", it.key).put("value", it.value) }))
        .put("reviews", JSONArray(s.reviews.map {
            JSONObject().put("day", it.day).put("went", it.went).put("blocked", it.blocked).put("change", it.change).put("mood", it.mood).put("at", it.at)
        }))
        .put("wheel", JSONArray(s.wheel.map { JSONObject().put("day", it.day).put("scores", it.scores).put("at", it.at) }))
        .put("ideas", JSONArray(s.ideas.map {
            JSONObject().put("id", it.id).put("tool", it.tool).put("slot", it.slot).put("title", it.title).put("text", it.text)
                .put("a", it.a).put("b", it.b).put("c", it.c).put("d", it.d).put("amount", it.amount).put("monthly", it.monthly)
                .put("chosen", it.chosen).put("goalId", it.goalId).put("sort", it.sort).put("at", it.at)
        }))
        .toString(1)

    /** Throws [IllegalArgumentException] when the text is not a Waypoint backup. */
    fun decode(text: String): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "waypoint") { "Not a Waypoint backup" }
        return Snapshot(
            goals = root.list("goals") {
                Goal(
                    id = getLong("id"), title = getString("title"), method = getString("method"), status = optInt("status"), why = optString("why"),
                    area = optInt("area", -1), startDay = getLong("startDay"), endDay = longOrNull("endDay"), unit = optString("unit"),
                    startValue = optDouble("startValue", 0.0), targetValue = optDouble("targetValue", 0.0), currentValue = optDouble("currentValue", 0.0),
                    seed = optInt("seed"), createdAt = getLong("createdAt"), closedAt = longOrNull("closedAt"), note = optString("note"), sort = optInt("sort"),
                )
            },
            nodes = root.list("nodes") {
                Node(
                    id = getLong("id"), goalId = getLong("goalId"), parentId = optLong("parentId"), kind = getString("kind"), slot = optInt("slot"),
                    title = optString("title"), note = optString("note"), days = optInt("days"), perWeek = optInt("perWeek"), week = optInt("week"),
                    dueDay = longOrNull("dueDay"), start = optDouble("start", 0.0), target = optDouble("target", 0.0), current = optDouble("current", 0.0),
                    unit = optString("unit"), done = optBoolean("done"), doneAt = longOrNull("doneAt"), remindAt = optInt("remindAt", -1),
                )
            },
            logs = root.list("logs") { Log(getLong("nodeId"), getLong("goalId"), getLong("day"), optDouble("value", 1.0), optLong("at")) },
            fields = root.list("fields") { Field(getLong("goalId"), getString("key"), optString("value")) },
            reviews = root.list("reviews") { Review(getLong("day"), optString("went"), optString("blocked"), optString("change"), optInt("mood"), optLong("at")) },
            wheel = root.list("wheel") { WheelCheck(getLong("day"), getString("scores"), optLong("at")) },
            ideas = root.list("ideas") {
                Idea(
                    id = getLong("id"), tool = getString("tool"), slot = optInt("slot"), title = optString("title"), text = optString("text"),
                    a = optInt("a"), b = optInt("b"), c = optInt("c"), d = optInt("d"), amount = optDouble("amount", 0.0), monthly = optDouble("monthly", 0.0),
                    chosen = optBoolean("chosen"), goalId = optLong("goalId"), sort = optInt("sort"), at = optLong("at"),
                )
            },
        )
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
