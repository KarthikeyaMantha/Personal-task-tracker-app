package com.personal.daymark.ui.screens.insights

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimalist, lightweight Compose bar chart for displaying daily metrics (Focus time, Tasks).
 * Clean, serene, zero heavy dependencies.
 */
@Composable
fun MinimalistBarChart(
    metrics: List<DailyMetric>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    todayBarColor: Color = MaterialTheme.colorScheme.secondary,
    chartHeight: Dp = 140.dp
) {
    val maxValue = remember(metrics) {
        val max = metrics.maxOfOrNull { it.value } ?: 0f
        if (max <= 0f) 1f else max
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Chart Bars Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            metrics.forEach { metric ->
                val targetFraction = if (maxValue > 0f) (metric.value / maxValue).coerceIn(0f, 1f) else 0f
                val animatedHeightFraction by animateFloatAsState(
                    targetValue = if (animateTrigger) targetFraction else 0f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "barHeightFraction"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Value label above bar
                    if (metric.value > 0f) {
                        Text(
                            text = metric.displayValue,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight(fraction = (animatedHeightFraction * 0.75f).coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (metric.isToday) todayBarColor else barColor.copy(
                                    alpha = if (metric.value > 0f) 0.85f else 0.2f
                                )
                            )
                    )
                }
            }
        }

        // Horizontal baseline separator
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        )

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis Day Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            metrics.forEach { metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = metric.dayLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (metric.isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (metric.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )

                    if (metric.isToday) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
