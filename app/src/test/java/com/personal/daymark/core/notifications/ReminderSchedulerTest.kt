package com.personal.daymark.core.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var scheduler: AlarmManagerReminderScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        scheduler = AlarmManagerReminderScheduler(context)

        // Ensure notification channels are set up
        NotificationHelper.createNotificationChannels(context)
    }

    @Test
    fun testNotificationChannelsCreated() {
        val reminderChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_TASK_REMINDERS)
        assertNotNull("Task Reminders channel should be created", reminderChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, reminderChannel.importance)

        val focusChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_FOCUS_TIMER)
        assertNotNull("Focus Timer channel should be created", focusChannel)
    }

    @Test
    fun testCalculateTriggerTimeMillis_withExplicitTime() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueDateMillis = calendar.timeInMillis
        val dueTimeMinutes = 10 * 60 + 30 // 10:30 AM = 630 minutes

        val calculated = AlarmManagerReminderScheduler.calculateTriggerTimeMillis(dueDateMillis, dueTimeMinutes)
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = calculated
        }

        assertEquals(2026, expectedCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, expectedCal.get(Calendar.MONTH))
        assertEquals(26, expectedCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, expectedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, expectedCal.get(Calendar.MINUTE))
        assertEquals(0, expectedCal.get(Calendar.SECOND))
    }

    @Test
    fun testCalculateTriggerTimeMillis_defaultToMorningWhenNoTime() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueDateMillis = calendar.timeInMillis

        val calculated = AlarmManagerReminderScheduler.calculateTriggerTimeMillis(dueDateMillis, null)
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = calculated
        }

        assertEquals(9, expectedCal.get(Calendar.HOUR_OF_DAY)) // Default 9:00 AM
        assertEquals(0, expectedCal.get(Calendar.MINUTE))
    }

    @Test
    fun testScheduleReminder_schedulesAlarmForFutureTask() {
        val futureTime = System.currentTimeMillis() + 60000L // 1 minute in future
        val task = TaskEntity(
            id = 42L,
            title = "Review Architecture",
            description = "Ensure exact alarm compliance",
            dueDate = futureTime,
            dueTime = null
        )

        scheduler.scheduleReminder(
            taskId = task.id,
            title = task.title,
            description = task.description,
            triggerTimeMillis = futureTime
        )

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("Alarm should be scheduled in AlarmManager", scheduledAlarms.isEmpty())

        val scheduled = scheduledAlarms.first()
        assertEquals(futureTime, scheduled.triggerAtTime)

        val shadowPendingIntent = shadowOf(scheduled.operation)
        val savedIntent = shadowPendingIntent.savedIntent
        assertEquals(TaskReminderReceiver.ACTION_TASK_REMINDER, savedIntent.action)
        assertEquals(42L, savedIntent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L))
        assertEquals("Review Architecture", savedIntent.getStringExtra(TaskReminderReceiver.EXTRA_TASK_TITLE))
    }

    @Test
    fun testCancelReminder_removesScheduledAlarm() {
        val futureTime = System.currentTimeMillis() + 120000L
        scheduler.scheduleReminder(
            taskId = 99L,
            title = "Disposable Task",
            description = null,
            triggerTimeMillis = futureTime
        )

        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelReminder(99L)

        // Shadow AlarmManager should register cancellation of the matching pending intent
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        val alarmForTask99 = remainingAlarms.find {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L) == 99L
        }
        assertTrue("Alarm for task 99 should be cancelled", alarmForTask99 == null || shadowOf(alarmForTask99.operation).isCanceled)
    }

    @Test
    fun testScheduleSnooze_schedulesNewAlarmWithSnoozeTitle() {
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes
        scheduler.scheduleSnooze(
            taskId = 101L,
            title = "Standup Meeting",
            snoozeTimeMillis = snoozeTime
        )

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("Snooze alarm should be scheduled", scheduledAlarms.isEmpty())

        val scheduled = scheduledAlarms.find {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L) == 101L
        }
        assertNotNull("Snooze alarm for task 101 must exist", scheduled)
        assertEquals(snoozeTime, scheduled!!.triggerAtTime)
    }

    @Test
    fun testSystemReliabilityHelper_permissionChecks() {
        // In Robolectric test context
        val exactAlarmsCanSchedule = SystemReliabilityHelper.canScheduleExactAlarms(context)
        // Robolectric returns default boolean according to SDK
        assertNotNull(exactAlarmsCanSchedule)

        val notifIntent = SystemReliabilityHelper.createNotificationSettingsIntent(context)
        assertNotNull(notifIntent)

        val alarmIntent = SystemReliabilityHelper.createExactAlarmSettingsIntent(context)
        assertNotNull(alarmIntent)

        val batteryIntent = SystemReliabilityHelper.createBatteryOptimizationIntent(context)
        assertNotNull(batteryIntent)
    }
}
