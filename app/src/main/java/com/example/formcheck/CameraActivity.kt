package com.example.formcheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.formcheck.ui.theme.Accent
import com.example.formcheck.ui.theme.Bad
import com.example.formcheck.ui.theme.FormCheckTheme
import com.example.formcheck.ui.theme.Good
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors
import kotlin.math.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private const val VISIBILITY_THRESHOLD = 0.5f
private const val TOLERANCE = 5f
private const val MODEL_ASSET = "pose_landmarker_lite.task"

class CameraActivity : ComponentActivity() {
    private lateinit var exercise: ExerciseDefinition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exerciseId = intent.getStringExtra("exerciseId") ?: "pushup"
        exercise = ExerciseRepository.byId(exerciseId)

        setContent {
            FormCheckTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                if (hasCameraPermission) {
                    CameraWorkoutScreen(
                        exercise = exercise,
                        onExitClick = { result ->
                            val data = Intent().apply {
                                putExtra("workoutResult", result)
                            }
                            setResult(RESULT_OK, data)
                            finish()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera permission is required")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraWorkoutScreen(
    exercise: ExerciseDefinition,
    onExitClick: (WorkoutResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val repCounter = remember {
        RepCounter(
            downThreshold = exercise.downThreshold,
            upThreshold = exercise.upThreshold,
            formCriteria = exercise.formCriteria,
            repCountingJoint = exercise.repCountingJoint
        )
    }

    var repCount by remember { mutableIntStateOf(0) }
    var goodRepCount by remember { mutableIntStateOf(0) }
    var badRepCount by remember { mutableIntStateOf(0) }
    var skipReason by remember { mutableStateOf<String?>(null) }

    val poseLandmarker = remember {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                if (result.landmarks().isNotEmpty()) {
                    val landmarks = result.landmarks()[0]
                    val angles = getJointAngles(landmarks, exercise.joints)
                    repCounter.update(angles, landmarks)
                    // Structural equality means these only trigger recomposition
                    // on the frames where a value actually changed.
                    repCount = repCounter.repCount
                    goodRepCount = repCounter.goodRepCount
                    badRepCount = repCounter.badRepCount
                }
            }
            .setErrorListener { e -> Log.e("PoseLandmarker", "Detection error", e) }
            .build()

        PoseLandmarker.createFromOptions(context, options)
    }

    repCounter.onRepSkipped = { reason ->
        skipReason = reason
    }

    LaunchedEffect(skipReason) {
        if (skipReason != null) {
            delay(2000.milliseconds)
            skipReason = null
        }
    }

    // The PreviewView instance itself is created once and never recreated.
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Camera binding runs exactly once per entry into composition - NOT on
    // every recomposition. This is the fix for the per-rep slowdown: binding
    // was previously living in AndroidView's `update` block, which re-ran on
    // every rep/skip/state change and tore down + rebuilt the whole camera
    // pipeline each time.
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val rotatedBitmap = if (rotation != 0) {
                        val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else bitmap

                    val mpImage: MPImage = BitmapImageBuilder(rotatedBitmap).build()
                    poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())
                    imageProxy.close()
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraActivity", "Camera bind failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            poseLandmarker.close()
        }
    }

    CameraScreen(
        exerciseName = exercise.displayName,
        repCount = repCount,
        goodRepCount = goodRepCount,
        badRepCount = badRepCount,
        skipReason = skipReason,
        onBack = {
            onExitClick(
                WorkoutResult(
                    exerciseId = exercise.id,
                    goodReps = repCounter.goodRepCount,
                    badReps = repCounter.badRepCount,
                    judgements = repCounter.judgements.toList()
                )
            )
        },
        onEndSet = {
            val result = WorkoutResult(
                exerciseId = exercise.id,
                goodReps = repCounter.goodRepCount,
                badReps = repCounter.badRepCount,
                judgements = repCounter.judgements.toList()
            )
            onExitClick(result)
        },
        cameraPreview = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView },
            )
        }
    )
}
@Composable
fun PoseOverlay(
    result: PoseLandmarkerResult?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (result == null || result.landmarks().isEmpty()) return@Canvas
        
        val landmarks = result.landmarks()[0]
        
        // Draw connections (example: shoulders)
        drawLandmarkConnection(landmarks, 11, 12, Color.Green)
        
        // Draw landmarks
        landmarks.forEach { landmark ->
            if (landmark.visibility().orElse(0f) > VISIBILITY_THRESHOLD) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = (1f - landmark.x()) * size.width, // Front camera mirror
                        y = landmark.y() * size.height
                    )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLandmarkConnection(
    landmarks: List<NormalizedLandmark>,
    from: Int,
    to: Int,
    color: Color
) {
    val start = landmarks.getOrNull(from) ?: return
    val end = landmarks.getOrNull(to) ?: return
    
    if (start.visibility().orElse(0f) > VISIBILITY_THRESHOLD && 
        end.visibility().orElse(0f) > VISIBILITY_THRESHOLD) {
        drawLine(
            color = color,
            start = Offset(
                x = (1f - start.x()) * size.width,
                y = start.y() * size.height
            ),
            end = Offset(
                x = (1f - end.x()) * size.width,
                y = end.y() * size.height
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun CameraScreen(
    exerciseName: String,
    repCount: Int,
    goodRepCount: Int,
    badRepCount: Int,
    skipReason: String?, // null when nothing to show; set transiently from onRepSkipped
    onBack: () -> Unit,
    onEndSet: () -> Unit,
    cameraPreview: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Live camera feed + pose overlay, supplied by the caller.
        cameraPreview()

        // Top chrome: back button + exercise label, floating over the feed.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            GlassLabel(text = exerciseName)
            Spacer(modifier = Modifier.size(44.dp)) // balances the back button for a centered label
        }

        // Transient skip-reason banner, top-anchored under the chrome.
        SkipBanner(
            reason = skipReason,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp),
        )

        // Bottom chrome: rep counter + good/bad tally + end-set action.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RepCounterCard(repCount = repCount, goodRepCount = goodRepCount, badRepCount = badRepCount)
            Spacer(Modifier.height(16.dp))
            EndSetButton(onClick = onEndSet)
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun GlassLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun SkipBanner(reason: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = reason != null,
        modifier = modifier,
        enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 },
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Rep not counted — ${reason.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun RepCounterCard(repCount: Int, goodRepCount: Int, badRepCount: Int) {
    // Quick scale-pulse whenever the rep count changes — the one deliberate
    // motion moment on this screen, tied directly to the event it reports.
    var pulseTarget by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(repCount) {
        pulseTarget = 1.08f
        delay(90.milliseconds)
        pulseTarget = 1f
    }
    val animatedScale by animateFloatAsState(
        targetValue = pulseTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "repPulse",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 28.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = repCount.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 56.sp),
                color = Color.White,
                modifier = Modifier.scale(animatedScale),
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TallyDot(count = goodRepCount, color = Good)
                TallyDot(count = badRepCount, color = Bad)
            }
        }
    }
}

@Composable
private fun TallyDot(count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun EndSetButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(text = "End set", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private fun NormalizedLandmark.vis(): Float = visibility().orElse(0f)

private fun calculateAngle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Float {
    val baX = a.x() - b.x()
    val baY = a.y() - b.y()
    val bcX = c.x() - b.x()
    val bcY = c.y() - b.y()

    val dot = (baX * bcX) + (baY * bcY)
    val normBa = sqrt(baX * baX + baY * baY)
    val normBc = sqrt(bcX * bcX + bcY * bcY)

    var cosine = dot / (normBa * normBc + 1e-6f)
    cosine = cosine.coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cosine).toDouble()).toFloat()
}

private fun getJointAngles(landmarks: List<NormalizedLandmark>, joints: Map<String, Triple<Int, Int, Int>>): Map<String, Float> {
    val angles = mutableMapOf<String, Float>()
    for ((name, triple) in joints) {
        val (i, j, k) = triple
        val a = landmarks[i]; val b = landmarks[j]; val c = landmarks[k]
        if (a.vis() < VISIBILITY_THRESHOLD || b.vis() < VISIBILITY_THRESHOLD || c.vis() < VISIBILITY_THRESHOLD) {
            continue
        }
        angles[name] = calculateAngle(a, b, c)
    }
    return angles
}

/** Average left/right angle, falling back to whichever side is visible. */
private fun sideAngle(angles: Map<String, Float>, baseName: String): Float? {
    val left = angles["left_$baseName"]
    val right = angles["right_$baseName"]
    return when {
        left != null && right != null -> (left + right) / 2f
        left != null -> left
        right != null -> right
        else -> null
    }
}

data class FormResult(
    val isGood: Boolean,
    val checked: Map<String, Boolean>,
    val measured: Map<String, Float>,
    val issues: List<String>,
)

private fun evaluateForm(
    angles: Map<String, Float>,
    phase: String,
    formCriteria: Map<String, Map<String, Pair<Float, Float>>>,
    tolerance: Float = TOLERANCE
): FormResult {
    val criteria = formCriteria[phase] ?: error("phase must be 'top' or 'bottom', got $phase")

    val checked = mutableMapOf<String, Boolean>()
    val measured = mutableMapOf<String, Float>()
    val issues = mutableListOf<String>()

    for ((jointName, bounds) in criteria) {
        val (low, high) = bounds
        val value = sideAngle(angles, jointName)
        if (value == null) {
            issues.add("$jointName: not visible, skipped")
            continue
        }
        measured[jointName] = value
        val passed = value in (low - tolerance)..(high + tolerance)
        checked[jointName] = passed
        if (!passed) {
            issues.add("$jointName: ${value.toInt()}\u00b0 (optimal: ${low.toInt()}-${high.toInt()}\u00b0)")
        }
    }

    val isGood = checked.isNotEmpty() && checked.values.all { it }
    return FormResult(isGood, checked, measured, issues)
}

/** Confirms the torso is roughly horizontal (floor push-up), same logic as the Python version. */
private fun isBodyHorizontal(landmarks: List<NormalizedLandmark>, maxDeviationDeg: Float = 45f): Pair<Boolean, Float> {
    val leftShoulder = landmarks[11]; val rightShoulder = landmarks[12]
    val leftHip = landmarks[23]; val rightHip = landmarks[24]

    val shoulderMidX = (leftShoulder.x() + rightShoulder.x()) / 2f
    val shoulderMidY = (leftShoulder.y() + rightShoulder.y()) / 2f
    val hipMidX = (leftHip.x() + rightHip.x()) / 2f
    val hipMidY = (leftHip.y() + rightHip.y()) / 2f

    val dx = hipMidX - shoulderMidX
    val dy = hipMidY - shoulderMidY
    var lineAngle = abs(Math.toDegrees(atan2(dy, dx).toDouble())).toFloat()
    if (lineAngle > 90f) lineAngle = 180f - lineAngle

    return (lineAngle <= maxDeviationDeg) to lineAngle
}

// =====================================================================================
// REP COUNTER  (direct translation of your RepCounter class)
// =====================================================================================

class RepCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val formCriteria: Map<String, Map<String, Pair<Float, Float>>>,
    private val repCountingJoint: String,
) {
    var state: String = "up"
        private set
    var repCount = 0
        private set
    var goodRepCount = 0
        private set
    var badRepCount = 0
        private set
    var skippedRepCount = 0
        private set

    private var currentRepMinAngle: Float? = null
    private var currentRepMinAngles: Map<String, Float>? = null

    // True if the counting joint dropped out of view at any point during the current "down" phase.
    private var countingJointLostMidRep = false

    var lastRepGood: Boolean? = null
        private set
    var lastRepIssues: List<String> = emptyList()
        private set

    /** Callback fired whenever a rep finishes being judged, for UI updates. */
    var onRepJudged: ((repNumber: Int, good: Boolean, issues: List<String>) -> Unit)? = null

    /** Callback fired when a rep-in-progress is discarded because a required joint wasn't visible. */
    var onRepSkipped: ((reason: String) -> Unit)? = null

    /** True only if every joint required by this phase's form criteria was visible. */
    private fun allCriteriaJointsVisible(angles: Map<String, Float>, phase: String): Boolean {
        val criteria = formCriteria[phase] ?: return true
        return criteria.keys.all { jointName -> sideAngle(angles, jointName) != null }
    }

    fun update(angles: Map<String, Float>, landmarks: List<NormalizedLandmark>?) {
        val countingAngle = sideAngle(angles, repCountingJoint)

        if (countingAngle == null) {
            // Counting joint isn't visible this frame. If we're mid-rep, remember that this
            // rep can no longer be trusted, but keep waiting rather than resetting state -
            // the joint may come back before the rep completes.
            if (state == "down") {
                countingJointLostMidRep = true
            }
            return
        }

        when (state) {
            "up" -> {
                if (countingAngle < downThreshold) {
                    state = "down"
                    currentRepMinAngle = countingAngle
                    currentRepMinAngles = angles
                    countingJointLostMidRep = false
                }
            }
            "down" -> {
                if (currentRepMinAngle == null || countingAngle < currentRepMinAngle!!) {
                    currentRepMinAngle = countingAngle
                    currentRepMinAngles = angles
                }

                if (countingAngle > upThreshold) {
                    state = "up"
                    finishRep(topAngles = angles)
                    currentRepMinAngle = null
                    currentRepMinAngles = null
                    countingJointLostMidRep = false
                }
            }
        }
    }

    val judgements = mutableListOf<RepJudgement>()

    /** Decides whether the just-completed rep can be judged, or must be discarded as unverifiable. */
    private fun finishRep(topAngles: Map<String, Float>) {
        val bottomAngles = currentRepMinAngles

        val reason = when {
            countingJointLostMidRep ->
                "$repCountingJoint not visible during rep"
            bottomAngles == null ->
                "no bottom-of-rep data captured"
            !allCriteriaJointsVisible(bottomAngles, "bottom") ->
                "form joint not visible at bottom of rep"
            !allCriteriaJointsVisible(topAngles, "top") ->
                "form joint not visible at top of rep"
            else -> null
        }

        if (reason != null) {
            skippedRepCount += 1
            Log.d("RepCounter", "Rep discarded: $reason")
            onRepSkipped?.invoke(reason)
            return
        }

        repCount += 1
        judgeRep(bottomAngles!!, topAngles)
    }

    private fun judgeRep(bottomAngles: Map<String, Float>, topAngles: Map<String, Float>) {
        val issues = mutableListOf<String>()

        val bottomResult = evaluateForm(bottomAngles, "bottom", formCriteria)
        issues += bottomResult.issues.map { "bottom - $it" }

        val topResult = evaluateForm(topAngles, "top", formCriteria)
        issues += topResult.issues.map { "top - $it" }

        val repIsGood = bottomResult.isGood && topResult.isGood
        if (repIsGood) goodRepCount += 1 else badRepCount += 1

        lastRepGood = repIsGood
        lastRepIssues = issues

        Log.d("RepCounter", "Rep $repCount: ${if (repIsGood) "GOOD" else "BAD"}")
        issues.forEach { Log.d("RepCounter", "  - $it") }

        judgements.add(RepJudgement(repCount, repIsGood, issues))
        onRepJudged?.invoke(repCount, repIsGood, issues)
    }
}
