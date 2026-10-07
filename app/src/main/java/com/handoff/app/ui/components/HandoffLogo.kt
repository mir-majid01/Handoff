package com.handoff.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.handoff.app.ui.theme.AccentSage
import com.handoff.app.util.Haptics
import kotlinx.coroutines.launch

/**
 * Brand mark, drawn entirely in code: a 44dp sage-gradient tile holding two cream
 * speech bubbles, the back one handing a conversation into the front one (arrow
 * notch at the overlap). Entrance springs the bubbles together; a slow idle breath
 * and a tap bounce keep it alive. Under reduce-motion (or in tests) a single static
 * frame is composed — no perpetual animation.
 */
@Composable
fun HandoffLogoMark(
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    if (rememberReduceMotion()) {
        Canvas(
            modifier = modifier.size(44.dp),
        ) { drawLogoMark(enter = 1f) }
    } else {
        AnimatedLogoMark(modifier, onTap)
    }
}

@Composable
private fun AnimatedLogoMark(
    modifier: Modifier,
    onTap: (() -> Unit)?,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    val bounce = remember { Animatable(1f) }

    val transition = rememberInfiniteTransition(label = "logoBreathe")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )

    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
    }

    Canvas(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = breathe * bounce.value
                scaleY = breathe * bounce.value
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    Haptics.tap(view)
                    onTap?.invoke()
                    scope.launch {
                        bounce.animateTo(0.9f, tween(80))
                        bounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                    }
                }
            },
    ) {
        drawLogoMark(enter.value)
    }
}

private fun DrawScope.drawLogoMark(enter: Float) {
    val s = size.minDimension
    val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF7FA08D), AccentSage),
            start = Offset(0f, 0f),
            end = Offset(s, s),
        ),
        cornerRadius = corner,
    )

    val e = enter
    val gap = (1f - e) * 0.16f
    val cream = Color(0xFFF6F1E9)
    val bubbleR = CornerRadius(0.13f * s, 0.13f * s)
    val bw = 0.52f * s
    val bh = 0.40f * s

    drawBubble(
        left = (0.12f - gap) * s,
        top = (0.14f - gap * 0.6f) * s,
        w = bw,
        h = bh,
        radius = bubbleR,
        color = cream.copy(alpha = 0.72f),
        tailLeft = true,
    )
    drawBubble(
        left = (0.34f + gap) * s,
        top = (0.44f + gap * 0.6f) * s,
        w = bw,
        h = bh,
        radius = bubbleR,
        color = cream,
        tailLeft = false,
    )

    val aAlpha = ((e - 0.5f) / 0.5f).coerceIn(0f, 1f)
    if (aAlpha > 0.001f) {
        val cx = 0.5f * s
        val cy = 0.5f * s
        val strokeW = 0.055f * s
        drawLine(
            color = AccentSage,
            start = Offset(cx - 0.05f * s, cy - 0.06f * s),
            end = Offset(cx + 0.05f * s, cy),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
            alpha = aAlpha,
        )
        drawLine(
            color = AccentSage,
            start = Offset(cx + 0.05f * s, cy),
            end = Offset(cx - 0.05f * s, cy + 0.06f * s),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
            alpha = aAlpha,
        )
    }
}

private fun DrawScope.drawBubble(
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    radius: CornerRadius,
    color: Color,
    tailLeft: Boolean,
) {
    drawRoundRect(color = color, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = radius)
    val tailW = 0.10f * w
    val tailH = 0.14f * h
    val ty = top + h
    val path = Path().apply {
        if (tailLeft) {
            moveTo(left + 0.16f * w, ty)
            lineTo(left + 0.16f * w - tailW, ty + tailH)
            lineTo(left + 0.16f * w + tailW, ty)
        } else {
            moveTo(left + w - 0.16f * w, ty)
            lineTo(left + w - 0.16f * w + tailW, ty + tailH)
            lineTo(left + w - 0.16f * w - tailW, ty)
        }
        close()
    }
    drawPath(path, color)
}
