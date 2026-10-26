package com.personal.daymark.ui.screens.focus

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.focus.FocusSessionType
import com.personal.daymark.core.focus.FocusTimerEngine
import com.personal.daymark.core.focus.FocusTimerState
import com.personal.daymark.ui.components.DaymarkCard
import com.personal.daymark.ui.components.DaymarkTopAppBar

@Composable
fun FocusScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.provideFactory(
            focusTimerManager = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.focusTimerManager,
            taskRepository = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.taskRepository,
            settingsRepository = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.settingsRepository
        )
    )
) {
    val timerData by viewModel.timerData.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = timerData.progressFraction,
        label = "timerProgress"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DaymarkTopAppBar(
                title = "Focus Timer",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.skipPhase() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Phase",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Phase Selection Chips (clickable when IDLE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FocusSessionType.entries.forEach { type ->
                    val isSelected = timerData.sessionType == type
                    val durationMin = when (type) {
                        FocusSessionType.FOCUS -> userPrefs.defaultFocusDurationMinutes
                        FocusSessionType.SHORT_BREAK -> userPrefs.shortBreakDurationMinutes
                        FocusSessionType.LONG_BREAK -> userPrefs.longBreakDurationMinutes
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPhase(type) },
                        label = { Text("${type.displayName} (${durationMin}m)") },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (type.isWork) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Circular Countdown Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = 10.dp
                )

                // Animated Progress Ring
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (timerData.sessionType.isWork) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    strokeWidth = 10.dp
                )

                // Centered Time and Phase Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerData.formattedRemainingTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusLabel = when (timerData.state) {
                        FocusTimerState.PAUSED -> "Paused"
                        FocusTimerState.RUNNING -> if (timerData.sessionType.isWork) "Deep Work" else "Resting"
                        FocusTimerState.IDLE -> if (timerData.sessionType.isWork) "Ready to Focus" else "Ready for Break"
                        FocusTimerState.COMPLETED -> "Session Complete!"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pomodoro Cycle Indicator Dots (e.g. 4 dots)
            val completedInCycle = timerData.completedSessionsCount % userPrefs.sessionsUntilLongBreak
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until userPrefs.sessionsUntilLongBreak) {
                    val isDone = i < completedInCycle
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$completedInCycle of ${userPrefs.sessionsUntilLongBreak} before long break",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Linked Task Selector Card
            DaymarkCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openTaskPicker() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link Task",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Task",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = timerData.linkedTaskTitle ?: "Tap to link a task",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (timerData.linkedTaskTitle != null) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (timerData.linkedTaskTitle != null) "Change" else "Select",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Timer Controls
            when (timerData.state) {
                FocusTimerState.IDLE, FocusTimerState.COMPLETED -> {
                    Button(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (timerData.sessionType.isWork) "Start Focus" else "Start Break",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                FocusTimerState.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.requestResetConfirmation() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }

                        Button(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                FocusTimerState.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.requestResetConfirmation() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }

                        Button(
                            onClick = { viewModel.resumeTimer() },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Task Picker Modal Bottom Sheet
    if (uiState.isTaskPickerOpen) {
        TaskPickerBottomSheet(
            tasks = activeTasks,
            selectedTaskId = timerData.linkedTaskId,
            onTaskSelected = { task -> viewModel.selectTask(task) },
            onDismissRequest = { viewModel.closeTaskPicker() }
        )
    }

    // Reset Confirmation Dialog (Honest Partial Recording)
    if (uiState.showResetConfirmation) {
        val elapsedMinutes = FocusTimerEngine.calculateActualMinutesElapsed(
            timerData.totalDurationMillis,
            timerData.remainingMillis
        )

        AlertDialog(
            onDismissRequest = { viewModel.dismissResetConfirmation() },
            title = { Text("Stop Session?") },
            text = {
                if (elapsedMinutes >= 1) {
                    Text("You have focused for $elapsedMinutes minutes. Would you like to save this partial session or discard it?")
                } else {
                    Text("Resetting will discard the current session.")
                }
            },
            confirmButton = {
                if (elapsedMinutes >= 1) {
                    Button(
                        onClick = { viewModel.confirmReset(recordPartial = true) }
                    ) {
                        Text("Save Partial & Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.confirmReset(recordPartial = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset")
                    }
                }
            },
            dismissButton = {
                if (elapsedMinutes >= 1) {
                    OutlinedButton(
                        onClick = { viewModel.confirmReset(recordPartial = false) }
                    ) {
                        Text("Discard")
                    }
                } else {
                    OutlinedButton(onClick = { viewModel.dismissResetConfirmation() }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
