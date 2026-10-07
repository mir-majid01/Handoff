package com.handoff.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.handoff.app.ui.theme.ButtonShape
import com.handoff.app.ui.theme.PillShape

/**
 * Button set: Primary = pill sage, Secondary = tonal neutral, Ghost = text.
 * All include press-scale feedback via their interaction sources.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = rememberPressInteraction(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(interactionSource),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = rememberPressInteraction(),
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(interactionSource),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {},
) {
    val interactionSource = rememberPressInteraction()
    TextButton(
        onClick = onClick,
        shape = ButtonShape,
        interactionSource = interactionSource,
        modifier = modifier.pressScale(interactionSource),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

/** Circular 48dp icon button for top bars; content description required. */
@Composable
fun IconButtonRound(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = rememberPressInteraction()
    androidx.compose.material3.IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(48.dp)
            .pressScale(interactionSource),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
