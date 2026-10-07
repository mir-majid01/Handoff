package com.handoff.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handoff.app.ui.theme.PillShape

/**
 * iOS segmented control: pill track, equal-width segments, sliding thumb with
 * a hairline border and soft shadow. Single-select, 14sp labels, no clipping.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val trackPadding = 3.dp
    val thumbHeight = height - trackPadding * 2
    val selected = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(trackPadding),
    ) {
        options.forEachIndexed { index, option ->
            SegmentCell(
                label = option,
                isSelected = index == selected,
                thumbHeight = thumbHeight,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun RowScope.SegmentCell(
    label: String,
    isSelected: Boolean,
    thumbHeight: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(thumbHeight)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(2.dp, PillShape, clip = false)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, PillShape)
                } else {
                    Modifier
                },
            )
            .clip(PillShape)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
