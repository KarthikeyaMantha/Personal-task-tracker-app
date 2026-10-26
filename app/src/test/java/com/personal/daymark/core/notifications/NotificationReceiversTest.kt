package com.personal.daymark.core.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.daymark.DaymarkApplication
import com.personal.daymark.data.local.DaymarkDatabase
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.repository.TaskRepository
import com.personal.daymark.data.repository.TaskRepositoryImpl
import com.personal.daymark.di.AppContainer
import kotlinx.coroutines.runBlocking
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
import org.robolectric.shadows.ShadowNotificationManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = DaymarkApplication::class)
class NotificationReceiversTest {

    private lateinit var application: DaymarkApplication
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var database: DaymarkDatabase
    private lateinit var taskRepository: TaskRepository

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        NotificationHelper.createNotificationChannels(application)
        shadowOf(application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        database = application.container.database
        taskRepository = application.container.taskRepository
    }

    private fun waitUntil(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun testTaskReminderReceiver_firesNotificationForActiveTask() = runBlocking {
        // Create an active task in DB
        val taskId = taskRepository.createTask(
            TaskEntity(
                title = "Important Review",
                description = "Check reminder firing",
                dueDate = System.currentTimeMillis() + 60000L,
                isCompleted = false
            )
        )

        val receiver = TaskReminderReceiver()
        val intent = Intent(TaskReminderReceiver.ACTION_TASK_REMINDER).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, "Important Review")
            putExtra(TaskReminderReceiver.EXTRA_TASK_NOTE, "Check reminder firing")
        }

        receiver.onReceive(application, intent)

        waitUntil { shadowNotificationManager.allNotifications.isNotEmpty() }

        val notifications = shadowNotificationManager.allNotifications
        assertFalse("Notification should be posted for active task", notifications.isEmpty())
        val postedNotification = notifications.first()
        val shadow = shadowOf(postedNotification)
        assertEquals("Important Review", shadow.contentTitle)
        assertEquals("Check reminder firing", shadow.contentText)
        assertEquals(3, postedNotification.actions.size) // Mark Complete, Snooze 10m, Snooze 30m
        assertEquals("Mark Complete", postedNotification.actions[0].title)
        assertEquals("Snooze 10m", postedNotification.actions[1].title)
        assertEquals("Snooze 30m", postedNotification.actions[2].title)
    }

    @Test
    fun testTaskReminderReceiver_suppressesNotificationIfTaskCompleted() = runBlocking {
        val taskId = taskRepository.createTask(
            TaskEntity(
                title = "Completed Task",
                description = "Should not notify",
                dueDate = System.currentTimeMillis() + 60000L,
                isCompleted = true // already completed
            )
        )

        val receiver = TaskReminderReceiver()
        val intent = Intent(TaskReminderReceiver.ACTION_TASK_REMINDER).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, "Completed Task")
        }

        notificationManager.cancelAll()
        receiver.onReceive(application, intent)
        Thread.sleep(300)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Notification should NOT be posted for already completed task", notifications.isEmpty())
    }

    @Test
    fun testNotificationActionReceiver_markCompleteUpdatesDbAndCancels() = runBlocking {
        val taskId = taskRepository.createTask(
            TaskEntity(
                title = "Notification Complete Test",
                dueDate = System.currentTimeMillis() + 60000L,
                isCompleted = false
            )
        )

        val actionReceiver = NotificationActionReceiver()
        val completeIntent = Intent(NotificationActionReceiver.ACTION_COMPLETE_TASK).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
        }

        actionReceiver.onReceive(application, completeIntent)

        waitUntil {
            runBlocking { taskRepository.getTaskById(taskId)?.isCompleted == true }
        }

        val updatedTask = taskRepository.getTaskById(taskId)
        assertNotNull(updatedTask)
        assertTrue("Task should be marked completed in Room DB", updatedTask!!.isCompleted)
    }

    @Test
    fun testBootCompletedReceiver_reschedulesPendingFutureTasks() = runBlocking {
        val tomorrowTime = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val taskId = taskRepository.createTask(
            TaskEntity(
                title = "Reboot Resilience Task",
                dueDate = tomorrowTime,
                dueTime = 10 * 60, // 10:00 AM tomorrow
                isCompleted = false
            )
        )

        val bootReceiver = BootCompletedReceiver()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)

        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        bootReceiver.onReceive(application, bootIntent)

        waitUntil { shadowAlarmManager.scheduledAlarms.isNotEmpty() }

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertFalse("Pending alarms should be rescheduled on BOOT_COMPLETED", scheduledAlarms.isEmpty())
    }
}
