package com.handoff.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.handoff.app.ui.theme.HandoffMotion

/**
 * iOS-style press feedback: scales to 0.97 while pressed with a 0.8-damped
 * spring. Attach to anything tappable; pair with the same interactionSource
 * passed to the clickable to share press state.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = HandoffMotion.springSnappy,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.pressScale(pressedScale: Float = 0.97f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    pressScale(interactionSource, pressedScale)
}

/** Convenience: a fresh interaction source remembered per call site. */
@Composable
fun rememberPressInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }
