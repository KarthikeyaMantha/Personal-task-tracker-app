package com.personal.daymark.core.focus

import java.util.Locale

data class FocusTimerData(
    val state: FocusTimerState = FocusTimerState.IDLE,
    val sessionType: FocusSessionType = FocusSessionType.FOCUS,
    val totalDurationMillis: Long = 25 * 60 * 1000L,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val targetEndTimeEpochMillis: Long = 0L,
    val sessionStartedAtEpochMillis: Long = 0L,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null,
    val completedSessionsCount: Int = 0
) {
    val progressFraction: Float
        get() = if (totalDurationMillis > 0L) {
            1f - (remainingMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemainingTime: String
        get() {
            val totalSeconds = (remainingMillis + 999) / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
}
