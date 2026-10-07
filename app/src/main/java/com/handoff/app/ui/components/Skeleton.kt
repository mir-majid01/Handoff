package com.handoff.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.handoff.app.ui.theme.HandoffMotion

/** Shimmer skeleton modifier: a soft highlight sweeping across the base color. */
fun Modifier.shimmer(baseColor: androidx.compose.ui.graphics.Color, highlightColor: androidx.compose.ui.graphics.Color): Modifier =
    composed {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "shimmerProgress",
        )
        background(
            Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(progress * 600f, 0f),
                end = Offset((progress + 1f) * 600f, 240f),
            ),
        )
    }

/** Skeleton rows mimicking the recent-handoffs list while loading. */
@Composable
fun SkeletonList(modifier: Modifier = Modifier, rows: Int = 3) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmer(base, highlight),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmer(base, highlight),
    )
}

/** Gentle entrance for list items: fade + slide up with the standard spring. */
fun Modifier.staggeredEntrance(index: Int): Modifier = composed {
    val transition = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 45L).coerceAtMost(360L))
        transition.animateTo(
            1f,
            androidx.compose.animation.core.tween(HandoffMotion.DURATION_MEDIUM, easing = HandoffMotion.EaseIOSEmphasized),
        )
    }
    graphicsLayer {
        alpha = transition.value
        translationY = (1f - transition.value) * 24f
    }
}
