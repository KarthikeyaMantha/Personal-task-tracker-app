package com.personal.daymark.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.personal.daymark.core.common.DateFormatter
import com.personal.daymark.core.voice.VoiceAction
import com.personal.daymark.core.voice.VoiceSpeechState
import com.personal.daymark.ui.components.DaymarkButton
import com.personal.daymark.ui.components.DaymarkTextField
import com.personal.daymark.ui.theme.PriorityHigh
import com.personal.daymark.ui.theme.PriorityLow
import com.personal.daymark.ui.theme.PriorityMedium
import com.personal.daymark.ui.theme.PriorityUrgent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputBottomSheet(
    viewModel: VoiceInputViewModel,
    onDismissRequest: () -> Unit,
    onActionCompleted: (String) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val speechState by viewModel.speechState.collectAsState()
    val parsedAction by viewModel.parsedAction.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VoiceInputUiEvent.ExecutionComplete -> {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onActionCompleted(event.message)
                    onDismissRequest()
                }
                is VoiceInputUiEvent.ShowError -> {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onActionCompleted(event.message)
                    onDismissRequest()
                }
                is VoiceInputUiEvent.Dismiss -> {
                    onDismissRequest()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancel()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice Action",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!hasAudioPermission) {
                // Permission Request Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Microphone Permission Needed",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Daymark uses your microphone to transcribe voice commands. Audio is processed strictly on-device without internet transmission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    DaymarkButton(
                        text = "Grant Permission",
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (parsedAction != null && parsedAction !is VoiceAction.Unknown) {
                // Action Confirmation Preview Card
                val action = parsedAction!!

                when (action) {
                    is VoiceAction.CreateTask -> {
                        var editedTitle by remember(action) { mutableStateOf(action.title) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(20.dp)
                        ) {
                            // Badge Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Add Task",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                if (isCountdownActive) {
                                    Text(
                                        text = "Auto-saving in ${countdownSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Editable Task Title Field
                            Text(
                                text = "Task Title (tap to edit):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DaymarkTextField(
                                value = editedTitle,
                                onValueChange = {
                                    editedTitle = it
                                    viewModel.pauseCountdown()
                                },
                                placeholder = "Task title",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Metadata Chips (Due Date, Time, Priority)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (action.dueDate != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = DateFormatter.formatRelativeDate(action.dueDate),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (action.dueTimeMinutes != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = DateFormatter.formatTime(action.dueTimeMinutes),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (action.priority > 0) {
                                    val (prioLabel, prioColor) = when (action.priority) {
                                        4 -> "! Urgent" to PriorityUrgent
                                        3 -> "▲ High" to PriorityHigh
                                        2 -> "■ Medium" to PriorityMedium
                                        else -> "▼ Low" to PriorityLow
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(prioColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = prioLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = prioColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (speechState is VoiceSpeechState.Success) {
                                Text(
                                    text = "Heard: “${(speechState as VoiceSpeechState.Success).rawText}”",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            DaymarkButton(
                                text = "Save Task",
                                onClick = { viewModel.confirmAction(editedTitle) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { viewModel.retry() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Speak Again")
                                }

                                TextButton(onClick = onDismissRequest) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    else -> {
                        // Other Actions (Complete Task, Habit Log, Habit Toggle, Habit Create)
                        val (badgeLabel, badgeColor) = when (action) {
                            is VoiceAction.CompleteTask -> "Complete Task" to MaterialTheme.colorScheme.primary
                            is VoiceAction.LogHabitProgress -> "Log Habit" to MaterialTheme.colorScheme.tertiary
                            is VoiceAction.ToggleHabit -> "Check Off Habit" to MaterialTheme.colorScheme.primary
                            is VoiceAction.CreateHabit -> "New Habit" to MaterialTheme.colorScheme.primary
                            else -> "Action" to MaterialTheme.colorScheme.primary
                        }

                        val explanation = when (action) {
                            is VoiceAction.CompleteTask -> action.explanation
                            is VoiceAction.LogHabitProgress -> action.explanation
                            is VoiceAction.ToggleHabit -> action.explanation
                            is VoiceAction.CreateHabit -> action.explanation
                            else -> ""
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (speechState is VoiceSpeechState.Success) {
                                Text(
                                    text = "“${(speechState as VoiceSpeechState.Success).rawText}”",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (isCountdownActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Auto-saving in ${countdownSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(tap to pause)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { viewModel.pauseCountdown() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            DaymarkButton(
                                text = "Save Now",
                                onClick = { viewModel.confirmAction() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { viewModel.retry() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Speak Again")
                                }

                                TextButton(onClick = onDismissRequest) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                // Active Listening / Pulse Animation & Live Transcript
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                val rmsDb = (speechState as? VoiceSpeechState.Listening)?.rmsDb ?: 0f
                val dynamicRmsScale by animateFloatAsState(
                    targetValue = 1f + (rmsDb / 15f).coerceIn(0f, 0.45f),
                    label = "rmsScale"
                )

                Box(
                    modifier = Modifier
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (speechState is VoiceSpeechState.Listening) {
                        // Concentric Outer Pulse Ring
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(pulseScale * dynamicRmsScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                        // Inner Pulse Ring
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(dynamicRmsScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        )
                    }

                    // Center Microphone Action Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                if (speechState is VoiceSpeechState.Listening) {
                                    viewModel.stopListening()
                                } else {
                                    viewModel.startListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val statusTitle = when (speechState) {
                    is VoiceSpeechState.Listening -> "Listening..."
                    is VoiceSpeechState.Processing -> "Understanding..."
                    is VoiceSpeechState.Error -> "Didn't Catch That"
                    else -> "Tap mic to speak"
                }

                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Streaming Live Partial Transcript or Examples
                val partialText = (speechState as? VoiceSpeechState.Listening)?.partialText.orEmpty()
                if (partialText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "“$partialText”",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (speechState is VoiceSpeechState.Error) {
                    val errorMsg = (speechState as VoiceSpeechState.Error).message
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(onClick = { viewModel.retry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again")
                    }
                } else {
                    Text(
                        text = "Try saying: “Remind me to call Mom tomorrow at 6pm”\nor “Drank 2 glasses of water” or “Check off meditation”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
