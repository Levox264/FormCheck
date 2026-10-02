package com.example.formcheck.engine

import com.example.formcheck.CameraView
import com.example.formcheck.ExerciseCategory
import com.example.formcheck.ExerciseDefinition
import com.example.formcheck.ExerciseRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    private fun lm(x: Float, y: Float, z: Float = 0f) = Landmark(x, y, z, 1f)

    @Test fun rightAngle() {
        assertEquals(90f, Geometry.interiorAngleDeg(lm(1f, 0f), lm(0f, 0f), lm(0f, 1f))!!, 0.01f)
    }

    @Test fun straightLine() {
        assertEquals(180f, Geometry.interiorAngleDeg(lm(-1f, 0f), lm(0f, 0f), lm(1f, 0f))!!, 0.01f)
    }

    @Test fun usesZ() {
        assertEquals(90f, Geometry.interiorAngleDeg(lm(1f, 0f, 0f), lm(0f, 0f, 0f), lm(0f, 0f, 1f))!!, 0.01f)
    }

    @Test fun degenerateReturnsNull() {
        assertNull(Geometry.interiorAngleDeg(lm(0f, 0f), lm(0f, 0f), lm(1f, 1f)))
    }
}

class ExerciseRepositoryTest {
    @Test fun idsAreUnique() {
        val ids = ExerciseRepository.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun allMetricIdsExist() {
        for (ex in ExerciseRepository.all) {
            for (id in ex.requiredMetricIds) {
                assertTrue("${ex.id}: unknown metric $id", id in MetricLibrary.standard)
            }
        }
    }

    @Test fun thresholdsAreOrdered() {
        for (ex in ExerciseRepository.all) {
            assertTrue("${ex.id}: down must be < up", ex.counting.downThreshold < ex.counting.upThreshold)
        }
    }

    @Test fun portedAllTwentyFive() {
        assertEquals(25, ExerciseRepository.all.size)
    }
}

class RepCounterTest {

    // ---- helpers ---------------------------------------------------------------

    /** Frame where the knee interior angle (both sides) equals [theta] degrees. */
    private fun kneeFrame(theta: Float): List<Landmark> {
        val blank = Landmark(0f, 0f, 0f, 1f)
        val lm = MutableList(33) { blank }
        val rad = Math.toRadians(theta.toDouble())
        val hip = Landmark(0f, -1f, 0f, 1f)
        val knee = Landmark(0f, 0f, 0f, 1f)
        val ankle = Landmark(Math.sin(rad).toFloat(), -Math.cos(rad).toFloat(), 0f, 1f)
        lm[23] = hip; lm[25] = knee; lm[27] = ankle
        lm[24] = hip; lm[26] = knee; lm[28] = ankle
        return lm
    }

    private fun testExercise(vararg checks: FormCheck) = ExerciseDefinition(
        "t", "Test", ExerciseCategory.SQUAT_LEGS, "", "", CameraView.SIDE,
        CountingSpec("knee", 80f, 170f), checks.toList(),
    )

    /** Linear down-and-back waveform, [reps] times, starting just above [up]. */
    private fun waveform(down: Float, up: Float, reps: Int): List<Float> {
        val out = mutableListOf<Float>()
        repeat(reps) {
            val hi = up + 5f; val lo = down - 5f
            for (i in 0..10) out += hi + (lo - hi) * i / 10f
            for (i in 1..10) out += lo + (hi - lo) * i / 10f
        }
        return out
    }

    // ---- engine + counter, end to end ---------------------------------------------

    @Test fun metricEngineMeasuresSyntheticKnee() {
        val engine = MetricEngine(setOf("knee"))
        assertEquals(123f, engine.compute(kneeFrame(123f))["knee"]!!, 0.01f)
    }

