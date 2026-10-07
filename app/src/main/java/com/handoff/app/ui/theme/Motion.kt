package com.handoff.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens. Springs use ~0.8 damping (tiny, organic overshoot); iOS-style
 * navigation pushes use an emphasized ease-out curve. All animations should
 * route through these tokens so timing stays consistent app-wide.
 */
object HandoffMotion {
    const val DAMPING_RATIO = 0.8f

    val springStandard: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = DAMPING_RATIO, stiffness = Spring.StiffnessMediumLow)
    val springSnappy: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = DAMPING_RATIO, stiffness = Spring.StiffnessMedium)
    val springGentle: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)

    /** iOS default ease-in-out. */
    val EaseIOS = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** iOS emphasized ease-out used for navigation pushes and sheets. */
    val EaseIOSEmphasized = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    const val DURATION_FAST = 180
    const val DURATION_MEDIUM = 280
    const val DURATION_SLOW = 380
    const val DURATION_NAV = 340

    fun <T> iosTween(duration: Int = DURATION_MEDIUM): DurationBasedAnimationSpec<T> =
        tween(duration, easing = EaseIOSEmphasized)
}
