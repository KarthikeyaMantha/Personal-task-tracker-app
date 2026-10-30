package com.personal.daymark.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

val medicineIdKey = ActionParameters.Key<Long>("medicine_id")
val waterHabitIdKey = ActionParameters.Key<Long>("water_habit_id")

object HealthWidgetUpdater {
    fun update(context: Context) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                HealthWidget().updateAll(context)
            }
        } catch (_: Throwable) {}
    }
}

class HealthWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as DaymarkApplication).container
        val taskDao = appContainer.database.taskDao()
        val habitRepository = appContainer.habitRepository

        provideContent {
            val today = remember { LocalDate.now() }
            val zone = remember { java.time.ZoneId.systemDefault() }
            val startOfDay = remember(today) { today.atStartOfDay(zone).toInstant().toEpochMilli() }
            val endOfDay = remember(today) { today.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant().toEpochMilli() }

            val medicines by taskDao.getTodayMedicines(startOfDay, endOfDay).collectAsState(initial = emptyList())
            val activeWithLogs by habitRepository.getActiveHabitsWithLogs().collectAsState(initial = emptyList())
            val waterHabitWithLogs = activeWithLogs.firstOrNull { it.habit.healthMetricType.equals("water", ignoreCase = true) }
            val waterHabit = waterHabitWithLogs?.habit
            val waterLog = waterHabitWithLogs?.logs?.firstOrNull { it.date == today.toEpochDay() }
            val waterGlasses = (waterLog?.value ?: 0f).toInt()
            val targetGlasses = waterHabit?.targetValue ?: 8

            GlanceTheme {
                HealthWidgetContent(
                    medicines = medicines,
                    waterHabit = waterHabit,
                    waterGlasses = waterGlasses,
                    targetGlasses = targetGlasses
                )
            }
        }
    }
}

class ToggleMedicineActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medicineId = parameters[medicineIdKey] ?: return
        val appContainer = (context.applicationContext as DaymarkApplication).container
        val taskRepository = appContainer.taskRepository
        val reminderScheduler = appContainer.reminderScheduler
        val task = taskRepository.getTaskById(medicineId) ?: return

        if (task.isCompleted) {
            taskRepository.setTaskCompleted(medicineId, false)
            if (task.dueDate != null) {
                reminderScheduler.scheduleReminder(task.copy(isCompleted = false))
            }
        } else {
            val nextTask = taskRepository.completeTaskAndGenerateNext(medicineId)
            reminderScheduler.cancelReminder(medicineId)
            if (nextTask != null && nextTask.dueDate != null) {
                reminderScheduler.scheduleReminder(nextTask)
            }
        }
        HealthWidgetUpdater.update(context)
    }
}

class IncrementWaterActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[waterHabitIdKey]
        val appContainer = (context.applicationContext as DaymarkApplication).container
        val habitRepository = appContainer.habitRepository
        val targetId = if (habitId != null && habitId > 0) {
            habitId
        } else {
            val activeWithLogs = habitRepository.getActiveHabitsWithLogs().first()
            val existing = activeWithLogs.firstOrNull { it.habit.healthMetricType.equals("water", ignoreCase = true) }
            existing?.habit?.id ?: habitRepository.createHabit(
                name = "Daily Hydration",
                note = "Drink enough water throughout the day",
                frequency = "DAILY",
                targetWeekdays = 127,
                colorHex = "#4A6FA5",
                categoryId = null,
                healthMetricType = "water",
                targetValue = 8,
                unit = "glasses"
            )
        }
        habitRepository.incrementWater(targetId, LocalDate.now())
        HealthWidgetUpdater.update(context)
    }
}

@Composable
private fun HealthWidgetContent(
    medicines: List<TaskEntity>,
    waterHabit: HabitEntity?,
    waterGlasses: Int,
    targetGlasses: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        // Header Row: Title & Mood Button
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Today's Health",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<com.personal.daymark.MainActivity>())
            )

            Button(
                text = "Log Mood",
                onClick = actionStartActivity<MoodLogOverlayActivity>()
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Medicine Schedule Section
        Text(
            text = "Medicines & Supplements",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))

        if (medicines.isEmpty()) {
            Text(
                text = "No medicines scheduled today",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        } else {
            medicines.take(3).forEach { medicine ->
                val timeStr = medicine.startTime ?: ""
                val dosageStr = if (!medicine.dosage.isNullOrBlank()) " (${medicine.dosage})" else ""
                val statusSymbol = if (medicine.isCompleted) "✓" else "○"
                val label = if (timeStr.isNotBlank()) "$timeStr — ${medicine.title}$dosageStr $statusSymbol"
                else "${medicine.title}$dosageStr $statusSymbol"

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(
                            actionRunCallback<ToggleMedicineActionCallback>(
                                actionParametersOf(medicineIdKey to medicine.id)
                            )
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            color = if (medicine.isCompleted) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (medicine.isCompleted) FontWeight.Normal else FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Water Progress & Quick +1 Action
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "$waterGlasses/$targetGlasses glasses",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            val habitId = waterHabit?.id ?: -1L
            Button(
                text = "+1 Glass",
                onClick = actionRunCallback<IncrementWaterActionCallback>(
                    actionParametersOf(waterHabitIdKey to habitId)
                )
            )
        }
    }
}
