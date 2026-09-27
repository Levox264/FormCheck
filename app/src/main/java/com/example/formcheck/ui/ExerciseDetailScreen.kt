package com.example.formcheck.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.formcheck.ui.theme.Accent
import com.example.formcheck.ui.theme.AccentMuted
import com.example.formcheck.ui.theme.Background
import com.example.formcheck.ui.theme.Bad
import com.example.formcheck.ui.theme.Divider
import com.example.formcheck.ui.theme.Good
import com.example.formcheck.ui.theme.OnSurfaceMuted
import com.example.formcheck.ui.theme.Surface

/**
 * Minimal shape the screen needs. Map your real ExerciseDefinition + past
 * WorkoutResult rows into these — the screen only reads what's below.
 */
data class ExerciseDetail(
    val name: String,
    val description: String,
    val trackedCriteria: List<String>, // e.g. "Elbow angle at bottom", "Body stays level"
)

data class SessionSummary(
    val dateLabel: String, // e.g. "Today, 8:14 AM"
    val repCount: Int,
    val goodRepCount: Int,
    val badRepCount: Int,
)

@Composable
fun ExerciseDetailScreen(
    exercise: ExerciseDetail,
    recentSessions: List<SessionSummary>,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(containerColor = Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }

            item {
                Column {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceMuted,
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                StartButton(onClick = onStart)
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "What we track",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(exercise.trackedCriteria) { criterion ->
                CriterionRow(label = criterion)
            }

            if (recentSessions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Recent sessions",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(recentSessions) { session ->
                    SessionRow(session)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "startScale",
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
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Surface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Start",
            style = MaterialTheme.typography.titleMedium,
            color = Surface,
        )
    }
}

@Composable
private fun CriterionRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AccentMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SessionRow(session: SessionSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = session.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${session.repCount} reps",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountBadge(count = session.goodRepCount, color = Good)
                CountBadge(count = session.badRepCount, color = Bad)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}