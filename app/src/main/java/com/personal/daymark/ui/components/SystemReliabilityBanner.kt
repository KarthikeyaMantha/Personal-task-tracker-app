package com.personal.daymark.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.daymark.core.notifications.SystemReliabilityHelper

sealed class ReliabilityIssue(
    val title: String,
    val description: String,
    val actionText: String,
    val icon: ImageVector
) {
    data object NotificationsDisabled : ReliabilityIssue(
        title = "Notifications Disabled",
        description = "Enable notifications so Daymark can alert you when tasks are due.",
        actionText = "Enable",
        icon = Icons.Default.NotificationsOff
    )

    data object ExactAlarmsRestricted : ReliabilityIssue(
        title = "Exact Alarms Restricted",
        description = "Reminders might be delayed by the system without exact alarm permission.",
        actionText = "Allow",
        icon = Icons.Default.Alarm
    )

    data object BatteryOptimizationActive : ReliabilityIssue(
        title = "Battery Optimization Active",
        description = "Allow unrestricted background battery for reliable on-time alarms on Samsung & Android devices.",
        actionText = "Unrestrict",
        icon = Icons.Default.BatteryAlert
    )
}

/**
 * An unobtrusive, calm system banner shown on dashboards when OS restrictions
 * might hinder notification reliability.
 */
@Composable
fun SystemReliabilityBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDismissed by remember { mutableStateOf(false) }
    var activeIssue by remember { mutableStateOf<ReliabilityIssue?>(null) }

    fun checkStatus() {
        val notificationsOk = SystemReliabilityHelper.areNotificationsEnabled(context)
        val exactAlarmsOk = SystemReliabilityHelper.canScheduleExactAlarms(context)
        val batteryOk = SystemReliabilityHelper.isIgnoringBatteryOptimizations(context)

        activeIssue = when {
            !notificationsOk -> ReliabilityIssue.NotificationsDisabled
            !exactAlarmsOk -> ReliabilityIssue.ExactAlarmsRestricted
            !batteryOk -> ReliabilityIssue.BatteryOptimizationActive
            else -> null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val issue = activeIssue
    AnimatedVisibility(
        visible = issue != null && !isDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (issue != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = issue.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    when (issue) {
                                        is ReliabilityIssue.NotificationsDisabled -> {
                                            context.startActivity(
                                                SystemReliabilityHelper.createNotificationSettingsIntent(context)
                                            )
                                        }
                                        is ReliabilityIssue.ExactAlarmsRestricted -> {
                                            context.startActivity(
                                                SystemReliabilityHelper.createExactAlarmSettingsIntent(context)
                                            )
                                        }
                                        is ReliabilityIssue.BatteryOptimizationActive -> {
                                            context.startActivity(
                                                SystemReliabilityHelper.createBatteryOptimizationIntent(context)
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = issue.actionText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss warning",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
