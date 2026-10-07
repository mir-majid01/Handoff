package com.handoff.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.handoff.app.ui.theme.HandoffMotion
import kotlinx.coroutines.delay

/**
 * Subtle success overlay: frosted dark pill with an animated checkmark draw
 * and a caption. Shows once per [trigger] change, auto-dismisses.
 */
@Composable
fun SuccessToast(
    message: String,
    trigger: Int,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(1400)
            visible = false
            onDone()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = HandoffMotion.springSnappy,
        ) + fadeIn(HandoffMotion.iosTween(HandoffMotion.DURATION_FAST)),
        exit = fadeOut(HandoffMotion.iosTween(HandoffMotion.DURATION_FAST)),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE62B2623))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                CheckmarkDraw(size = 22.dp, color = Color(0xFF8FB5A0))
                Text(
                    text = message,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF2ECE4),
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

/** Animated checkmark: path measured once, drawn 0→1 with the standard tween. */
@Composable
fun CheckmarkDraw(size: androidx.compose.ui.unit.Dp, color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(320, easing = HandoffMotion.EaseIOSEmphasized))
    }
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.12f, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.22f, this@Canvas.size.height * 0.54f)
            lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.74f)
            lineTo(this@Canvas.size.width * 0.80f, this@Canvas.size.height * 0.30f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress.value, partial, true)
        drawPath(partial, color, style = stroke)
    }
}
