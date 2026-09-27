package com.example.formcheck

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

data class ExerciseDefinition(
    val id: String,
    val displayName: String,
    val category: ExerciseCategory,
    val cameraAngleDescription: String,
    val description: String,
    val joints: Map<String, Triple<Int, Int, Int>>,
    val formCriteria: Map<String, Map<String, Pair<Float, Float>>>,
    val downThreshold: Float,
    val upThreshold: Float,
    val repCountingJoint: String,     // e.g. "elbow", "knee"
)

// Standard landmark set reused across every side-view, angle-based exercise.
// left/right elbow, shoulder, hip, knee, body_line, wrist — covers everything
// needed for squat/hinge/push/pull/curl-family movements without new triples.
private val standardJoints: Map<String, Triple<Int, Int, Int>> = mapOf(
    "left_elbow" to Triple(11, 13, 15),      // shoulder-elbow-wrist
    "right_elbow" to Triple(12, 14, 16),
    "left_shoulder" to Triple(13, 11, 23),   // elbow-shoulder-hip
    "right_shoulder" to Triple(14, 12, 24),
    "left_hip" to Triple(11, 23, 25),        // shoulder-hip-knee
    "right_hip" to Triple(12, 24, 26),
    "left_knee" to Triple(23, 25, 27),       // hip-knee-ankle
    "right_knee" to Triple(24, 26, 28),
    "left_body_line" to Triple(11, 23, 27),  // shoulder-hip-ankle
    "right_body_line" to Triple(12, 24, 28),
    "left_wrist" to Triple(13, 15, 19),      // elbow-wrist-index
    "right_wrist" to Triple(14, 16, 20),
)

