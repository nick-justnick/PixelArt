package com.example.pixelart.ui.coloring

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelart.domain.processing.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PixelArtViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PixelArtUiState())
    val uiState = _uiState.asStateFlow()

    fun onImageSelected(imageUri: Uri?) {
        if (imageUri == null) return
        _uiState.update { it.copy(selectedImageUri = imageUri, showSettingsDialog = true) }
    }

    fun dismissDialogue() {
        _uiState.update { it.copy(showSettingsDialog = false, selectedImageUri = null) }
    }

    fun createPixelArt(context: Context, width: Int, colorCount: Int) {
        val imageUri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showSettingsDialog = false) }
            try {
                val result = ImageProcessor.imageToPixelArt(context, imageUri, width, colorCount)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        grid = result.grid,
                        palette = result.palette,
                        selectedColorIndex = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                Log.e("PixelArtViewModel", "Error creating pixel art", e)
            }
        }
    }

    fun onPixelTapped(row: Int, col: Int) {
        val currentState = _uiState.value
        val grid = currentState.grid
        if (row !in grid.indices || col !in grid.first().indices) return

        val pixel = grid[row][col]
        if (!pixel.isColored && pixel.colorIndex == currentState.selectedColorIndex) {
            _uiState.update {
                it.copy(
                    grid = it.grid.toMutableList().apply {
                        this[row] = it.grid[row].toMutableList().apply {
                            this[col] = pixel.copy(isColored = true)
                        }.toList()
                    }.toList()
                )
            }
        }
    }

    fun selectColor(index: Int) {
        _uiState.update { it.copy(selectedColorIndex = index) }
    }
}