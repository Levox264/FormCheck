package com.example.formcheck

import com.example.formcheck.engine.CountingSpec
import com.example.formcheck.engine.FormCheck
import com.example.formcheck.engine.Phase

enum class ExerciseCategory(val displayName: String) {
    SQUAT_LEGS("Squat & Legs"),
    HIP_HINGE("Hip Hinge & Posterior Chain"),
    GLUTES_UNILATERAL("Glutes & Unilateral Legs"),
    PUSH("Push"),
    PULL("Pull"),
    SHOULDERS_ARMS("Shoulders & Arms"),
    CORE("Core"),
    MOBILITY("Mobility & Corrective"),
}

enum class CameraView { SIDE, FRONT, BACK_OR_FRONT }

data class ExerciseDefinition(
    val id: String,
    val displayName: String,
    val category: ExerciseCategory,
    val cameraAngleDescription: String,
    val description: String,
    val view: CameraView,
    val counting: CountingSpec,
    val checks: List<FormCheck>,
) {
    /** Metric ids the MetricEngine must compute every frame for this exercise. */
    val requiredMetricIds: Set<String>
        get() = setOf(counting.metricId) + checks.map { it.metricId }
}

// ---------------------------------------------------------------------------------
// Builders. The numbers below are copied 1:1 from the old repository so behaviour
// is unchanged. Legacy "bottom" criteria -> active(), legacy "top" -> rest().
// ---------------------------------------------------------------------------------

private const val SIDE_HINT = "Place your phone to see you from the side."

private fun rest(metric: String, min: Float?, max: Float?, low: String, high: String = low) =
    FormCheck(metric, Phase.REST, min, max, low, high)

private fun active(metric: String, min: Float?, max: Float?, low: String, high: String = low) =
    FormCheck(metric, Phase.ACTIVE_EXTREME, min, max, low, high)

private fun ex(
    id: String,
    name: String,
    category: ExerciseCategory,
    description: String,
    countingMetric: String,
    down: Float,
    up: Float,
    checks: List<FormCheck>,
    view: CameraView = CameraView.SIDE,
    cameraHint: String = SIDE_HINT,
) = ExerciseDefinition(
    id, name, category, cameraHint, description, view,
    CountingSpec(countingMetric, down, up), checks,
)

// Draft coaching messages. Review the wording - these are my guesses at the biomechanics.
private object Msg {
    const val BODY_LINE = "Your hips are sagging or piking. Keep a straight line from shoulders to ankles."
    const val STAND_TALL = "Stand all the way up and finish with your hips extended."
    const val SQUAT_FOLDING = "Your torso is folding too far forward. Keep your chest up."
    const val SQUAT_DEEPER = "Sit back and down a little more."
    const val LUNGE_UPRIGHT = "Stay tall at the top of each rep."
    const val LUNGE_LEAN = "Keep your torso more upright."
    const val LUNGE_DEEPER = "Sink a little deeper into the lunge."
    const val KNEE_SOFT = "You are bending your knees too much. Keep only a soft bend and hinge at the hips."
    const val KNEE_STRAIGHT = "Your knees are locked too straight. Unlock them slightly."
    const val LEGS_STRAIGHT = "Keep your legs nearly straight."
    const val FEET_CLOSE = "Your knees are bent too much. Move your feet a little further out."
    const val FEET_FAR = "Your knees are too straight. Bring your feet closer to your hips."
    const val ELBOWS_TUCKED = "Your elbows are tucked too tightly. Let them flare out slightly."
    const val ELBOWS_FLARED = "Your elbows are flaring too wide. Tuck them in a little."
    const val ELBOWS_PINNED = "Keep your elbows pinned to your sides."
}

object ExerciseRepository {

    private fun squat(id: String, name: String, desc: String, down: Float, bottomMin: Float, bottomMax: Float) = ex(
        id, name, ExerciseCategory.SQUAT_LEGS, desc, "knee", down, 170f,
        listOf(
            rest("hip", 160f, 180f, Msg.STAND_TALL),
            active("hip", bottomMin, bottomMax, Msg.SQUAT_FOLDING, Msg.SQUAT_DEEPER),
        ),
    )

    private fun lunge(id: String, name: String, desc: String, hint: String = SIDE_HINT) = ex(
        id, name, ExerciseCategory.GLUTES_UNILATERAL, desc, "knee", 90f, 165f,
        listOf(
            rest("hip", 150f, 180f, Msg.LUNGE_UPRIGHT),
            active("hip", 70f, 100f, Msg.LUNGE_LEAN, Msg.LUNGE_DEEPER),
        ),
        cameraHint = hint,
    )

