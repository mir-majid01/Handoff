package com.handoff.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.handoff.app.R

/**
 * Warm, code-drawn empty state: two handoff bubbles, one offering the other.
 * No stock imagery — everything is Canvas in the app palette.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val bubbleColor = MaterialTheme.colorScheme.primary
    val softColor = MaterialTheme.colorScheme.primaryContainer
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(148.dp)) {
            // Back bubble (outline only)
            drawRoundRect(
                color = softColor,
                topLeft = Offset(size.width * 0.10f, size.height * 0.18f),
                size = Size(size.width * 0.46f, size.height * 0.34f),
                cornerRadius = CornerRadius(26f, 26f),
                style = Stroke(width = 6f),
            )
            // Front bubble (filled)
            drawRoundRect(
                color = bubbleColor,
                topLeft = Offset(size.width * 0.42f, size.height * 0.46f),
                size = Size(size.width * 0.46f, size.height * 0.34f),
                cornerRadius = CornerRadius(26f, 26f),
            )
            // Baton line between them
            drawLine(
                color = bubbleColor,
                start = Offset(size.width * 0.34f, size.height * 0.42f),
                end = Offset(size.width * 0.52f, size.height * 0.58f),
                strokeWidth = 6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
