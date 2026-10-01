package com.example.formcheck.engine

import java.util.concurrent.CopyOnWriteArrayList

data class RepResult(val repNumber: Int, val isGood: Boolean, val issues: List<String>)

/**
 * Extracted from CameraActivity.kt. Same state machine as before (UP/DOWN with the
 * two thresholds, same discard rules), but:
 *  - input is a metric map from [MetricEngine], not raw joint angles
 *  - no Android types (Log -> [RepLogger], RepJudgement -> [RepResult])
 *  - counters are @Volatile and results are a CopyOnWriteArrayList, because
 *    update() runs on MediaPipe's thread and the UI reads from the main thread
 *
 * Known legacy quirk kept on purpose for parity (fixed in phase 4): the REST
 * snapshot is taken on the frame that crosses upThreshold, not at the extremum.
 */
class RepCounter(
    private val counting: CountingSpec,
    private val checks: List<FormCheck>,
    private val tolerance: Float = FORM_TOLERANCE_DEG,
    private val logger: RepLogger = RepLogger.None,
) {
    private enum class State { UP, DOWN }

    private var state = State.UP
    // inside class RepCounter
    @Volatile
    var repCount = 0
        private set

    @Volatile
    var goodRepCount = 0
        private set

    @Volatile
    var badRepCount = 0
        private set

    @Volatile
    var skippedRepCount = 0
        private set

    @Volatile
    var lastRepGood: Boolean? = null
        private set

    @Volatile
    var lastRepIssues: List<String> = emptyList()
        private set

    private val results = CopyOnWriteArrayList<RepResult>()
    val judgements: List<RepResult> get() = results

    /** Fired when a rep finishes being judged. */
    var onRepJudged: ((repNumber: Int, good: Boolean, issues: List<String>) -> Unit)? = null

    /** Fired when a rep is discarded because something required wasn't visible. */
    var onRepSkipped: ((reason: String) -> Unit)? = null

    private var activeExtreme: Float? = null
    private var activeSnapshot: Map<String, Float>? = null
    private var countingMetricLostMidRep = false

    fun update(metrics: Map<String, Float>) {
        val value = metrics[counting.metricId]

        if (value == null) {
            // Counting metric not visible. Mid-rep, the rep can no longer be trusted,
            // but keep waiting - it may come back before the rep completes.
            if (state == State.DOWN) countingMetricLostMidRep = true
            return
        }

        when (state) {
            State.UP -> if (value < counting.downThreshold) {
                state = State.DOWN
                activeExtreme = value
                activeSnapshot = metrics
                countingMetricLostMidRep = false
            }

            State.DOWN -> {
                val extreme = activeExtreme
                if (extreme == null || value < extreme) {
                    activeExtreme = value
                    activeSnapshot = metrics
                }
                if (value > counting.upThreshold) {
                    state = State.UP
                    finishRep(restSnapshot = metrics)
                    activeExtreme = null
                    activeSnapshot = null
                    countingMetricLostMidRep = false
                }
            }
        }
    }

    private fun visibleFor(snapshot: Map<String, Float>, phase: Phase): Boolean =
        checks.filter { it.phase == phase }.all { snapshot.containsKey(it.metricId) }

    private fun finishRep(restSnapshot: Map<String, Float>) {
        val active = activeSnapshot

        val reason = when {
            countingMetricLostMidRep -> "${counting.metricId} not visible during rep"
            active == null -> "no bottom-of-rep data captured"
            !visibleFor(active, Phase.ACTIVE_EXTREME) -> "form joint not visible at bottom of rep"
            !visibleFor(restSnapshot, Phase.REST) -> "form joint not visible at top of rep"
            else -> null
        }

        if (reason != null || active == null) {
            skippedRepCount += 1
            logger.log("Rep discarded: $reason")
            onRepSkipped?.invoke(reason ?: "unknown")
            return
        }

        repCount += 1
        judgeRep(active, restSnapshot)
    }

    private fun judgeRep(active: Map<String, Float>, rest: Map<String, Float>) {
        val issues = mutableListOf<String>()

        for (check in checks) {
            val snapshot = if (check.phase == Phase.ACTIVE_EXTREME) active else rest
            val value = snapshot[check.metricId] ?: continue // guaranteed present by finishRep
            val lo = check.min?.minus(tolerance)
            val hi = check.max?.plus(tolerance)
            when {
                lo != null && value < lo -> issues += check.messageLow
                hi != null && value > hi -> issues += check.messageHigh
            }
            logger.log("  ${check.phase} ${check.metricId}: ${value.toInt()} (expected ${check.min}..${check.max})")
        }

        val distinct = issues.distinct()
        val good = distinct.isEmpty()
        if (good) goodRepCount += 1 else badRepCount += 1

        lastRepGood = good
        lastRepIssues = distinct
        logger.log("Rep $repCount: ${if (good) "GOOD" else "BAD"}")

        results.add(RepResult(repCount, good, distinct))
        onRepJudged?.invoke(repCount, good, distinct)
    }
}
