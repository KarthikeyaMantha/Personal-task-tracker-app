package com.personal.daymark.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.daymark.DaymarkApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered after device reboot or package update.
 * Restores and reschedules all pending task and habit reminder alarms from Room SQLite.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val app = context.applicationContext as? DaymarkApplication ?: return
        val pendingResult = try { goAsync() } catch (e: Exception) { null }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendingTasks = app.container.taskRepository.getPendingTasksWithDueDate()
                val now = System.currentTimeMillis()

                for (task in pendingTasks) {
                    val dueDate = task.dueDate ?: continue
                    val triggerTime = AlarmManagerReminderScheduler.calculateTriggerTimeMillis(
                        dueDate,
                        task.dueTime
                    )
                    // If reminder trigger is in the future and task is not completed, reschedule it
                    if (triggerTime > now && !task.isCompleted) {
                        app.container.reminderScheduler.scheduleReminder(task)
                    }
                }

                // Reschedule active habit reminders
                val habits = app.container.habitRepository.getAllHabitsWithLogs().first()
                for (hwl in habits) {
                    if (!hwl.habit.isArchived && hwl.habit.reminderTime != null) {
                        app.container.reminderScheduler.scheduleHabitReminder(hwl.habit)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