    @Test fun endToEndCountsThreeReps() {
        val ex = testExercise()
        val engine = MetricEngine(ex.requiredMetricIds)
        val counter = RepCounter(ex.counting, ex.checks)
        for (theta in waveform(80f, 170f, 3)) counter.update(engine.compute(kneeFrame(theta)))
        assertEquals(3, counter.repCount)
        assertEquals(0, counter.skippedRepCount)
    }

    @Test fun badRepGetsHumanMessage() {
        val ex = testExercise(
            FormCheck("knee", Phase.ACTIVE_EXTREME, 60f, 100f, "Too deep.", "Not deep enough."),
        )
        val counter = RepCounter(ex.counting, ex.checks)
        // bottoms out at 40 degrees -> below 60 - 5 tolerance
        for (v in waveform(down = 45f, up = 170f, reps = 1)) counter.update(mapOf("knee" to v))
        assertEquals(1, counter.repCount)
        assertEquals(1, counter.badRepCount)
        assertEquals(listOf("Too deep."), counter.judgements[0].issues)
    }

    @Test fun goodRepWithNoFailingChecks() {
        val counter = RepCounter(CountingSpec("knee", 80f, 170f), emptyList())
        for (v in waveform(80f, 170f, 2)) counter.update(mapOf("knee" to v))
        assertEquals(2, counter.goodRepCount)
        assertEquals(0, counter.badRepCount)
    }

    @Test fun lostCountingMetricMidRepDiscardsRep() {
        val counter = RepCounter(CountingSpec("knee", 80f, 170f), emptyList())
        var reason: String? = null
        counter.onRepSkipped = { reason = it }
        val wave = waveform(80f, 170f, 1)
        wave.forEachIndexed { i, v ->
            // frame 10 is the bottom (enters DOWN); drop frames 11..13 on the way back up
            if (i in 11..13) counter.update(emptyMap()) else counter.update(mapOf("knee" to v))
        }
        assertEquals(0, counter.repCount)
        assertEquals(1, counter.skippedRepCount)
        assertNotNull(reason)
    }

    // ---- parity with the legacy state machine ----------------------------------------

    /** Literal copy of the old state machine, counts only. */
    private class Legacy(val down: Float, val up: Float) {
        var state = "up"; var reps = 0; var skipped = 0; var lost = false
        fun update(angle: Float?) {
            if (angle == null) { if (state == "down") lost = true; return }
            when (state) {
                "up" -> if (angle < down) { state = "down"; lost = false }
                "down" -> if (angle > up) {
                    state = "up"
                    if (lost) skipped++ else reps++
                    lost = false
                }
            }
        }
    }

    @Test fun parityWithLegacyAcrossAllExercises() {
        for (ex in ExerciseRepository.all) {
            val wave = waveform(ex.counting.downThreshold, ex.counting.upThreshold, reps = 4)
            val legacy = Legacy(ex.counting.downThreshold, ex.counting.upThreshold)
            val counter = RepCounter(ex.counting, ex.checks)

            wave.forEachIndexed { i, v ->
                // dropout in the middle of the 2nd rep (frames 21..41 is rep 2)
                val visible = i !in 32..34
                legacy.update(if (visible) v else null)
                val metrics = if (visible) ex.requiredMetricIds.associateWith { 0f } + (ex.counting.metricId to v)
                else ex.requiredMetricIds.filter { it != ex.counting.metricId }.associateWith { 0f }
                counter.update(metrics)
            }
            assertEquals("${ex.id} reps", legacy.reps, counter.repCount)
            assertEquals("${ex.id} skipped", legacy.skipped, counter.skippedRepCount)
            assertEquals("${ex.id} dropout actually exercised", 1, legacy.skipped)
        }
    }

    @Test fun shortDropoutIsTolerated() {
        val counter = RepCounter(CountingSpec("knee", 80f, 170f), emptyList())
        waveform(80f, 170f, 1).forEachIndexed { i, v ->
            if (i in 11..13) counter.update(emptyMap()) else counter.update(mapOf("knee" to v))
        }
        assertEquals(1, counter.repCount)
        assertEquals(0, counter.skippedRepCount)
    }
}