    private fun bridge(id: String, name: String, desc: String) = ex(
        id, name, ExerciseCategory.HIP_HINGE, desc, "hip", 110f, 170f,
        listOf(
            rest("knee", 80f, 100f, Msg.FEET_CLOSE, Msg.FEET_FAR),
            active("knee", 80f, 100f, Msg.FEET_CLOSE, Msg.FEET_FAR),
        ),
    )

    private fun benchPress(id: String, name: String, desc: String) = ex(
        id, name, ExerciseCategory.PUSH, desc, "elbow", 100f, 165f,
        listOf(
            rest("shoulder", 60f, 100f, Msg.ELBOWS_TUCKED, Msg.ELBOWS_FLARED),
            active("shoulder", 60f, 100f, Msg.ELBOWS_TUCKED, Msg.ELBOWS_FLARED),
        ),
    )

    private fun curl(id: String, name: String, desc: String) = ex(
        id, name, ExerciseCategory.SHOULDERS_ARMS, desc, "elbow", 30f, 160f,
        listOf(
            rest("shoulder", 0f, 30f, Msg.ELBOWS_PINNED),
            active("shoulder", 0f, 30f, Msg.ELBOWS_PINNED),
        ),
    )

    val all: List<ExerciseDefinition> = listOf(
        ex(
            "pushup", "Push-up", ExerciseCategory.PUSH, "Standard push-up with full range of motion.",
            "elbow", 100f, 160f,
            listOf(
                rest("body_line", 170f, 180f, Msg.BODY_LINE),
                active("body_line", 170f, 180f, Msg.BODY_LINE),
            ),
        ),
        ex(
            "pullup", "Pull-up", ExerciseCategory.PULL, "Standard pull-up with full range of motion.",
            "elbow", 55f, 155f,
            listOf(
                rest("body_line", 160f, 180f, "Avoid swinging. Keep your body straight."),
                rest("shoulder", 120f, 180f, "Start each rep from a full hang with your arms fully overhead."),
                active("body_line", 160f, 180f, "Avoid swinging. Keep your body straight."),
                active("shoulder", 0f, 30f, "Pull your elbows down toward your sides at the top."),
            ),
            view = CameraView.BACK_OR_FRONT,
            cameraHint = "Place your phone to see you from the back or the front.",
        ),
        squat("squat", "Squat", "Standard squat with full range of motion.", 80f, 60f, 70f),
        ex(
            "dip", "Dip", ExerciseCategory.PUSH, "Standard dip with full range of motion.",
            "elbow", 100f, 160f,
            listOf(
                rest("hip", 160f, 180f, "Keep your legs and torso in line instead of folding at the hips."),
                rest("shoulder", 0f, 30f, "Keep your shoulders down and back at the top."),
                active("hip", 140f, 180f, "Avoid folding at the hips as you lower."),
                active("shoulder", 70f, 110f, "Lower yourself a little further.", "Do not drop too deep. Keep your shoulders above your elbows."),
            ),
        ),

        // ---------------- Squat & Legs ----------------
        squat("back_squat", "Back Squat", "Barbell back squat, bar on the upper back.", 80f, 60f, 90f),
        squat("goblet_squat", "Goblet Squat", "Squat holding a dumbbell or kettlebell at chest height.", 80f, 60f, 90f),
        squat("front_squat", "Front Squat", "Barbell front squat, bar racked across the front shoulders.", 75f, 55f, 85f),
        squat("sumo_squat", "Sumo Squat", "Wide-stance squat, toes turned out.", 80f, 60f, 90f),

        // ---------------- Glutes & Unilateral Legs ----------------
        lunge(
            "bulgarian_split_squat", "Bulgarian Split Squat", "Rear-foot-elevated single-leg squat.",
            hint = "Place your phone to see your front (working) leg from the side.",
        ),
        lunge("walking_lunge", "Walking Lunge", "Alternating forward lunges, stepping through each rep."),
        lunge("reverse_lunge", "Reverse Lunge", "Step backward into a lunge, front knee tracks over the foot."),

        // ---------------- Hip Hinge & Posterior Chain ----------------
        ex(
            "romanian_deadlift", "Romanian Deadlift", ExerciseCategory.HIP_HINGE,
            "Hip-hinge movement with a near-straight leg, bar close to the body.",
            "hip", 100f, 165f,
            listOf(
                rest("knee", 160f, 180f, Msg.KNEE_SOFT),
                active("knee", 150f, 175f, Msg.KNEE_SOFT, Msg.KNEE_STRAIGHT),
            ),
        ),
        ex(
            "stiff_leg_deadlift", "Stiff-Leg Deadlift", ExerciseCategory.HIP_HINGE,
            "Hip hinge with legs kept nearly fully extended throughout.",
            "hip", 100f, 165f,
            listOf(
                rest("knee", 165f, 180f, Msg.LEGS_STRAIGHT),
                active("knee", 160f, 180f, Msg.LEGS_STRAIGHT),
            ),
        ),
        ex(
            "good_morning", "Good Morning", ExerciseCategory.HIP_HINGE,
            "Barbell on the back, hinge forward at the hips with a soft knee bend.",
            "hip", 110f, 165f,
            listOf(
                rest("knee", 150f, 175f, Msg.KNEE_SOFT, Msg.KNEE_STRAIGHT),
                active("knee", 150f, 175f, Msg.KNEE_SOFT, Msg.KNEE_STRAIGHT),
            ),
        ),
        bridge("hip_thrust", "Hip Thrust", "Shoulders elevated on a bench, drive the hips up to full extension."),
        bridge("glute_bridge", "Glute Bridge", "Bodyweight bridge, shoulders on the floor, drive the hips up."),

        // ---------------- Push ----------------
        benchPress("bench_press", "Bench Press", "Barbell bench press, bar to chest, press to just short of lockout."),
        benchPress("incline_db_press", "Incline DB Press", "Dumbbell press on an incline bench."),

        // ---------------- Pull ----------------
        ex(
            "lat_pulldown", "Lat Pulldown", ExerciseCategory.PULL,
            "Cable pulldown to the upper chest, elbows driving down and back.",
            "elbow", 40f, 150f,
            listOf(
                active("shoulder", 0f, 40f, "Pull your elbows down and back toward your sides."),
                rest("shoulder", 120f, 180f, "Let the bar rise all the way up for a full stretch."),
            ),
            view = CameraView.FRONT,
            cameraHint = "Place your phone to see you from the front.",
        ),
        ex(
            "seated_cable_row", "Seated Cable Row", ExerciseCategory.PULL,
            "Seated cable row, torso upright, pull to the torso.",
            "elbow", 60f, 150f,
            listOf(
                rest("hip", 80f, 100f, "You are leaning forward. Sit tall.", "You are leaning back. Keep your torso upright."),
                active("hip", 80f, 100f, "You are leaning forward. Sit tall.", "You are leaning back. Keep your torso upright."),
            ),
        ),
        ex(
            "barbell_row", "Barbell Row", ExerciseCategory.PULL,
            "Bent-over barbell row, hinge held throughout the set.",
            "elbow", 60f, 150f,
            listOf(
                rest("hip", 30f, 60f, "You are bent over too far. Lift your chest slightly.", "You are too upright. Hinge forward more."),
                active("hip", 30f, 60f, "You are bent over too far. Lift your chest slightly.", "You are too upright. Hinge forward more."),
            ),
        ),

        // ---------------- Shoulders & Arms ----------------
        ex(
            "overhead_press", "Overhead Press", ExerciseCategory.SHOULDERS_ARMS,
            "Standing barbell or dumbbell press overhead.",
            "elbow", 100f, 160f,
            listOf(
                rest("shoulder", 160f, 180f, "Press all the way overhead."),
                active("shoulder", 60f, 100f, "Keep your elbows a little further out at the bottom.", "Lower the weight to shoulder height."),
            ),
        ),
        ex(
            "triceps_pushdown", "Triceps Pushdown", ExerciseCategory.SHOULDERS_ARMS,
            "Cable pushdown, elbows pinned to the sides throughout.",
            "elbow", 100f, 165f,
            listOf(
                rest("shoulder", 0f, 30f, Msg.ELBOWS_PINNED),
                active("shoulder", 0f, 30f, Msg.ELBOWS_PINNED),
            ),
        ),
        curl("barbell_curl", "Barbell Curl", "Standing barbell curl, elbows pinned to the sides."),
        curl("hammer_curl", "Hammer Curl", "Neutral-grip dumbbell curl, elbows pinned to the sides."),
    )

    fun byId(id: String) = all.first { it.id == id }
}
