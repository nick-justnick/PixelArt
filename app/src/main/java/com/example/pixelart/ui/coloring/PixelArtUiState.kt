package com.example.pixelart.ui.coloring

import androidx.compose.ui.graphics.Color
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.ProgressInfo
import com.example.pixelart.data.model.ProgressState

data class PixelArtUiState(
    val grid: List<List<Pixel>> = emptyList(),
    val palette: List<Color> = emptyList(),
    val selectedColorIndex: Int = -1,
    val isLoading: Boolean = false,
    val progress: ProgressState = ProgressState(),
    internal val progressInfo: ProgressInfo = ProgressInfo(),
    val wronglyColoredPixels: Map<Pair<Int, Int>, Int> = emptyMap()
)