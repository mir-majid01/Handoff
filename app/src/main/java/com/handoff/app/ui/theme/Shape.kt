package com.handoff.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale: cards 24dp, buttons 16dp (primary actions go pill via ButtonDefaults
 * or a dedicated pill shape), sheets 28dp on top corners.
 */
val HandoffShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val CardShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(16.dp)
val PillShape = RoundedCornerShape(50)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
