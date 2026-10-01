package com.example.formcheck.engine

/** How a left/right metric pair collapses into one value. */
enum class SideMode {
    /** Legacy behaviour: mean of both sides, falling back to whichever is visible. */
    AVERAGE,
    LEFT,
    RIGHT,
    /** Per frame, the side whose three landmarks are most visible. */
    BEST_VISIBLE,
}

sealed interface Metric {
    /** Interior angle at the middle landmark of each triple (MediaPipe indices). */
    data class Angle(
        val left: Triple<Int, Int, Int>,
        val right: Triple<Int, Int, Int>,
    ) : Metric
    // Phase 4+: SegmentTilt, NormDistance, HeightDelta, ...
}

data class MetricDef(
    val id: String,
    val metric: Metric,
    val side: SideMode = SideMode.AVERAGE,
)

/**
 * Replaces the old `standardJoints` map. Ids are the base names the old
 * `formCriteria` already used ("elbow", "knee", ...), so exercise data ports 1:1.
 */
object MetricLibrary {
    val standard: Map<String, MetricDef> = listOf(
        MetricDef("elbow", Metric.Angle(Triple(11, 13, 15), Triple(12, 14, 16))),     // shoulder-elbow-wrist
        MetricDef("shoulder", Metric.Angle(Triple(13, 11, 23), Triple(14, 12, 24))),  // elbow-shoulder-hip
        MetricDef("hip", Metric.Angle(Triple(11, 23, 25), Triple(12, 24, 26))),       // shoulder-hip-knee
        MetricDef("knee", Metric.Angle(Triple(23, 25, 27), Triple(24, 26, 28))),      // hip-knee-ankle
        MetricDef("body_line", Metric.Angle(Triple(11, 23, 27), Triple(12, 24, 28))), // shoulder-hip-ankle
        MetricDef("wrist", Metric.Angle(Triple(13, 15, 19), Triple(14, 16, 20))),     // elbow-wrist-index
    ).associateBy { it.id }
}

/**
 * Turns one frame of landmarks into `metricId -> value`. A metric whose landmarks
 * are not visible (or degenerate) is simply ABSENT from the map - the RepCounter's
 * discard logic relies on that, exactly like the old getJointAngles.
 */
class MetricEngine(
    metricIds: Set<String>,
    library: Map<String, MetricDef> = MetricLibrary.standard,
    private val visibilityThreshold: Float = 0.5f,
) {
    private val defs: List<MetricDef> = metricIds.map { id ->
        library[id] ?: error("Unknown metric id '$id'. Known: ${library.keys}")
    }

    fun compute(landmarks: List<Landmark>): Map<String, Float> {
        val out = mutableMapOf<String, Float>()
        for (def in defs) {
            val value = when (val m = def.metric) {
                is Metric.Angle -> angleValue(landmarks, m, def.side)
            }
            if (value != null) out[def.id] = value
        }
        return out
    }

    private class Reading(val angle: Float, val visibility: Float)

    private fun read(lm: List<Landmark>, t: Triple<Int, Int, Int>): Reading? {
        val a = lm.getOrNull(t.first) ?: return null
        val b = lm.getOrNull(t.second) ?: return null
        val c = lm.getOrNull(t.third) ?: return null
        val vis = minOf(a.visibility, b.visibility, c.visibility)
        if (vis < visibilityThreshold) return null
        val angle = Geometry.interiorAngleDeg(a, b, c) ?: return null
        return Reading(angle, vis)
    }

    private fun angleValue(lm: List<Landmark>, m: Metric.Angle, side: SideMode): Float? {
        val l = read(lm, m.left)
        val r = read(lm, m.right)
        return when (side) {
            SideMode.LEFT -> l?.angle
            SideMode.RIGHT -> r?.angle
            SideMode.AVERAGE ->
                if (l != null && r != null) (l.angle + r.angle) / 2f else (l ?: r)?.angle
            SideMode.BEST_VISIBLE ->
                if (l == null) r?.angle
                else if (r == null) l.angle
                else if (l.visibility >= r.visibility) l.angle else r.angle
        }
    }
}
