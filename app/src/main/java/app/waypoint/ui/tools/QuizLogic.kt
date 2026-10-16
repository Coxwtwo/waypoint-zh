package app.waypoint.ui.tools

import app.waypoint.domain.Method

/** Question 1: what the goal is like. */
enum class GoalKind { DREAM, NUMBER, HABIT, PROJECT, UNSURE }

enum class Horizon { WEEKS, QUARTER, YEAR }

/** Question 4: what usually gets in the way. */
enum class Blocker { FOCUS, DELAY, GIVE_UP, OVERWHELM, START }

data class QuizAnswers(
    val kind: GoalKind,
    val measurable: Boolean,
    val horizon: Horizon,
    val blocker: Blocker,
    /** Likes a lot of structure. */
    val structured: Boolean,
)

/** Which answer speaks for a method the most; shown as the reason it was picked. */
enum class Factor {
    KIND_DREAM, KIND_NUMBER, KIND_HABIT, KIND_PROJECT, KIND_UNSURE,
    MEASURABLE, NOT_MEASURABLE,
    WEEKS, QUARTER, YEAR,
    FOCUS, DELAY, GIVE_UP, OVERWHELM, START,
    STRUCTURED, LIGHT,
}

data class Pick(val method: Method, val score: Int, val because: Factor)

data class Recommendation(
    /** The two best drive methods, best first. */
    val picks: List<Pick>,
    val lens: Method,
    /** Recommend the goal-choosing tools first: the user isn't sure yet what they want. */
    val toolsFirst: Boolean,
)

object Quiz {
    private val drive = listOf(Method.MANDALA, Method.OKR, Method.WEEKS12, Method.FOURDX, Method.KAIZEN, Method.PACT, Method.LADDER, Method.SIMPLE)

    private fun weights(f: Factor): Map<Method, Int> = when (f) {
        Factor.KIND_DREAM -> mapOf(Method.MANDALA to 4, Method.LADDER to 1, Method.OKR to 1)
        Factor.KIND_NUMBER -> mapOf(Method.OKR to 3, Method.FOURDX to 3, Method.KAIZEN to 1, Method.SIMPLE to 1)
        Factor.KIND_HABIT -> mapOf(Method.PACT to 4, Method.KAIZEN to 3, Method.SIMPLE to 1)
        Factor.KIND_PROJECT -> mapOf(Method.LADDER to 4, Method.WEEKS12 to 3, Method.SIMPLE to 1)
        Factor.KIND_UNSURE -> mapOf(Method.SIMPLE to 1, Method.MANDALA to 1, Method.PACT to 1)
        Factor.MEASURABLE -> mapOf(Method.OKR to 2, Method.FOURDX to 2, Method.KAIZEN to 2)
        Factor.NOT_MEASURABLE -> mapOf(Method.MANDALA to 2, Method.PACT to 2, Method.LADDER to 1, Method.SIMPLE to 1, Method.OKR to -1, Method.FOURDX to -2, Method.KAIZEN to -2)
        Factor.WEEKS -> mapOf(Method.PACT to 3, Method.KAIZEN to 1, Method.SIMPLE to 1, Method.MANDALA to -2, Method.LADDER to -1)
        Factor.QUARTER -> mapOf(Method.WEEKS12 to 3, Method.OKR to 2, Method.FOURDX to 1)
        Factor.YEAR -> mapOf(Method.MANDALA to 3, Method.LADDER to 2, Method.WEEKS12 to -1, Method.PACT to -2)
        Factor.FOCUS -> mapOf(Method.FOURDX to 3, Method.WEEKS12 to 2)
        Factor.DELAY -> mapOf(Method.WEEKS12 to 3, Method.PACT to 1)
        Factor.GIVE_UP -> mapOf(Method.KAIZEN to 3, Method.PACT to 2)
        Factor.OVERWHELM -> mapOf(Method.MANDALA to 2, Method.LADDER to 2, Method.SIMPLE to 1)
        Factor.START -> mapOf(Method.LADDER to 3, Method.SIMPLE to 2, Method.PACT to 1)
        Factor.STRUCTURED -> mapOf(Method.OKR to 2, Method.WEEKS12 to 2, Method.MANDALA to 2, Method.FOURDX to 1)
        Factor.LIGHT -> mapOf(Method.SIMPLE to 2, Method.PACT to 2, Method.KAIZEN to 2, Method.OKR to -1)
    }

    fun factors(a: QuizAnswers): List<Factor> = listOf(
        when (a.kind) {
            GoalKind.DREAM -> Factor.KIND_DREAM
            GoalKind.NUMBER -> Factor.KIND_NUMBER
            GoalKind.HABIT -> Factor.KIND_HABIT
            GoalKind.PROJECT -> Factor.KIND_PROJECT
            GoalKind.UNSURE -> Factor.KIND_UNSURE
        },
        if (a.measurable) Factor.MEASURABLE else Factor.NOT_MEASURABLE,
        when (a.horizon) {
            Horizon.WEEKS -> Factor.WEEKS
            Horizon.QUARTER -> Factor.QUARTER
            Horizon.YEAR -> Factor.YEAR
        },
        when (a.blocker) {
            Blocker.FOCUS -> Factor.FOCUS
            Blocker.DELAY -> Factor.DELAY
            Blocker.GIVE_UP -> Factor.GIVE_UP
            Blocker.OVERWHELM -> Factor.OVERWHELM
            Blocker.START -> Factor.START
        },
        if (a.structured) Factor.STRUCTURED else Factor.LIGHT,
    )

    fun scores(a: QuizAnswers): Map<Method, Int> {
        val fs = factors(a)
        return drive.associateWith { m -> fs.sumOf { weights(it)[m] ?: 0 } }
    }

    fun recommend(a: QuizAnswers): Recommendation {
        val fs = factors(a)
        val s = scores(a)
        val picks = drive
            .sortedWith(compareByDescending<Method> { s[it] ?: 0 }.thenBy { drive.indexOf(it) })
            .take(2)
            .map { m -> Pick(m, s[m] ?: 0, fs.maxBy { weights(it)[m] ?: 0 }) }
        val lens = when {
            a.blocker == Blocker.GIVE_UP || a.blocker == Blocker.DELAY -> Method.WOOP
            a.kind == GoalKind.UNSURE || a.blocker == Blocker.START || a.blocker == Blocker.OVERWHELM -> Method.SMART
            a.horizon == Horizon.YEAR -> Method.PREMORTEM
            else -> Method.WHYS
        }
        return Recommendation(picks, lens, toolsFirst = a.kind == GoalKind.UNSURE)
    }
}
