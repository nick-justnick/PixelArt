package com.example.pixelart.viewmodel

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.pixelart.data.Pixel

data class PixelArtUiState(
    val grid: List<List<Pixel>> = emptyList(),
    val palette: List<Color> = emptyList(),
    val selectedColorIndex: Int = 0,
    val isLoading: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val selectedImageUri: Uri? = null
)
