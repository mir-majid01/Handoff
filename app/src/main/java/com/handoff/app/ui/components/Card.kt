package com.handoff.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.handoff.app.ui.theme.CardShape

/**
 * The one card treatment used app-wide: flat surface, 1dp hairline border,
 * and a single soft shadow level. Content gets [contentPadding] inner padding.
 */
@Composable
fun Modifier.cozyCard(shape: Shape = CardShape): Modifier = this
    .shadow(elevation = 3.dp, shape = shape, clip = false)
    .clip(shape)
    .background(MaterialTheme.colorScheme.surface)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cozyCard(shape)
            .padding(contentPadding),
        content = content,
    )
}
