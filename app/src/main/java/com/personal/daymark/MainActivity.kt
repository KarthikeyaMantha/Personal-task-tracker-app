package com.personal.daymark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.personal.daymark.core.notifications.TaskReminderReceiver
import com.personal.daymark.data.datastore.UserPreferences
import com.personal.daymark.ui.navigation.DaymarkScaffold
import com.personal.daymark.ui.theme.DaymarkTheme

class MainActivity : ComponentActivity() {

    private var incomingTargetTaskId by mutableStateOf<Long?>(null)
    private var incomingTargetRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val initialTaskId = intent?.getLongExtra(TaskReminderReceiver.EXTRA_TARGET_TASK_ID, -1L)
            ?.takeIf { it > 0 }
        incomingTargetTaskId = initialTaskId
        incomingTargetRoute = intent?.getStringExtra("navigate_to")

        val appContainer = (application as DaymarkApplication).container

        setContent {
            val context = LocalContext.current
            val userPrefs by appContainer.settingsRepository.userPreferencesFlow
                .collectAsState(initial = UserPreferences())

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (userPrefs.themePreference) {
                UserPreferences.THEME_LIGHT -> false
                UserPreferences.THEME_DARK -> true
                else -> isSystemDark
            }

            // Android 13+ (API 33+) Runtime Notification Permission Request
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled gracefully by system reliability checks */ }

                LaunchedEffect(Unit) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isGranted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            DaymarkTheme(darkTheme = isDarkTheme) {
                DaymarkScaffold(
                    targetTaskId = incomingTargetTaskId,
                    targetRoute = incomingTargetRoute
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val taskId = intent.getLongExtra(TaskReminderReceiver.EXTRA_TARGET_TASK_ID, -1L)
            ?.takeIf { it > 0 }
        if (taskId != null) {
            incomingTargetTaskId = taskId
        }
        val route = intent.getStringExtra("navigate_to")
        if (!route.isNullOrBlank()) {
            incomingTargetRoute = route
        }
    }

    override fun onResume() {
        super.onResume()
        com.personal.daymark.core.common.DaymarkDayManager.notifyDayChangedOrResumed()
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    DaymarkTheme {
        DaymarkScaffold()
    }
}
