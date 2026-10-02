package com.example.formcheck.stats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.formcheck.ExerciseRepository
import com.example.formcheck.ui.theme.Accent
import com.example.formcheck.ui.theme.Background
import com.example.formcheck.ui.theme.Divider
import com.example.formcheck.ui.theme.OnSurfaceMuted
import com.example.formcheck.ui.theme.Surface as SurfaceColor
import com.example.formcheck.ui.theme.Bad
import com.example.formcheck.ui.theme.Good
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import kotlin.math.roundToInt

/** Navigation entry point: loads history off the main thread, then shows the screen. */
@Composable
fun StatsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val sessions by produceState<List<SessionRecord>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { WorkoutHistory.load(context) }
    }
    sessions?.let { StatsScreen(sessions = it, onBack = onBack) }
}

private fun exerciseName(id: String): String =
    ExerciseRepository.all.firstOrNull { it.id == id }?.displayName ?: id

@Composable
fun StatsScreen(sessions: List<SessionRecord>, onBack: () -> Unit) {
    var range by remember { mutableStateOf(StatsRange.WEEK) }
    val now = remember { System.currentTimeMillis() }
    val zone = remember { TimeZone.getDefault() }
    val summary = remember(sessions, range) {
        StatsCalculator.summarize(sessions, range, now, zone, ::exerciseName)
    }
    val week = remember(sessions) { StatsCalculator.lastSevenDays(sessions, now, zone) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.size(8.dp))
            Text("Stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }

        if (sessions.isEmpty()) {
            EmptyState()
            return@Column
        }

        RangeChips(selected = range, onSelect = { range = it })

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Sets", summary.sessions.toString(), Modifier.weight(1f))
            StatCard("Reps", summary.totalReps.toString(), Modifier.weight(1f))
            val score = summary.formScore
            StatCard(
                label = "Form",
                value = score?.let { "${(it * 100).roundToInt()}%" } ?: "-",
                modifier = Modifier.weight(1f),
                valueColor = when {
                    score == null -> MaterialTheme.colorScheme.onSurface
                    score >= 0.75f -> Good
                    else -> Bad
                },
            )
        }

        SectionCard(title = "Last 7 days") { WeekChart(week) }

        SectionCard(title = "By exercise") {
            if (summary.exercises.isEmpty()) {
                Muted("No sets in this range.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    summary.exercises.forEach { ExerciseRow(it) }
                }
            }
        }

        SectionCard(title = "Most common form issues") {
            if (summary.topIssues.isEmpty()) {
                Muted("No form issues recorded in this range.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    summary.topIssues.forEach { IssueRow(it) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------------
// Pieces. Cards use a 1dp border instead of elevation, per the design brief.
// ---------------------------------------------------------------------------------

@Composable
private fun CardSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        border = BorderStroke(1.dp, Divider),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    CardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceMuted,
            )
            content()
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    CardSurface(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
        }
    }
}

@Composable
private fun RangeChips(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsRange.entries.forEach { r ->
            val isSelected = r == selected
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(shape)
                    .background(if (isSelected) Accent else Color.Transparent)
                    .border(1.dp, if (isSelected) Accent else Divider, shape)
                    .clickable { onSelect(r) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else OnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun WeekChart(bars: List<DayBar>) {
    val maxReps = (bars.maxOfOrNull { it.reps } ?: 0).coerceAtLeast(1)
    val maxBar = 80.dp
    Row(
        modifier = Modifier.fillMaxWidth().height(maxBar + 44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val target: Dp = if (bar.reps == 0) 4.dp else maxBar * (bar.reps.toFloat() / maxReps)
            val height by animateDpAsState(target, tween(400), label = "bar")
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (bar.reps > 0) {
                    Text(
                        bar.reps.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                bar.reps == 0 -> Divider
                                bar.isToday -> Accent
                                else -> Accent.copy(alpha = 0.35f)
                            },
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (bar.isToday) MaterialTheme.colorScheme.onSurface
                    else OnSurfaceMuted,
                    fontWeight = if (bar.isToday) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(stat: ExerciseStat) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                "${stat.totalReps} reps",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(Divider),
        ) {
            if (stat.goodReps > 0) Box(Modifier.weight(stat.goodReps.toFloat()).fillMaxHeight().background(Good))
            if (stat.badReps > 0) Box(Modifier.weight(stat.badReps.toFloat()).fillMaxHeight().background(Bad))
        }
        Text(
            "${stat.goodReps} good · ${stat.badReps} to fix · ${stat.sessions} ${if (stat.sessions == 1) "set" else "sets"}",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceMuted,
        )
    }
}

@Composable
private fun IssueRow(issue: IssueStat) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(issue.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "×${issue.repCount}",
            style = MaterialTheme.typography.labelLarge,
            color = Bad,
        )
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("No sets yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Muted("Finish a set and your stats will show up here.")
    }
}