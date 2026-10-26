package com.personal.daymark.core.focus

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.personal.daymark.core.notifications.NotificationHelper
import com.personal.daymark.data.repository.FocusRepository
import com.personal.daymark.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Singleton state manager coordinating active focus sessions, persistence across
 * process death, Room database logging, and Foreground Service lifecycle.
 */
class FocusTimerManager(
    private val context: Context,
    private val focusRepository: FocusRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _timerState = MutableStateFlow(loadInitialState())
    val timerState: StateFlow<FocusTimerData> = _timerState.asStateFlow()

    init {
        restoreStateOnLaunch()
    }

    private fun loadInitialState(): FocusTimerData {
        val stateName = prefs.getString(KEY_STATE, FocusTimerState.IDLE.name) ?: FocusTimerState.IDLE.name
        val state = try {
            FocusTimerState.valueOf(stateName)
        } catch (_: Exception) {
            FocusTimerState.IDLE
        }

        val typeName = prefs.getString(KEY_SESSION_TYPE, FocusSessionType.FOCUS.name) ?: FocusSessionType.FOCUS.name
        val sessionType = try {
            FocusSessionType.valueOf(typeName)
        } catch (_: Exception) {
            FocusSessionType.FOCUS
        }

        val totalDuration = prefs.getLong(KEY_TOTAL_DURATION, 25 * 60 * 1000L)
        val remaining = prefs.getLong(KEY_REMAINING, totalDuration)
        val targetEndTime = prefs.getLong(KEY_TARGET_END_TIME, 0L)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val taskId = if (prefs.contains(KEY_TASK_ID)) prefs.getLong(KEY_TASK_ID, -1L).takeIf { it > 0 } else null
        val taskTitle = prefs.getString(KEY_TASK_TITLE, null)
        val completedCount = prefs.getInt(KEY_COMPLETED_COUNT, 0)

        return FocusTimerData(
            state = state,
            sessionType = sessionType,
            totalDurationMillis = totalDuration,
            remainingMillis = remaining,
            targetEndTimeEpochMillis = targetEndTime,
            sessionStartedAtEpochMillis = startedAt,
            linkedTaskId = taskId,
            linkedTaskTitle = taskTitle,
            completedSessionsCount = completedCount
        )
    }

    private fun persistState(data: FocusTimerData) {
        prefs.edit()
            .putString(KEY_STATE, data.state.name)
            .putString(KEY_SESSION_TYPE, data.sessionType.name)
            .putLong(KEY_TOTAL_DURATION, data.totalDurationMillis)
            .putLong(KEY_REMAINING, data.remainingMillis)
            .putLong(KEY_TARGET_END_TIME, data.targetEndTimeEpochMillis)
            .putLong(KEY_STARTED_AT, data.sessionStartedAtEpochMillis)
            .apply {
                if (data.linkedTaskId != null) {
                    putLong(KEY_TASK_ID, data.linkedTaskId)
                } else {
                    remove(KEY_TASK_ID)
                }
            }
            .putString(KEY_TASK_TITLE, data.linkedTaskTitle)
            .putInt(KEY_COMPLETED_COUNT, data.completedSessionsCount)
            .apply()
    }

    /**
     * Checks if a session finished while process was away, or restores active countdown.
     */
    fun restoreStateOnLaunch() {
        val current = _timerState.value
        if (current.state == FocusTimerState.RUNNING) {
            val now = System.currentTimeMillis()
            val (updated, hasCompleted) = FocusTimerEngine.restoreState(current, now)
            if (hasCompleted) {
                // Session expired while app was dead or backgrounded!
                completeSession(updated)
            } else {
                _timerState.value = updated
                persistState(updated)
                startService()
            }
        }
    }

    /**
     * Starts or restarts the focus/break timer.
     */
    fun startTimer(
        durationMinutes: Int? = null,
        sessionType: FocusSessionType? = null,
        taskId: Long? = null,
        taskTitle: String? = null
    ) {
        scope.launch {
            val userPrefs = settingsRepository.userPreferencesFlow.first()
            val type = sessionType ?: _timerState.value.sessionType
            val durationMin = durationMinutes ?: when (type) {
                FocusSessionType.FOCUS -> userPrefs.defaultFocusDurationMinutes
                FocusSessionType.SHORT_BREAK -> userPrefs.shortBreakDurationMinutes
                FocusSessionType.LONG_BREAK -> userPrefs.longBreakDurationMinutes
            }

            val totalMillis = durationMin * 60 * 1000L
            val now = System.currentTimeMillis()
            val targetEnd = FocusTimerEngine.computeTargetEndTime(totalMillis, now)

            val effectiveTaskId = taskId ?: _timerState.value.linkedTaskId
            val effectiveTaskTitle = taskTitle ?: _timerState.value.linkedTaskTitle

            val newState = FocusTimerData(
                state = FocusTimerState.RUNNING,
                sessionType = type,
                totalDurationMillis = totalMillis,
                remainingMillis = totalMillis,
                targetEndTimeEpochMillis = targetEnd,
                sessionStartedAtEpochMillis = now,
                linkedTaskId = effectiveTaskId,
                linkedTaskTitle = effectiveTaskTitle,
                completedSessionsCount = _timerState.value.completedSessionsCount
            )

            _timerState.value = newState
            persistState(newState)
            startService()
        }
    }

    /**
     * Pauses the timer and stores remaining milliseconds.
     */
    fun pauseTimer() {
        val current = _timerState.value
        if (current.state != FocusTimerState.RUNNING) return
        val now = System.currentTimeMillis()
        val paused = FocusTimerEngine.pause(current, now)
        _timerState.value = paused
        persistState(paused)
        sendServiceAction(FocusTimerService.ACTION_PAUSE)
    }

    /**
     * Resumes the paused timer.
     */
    fun resumeTimer() {
        val current = _timerState.value
        if (current.state != FocusTimerState.PAUSED && current.state != FocusTimerState.IDLE) return
        val now = System.currentTimeMillis()
        val resumed = FocusTimerEngine.resume(current, now)
        _timerState.value = resumed
        persistState(resumed)
        startService()
    }

    /**
     * Resets/cancels the timer. If recordPartial is true and >= 1 minute elapsed,
     * logs the honest partial duration to Room. Never fabricates completed sessions.
     */
    fun resetTimer(recordPartial: Boolean = false) {
        val current = _timerState.value
        if (recordPartial && current.state != FocusTimerState.IDLE) {
            val elapsedMinutes = FocusTimerEngine.calculateActualMinutesElapsed(
                current.totalDurationMillis,
                current.remainingMillis
            )
            if (elapsedMinutes >= 1) {
                scope.launch {
                    focusRepository.recordFocusSession(
                        taskId = current.linkedTaskId,
                        durationMinutes = elapsedMinutes,
                        plannedDurationMinutes = (current.totalDurationMillis / 60000L).toInt(),
                        sessionType = current.sessionType.name,
                        startedAt = current.sessionStartedAtEpochMillis,
                        completedAt = System.currentTimeMillis(),
                        notes = "Partial session (stopped early: $elapsedMinutes min)"
                    )
                }
            }
        }

        val resetState = FocusTimerData(
            state = FocusTimerState.IDLE,
            sessionType = current.sessionType,
            totalDurationMillis = current.totalDurationMillis,
            remainingMillis = current.totalDurationMillis,
            targetEndTimeEpochMillis = 0L,
            sessionStartedAtEpochMillis = 0L,
            linkedTaskId = current.linkedTaskId,
            linkedTaskTitle = current.linkedTaskTitle,
            completedSessionsCount = current.completedSessionsCount
        )

        _timerState.value = resetState
        persistState(resetState)
        stopService()
    }

    /**
     * Skips current phase and transitions to next Pomodoro phase.
     */
    fun skipPhase() {
        scope.launch {
            val userPrefs = settingsRepository.userPreferencesFlow.first()
            val next = FocusTimerEngine.advanceToNextPhase(
                state = _timerState.value,
                defaultFocusMinutes = userPrefs.defaultFocusDurationMinutes,
                shortBreakMinutes = userPrefs.shortBreakDurationMinutes,
                longBreakMinutes = userPrefs.longBreakDurationMinutes,
                sessionsUntilLongBreak = userPrefs.sessionsUntilLongBreak
            )
            _timerState.value = next
            persistState(next)
            stopService()
        }
    }

    /**
     * Associates or disassociates an existing task with the active focus session.
     */
    fun linkTask(taskId: Long?, taskTitle: String?) {
        val updated = _timerState.value.copy(
            linkedTaskId = taskId,
            linkedTaskTitle = taskTitle
        )
        _timerState.value = updated
        persistState(updated)
    }

    /**
     * Called by Service or coroutine ticker every second when running.
     */
    fun tick(): FocusTimerData {
        val current = _timerState.value
        if (current.state != FocusTimerState.RUNNING) return current

        val now = System.currentTimeMillis()
        val remaining = FocusTimerEngine.calculateRemaining(current.targetEndTimeEpochMillis, now)

        return if (remaining <= 0L) {
            val completed = current.copy(state = FocusTimerState.COMPLETED, remainingMillis = 0L)
            completeSession(completed)
            completed
        } else {
            val updated = current.copy(remainingMillis = remaining)
            _timerState.value = updated
            updated
        }
    }

    /**
     * Completes the current session, writes to Room, posts notification, and advances phase.
     */
    private fun completeSession(completedData: FocusTimerData) {
        scope.launch {
            val durationMinutes = (completedData.totalDurationMillis / 60000L).toInt()
            focusRepository.recordFocusSession(
                taskId = completedData.linkedTaskId,
                durationMinutes = durationMinutes,
                plannedDurationMinutes = durationMinutes,
                sessionType = completedData.sessionType.name,
                startedAt = completedData.sessionStartedAtEpochMillis,
                completedAt = System.currentTimeMillis(),
                notes = if (completedData.sessionType == FocusSessionType.FOCUS) "Completed Focus Session" else "Completed Break"
            )

            // Notify user of completion using existing channel system
            val title = if (completedData.sessionType == FocusSessionType.FOCUS) {
                "Focus Session Completed!"
            } else {
                "Break Finished!"
            }
            val msg = if (completedData.sessionType == FocusSessionType.FOCUS) {
                "Well done! Take a mindful rest before your next session."
            } else {
                "Ready to focus again? Start your next session."
            }
            NotificationHelper.showFocusCompletionNotification(context, title, msg)

            // Advance phase
            val userPrefs = settingsRepository.userPreferencesFlow.first()
            val nextState = FocusTimerEngine.advanceToNextPhase(
                state = completedData,
                defaultFocusMinutes = userPrefs.defaultFocusDurationMinutes,
                shortBreakMinutes = userPrefs.shortBreakDurationMinutes,
                longBreakMinutes = userPrefs.longBreakDurationMinutes,
                sessionsUntilLongBreak = userPrefs.sessionsUntilLongBreak
            )

            _timerState.value = nextState
            persistState(nextState)
            stopService()
        }
    }

    private fun startService() {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            // Handled gracefully if exact constraints or background restrictions apply
        }
    }

    private fun sendServiceAction(actionStr: String) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = actionStr
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
            // Ignored if service is not running
        }
    }

    private fun stopService() {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESET
        }
        try {
            context.stopService(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }

    companion object {
        private const val PREFS_NAME = "daymark_focus_timer_state"
        private const val KEY_STATE = "state"
        private const val KEY_SESSION_TYPE = "session_type"
        private const val KEY_TOTAL_DURATION = "total_duration"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_TARGET_END_TIME = "target_end_time"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_TASK_TITLE = "task_title"
        private const val KEY_COMPLETED_COUNT = "completed_count"
    }
}
