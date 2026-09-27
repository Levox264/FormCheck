package com.example.formcheck.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.formcheck.ExerciseCategory
import com.example.formcheck.ExerciseDefinition
import com.example.formcheck.ui.theme.Accent
import com.example.formcheck.ui.theme.AccentMuted
import com.example.formcheck.ui.theme.Background
import com.example.formcheck.ui.theme.Divider
import com.example.formcheck.ui.theme.OnSurfaceMuted
import com.example.formcheck.ui.theme.Surface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseListScreen(
    exercises: List<ExerciseDefinition>,
    onExerciseClick: (String) -> Unit,
    onQuickStart: () -> Unit = {},
) {
    // Group by category, preserving declaration order, dropping empty categories.
    val grouped: List<Pair<ExerciseCategory, List<ExerciseDefinition>>> = remember(exercises) {
        ExerciseCategory.entries
            .map { category -> category to exercises.filter { it.category == category } }
            .filter { (_, list) -> list.isNotEmpty() }
    }

    Scaffold(containerColor = Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pick a movement and FormCheck will count your reps and watch your form.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceMuted,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                QuickStartButton(onClick = onQuickStart)
                Spacer(Modifier.height(8.dp))
            }

            if (exercises.isEmpty()) {
                item { EmptyState() }
            } else {
                grouped.forEach { (category, categoryExercises) ->
                    stickyHeader(key = "header_${category.name}") {
                        CategoryHeader(category)
                    }
                    items(categoryExercises, key = { it.id }) { exercise ->
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            ExerciseCard(
                                exercise = exercise,
                                onClick = { onExerciseClick(exercise.id) },
                            )
                        }
                    }
                    item(key = "spacer_${category.name}") {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: ExerciseCategory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background),
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceMuted,
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun QuickStartButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "quickStartScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Quick start",
                style = MaterialTheme.typography.titleMedium,
                color = Surface,
            )
            Text(
                text = "Jump into the camera with your last exercise",
                style = MaterialTheme.typography.bodyMedium,
                color = Surface.copy(alpha = 0.85f),
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Surface.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Surface,
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseDefinition,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Accent,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open ${exercise.displayName}",
                tint = OnSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = OnSurfaceMuted,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No exercises yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Add a movement to start tracking your form.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}