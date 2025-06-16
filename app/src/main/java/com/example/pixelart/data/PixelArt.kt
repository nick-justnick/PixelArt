package com.example.pixelart.data

import androidx.compose.ui.graphics.Color

data class PixelArt(
    val grid: List<List<Pixel>>, val palette: List<Color>
)
