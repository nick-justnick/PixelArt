package com.example.pixelart.data.model

data class ProgressState(
    val globalProgress: Float = 0f,
    val perColorProgress: List<Float> = emptyList()
)

data class ProgressInfo(
    val totalPixels: Int = 0,
    val perColorTotal: List<Int> = emptyList(),
    val perColorColored: List<Int> = emptyList(),
    val totalColored: Int = 0
)