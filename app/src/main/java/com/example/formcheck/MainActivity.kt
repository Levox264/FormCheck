package com.example.formcheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.formcheck.stats.StatsRoute
import com.example.formcheck.ui.ExerciseListScreen
import com.example.formcheck.ui.screens.SummaryScreen
import com.example.formcheck.ui.screens.WorkoutSummary
import com.example.formcheck.ui.screens.RepBreakdownItem
import com.example.formcheck.ui.screens.ExerciseDetail
import com.example.formcheck.ui.screens.ExerciseDetailScreen
import com.example.formcheck.ui.theme.FormCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FormCheckTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val context = LocalContext.current

            val quickStartLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { activityResult ->
                val result = activityResult.data?.let {
                    IntentCompat.getParcelableExtra(it, "workoutResult", WorkoutResult::class.java)
                }
                if (result != null) {
                    navController.currentBackStackEntry
                        ?.savedStateHandle?.set("workoutResult", result)
                    navController.navigate("summary")
                }
            }

            ExerciseListScreen(
                exercises = ExerciseRepository.all,
                onExerciseClick = { id -> navController.navigate("detail/$id") },
                onStatsClick = { navController.navigate("stats") },
                onQuickStart = {
                    // Fall back to the first exercise if none has been done yet.
                    val exerciseId = LastExerciseStore.get(context)
                        ?: ExerciseRepository.all.firstOrNull()?.id

                    if (exerciseId != null) {
                        LastExerciseStore.save(context, exerciseId)
                        val intent = Intent(context, CameraActivity::class.java)
                        intent.putExtra("exerciseId", exerciseId)
                        quickStartLauncher.launch(intent)
                    }
                }
            )
        }
        composable(
            "detail/{exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments!!.getString("exerciseId")!!
            val exercise = ExerciseRepository.byId(exerciseId)
            val context = LocalContext.current

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { activityResult ->
                val result = activityResult.data?.let {
                    IntentCompat.getParcelableExtra(it, "workoutResult", WorkoutResult::class.java)
                }
                if (result != null) {
                    navController.currentBackStackEntry
                        ?.savedStateHandle?.set("workoutResult", result)
                    navController.navigate("summary")
                }
            }

            ExerciseDetailScreen(
                exercise = ExerciseDetail(
                    name = exercise.displayName,
                    description = exercise.description,
                    trackedCriteria = exercise.checks
                        .map { it.metricId }
                        .distinct()
                        .map { it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } }
                ),
                recentSessions = emptyList(), // TODO: Fetch from local DB
                onBack = { navController.popBackStack() },
                onStart = {
                    LastExerciseStore.save(context, exerciseId)
                    val intent = Intent(context, CameraActivity::class.java)
                    intent.putExtra("exerciseId", exerciseId)
                    launcher.launch(intent)
                }
            )
        }
        composable("summary") {
            val result = navController.previousBackStackEntry
                ?.savedStateHandle?.get<WorkoutResult>("workoutResult")
            val summary = result?.let { res ->
                val exercise = ExerciseRepository.byId(res.exerciseId)
                WorkoutSummary(
                    exerciseName = exercise.displayName,
                    goodReps = res.goodReps,
                    badReps = res.badReps,
                    reps = res.judgements.map { j ->
                        RepBreakdownItem(j.repNumber, j.good, j.issues)
                    }
                )
            } ?: WorkoutSummary("", 0, 0, emptyList())

            SummaryScreen(
                result = result,
                summary = summary,
                onDone = { navController.popBackStack("list", inclusive = false) }
            )
        }
        composable("stats") {
            StatsRoute(onBack = { navController.popBackStack() })
        }
    }
}