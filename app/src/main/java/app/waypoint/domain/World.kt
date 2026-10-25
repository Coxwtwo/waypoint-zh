package app.waypoint.domain

import app.waypoint.data.Entry
import app.waypoint.data.Field
import app.waypoint.data.Goal
import app.waypoint.data.Idea
import app.waypoint.data.Log
import app.waypoint.data.Node
import app.waypoint.data.Review
import app.waypoint.data.WheelCheck
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.pow

/** A step to take today, from any goal. */
class TodayItem(
    val node: Node,
    val goal: Goal,
    /** Ticked today (repeating) or finished today (one-off). */
    val done: Boolean,
    /** Today's logged value: 1 for a tick, a count for lead measures, a number for kaizen. */
    val value: Double,
    /** For weekly targets: how many this week so far, and the target. */
    val weekCount: Int,
    val weekTarget: Int,
)

/** How far a goal has come, the way its method measures it. */
class Progress(
    val fraction: Float,
    val done: Int,
    val total: Int,
)

/**
 * Everything stored, indexed for the screens, and the rules every method shares:
 * what is due today, how weeks are counted, how far each goal has come.
 */
class World(
    val goals: List<Goal>,
    val nodes: List<Node>,
    val logs: List<Log>,
    val entries: List<Entry>,
    val fields: List<Field>,
    val reviews: List<Review>,
    val wheel: List<WheelCheck>,
    val ideas: List<Idea>,
    val today: LocalDate,
) {
    val day: Long = today.toEpochDay()

    private val goalById = goals.associateBy { it.id }
    private val nodeById = nodes.associateBy { it.id }
    private val nodesByGoal = nodes.groupBy { it.goalId }
    private val childrenOf = nodes.groupBy { it.parentId }
    private val logsByNode = logs.groupBy { it.nodeId }
    private val logIndex = logs.associateBy { it.nodeId to it.day }
    private val entriesByNode = entries.groupBy { it.nodeId }
    private val fieldIndex = fields.associate { (it.goalId to it.key) to it.value }

    val active: List<Goal> = goals.filter { it.status == Goal.ACTIVE }
    val queued: List<Goal> = goals.filter { it.status == Goal.QUEUED }
    val closed: List<Goal> = goals.filter { it.status == Goal.DONE || it.status == Goal.DROPPED }.sortedByDescending { it.closedAt ?: 0 }
    val reached: List<Goal> = closed.filter { it.status == Goal.DONE }

    fun goal(id: Long): Goal? = goalById[id]
    fun node(id: Long): Node? = nodeById[id]
    fun nodesOf(goalId: Long): List<Node> = nodesByGoal[goalId].orEmpty()
    fun nodesOf(goalId: Long, kind: NodeKind): List<Node> = nodesOf(goalId).filter { it.kind == kind.name }
    fun children(nodeId: Long): List<Node> = childrenOf[nodeId].orEmpty()
    fun logsOf(nodeId: Long): List<Log> = logsByNode[nodeId].orEmpty()
    fun log(nodeId: Long, day: Long): Log? = logIndex[nodeId to day]
    fun field(goalId: Long, key: String): String = fieldIndex[goalId to key].orEmpty()
    fun hasField(goalId: Long, prefix: String): Boolean = fields.any { it.goalId == goalId && it.key.startsWith(prefix) && it.value.isNotBlank() }
    fun ideas(tool: Method): List<Idea> = ideas.filter { it.tool == tool.name }

    /** Lenses the goal already uses: SMART, WOOP… */
    fun lensesOf(goalId: Long): List<Method> = Method.lenses.filter { usesLens(goalId, it) }

    fun usesLens(goalId: Long, lens: Method): Boolean = when (lens) {
        Method.WOOP -> hasField(goalId, "woop.") || nodesOf(goalId, NodeKind.PLAN).isNotEmpty()
        Method.PREMORTEM -> nodesOf(goalId, NodeKind.RISK).isNotEmpty()
        Method.GROW -> hasField(goalId, "grow.") || nodesOf(goalId, NodeKind.OPTION).isNotEmpty()
        else -> hasField(goalId, lens.name.lowercase() + ".")
    }

    /** Every if-then plan of active goals (WOOP plans, and pre-mortem risks with a prevention), the "backup course" on Today. */
    val plans: List<Pair<Node, Goal>> = active.flatMap { g ->
        nodesOf(g.id).filter { (it.kind == NodeKind.PLAN.name || it.kind == NodeKind.RISK.name) && it.title.isNotBlank() && it.note.isNotBlank() }.map { it to g }
    }

    // ---- Weeks ----

    /** Monday of the week [day] falls in. */
    fun weekStart(day: Long): Long = weekStartOf(day)

    val thisWeek: Long = weekStart(day)

    /** Sum of a node's logged values in the week starting [weekStart]. */
    fun weekCount(nodeId: Long, weekStart: Long): Int =
        logsOf(nodeId).filter { it.day in weekStart until weekStart + 7 }.sumOf { it.value }.toInt()

    // ---- Today ----

    /** True when a repeating node is planned on [day] (weekday mask; or any day for a weekly count). */
    fun planned(n: Node, day: Long): Boolean {
        if (n.days != 0) return n.days and (1 shl (LocalDate.ofEpochDay(day).dayOfWeek.value - 1)) != 0
        return n.perWeek > 0
    }

    /** Kinds that are steps to do, as opposed to structure (pillars, key results) or notes (plans, risks). */
    private val doable = setOf(NodeKind.ACTION.name, NodeKind.TACTIC.name, NodeKind.LEAD.name, NodeKind.STEP.name, NodeKind.MILESTONE.name, NodeKind.TASK.name)

    fun isDoable(n: Node): Boolean = n.kind in doable

    /** Whether [n] belongs on the Today list for [day]. */
    fun dueOn(n: Node, goal: Goal, day: Long): Boolean {
        if (goal.status != Goal.ACTIVE || !isDoable(n) || goal.startDay > day) return false
        if (n.title.isBlank()) return false
        if (n.kind == NodeKind.TACTIC.name && n.week != 0 && n.week != week12(goal, day)) return false
        if (n.kind == NodeKind.TASK.name) {
            if (n.startDay != null && n.startDay > day) return false
            if (n.dueDay != null && n.dueDay < day) return false
            if (n.days != 0) return planned(n, day)
            return true
        }
        if (n.repeats) {
            if (log(n.id, day) != null) return true
            if (n.days != 0) return planned(n, day)
            return weekCount(n.id, weekStart(day)) < n.perWeek
        }
        // One-offs: due once their day comes, and they stay on the list the day they're finished.
        if (n.done) return n.doneAt?.let { epochDay(it) == day } ?: false
        return n.dueDay != null && n.dueDay <= day
    }

    val todayItems: List<TodayItem> = active.flatMap { g ->
        nodesOf(g.id).filter { dueOn(it, g, day) }.map { n ->
            val l = log(n.id, day)
            TodayItem(
                node = n,
                goal = g,
                done = if (n.repeats) l != null && (n.kind != NodeKind.LEAD.name || l.value >= 1) else n.done,
                value = l?.value ?: 0.0,
                weekCount = if (n.perWeek > 0) weekCount(n.id, thisWeek) else 0,
                weekTarget = n.perWeek,
            )
        }.sortedWith(compareBy({ it.done }, { if (it.node.remindAt < 0) Int.MAX_VALUE else it.node.remindAt }))
    }

    val doneToday: Int = todayItems.count { it.done }

    /** Consecutive planned days a repeating node was ticked, ending today (or yesterday if today is still open). */
    fun streak(n: Node): Int {
        if (!n.repeats) return 0
        if (n.days == 0) return weekStreak(n)
        var d = if (log(n.id, day) != null) day else day - 1
        var count = 0
        var guard = 0
        while (guard++ < 400) {
            if (planned(n, d)) {
                if (log(n.id, d) == null) break
                count++
            }
            d--
        }
        return count
    }

    /** Weeks in a row a weekly target was met, counting this week only once it is met. */
    private fun weekStreak(n: Node): Int {
        var w = thisWeek
        if (weekCount(n.id, w) < n.perWeek) w -= 7
        var count = 0
        while (count < 200 && weekCount(n.id, w) >= n.perWeek) { count++; w -= 7 }
        return count
    }

    /** Share of planned days ticked over the last [days] days, 0..1, for a habit's glow. */
    fun consistency(n: Node, days: Int = 14): Float {
        if (!n.repeats) return if (n.done) 1f else 0f
        var planned = 0
        var hit = 0
        for (d in day - days + 1..day) {
            if (n.days != 0 && !planned(n, d)) continue
            if (n.days == 0 && d % 7 != day % 7) continue
            planned++
            if (n.days != 0) { if (log(n.id, d) != null) hit++ } else if (weekCount(n.id, weekStart(d)) >= n.perWeek) hit++
        }
        return if (planned == 0) 0f else hit.toFloat() / planned
    }

    // ---- Method rules ----

    /** 12-week year: the week [day] falls in, 1..12, or 13 for the review week after. */
    fun week12(goal: Goal, day: Long = this.day): Int = (((day - goal.startDay) / 7) + 1).toInt().coerceIn(1, 13)

    /** 12-week year: share of the week's planned tactic occurrences that were done, 0..1, or null if nothing was planned. */
    fun weekScore(goal: Goal, week: Int): Float? {
        val ws = goal.startDay + (week - 1) * 7L
        val upTo = minOf(ws + 6, day)
        if (upTo < ws) return null
        var planned = 0
        var done = 0
        nodesOf(goal.id, NodeKind.TACTIC).filter { it.title.isNotBlank() && (it.week == 0 || it.week == week) }.forEach { n ->
            when {
                n.days != 0 -> {
                    // A whole week is planned up front, so the score reads honestly on Monday too.
                    for (d in ws..ws + 6) if (planned(n, d)) {
                        planned++
                        if (log(n.id, d) != null) done++
                    }
                }
                n.perWeek > 0 -> {
                    planned += n.perWeek
                    done += minOf(n.perWeek, weekCount(n.id, ws))
                }
                else -> {
                    planned++
                    if (n.done) done++
                }
            }
        }
        return if (planned == 0) null else done.toFloat() / planned
    }

    /** Kaizen: today's target, the base grown by the daily rate (1 % by default) for every day since the start. */
    fun kaizenTarget(goal: Goal, day: Long = this.day): Double {
        val rate = field(goal.id, "kaizen.rate").toDoubleOrNull() ?: 1.0
        val n = (day - goal.startDay).coerceAtLeast(0)
        return goal.startValue * (1 + rate / 100).pow(n.toDouble())
    }

    fun kaizenTargetRounded(goal: Goal, day: Long = this.day): Int = ceil(kaizenTarget(goal, day) - 1e-9).toInt()

    /** Progress tracker: the task's current value — its start plus one increment per logged entry. */
    fun trackerCurrent(n: Node): Double = n.start + n.increment * entriesByNode[n.id].orEmpty().size

    /** Fraction from [start] to [target] that [current] has covered, 0..1. Works for goals that go down, too. */
    fun metricFraction(start: Double, target: Double, current: Double): Float {
        if (target == start) return if (current == target) 1f else 0f
        return ((current - start) / (target - start)).toFloat().coerceIn(0f, 1f)
    }

    fun progress(goal: Goal): Progress {
        val p = rawProgress(goal)
        return if (goal.status == Goal.DONE) Progress(1f, maxOf(p.done, p.total), p.total) else p
    }

    private fun rawProgress(goal: Goal): Progress {
        val id = goal.id
        return when (Method.of(goal.method)) {
            Method.MANDALA -> {
                val pillars = nodesOf(id, NodeKind.PILLAR).map { it.id }.toSet()
                val lit = nodesOf(id, NodeKind.ACTION).count { it.parentId in pillars && it.done }
                Progress(lit / 64f, lit, 64)
            }
            Method.OKR -> {
                val krs = nodesOf(id, NodeKind.KR).filter { it.title.isNotBlank() }
                if (krs.isEmpty()) Progress(0f, 0, 0)
                else {
                    val f = krs.map { metricFraction(it.start, it.target, it.current) }
                    Progress(f.average().toFloat(), f.count { it >= 1f }, krs.size)
                }
            }
            Method.WEEKS12 -> {
                val week = week12(goal)
                val scores = (1..minOf(week, 12)).mapNotNull { weekScore(goal, it) }
                Progress(if (scores.isEmpty()) 0f else scores.average().toFloat(), minOf(week, 12), 12)
            }
            Method.FOURDX -> Progress(metricFraction(goal.startValue, goal.targetValue, goal.currentValue), 0, 0)
            Method.KAIZEN -> {
                val total = goal.endDay?.let { (it - goal.startDay + 1).toInt() } ?: 0
                val practised = nodesOf(id, NodeKind.ACTION).sumOf { n -> logsOf(n.id).count { it.value > 0 } }
                if (goal.hasMetric) Progress(metricFraction(goal.startValue, goal.targetValue, goal.currentValue), practised, total)
                else Progress(if (total > 0) practised.toFloat() / total else 0f, practised, total)
            }
            Method.PACT -> {
                val total = goal.endDay?.let { (it - goal.startDay + 1).toInt() } ?: 30
                val checked = nodesOf(id, NodeKind.ACTION).firstOrNull()?.let { n -> logsOf(n.id).count { it.day in goal.startDay..goal.startDay + total - 1 } } ?: 0
                Progress((checked.toFloat() / total).coerceIn(0f, 1f), checked, total)
            }
            Method.LADDER -> {
                val steps = nodesOf(id, NodeKind.STEP).filter { it.title.isNotBlank() }
                Progress(if (steps.isEmpty()) 0f else steps.count { it.done }.toFloat() / steps.size, steps.count { it.done }, steps.size)
            }
            Method.TRACKER -> {
                val tasks = nodesOf(id, NodeKind.TASK).filter { it.title.isNotBlank() }
                if (tasks.isEmpty()) Progress(0f, 0, 0)
                else {
                    val f = tasks.map { metricFraction(it.start, it.target, trackerCurrent(it)) }
                    Progress(f.average().toFloat(), f.count { it >= 1f }, tasks.size)
                }
            }
            else -> {
                if (goal.hasMetric) Progress(metricFraction(goal.startValue, goal.targetValue, goal.currentValue), 0, 0)
                else {
                    val ms = nodesOf(id, NodeKind.MILESTONE).filter { it.title.isNotBlank() }
                    Progress(if (ms.isEmpty()) 0f else ms.count { it.done }.toFloat() / ms.size, ms.count { it.done }, ms.size)
                }
            }
        }
    }

    /** Days left to the deadline (negative when overdue), or null without one. */
    fun daysLeft(goal: Goal): Long? = goal.endDay?.let { it - day }

    /** Active goals count against the focus limit; 0 means no limit. */
    fun overLimit(limit: Int): Boolean = limit > 0 && active.size >= limit

    // ---- Reviews ----

    /** The review of the week that is ending (this week), if written. */
    fun review(weekStart: Long): Review? = reviews.firstOrNull { it.day == weekStart }

    val lastWheel: WheelCheck? = wheel.maxByOrNull { it.day }

    companion object {

        /** Monday of the week [day] falls in. */
        fun weekStartOf(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

        fun epochDay(millis: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long =
            java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

        /** Weekday mask with every day set. */
        const val EVERY_DAY = 0b1111111
        const val WEEKDAYS = 0b0011111
    }
}
