package app.waypoint

import app.waypoint.domain.Method
import app.waypoint.ui.tools.Blocker
import app.waypoint.ui.tools.Factor
import app.waypoint.ui.tools.GoalKind
import app.waypoint.ui.tools.Horizon
import app.waypoint.ui.tools.Quiz
import app.waypoint.ui.tools.QuizAnswers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizTest {
    @Test
    fun bigLongDreamPicksMandala() {
        val r = Quiz.recommend(QuizAnswers(GoalKind.DREAM, measurable = false, horizon = Horizon.YEAR, blocker = Blocker.OVERWHELM, structured = true))
        assertEquals(Method.MANDALA, r.picks.first().method)
        assertEquals(Method.SMART, r.lens)
        assertFalse(r.toolsFirst)
    }

    @Test
    fun shortHabitPicksPact() {
        val r = Quiz.recommend(QuizAnswers(GoalKind.HABIT, measurable = false, horizon = Horizon.WEEKS, blocker = Blocker.GIVE_UP, structured = false))
        assertEquals(Method.PACT, r.picks.first().method)
        assertEquals(Method.WOOP, r.lens)
    }

    @Test
    fun measurableQuarterWithFocusTroublePicksNumbersMethods() {
        val r = Quiz.recommend(QuizAnswers(GoalKind.NUMBER, measurable = true, horizon = Horizon.QUARTER, blocker = Blocker.FOCUS, structured = true))
        val top = r.picks.map { it.method }.toSet()
        assertEquals(setOf(Method.OKR, Method.FOURDX), top)
        assertEquals(Method.WHYS, r.lens)
    }

    @Test
    fun deadlineProjectPicksReversePlan() {
        val r = Quiz.recommend(QuizAnswers(GoalKind.PROJECT, measurable = false, horizon = Horizon.YEAR, blocker = Blocker.START, structured = false))
        assertEquals(Method.LADDER, r.picks.first().method)
        assertEquals(Factor.KIND_PROJECT, r.picks.first().because)
    }

    @Test
    fun unsureRecommendsToolsFirst() {
        val r = Quiz.recommend(QuizAnswers(GoalKind.UNSURE, measurable = false, horizon = Horizon.QUARTER, blocker = Blocker.START, structured = false))
        assertTrue(r.toolsFirst)
        assertEquals(2, r.picks.size)
        assertTrue(r.picks.all { it.method.drives })
    }
}
