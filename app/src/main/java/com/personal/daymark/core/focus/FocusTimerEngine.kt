package com.personal.daymark.core.focus

/**
 * Pure calculation engine for the Focus timer.
 * Computes remaining time, monotonic / wall-clock epoch transitions, pause/resume durations,
 * and state transitions across process death or simulated time gaps.
 */
object FocusTimerEngine {

    /**
     * Computes the remaining milliseconds given the target epoch end time and current epoch time.
     */
    fun calculateRemaining(targetEndTimeEpochMillis: Long, currentEpochMillis: Long): Long {
        if (targetEndTimeEpochMillis <= 0L) return 0L
        return (targetEndTimeEpochMillis - currentEpochMillis).coerceAtLeast(0L)
    }

    /**
     * Computes the target epoch end time when starting or resuming a session.
     */
    fun computeTargetEndTime(remainingMillis: Long, currentEpochMillis: Long): Long {
        return currentEpochMillis + remainingMillis.coerceAtLeast(0L)
    }

    /**
     * Freezes a running timer into paused state, recording exact remaining milliseconds.
     */
    fun pause(state: FocusTimerData, currentEpochMillis: Long): FocusTimerData {
        if (state.state != FocusTimerState.RUNNING) return state
        val remaining = calculateRemaining(state.targetEndTimeEpochMillis, currentEpochMillis)
        return state.copy(
            state = FocusTimerState.PAUSED,
            remainingMillis = remaining,
            targetEndTimeEpochMillis = 0L
        )
    }

    /**
     * Resumes a paused timer by calculating a new target end timestamp.
     */
    fun resume(state: FocusTimerData, currentEpochMillis: Long): FocusTimerData {
        if (state.state != FocusTimerState.PAUSED && state.state != FocusTimerState.IDLE) return state
        val targetEnd = computeTargetEndTime(state.remainingMillis, currentEpochMillis)
        return state.copy(
            state = FocusTimerState.RUNNING,
            targetEndTimeEpochMillis = targetEnd
        )
    }

    /**
     * Calculates the actual minutes invested in a session so far (for honest partial recording).
     */
    fun calculateActualMinutesElapsed(totalDurationMillis: Long, remainingMillis: Long): Int {
        val elapsedMillis = (totalDurationMillis - remainingMillis).coerceAtLeast(0L)
        return (elapsedMillis / 60000L).toInt()
    }

    /**
     * Handles app restoration / process death / backgrounding:
     * Evaluates whether a session that was RUNNING completed in the background or is still active.
     * Returns the updated timer data and indicates if the session finished while away.
     */
    fun restoreState(state: FocusTimerData, currentEpochMillis: Long): Pair<FocusTimerData, Boolean> {
        if (state.state != FocusTimerState.RUNNING) {
            return Pair(state, false)
        }

        val remaining = calculateRemaining(state.targetEndTimeEpochMillis, currentEpochMillis)
        return if (remaining <= 0L) {
            // Session completed while backgrounded or during process death!
            Pair(
                state.copy(
                    state = FocusTimerState.COMPLETED,
                    remainingMillis = 0L
                ),
                true
            )
        } else {
            // Session is still actively running!
            Pair(
                state.copy(
                    remainingMillis = remaining
                ),
                false
            )
        }
    }

    /**
     * Transitions between Pomodoro phases:
     * FOCUS -> (after N sessions) LONG_BREAK or SHORT_BREAK
     * SHORT_BREAK / LONG_BREAK -> FOCUS
     */
    fun advanceToNextPhase(
        state: FocusTimerData,
        defaultFocusMinutes: Int = 25,
        shortBreakMinutes: Int = 5,
        longBreakMinutes: Int = 15,
        sessionsUntilLongBreak: Int = 4
    ): FocusTimerData {
        return if (state.sessionType == FocusSessionType.FOCUS) {
            val newCompletedCount = state.completedSessionsCount + 1
            val isLongBreak = (newCompletedCount % sessionsUntilLongBreak) == 0
            val nextType = if (isLongBreak) FocusSessionType.LONG_BREAK else FocusSessionType.SHORT_BREAK
            val nextDurationMin = if (isLongBreak) longBreakMinutes else shortBreakMinutes
            val totalMillis = nextDurationMin * 60 * 1000L

            FocusTimerData(
                state = FocusTimerState.IDLE,
                sessionType = nextType,
                totalDurationMillis = totalMillis,
                remainingMillis = totalMillis,
                targetEndTimeEpochMillis = 0L,
                sessionStartedAtEpochMillis = 0L,
                linkedTaskId = state.linkedTaskId,
                linkedTaskTitle = state.linkedTaskTitle,
                completedSessionsCount = newCompletedCount
            )
        } else {
            // Completed a break -> back to Focus
            val totalMillis = defaultFocusMinutes * 60 * 1000L
            FocusTimerData(
                state = FocusTimerState.IDLE,
                sessionType = FocusSessionType.FOCUS,
                totalDurationMillis = totalMillis,
                remainingMillis = totalMillis,
                targetEndTimeEpochMillis = 0L,
                sessionStartedAtEpochMillis = 0L,
                linkedTaskId = state.linkedTaskId,
                linkedTaskTitle = state.linkedTaskTitle,
                completedSessionsCount = state.completedSessionsCount
            )
        }
    }
}
