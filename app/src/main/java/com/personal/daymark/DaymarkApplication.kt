package com.personal.daymark

import android.app.Application
import com.personal.daymark.di.AppContainer
import com.personal.daymark.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class DaymarkApplication : Application() {

    lateinit var container: AppContainer
        private set

    val appContainer: AppContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        com.personal.daymark.core.notifications.NotificationHelper.createNotificationChannels(this)
        com.personal.daymark.core.common.DaymarkDayManager.init(this)
        container.focusTimerManager.restoreStateOnLaunch()
        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.seedDefaultCategoriesIfEmpty()
        }

        // Observe changing medicine and habit data to keep Glance widget fresh reactively
        CoroutineScope(Dispatchers.IO).launch {
            container.database.taskDao().getAllMedicines()
                .distinctUntilChanged()
                .collect {
                    com.personal.daymark.widget.HealthWidgetUpdater.update(this@DaymarkApplication)
                }
        }
        CoroutineScope(Dispatchers.IO).launch {
            container.habitRepository.getActiveHabitsWithLogs()
                .distinctUntilChanged()
                .collect {
                    com.personal.daymark.widget.HealthWidgetUpdater.update(this@DaymarkApplication)
                }
        }
    }
}
