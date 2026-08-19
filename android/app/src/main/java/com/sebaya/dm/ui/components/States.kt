package com.sebaya.dm.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sebaya.dm.ui.UiState

/**
 * Renders the right thing for a [UiState]: a loading placeholder, a retryable
 * error, an empty message, or the [content] for a success.
 */
@Composable
fun <T> StateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "Belum ada data.",
    loading: @Composable () -> Unit = { LoadingList() },
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Loading -> Box(modifier.fillMaxSize()) { loading() }
        is UiState.Error -> Box(modifier.fillMaxSize()) {
            MessageState(Icons.Filled.CloudOff, state.message, onRetry)
        }
        is UiState.Empty -> Box(modifier.fillMaxSize()) {
            MessageState(Icons.Filled.Inbox, emptyText, null)
        }
        is UiState.Success -> Box(modifier) { content(state.data) }
    }
}

@Composable
private fun MessageState(icon: ImageVector, message: String, onRetry: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.height(44.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Coba lagi") }
        }
    }
}

/** A few shimmering card placeholders for loading lists. */
@Composable
fun LoadingList(count: Int = 4) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { ShimmerBox(Modifier.fillMaxWidth().height(84.dp)) }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier.background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
            RoundedCornerShape(16.dp),
        ),
    )
}
