package com.example.pixelart.ui.coloring

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

@Stable
data class TransformState(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
)
