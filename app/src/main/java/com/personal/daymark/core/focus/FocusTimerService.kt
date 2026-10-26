package com.personal.daymark.core.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.MainActivity
import com.personal.daymark.R
import com.personal.daymark.core.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service hosting the active Pomodoro focus countdown.
 * Survives backgrounding, screen lock, and maintains an active persistent notification
 * with Pause/Resume/Reset controls and live time updates.
 */
class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private val timerManager: FocusTimerManager by lazy {
        (application as DaymarkApplication).appContainer.focusTimerManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startTicker()
            }
            ACTION_PAUSE -> {
                timerManager.pauseTimer()
                stopTicker()
                updateNotification()
            }
            ACTION_RESUME -> {
                timerManager.resumeTimer()
                startTicker()
            }
            ACTION_RESET -> {
                timerManager.resetTimer(recordPartial = true)
                stopForegroundAndSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildTimerNotification(timerManager.timerState.value)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundType
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val updated = timerManager.tick()
                if (updated.state == FocusTimerState.RUNNING) {
                    updateNotification()
                } else {
                    stopForegroundAndSelf()
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateNotification() {
        val notification = buildTimerNotification(timerManager.timerState.value)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundAndSelf() {
        stopTicker()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTimerNotification(data: FocusTimerData): Notification {
        val title = when (data.sessionType) {
            FocusSessionType.FOCUS -> {
                if (!data.linkedTaskTitle.isNullOrBlank()) {
                    "Focus: ${data.linkedTaskTitle}"
                } else {
                    "Focus Session (Deep Work)"
                }
            }
            FocusSessionType.SHORT_BREAK -> "Short Break"
            FocusSessionType.LONG_BREAK -> "Long Break"
        }

        val statusText = if (data.state == FocusTimerState.PAUSED) {
            "Paused • ${data.formattedRemainingTime} remaining"
        } else {
            "${data.formattedRemainingTime} remaining"
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, if (data.linkedTaskId != null) "focus_mode" else "focus")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            2001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOCUS_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(data.state == FocusTimerState.RUNNING)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        // Add contextual action buttons
        if (data.state == FocusTimerState.RUNNING) {
            val pauseIntent = Intent(this, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                2002,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Pause", pausePendingIntent)
        } else if (data.state == FocusTimerState.PAUSED) {
            val resumeIntent = Intent(this, FocusTimerService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                2003,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumePendingIntent)
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_RESET
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2004,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Stop", stopPendingIntent)

        return builder.build()
    }

    override fun onDestroy() {
        stopTicker()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.personal.daymark.action.FOCUS_START"
        const val ACTION_PAUSE = "com.personal.daymark.action.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.personal.daymark.action.FOCUS_RESUME"
        const val ACTION_RESET = "com.personal.daymark.action.FOCUS_RESET"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}
