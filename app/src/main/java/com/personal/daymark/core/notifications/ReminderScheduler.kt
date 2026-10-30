package com.personal.daymark.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.personal.daymark.data.local.entity.HabitEntity
import com.personal.daymark.data.local.entity.TaskEntity
import java.util.Calendar

interface ReminderScheduler {
    fun scheduleReminder(task: TaskEntity)
    fun scheduleReminder(taskId: Long, title: String, description: String?, triggerTimeMillis: Long)
    fun scheduleSnooze(taskId: Long, title: String, snoozeTimeMillis: Long)
    fun cancelReminder(taskId: Long)
    fun canScheduleExactAlarms(): Boolean
    fun scheduleHabitReminder(habit: HabitEntity)
    fun cancelHabitReminder(habitId: Long)
}

class AlarmManagerReminderScheduler(
    private val context: Context
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return SystemReliabilityHelper.canScheduleExactAlarms(context)
    }

    override fun scheduleReminder(task: TaskEntity) {
        if (task.isCompleted || task.dueDate == null) {
            cancelReminder(task.id)
            return
        }

        val triggerTime = calculateTriggerTimeMillis(task.dueDate, task.dueTime)
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        scheduleReminder(
            taskId = task.id,
            title = task.title,
            description = task.description,
            triggerTimeMillis = triggerTime
        )
    }

    override fun scheduleReminder(
        taskId: Long,
        title: String,
        description: String?,
        triggerTimeMillis: Long
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_NOTE, description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTimeMillis, pendingIntent)
    }

    override fun scheduleSnooze(taskId: Long, title: String, snoozeTimeMillis: Long) {
        if (snoozeTimeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_NOTE, "Snoozed task reminder")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(snoozeTimeMillis, pendingIntent)
    }

    override fun cancelReminder(taskId: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun scheduleHabitReminder(habit: HabitEntity) {
        if (habit.isArchived || habit.reminderTime == null) {
            cancelHabitReminder(habit.id)
            return
        }

        val triggerTime = calculateNextHabitTriggerMillis(habit) ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habit.name)
        }

        val requestCode = (100_000_000L + habit.id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTime, pendingIntent)
    }

    override fun cancelHabitReminder(habitId: Long) {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_HABIT_REMINDER
        }
        val requestCode = (100_000_000L + habitId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    // Graceful fallback to inexact when exact alarms are denied by user or OEM
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "SecurityException scheduling exact alarm, falling back to inexact", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    companion object {
        fun calculateTriggerTimeMillis(dueDateMillis: Long, dueTimeMinutes: Int?): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dueDateMillis
                if (dueTimeMinutes != null) {
                    val hours = dueTimeMinutes / 60
                    val minutes = dueTimeMinutes % 60
                    set(Calendar.HOUR_OF_DAY, hours)
                    set(Calendar.MINUTE, minutes)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                } else {
                    // Default reminder time for tasks with only a date: 9:00 AM
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
            return calendar.timeInMillis
        }

        fun calculateNextHabitTriggerMillis(habit: HabitEntity, nowMillis: Long = System.currentTimeMillis()): Long? {
            val reminderMinutes = habit.reminderTime ?: return null
            val targetWeekdays = habit.targetWeekdays
            val reminderHour = reminderMinutes / 60
            val reminderMinute = reminderMinutes % 60

            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }

            for (dayOffset in 0..7) {
                val candidate = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val dayOfWeekValue = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                val bit = 1 shl (dayOfWeekValue - 1)
                val isScheduled = (habit.frequency.equals("DAILY", ignoreCase = true) && targetWeekdays == 127) || ((targetWeekdays and bit) != 0)

                if (isScheduled && candidate.timeInMillis > nowMillis) {
                    return candidate.timeInMillis
                }
            }
            return null
        }
    }
}
