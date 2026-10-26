package com.personal.daymark.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.personal.daymark.ui.theme.DaymarkTheme

/**
 * Compact, serene progress card for the Today dashboard.
 * Visualizes real-time completion progress with animated indicators and calm typography.
 */
@Composable
fun DaymarkProgressCard(
    completedCount: Int,
    totalCount: Int,
    progressRatio: Float,
    modifier: Modifier = Modifier,
    useCircular: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progressAnimation"
    )

    val percentage = (animatedProgress * 100).toInt()

    val supportiveText = when {
        totalCount == 0 -> "All clear for today. Add tasks whenever ready."
        percentage == 100 -> "All daymark tasks complete. Enjoy the rest of your day."
        percentage >= 50 -> "Over halfway through. Keep up the calm momentum."
        percentage > 0 -> "Making steady progress, one deliberate step at a time."
        else -> "Ready to begin today's focus."
    }

    DaymarkCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Daymark",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportiveText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                if (useCircular) {
                    Spacer(modifier = Modifier.width(16.dp))
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sleek Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount of $totalCount completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (totalCount > completedCount) {
                    Text(
                        text = "${totalCount - completedCount} remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DaymarkProgressCardPreview() {
    DaymarkTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DaymarkProgressCard(
                completedCount = 2,
                totalCount = 3,
                progressRatio = 2f / 3f
            )
        }
    }
}
