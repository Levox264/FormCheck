package com.example.formcheck.engine

/**
 * Phase 3 keeps the legacy two-threshold semantics so behaviour matches the old
 * RepCounter: a rep starts when the metric drops below [downThreshold] and
 * completes when it rises above [upThreshold]. Phase 4 replaces this with
 * leave-rest / valid-depth / back-to-rest hysteresis.
 */
data class CountingSpec(
    val metricId: String,
    val downThreshold: Float,
    val upThreshold: Float,
)

/** Legacy "bottom" = ACTIVE_EXTREME (min counting value); legacy "top" = REST. */
enum class Phase { ACTIVE_EXTREME, REST }

/**
 * One-sided ranges are allowed (null = unbounded). [messageLow] is shown when the
 * value is below [min] (minus tolerance), [messageHigh] when above [max].
 */
data class FormCheck(
    val metricId: String,
    val phase: Phase,
    val min: Float?,
    val max: Float?,
    val messageLow: String,
    val messageHigh: String,
) {
    init {
        require(min == null || max == null || min <= max) { "min > max for $metricId/$phase" }
    }
}

const val FORM_TOLERANCE_DEG = 5f
