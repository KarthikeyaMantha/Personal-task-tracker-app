package com.personal.daymark.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.MainActivity
import com.personal.daymark.R
import com.personal.daymark.core.habits.HabitCalculationHelper
import com.personal.daymark.data.local.entity.HabitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled habit reminder fires.
 */
class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HABIT_REMINDER) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        val pendingResult = try { goAsync() } catch (e: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? DaymarkApplication ?: return@launch
                val habits = app.container.habitRepository.getAllHabitsWithLogs().first()
                val targetHabitWithLogs = habits.find { it.habit.id == habitId } ?: return@launch
                val habit = targetHabitWithLogs.habit

                if (!habit.isArchived) {
                    val today = LocalDate.now()
                    val isScheduledToday = HabitCalculationHelper.isScheduledOnDate(habit, today)
                    val isCompletedToday = HabitCalculationHelper.isCompletedOnDate(targetHabitWithLogs.logs, today)

                    // Only show notification if scheduled for today and not yet completed
                    if (isScheduledToday && !isCompletedToday) {
                        showNotification(context, habit)
                    }

                    // Reschedule for next occurrence
                    app.container.reminderScheduler.scheduleHabitReminder(habit)
                }
            } catch (e: Exception) {
                // Safe fallback log
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showNotification(context: Context, habit: HabitEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notificationId = (100_000_000L + habit.id).toInt()

        // Tap opens MainActivity and navigates to habits
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "habits")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Done
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_HABIT
            putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habit.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifContent = if (habit.targetDurationMinutes != null) {
            "Time for your ${habit.targetDurationMinutes}-minute session"
        } else {
            habit.note?.takeIf { it.isNotBlank() } ?: "Time for your daily ${habit.name}"
        }

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Habit: ${habit.name}")
            .setContentText(notifContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Mark Done", completePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled gracefully
        }
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.personal.daymark.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
    }
}

