package com.personal.daymark.ui.screens.focus

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.core.focus.FocusTimerData
import com.personal.daymark.core.focus.FocusTimerManager
import com.personal.daymark.core.focus.FocusTimerState

/**
 * Dedicated, distraction-free full-screen Focus Mode UI.
 * Hides standard app bar and navigation bar. Displays solely the linked task name,
 * prominent countdown timer, calming background color shift, and minimal Pause/Resume/Stop controls.
 * Bound to the Foreground Service state via FocusTimerManager.
 */
@Composable
fun FocusModeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    focusTimerManager: FocusTimerManager = (LocalContext.current.applicationContext as DaymarkApplication).appContainer.focusTimerManager
) {
    val timerData by focusTimerManager.timerState.collectAsState()
    val themeBg = MaterialTheme.colorScheme.background
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Calming subtle background animation adhering strictly to the active theme palette
    val infiniteTransition = rememberInfiniteTransition(label = "focusModeColorShift")
    val startBgColor = themeBg
    val endBgColor = surfaceVariant.copy(alpha = 0.35f)

    val animatedBgColor by infiniteTransition.animateColor(
        initialValue = startBgColor,
        targetValue = endBgColor,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgColorShift"
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            animatedBgColor,
            themeBg
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Minimize/Back Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Minimize Focus Mode",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = when (timerData.state) {
                        FocusTimerState.PAUSED -> "PAUSED"
                        FocusTimerState.RUNNING -> "DEEP WORK"
                        FocusTimerState.COMPLETED -> "SESSION COMPLETED"
                        FocusTimerState.IDLE -> "READY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Content: Linked Task Title & Prominent Countdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Task Name
            val taskTitle = timerData.linkedTaskTitle ?: "Deep Work Focus"
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Prominent Countdown Display
            Text(
                text = timerData.formattedRemainingTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Subtle Controls: Pause/Resume and Stop
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop Button
                OutlinedButton(
                    onClick = {
                        focusTimerManager.resetTimer(recordPartial = true)
                        onNavigateBack()
                    },
                    modifier = Modifier.sizeIn(minWidth = 110.dp, minHeight = 52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Session",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Pause / Resume Button
                if (timerData.state == FocusTimerState.RUNNING) {
                    Button(
                        onClick = { focusTimerManager.pauseTimer() },
                        modifier = Modifier.sizeIn(minWidth = 120.dp, minHeight = 52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause Session",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pause",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (timerData.state == FocusTimerState.PAUSED) {
                                focusTimerManager.resumeTimer()
                            } else {
                                focusTimerManager.startTimer()
                            }
                        },
                        modifier = Modifier.sizeIn(minWidth = 120.dp, minHeight = 52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Session",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (timerData.state == FocusTimerState.PAUSED) "Resume" else "Start",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }
}
