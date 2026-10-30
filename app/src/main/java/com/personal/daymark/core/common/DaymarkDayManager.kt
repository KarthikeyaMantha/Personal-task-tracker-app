package com.personal.daymark.core.common

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Manages reactive date changes (midnight rollover, device time change, app resume)
 * across Today, Habits, and the rest of the app.
 */
object DaymarkDayManager {
    private val _dayEpoch = MutableStateFlow(System.currentTimeMillis())
    val dayEpoch = _dayEpoch.asStateFlow()

    val currentDay: LocalDate
        get() = LocalDate.now()

    fun notifyDayChangedOrResumed() {
        _dayEpoch.value = System.currentTimeMillis()
    }

    fun init(application: Application, scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        // BroadcastReceiver for system date/time/timezone updates
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                notifyDayChangedOrResumed()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                application,
                receiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            try {
                application.registerReceiver(receiver, filter)
            } catch (_: Exception) {}
        }

        // Midnight coroutine ticker
        scope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisUntilMidnight = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000L)
                delay(millisUntilMidnight + 200L)
                notifyDayChangedOrResumed()
            }
        }
    }
}
