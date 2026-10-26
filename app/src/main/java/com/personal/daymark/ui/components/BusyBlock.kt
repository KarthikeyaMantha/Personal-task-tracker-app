package com.personal.daymark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.daymark.data.local.entity.TimeBlockEntity
import java.util.Locale

/**
 * Visual timeline block representing a protected "Busy Block" (e.g. Lunch, Meetings).
 * Rendered in a muted, neutral color with a lock icon to visually distinguish from actionable tasks.
 */
@Composable
fun BusyBlock(
    busyBlock: TimeBlockEntity,
    height: Dp,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val startMin = busyBlock.getStartMinutes()
    val endMin = busyBlock.getEndMinutes()
    val endH = (endMin / 60) % 24
    val endM = endMin % 60
    val endTimeStr = String.format(Locale.US, "%02d:%02d", endH, endM)
    val timeRangeStr = "${busyBlock.startTime} – $endTimeStr • ${busyBlock.durationMinutes}m"

    val shape = RoundedCornerShape(8.dp)
    // Muted slate/neutral color styling
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muted vertical accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(2.dp))
                    .background(contentColor.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Icon + Title & Time
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Busy Block (Protected Time)",
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = busyBlock.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (height >= 40.dp) {
                    Text(
                        text = timeRangeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = contentColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDeleteClick != null) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Delete busy block",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
