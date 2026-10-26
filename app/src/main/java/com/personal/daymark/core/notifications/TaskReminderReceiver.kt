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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled task reminder fires.
 */
class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TASK_REMINDER) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val fallbackTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val fallbackNote = intent.getStringExtra(EXTRA_TASK_NOTE)

        val pendingResult = try { goAsync() } catch (e: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? DaymarkApplication
                val task = app?.container?.taskRepository?.getTaskById(taskId)

                // Only show notification if task exists in database and is not completed
                if (task != null) {
                    if (!task.isCompleted) {
                        showNotification(context, task.id, task.title, task.description)
                    }
                } else if (app == null) {
                    // Fallback for tests or decoupled context
                    showNotification(context, taskId, fallbackTitle, fallbackNote)
                }
            } catch (e: Exception) {
                // Safe fallback
                showNotification(context, taskId, fallbackTitle, fallbackNote)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Long,
        title: String,
        note: String?
    ) {
        // Android 13+ runtime notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Tap Intent: Launches MainActivity with target task ID
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Complete
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10m
        val snooze10Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Snooze 30m
        val snooze30Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 3).toInt(),
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(note?.takeIf { it.isNotBlank() } ?: "Task reminder is due now")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Mark Complete", completePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze 10m", snooze10PendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze 30m", snooze30PendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.personal.daymark.ACTION_TASK_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_NOTE = "extra_task_note"
        const val EXTRA_TARGET_TASK_ID = "extra_target_task_id"
    }
}
