package com.example.pixelart.data.model

data class ProgressState(
    val globalProgress: Float = 0f,
    val perColorProgress: List<Float> = emptyList()
)

data class ProgressInfo(
    val totalPixels: Int = 0,
    val perColorTotal: List<Int> = emptyList(),
    var perColorColored: MutableList<Int> = mutableListOf(),
    var totalColored: Int = 0
)