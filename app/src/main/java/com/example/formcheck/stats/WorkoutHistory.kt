package com.example.formcheck.stats

import android.content.Context
import com.example.formcheck.WorkoutResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores finished sets in filesDir/workout_history.json. Uses org.json (built into
 * Android), so no Gradle changes. Fine for thousands of sets; if history ever gets
 * huge or needs querying, swap this object for Room without touching the UI.
 *
 * Call from a background thread (the StatsRoute does this for load()).
 */
object WorkoutHistory {
    private const val FILE_NAME = "workout_history.json"
    private val lock = Any()

    /** Saves a finished set. Empty sets (0 counted reps) are ignored. */
    fun append(context: Context, result: WorkoutResult, nowMs: Long = System.currentTimeMillis()) {
        if (result.goodReps + result.badReps == 0) return

        // ASSUMPTION: RepJudgement exposes its coaching messages as `issues`.
        // If your property is named differently, this is the only line to change.
        val issueCounts = result.judgements
            .flatMap { it.issues }
            .groupingBy { it }
            .eachCount()

        val record = SessionRecord(nowMs, result.exerciseId, result.goodReps, result.badReps, issueCounts)
        synchronized(lock) {
            writeAll(context, readAll(context) + record)
        }
    }

    fun load(context: Context): List<SessionRecord> = synchronized(lock) { readAll(context) }

    fun clear(context: Context) = synchronized(lock) { file(context).delete(); Unit }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun readAll(context: Context): List<SessionRecord> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val issues = o.optJSONObject("issues")
                SessionRecord(
                    timestampMs = o.getLong("t"),
                    exerciseId = o.getString("ex"),
                    goodReps = o.getInt("good"),
                    badReps = o.getInt("bad"),
                    issueCounts = issues?.keys()?.asSequence()?.associateWith { issues.getInt(it) } ?: emptyMap(),
                )
            }
        } catch (e: Exception) {
            emptyList() // corrupt file: start fresh rather than crash
        }
    }

    private fun writeAll(context: Context, records: List<SessionRecord>) {
        val arr = JSONArray()
        for (r in records) {
            arr.put(
                JSONObject()
                    .put("t", r.timestampMs)
                    .put("ex", r.exerciseId)
                    .put("good", r.goodReps)
                    .put("bad", r.badReps)
                    .put("issues", JSONObject(r.issueCounts as Map<*, *>)),
            )
        }
        // Write to a temp file then rename, so a crash mid-write cannot corrupt history.
        val target = file(context)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}
