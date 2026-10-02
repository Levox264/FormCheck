package com.example.formcheck.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class StatsCalculatorTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val day = 86_400_000L

    // Thursday 1 Oct 2026, 12:00 UTC
    private val now = 1_790_856_000_000L

    private fun rec(daysAgo: Int, ex: String, good: Int, bad: Int, issues: Map<String, Int> = emptyMap()) =
        SessionRecord(now - daysAgo * day, ex, good, bad, issues)

    private fun summarize(sessions: List<SessionRecord>, range: StatsRange) =
        StatsCalculator.summarize(sessions, range, now, utc, nameOf = { it })

    @Test fun emptyHistoryHasNoFormScore() {
        val s = summarize(emptyList(), StatsRange.ALL)
        assertEquals(0, s.totalReps)
        assertNull(s.formScore)
    }

    @Test fun formScoreIsGoodOverTotal() {
        val s = summarize(listOf(rec(0, "squat", 6, 2)), StatsRange.ALL)
        assertEquals(0.75f, s.formScore!!, 0.001f)
        assertEquals(8, s.totalReps)
    }

    @Test fun rangeFiltersByDay() {
        val sessions = listOf(rec(0, "squat", 5, 0), rec(6, "squat", 5, 0), rec(7, "squat", 5, 0), rec(40, "squat", 5, 0))
        assertEquals(2, summarize(sessions, StatsRange.WEEK).sessions)   // today and 6 days ago
        assertEquals(3, summarize(sessions, StatsRange.MONTH).sessions)  // adds 7 days ago
        assertEquals(4, summarize(sessions, StatsRange.ALL).sessions)
    }

    @Test fun exercisesAreGroupedAndSortedByReps() {
        val sessions = listOf(rec(0, "pushup", 3, 0), rec(1, "squat", 5, 1), rec(2, "squat", 4, 0))
        val ex = summarize(sessions, StatsRange.ALL).exercises
        assertEquals(listOf("squat", "pushup"), ex.map { it.exerciseId })
        assertEquals(2, ex[0].sessions)
        assertEquals(10, ex[0].totalReps)
    }

    @Test fun issuesAreSummedAndRanked() {
        val sessions = listOf(
            rec(0, "squat", 1, 2, mapOf("Keep your chest up." to 2)),
            rec(1, "squat", 1, 3, mapOf("Keep your chest up." to 1, "Go deeper." to 3)),
        )
        val issues = summarize(sessions, StatsRange.ALL).topIssues
        assertEquals(listOf("Go deeper.", "Keep your chest up."), issues.map { it.message })
        assertEquals(3, issues[1].repCount)
    }

    @Test fun lastSevenDaysEndsToday() {
        val bars = StatsCalculator.lastSevenDays(listOf(rec(0, "squat", 4, 1), rec(2, "squat", 3, 0)), now, utc)
        assertEquals(7, bars.size)
        assertEquals("T", bars.last().label)      // Thursday
        assertEquals(true, bars.last().isToday)
        assertEquals(5, bars.last().reps)
        assertEquals(3, bars[4].reps)             // two days ago
        assertEquals(0, bars[0].reps)
    }
}
