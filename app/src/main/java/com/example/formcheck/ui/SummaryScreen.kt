/*package com.example.formcheck.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.formcheck.WorkoutResult

@Composable
fun SummaryScreen(result: WorkoutResult?, onDone: () -> Unit) {
    if (result == null) {
        Text("No workout data.")
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Good reps: ${result.goodReps}", style = MaterialTheme.typography.headlineSmall)
        Text("Bad reps: ${result.badReps}", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(result.judgements.filter { !it.good }) { rep ->
                Text("Rep ${rep.repNumber}:", style = MaterialTheme.typography.titleSmall)
                rep.issues.forEach { Text("  • $it") }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}*/
package com.example.formcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.formcheck.ui.theme.Accent
import com.example.formcheck.ui.theme.Background
import com.example.formcheck.ui.theme.Bad
import com.example.formcheck.ui.theme.Divider
import com.example.formcheck.ui.theme.Good
import com.example.formcheck.ui.theme.OnSurfaceMuted
import com.example.formcheck.ui.theme.Surface
import com.example.formcheck.WorkoutResult

/**
 * Read model for the summary screen. Map straight from your WorkoutResult /
 * RepJudgement list — this is the same shape, just with a display-ready
 * exercise name attached.
 */
data class RepBreakdownItem(
    val repNumber: Int,
    val good: Boolean,
    val issues: List<String>,
)

data class WorkoutSummary(
    val exerciseName: String,
    val goodReps: Int,
    val badReps: Int,
    val reps: List<RepBreakdownItem>,
)

@Composable
fun SummaryScreen(
    result: WorkoutResult?,
    summary: WorkoutSummary,
    onDone: () -> Unit,
) {
    val totalReps = summary.goodReps + summary.badReps

    Scaffold(containerColor = Background) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = "Set complete",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${summary.exerciseName} · $totalReps reps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceMuted,
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    StatsRow(goodReps = summary.goodReps, badReps = summary.badReps)
                }

                if (summary.reps.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Rep breakdown",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(summary.reps, key = { it.repNumber }) { rep ->
                        RepRow(rep)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Primary action anchored to the bottom - the natural place to
            // confirm "done reviewing" after scrolling the breakdown, rather
            // than forcing it above content the person hasn't read yet.
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                DoneButton(onClick = onDone)
            }
        }
    }
}

@Composable
private fun StatsRow(goodReps: Int, badReps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(label = "Good form", value = goodReps, color = Good, modifier = Modifier.weight(1f))
        StatCard(label = "Needs work", value = badReps, color = Bad, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = color,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RepRow(rep: RepBreakdownItem) {
    var expanded by remember { mutableStateOf(false) }
    val color = if (rep.good) Good else Bad
    val canExpand = rep.issues.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canExpand) { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (rep.good) Icons.Filled.Check else Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = "Rep ${rep.repNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                if (canExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Hide details" else "Show details",
                        tint = OnSurfaceMuted,
                    )
                }
            }

            if (expanded && canExpand) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rep.issues.forEach { issue ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(OnSurfaceMuted),
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Done",
            style = MaterialTheme.typography.titleMedium,
            color = Surface,
        )
    }
}