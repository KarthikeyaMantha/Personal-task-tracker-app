package com.personal.daymark.ui.screens.today

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * End-of-Day Shutdown Ritual modal sheet hosting the Smart Visual Migration screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndOfDayReviewSheet(
    viewModel: EndOfDayViewModel,
    onDismiss: () -> Unit,
    onShutdownCompleted: (message: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        MigrationScreen(
            viewModel = viewModel,
            onDismiss = onDismiss,
            onShutdownCompleted = onShutdownCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        )
    }
}
