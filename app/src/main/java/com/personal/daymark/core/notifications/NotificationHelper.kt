package com.personal.daymark.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationHelper {
    const val CHANNEL_TASK_REMINDERS = "channel_task_reminders"
    const val CHANNEL_FOCUS_TIMER = "channel_focus_timer"

    /**
     * Initializes notification channels required for Daymark on Android O+ (API 26+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Task Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and reminders for scheduled tasks and due dates"
                enableVibration(true)
                setSound(alarmSound, audioAttributes)
                setShowBadge(true)
            }

            // 2. Focus Timer Channel
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                "Focus Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts and status for Pomodoro focus sessions"
                enableVibration(true)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, focusChannel))
        }
    }

    /**
     * Shows a completion notification when a Pomodoro focus or break session finishes.
     */
    fun showFocusCompletionNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "focus")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_FOCUS_TIMER)
            .setSmallIcon(com.personal.daymark.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(1002, notification)
    }
}