object ExerciseRepository {
    val all: List<ExerciseDefinition> = listOf(
        ExerciseDefinition(
            id = "pushup",
            displayName = "Push-up",
            category = ExerciseCategory.PUSH,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Standard push-up with full range of motion.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("body_line" to (170f to 180f)),
                "bottom" to mapOf("body_line" to (170f to 180f)),
            ),
            downThreshold = 100f,
            upThreshold = 160f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "pullup",
            displayName = "Pull-up",
            category = ExerciseCategory.PULL,
            cameraAngleDescription = "Place your phone to see you from the back or the front.",
            description = "Standard pull-up with full range of motion.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("body_line" to (160f to 180f), "shoulder" to (120f to 180f)),
                "bottom" to mapOf("body_line" to (160f to 180f), "shoulder" to (0f to 30f)),
            ),
            downThreshold = 55f,
            upThreshold = 155f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "squat",
            displayName = "Squat",
            category = ExerciseCategory.SQUAT_LEGS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Standard squat with full range of motion.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f)),
                "bottom" to mapOf("hip" to (60f to 70f)),
            ),
            downThreshold = 80f,
            upThreshold = 170f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "dip",
            displayName = "Dip",
            category = ExerciseCategory.PUSH,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Standard dip with full range of motion.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f), "shoulder" to (0f to 30f)),
                "bottom" to mapOf("hip" to (140f to 180f), "shoulder" to (70f to 110f)),
            ),
            downThreshold = 100f,
            upThreshold = 160f,
            repCountingJoint = "elbow",
        ),

        // ---------------- Squat & Legs ----------------
        ExerciseDefinition(
            id = "back_squat",
            displayName = "Back Squat",
            category = ExerciseCategory.SQUAT_LEGS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Barbell back squat, bar on the upper back.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f)),
                "bottom" to mapOf("hip" to (60f to 90f)),
            ),
            downThreshold = 80f,
            upThreshold = 170f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "goblet_squat",
            displayName = "Goblet Squat",
            category = ExerciseCategory.SQUAT_LEGS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Squat holding a dumbbell or kettlebell at chest height.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f)),
                "bottom" to mapOf("hip" to (60f to 90f)),
            ),
            downThreshold = 80f,
            upThreshold = 170f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "front_squat",
            displayName = "Front Squat",
            category = ExerciseCategory.SQUAT_LEGS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Barbell front squat, bar racked across the front shoulders.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f)),
                "bottom" to mapOf("hip" to (55f to 85f)),
            ),
            downThreshold = 75f,
            upThreshold = 170f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "sumo_squat",
            displayName = "Sumo Squat",
            category = ExerciseCategory.SQUAT_LEGS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Wide-stance squat, toes turned out.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (160f to 180f)),
                "bottom" to mapOf("hip" to (60f to 90f)),
            ),
            downThreshold = 80f,
            upThreshold = 170f,
            repCountingJoint = "knee",
        ),

        // ---------------- Glutes & Unilateral Legs ----------------
        ExerciseDefinition(
            id = "bulgarian_split_squat",
            displayName = "Bulgarian Split Squat",
            category = ExerciseCategory.GLUTES_UNILATERAL,
            cameraAngleDescription = "Place your phone to see your front (working) leg from the side.",
            description = "Rear-foot-elevated single-leg squat.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (150f to 180f)),
                "bottom" to mapOf("hip" to (70f to 100f)),
            ),
            downThreshold = 90f,
            upThreshold = 165f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "walking_lunge",
            displayName = "Walking Lunge",
            category = ExerciseCategory.GLUTES_UNILATERAL,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Alternating forward lunges, stepping through each rep.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (150f to 180f)),
                "bottom" to mapOf("hip" to (70f to 100f)),
            ),
            downThreshold = 90f,
            upThreshold = 165f,
            repCountingJoint = "knee",
        ),
        ExerciseDefinition(
            id = "reverse_lunge",
            displayName = "Reverse Lunge",
            category = ExerciseCategory.GLUTES_UNILATERAL,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Step backward into a lunge, front knee tracks over the foot.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (150f to 180f)),
                "bottom" to mapOf("hip" to (70f to 100f)),
            ),
            downThreshold = 90f,
            upThreshold = 165f,
            repCountingJoint = "knee",
        ),

        // ---------------- Hip Hinge & Posterior Chain ----------------
        ExerciseDefinition(
            id = "romanian_deadlift",
            displayName = "Romanian Deadlift",
            category = ExerciseCategory.HIP_HINGE,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Hip-hinge movement with a near-straight leg, bar close to the body.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("knee" to (160f to 180f)),
                "bottom" to mapOf("knee" to (150f to 175f)),
            ),
            downThreshold = 100f,
            upThreshold = 165f,
            repCountingJoint = "hip",
        ),
        ExerciseDefinition(
            id = "stiff_leg_deadlift",
            displayName = "Stiff-Leg Deadlift",
            category = ExerciseCategory.HIP_HINGE,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Hip hinge with legs kept nearly fully extended throughout.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("knee" to (165f to 180f)),
                "bottom" to mapOf("knee" to (160f to 180f)),
            ),
            downThreshold = 100f,
            upThreshold = 165f,
            repCountingJoint = "hip",
        ),
        ExerciseDefinition(
            id = "good_morning",
            displayName = "Good Morning",
            category = ExerciseCategory.HIP_HINGE,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Barbell on the back, hinge forward at the hips with a soft knee bend.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("knee" to (150f to 175f)),
                "bottom" to mapOf("knee" to (150f to 175f)),
            ),
            downThreshold = 110f,
            upThreshold = 165f,
            repCountingJoint = "hip",
        ),
        ExerciseDefinition(
            id = "hip_thrust",
            displayName = "Hip Thrust",
            category = ExerciseCategory.HIP_HINGE,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Shoulders elevated on a bench, drive the hips up to full extension.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("knee" to (80f to 100f)),
                "bottom" to mapOf("knee" to (80f to 100f)),
            ),
            downThreshold = 110f,
            upThreshold = 170f,
            repCountingJoint = "hip",
        ),
        ExerciseDefinition(
            id = "glute_bridge",
            displayName = "Glute Bridge",
            category = ExerciseCategory.HIP_HINGE,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Bodyweight bridge, shoulders on the floor, drive the hips up.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("knee" to (80f to 100f)),
                "bottom" to mapOf("knee" to (80f to 100f)),
            ),
            downThreshold = 110f,
            upThreshold = 170f,
            repCountingJoint = "hip",
        ),

        // ---------------- Push ----------------
        ExerciseDefinition(
            id = "bench_press",
            displayName = "Bench Press",
            category = ExerciseCategory.PUSH,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Barbell bench press, bar to chest, press to just short of lockout.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (60f to 100f)),
                "bottom" to mapOf("shoulder" to (60f to 100f)),
            ),
            downThreshold = 100f,
            upThreshold = 165f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "incline_db_press",
            displayName = "Incline DB Press",
            category = ExerciseCategory.PUSH,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Dumbbell press on an incline bench.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (60f to 100f)),
                "bottom" to mapOf("shoulder" to (60f to 100f)),
            ),
            downThreshold = 100f,
            upThreshold = 165f,
            repCountingJoint = "elbow",
        ),

        // ---------------- Pull ----------------
        ExerciseDefinition(
            id = "lat_pulldown",
            displayName = "Lat Pulldown",
            category = ExerciseCategory.PULL,
            cameraAngleDescription = "Place your phone to see you from the front.",
            description = "Cable pulldown to the upper chest, elbows driving down and back.",
            joints = standardJoints,
            formCriteria = mapOf(
                "bottom" to mapOf("shoulder" to (0f to 40f)),
                "top" to mapOf("shoulder" to (120f to 180f)),
            ),
            downThreshold = 40f,
            upThreshold = 150f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "seated_cable_row",
            displayName = "Seated Cable Row",
            category = ExerciseCategory.PULL,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Seated cable row, torso upright, pull to the torso.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (80f to 100f)),
                "bottom" to mapOf("hip" to (80f to 100f)),
            ),
            downThreshold = 60f,
            upThreshold = 150f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "barbell_row",
            displayName = "Barbell Row",
            category = ExerciseCategory.PULL,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Bent-over barbell row, hinge held throughout the set.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("hip" to (30f to 60f)),
                "bottom" to mapOf("hip" to (30f to 60f)),
            ),
            downThreshold = 60f,
            upThreshold = 150f,
            repCountingJoint = "elbow",
        ),

        // ---------------- Shoulders & Arms ----------------
        ExerciseDefinition(
            id = "overhead_press",
            displayName = "Overhead Press",
            category = ExerciseCategory.SHOULDERS_ARMS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Standing barbell or dumbbell press overhead.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (160f to 180f)),
                "bottom" to mapOf("shoulder" to (60f to 100f)),
            ),
            downThreshold = 100f,
            upThreshold = 160f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "triceps_pushdown",
            displayName = "Triceps Pushdown",
            category = ExerciseCategory.SHOULDERS_ARMS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Cable pushdown, elbows pinned to the sides throughout.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (0f to 30f)),
                "bottom" to mapOf("shoulder" to (0f to 30f)),
            ),
            downThreshold = 100f,
            upThreshold = 165f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "barbell_curl",
            displayName = "Barbell Curl",
            category = ExerciseCategory.SHOULDERS_ARMS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Standing barbell curl, elbows pinned to the sides.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (0f to 30f)),
                "bottom" to mapOf("shoulder" to (0f to 30f)),
            ),
            downThreshold = 30f,
            upThreshold = 160f,
            repCountingJoint = "elbow",
        ),
        ExerciseDefinition(
            id = "hammer_curl",
            displayName = "Hammer Curl",
            category = ExerciseCategory.SHOULDERS_ARMS,
            cameraAngleDescription = "Place your phone to see you from the side.",
            description = "Neutral-grip dumbbell curl, elbows pinned to the sides.",
            joints = standardJoints,
            formCriteria = mapOf(
                "top" to mapOf("shoulder" to (0f to 30f)),
                "bottom" to mapOf("shoulder" to (0f to 30f)),
            ),
            downThreshold = 30f,
            upThreshold = 160f,
            repCountingJoint = "elbow",
        ),

        // Add more ExerciseDefinition entries here as you build them out
    )

    fun byId(id: String) = all.first { it.id == id }
}