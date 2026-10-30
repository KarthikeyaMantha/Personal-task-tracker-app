package com.personal.daymark.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.ui.components.DaymarkTextField
import com.personal.daymark.ui.theme.DaymarkTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MoodLogOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (applicationContext as DaymarkApplication).container
        val habitRepository = appContainer.habitRepository

        setContent {
            DaymarkTheme {
                MoodLogOverlay(
                    onDismiss = { finish() },
                    onLogMood = { rating, note ->
                        lifecycleScope.launch {
                            val activeWithLogs = habitRepository.getActiveHabitsWithLogs().first()
                            val moodHabitWithLogs = activeWithLogs.firstOrNull { it.habit.healthMetricType.equals("mood", ignoreCase = true) }
                            val moodHabitId = moodHabitWithLogs?.habit?.id ?: habitRepository.createHabit(
                                name = "Daily Mood",
                                note = "Track daily mood and reflections",
                                frequency = "DAILY",
                                targetWeekdays = 127,
                                colorHex = "#D97706",
                                categoryId = null,
                                healthMetricType = "mood",
                                targetValue = 5,
                                unit = "rating"
                            )
                            habitRepository.logHealthMetric(
                                habitId = moodHabitId,
                                value = rating.toFloat(),
                                note = note,
                                localDate = LocalDate.now()
                            )
                            Toast.makeText(this@MoodLogOverlayActivity, "Mood logged", Toast.LENGTH_SHORT).show()
                            HealthWidget().updateAll(this@MoodLogOverlayActivity)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MoodLogOverlay(
    onDismiss: () -> Unit,
    onLogMood: (Int, String?) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }

    val moodIcons = listOf(
        1 to Icons.Outlined.SentimentVeryDissatisfied,
        2 to Icons.Outlined.SentimentDissatisfied,
        3 to Icons.Outlined.SentimentNeutral,
        4 to Icons.Outlined.SentimentSatisfied,
        5 to Icons.Outlined.SentimentVerySatisfied
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Daily Mood Check-in",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "How are you feeling right now?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    moodIcons.forEach { (rating, icon) ->
                        val isSelected = selectedRating == rating
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedRating = rating
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DaymarkTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Optional reflection note...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            if (selectedRating > 0) {
                                onLogMood(selectedRating, note.ifBlank { null })
                            }
                        },
                        enabled = selectedRating > 0
                    ) {
                        Text("Log Mood")
                    }
                }
            }
        }
    }
}
