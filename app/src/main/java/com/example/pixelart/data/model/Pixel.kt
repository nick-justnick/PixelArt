package com.example.pixelart.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Pixel(val colorIndex: Int, val isColored: Boolean = false)