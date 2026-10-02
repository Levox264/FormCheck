package com.example.formcheck.stats

/** One finished set, as stored on disk. Pure Kotlin so it can be unit tested. */
data class SessionRecord(
    val timestampMs: Long,
    val exerciseId: String,
    val goodReps: Int,
    val badReps: Int,
    /** Coaching message -> number of reps in this set that triggered it. */
    val issueCounts: Map<String, Int>,
) {
    val totalReps: Int get() = goodReps + badReps
}
