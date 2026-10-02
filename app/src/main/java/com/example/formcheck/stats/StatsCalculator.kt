package com.example.formcheck.stats

import java.util.TimeZone

enum class StatsRange(val label: String, val days: Int?) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    ALL("All", null),
}

data class ExerciseStat(
    val exerciseId: String,
    val name: String,
    val sessions: Int,
    val goodReps: Int,
    val badReps: Int,
) {
    val totalReps: Int get() = goodReps + badReps
}

data class IssueStat(val message: String, val repCount: Int)

data class DayBar(val label: String, val reps: Int, val isToday: Boolean)

data class StatsSummary(
    val sessions: Int,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    /** 0..1, or null when there are no reps in range. */
    val formScore: Float?,
    val exercises: List<ExerciseStat>,
    val topIssues: List<IssueStat>,
)

object StatsCalculator {
    private const val DAY_MS = 86_400_000L
    private const val DAY_LETTERS = "SMTWTFS" // index 0 = Sunday

    /** Days since 1970-01-01 in [zone]. Avoids java.time so it works on any minSdk. */
    fun dayIndex(timeMs: Long, zone: TimeZone): Long =
        Math.floorDiv(timeMs + zone.getOffset(timeMs), DAY_MS)

    fun summarize(
        sessions: List<SessionRecord>,
        range: StatsRange,
        nowMs: Long,
        zone: TimeZone,
        nameOf: (String) -> String,
        maxIssues: Int = 5,
    ): StatsSummary {
        val today = dayIndex(nowMs, zone)
        val days = range.days
        val inRange = if (days == null) sessions
        else sessions.filter { dayIndex(it.timestampMs, zone) > today - days }

        val good = inRange.sumOf { it.goodReps }
        val bad = inRange.sumOf { it.badReps }

        val exercises = inRange.groupBy { it.exerciseId }
            .map { (id, list) ->
                ExerciseStat(id, nameOf(id), list.size, list.sumOf { it.goodReps }, list.sumOf { it.badReps })
            }
            .sortedByDescending { it.totalReps }

        val issues = HashMap<String, Int>()
        for (s in inRange) for ((msg, n) in s.issueCounts) issues[msg] = (issues[msg] ?: 0) + n
        val topIssues = issues.entries
            .sortedByDescending { it.value }
            .take(maxIssues)
            .map { IssueStat(it.key, it.value) }

        return StatsSummary(
            sessions = inRange.size,
            totalReps = good + bad,
            goodReps = good,
            badReps = bad,
            formScore = if (good + bad == 0) null else good.toFloat() / (good + bad),
            exercises = exercises,
            topIssues = topIssues,
        )
    }

    /** Reps per day for the last 7 days, oldest first, today last. */
    fun lastSevenDays(sessions: List<SessionRecord>, nowMs: Long, zone: TimeZone): List<DayBar> {
        val today = dayIndex(nowMs, zone)
        val repsByDay = sessions.groupBy { dayIndex(it.timestampMs, zone) }
            .mapValues { (_, list) -> list.sumOf { it.totalReps } }
        return (6 downTo 0).map { offset ->
            val day = today - offset
            // 1970-01-01 was a Thursday, i.e. index 4 with Sunday = 0
            val dow = Math.floorMod(day + 4, 7L).toInt()
            DayBar(DAY_LETTERS[dow].toString(), repsByDay[day] ?: 0, isToday = offset == 0)
        }
    }
}
