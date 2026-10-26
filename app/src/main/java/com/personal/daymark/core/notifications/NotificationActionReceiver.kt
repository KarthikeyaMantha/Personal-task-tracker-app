package com.personal.daymark.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.personal.daymark.DaymarkApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by user interaction with notification action buttons:
 * - "Mark Complete": updates the database directly and cancels the notification
 * - "Snooze": dismisses the notification and reschedules an alarm for the snooze interval
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // Immediately dismiss the current notification
        NotificationManagerCompat.from(context).cancel(taskId.toInt())

        val app = context.applicationContext as? DaymarkApplication ?: return
        val pendingResult = try { goAsync() } catch (e: Exception) { null }

        when (intent.action) {
            ACTION_COMPLETE_TASK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val nextTask = app.container.taskRepository.completeTaskAndGenerateNext(taskId)
                        app.container.reminderScheduler.cancelReminder(taskId)
                        if (nextTask != null && nextTask.dueDate != null) {
                            app.container.reminderScheduler.scheduleReminder(nextTask)
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_SNOOZE_TASK -> {
                val title = intent.getStringExtra(TaskReminderReceiver.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                val snoozeTimeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.container.reminderScheduler.scheduleSnooze(
                            taskId = taskId,
                            title = title,
                            snoozeTimeMillis = snoozeTimeMillis
                        )
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            else -> {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.personal.daymark.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.personal.daymark.ACTION_SNOOZE_TASK"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
