package com.personal.daymark.core.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerEngineTest {

    @Test
    fun calculateRemaining_computesCorrectRemainingMillis() {
        val targetEndTime = 100_000L
        val currentNow = 40_000L

        val remaining = FocusTimerEngine.calculateRemaining(targetEndTime, currentNow)
        assertEquals(60_000L, remaining)

        // When current time exceeds target end time -> returns 0
        val expired = FocusTimerEngine.calculateRemaining(targetEndTime, 110_000L)
        assertEquals(0L, expired)
    }

    @Test
    fun pauseAndResume_maintainsExactRemainingWithoutDrift() {
        val startEpoch = 1_000_000L
        val totalDuration = 25 * 60 * 1000L // 25 min = 1,500,000 ms
        val targetEnd = startEpoch + totalDuration

        val runningState = FocusTimerData(
            state = FocusTimerState.RUNNING,
            sessionType = FocusSessionType.FOCUS,
            totalDurationMillis = totalDuration,
            remainingMillis = totalDuration,
            targetEndTimeEpochMillis = targetEnd,
            sessionStartedAtEpochMillis = startEpoch
        )

        // User pauses after 5 minutes (300,000 ms)
        val pauseEpoch = startEpoch + 300_000L
        val pausedState = FocusTimerEngine.pause(runningState, pauseEpoch)

        assertEquals(FocusTimerState.PAUSED, pausedState.state)
        assertEquals(1_200_000L, pausedState.remainingMillis) // 20 min remaining
        assertEquals(0L, pausedState.targetEndTimeEpochMillis)

        // User stays paused for 10 minutes, then resumes at pauseEpoch + 600,000 ms
        val resumeEpoch = pauseEpoch + 600_000L
        val resumedState = FocusTimerEngine.resume(pausedState, resumeEpoch)

        assertEquals(FocusTimerState.RUNNING, resumedState.state)
        assertEquals(1_200_000L, resumedState.remainingMillis)
        // New target end must be exactly resumeEpoch + remainingMillis
        assertEquals(resumeEpoch + 1_200_000L, resumedState.targetEndTimeEpochMillis)
    }

    @Test
    fun simulatedTimeGap_activeTimerAdvancesCorrectly() {
        val startEpoch = 5_000_000L
        val totalDuration = 25 * 60 * 1000L
        val targetEnd = startEpoch + totalDuration

        val runningState = FocusTimerData(
            state = FocusTimerState.RUNNING,
            sessionType = FocusSessionType.FOCUS,
            totalDurationMillis = totalDuration,
            remainingMillis = totalDuration,
            targetEndTimeEpochMillis = targetEnd,
            sessionStartedAtEpochMillis = startEpoch
        )

        // Simulate 12 minutes elapsed while screen is locked
        val twelveMinutesLater = startEpoch + (12 * 60 * 1000L)
        val (restored, completed) = FocusTimerEngine.restoreState(runningState, twelveMinutesLater)

        assertFalse("Timer is not completed yet", completed)
        assertEquals(FocusTimerState.RUNNING, restored.state)
        assertEquals(13 * 60 * 1000L, restored.remainingMillis) // 13 min remaining
    }

    @Test
    fun processDeath_completedWhileAway_detectsSessionCompletion() {
        val startEpoch = 10_000_000L
        val totalDuration = 25 * 60 * 1000L // 25 min
        val targetEnd = startEpoch + totalDuration

        val runningState = FocusTimerData(
            state = FocusTimerState.RUNNING,
            sessionType = FocusSessionType.FOCUS,
            totalDurationMillis = totalDuration,
            remainingMillis = totalDuration,
            targetEndTimeEpochMillis = targetEnd,
            sessionStartedAtEpochMillis = startEpoch
        )

        // App was killed or suspended for 30 minutes (past target end)
        val thirtyMinutesLater = startEpoch + (30 * 60 * 1000L)
        val (restored, completed) = FocusTimerEngine.restoreState(runningState, thirtyMinutesLater)

        assertTrue("Session completed while away must be detected", completed)
        assertEquals(FocusTimerState.COMPLETED, restored.state)
        assertEquals(0L, restored.remainingMillis)
    }

    @Test
    fun processDeath_partiallyRemaining_restoresAccurateRemainingTime() {
        val startEpoch = 20_000_000L
        val totalDuration = 25 * 60 * 1000L
        val targetEnd = startEpoch + totalDuration

        val runningState = FocusTimerData(
            state = FocusTimerState.RUNNING,
            sessionType = FocusSessionType.FOCUS,
            totalDurationMillis = totalDuration,
            remainingMillis = totalDuration,
            targetEndTimeEpochMillis = targetEnd,
            sessionStartedAtEpochMillis = startEpoch
        )

        // App relaunch 18 minutes in
        val eighteenMinLater = startEpoch + (18 * 60 * 1000L)
        val (restored, completed) = FocusTimerEngine.restoreState(runningState, eighteenMinLater)

        assertFalse("Session is still active", completed)
        assertEquals(FocusTimerState.RUNNING, restored.state)
        assertEquals(7 * 60 * 1000L, restored.remainingMillis) // Exactly 7 min remaining, no reset to 25:00!
    }

    @Test
    fun phaseTransition_advancesToBreakAndLongBreakEvery4th() {
        val baseFocus = FocusTimerData(
            state = FocusTimerState.COMPLETED,
            sessionType = FocusSessionType.FOCUS,
            completedSessionsCount = 0
        )

        // 1st completion -> Short Break (completedSessionsCount becomes 1)
        val after1st = FocusTimerEngine.advanceToNextPhase(baseFocus)
        assertEquals(FocusSessionType.SHORT_BREAK, after1st.sessionType)
        assertEquals(5 * 60 * 1000L, after1st.totalDurationMillis)
        assertEquals(1, after1st.completedSessionsCount)

        // Complete the break -> back to Focus
        val backToFocus = FocusTimerEngine.advanceToNextPhase(after1st)
        assertEquals(FocusSessionType.FOCUS, backToFocus.sessionType)
        assertEquals(25 * 60 * 1000L, backToFocus.totalDurationMillis)

        // Simulate 3rd focus completion (completedCount was 3 -> now 4)
        val thirdFocus = FocusTimerData(
            state = FocusTimerState.COMPLETED,
            sessionType = FocusSessionType.FOCUS,
            completedSessionsCount = 3
        )
        val after4th = FocusTimerEngine.advanceToNextPhase(thirdFocus, sessionsUntilLongBreak = 4)
        assertEquals("4th completed session must transition to LONG_BREAK", FocusSessionType.LONG_BREAK, after4th.sessionType)
        assertEquals(15 * 60 * 1000L, after4th.totalDurationMillis)
        assertEquals(4, after4th.completedSessionsCount)
    }

    @Test
    fun actualMinutesElapsed_calculatesHonestDuration() {
        val total25Min = 25 * 60 * 1000L
        val remaining11Min = 11 * 60 * 1000L

        val elapsed = FocusTimerEngine.calculateActualMinutesElapsed(total25Min, remaining11Min)
        assertEquals("Should calculate exactly 14 minutes elapsed", 14, elapsed)

        // Less than 1 minute
        val elapsedShort = FocusTimerEngine.calculateActualMinutesElapsed(total25Min, total25Min - 30_000L)
        assertEquals("Less than 1 minute should be 0", 0, elapsedShort)
    }
}
