package com.example.pixelart.ui.coloring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixelart.data.model.ArtProject
import com.example.pixelart.data.repository.ArtProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PixelArtViewModel(
    private val repository: ArtProjectRepository,
    private val projectId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(PixelArtUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (projectId != -1L) loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = repository.getProjectById(projectId)
            if (project != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        grid = project.gridState,
                        palette = project.palette
                    )
                }
            }
        }
    }

    fun colorAllPixels() {
        _uiState.update {
            it.copy(
                grid = it.grid.map { row -> row.map { pixel -> pixel.copy(isColored = true) } }
            )
        }
    }

    fun saveProgress() {
        if (projectId == -1L || _uiState.value.grid.isEmpty()) return

        viewModelScope.launch {
            val currentProjectState = ArtProject(
                id = projectId,
                gridState = _uiState.value.grid,
                palette = _uiState.value.palette,
                thumbnail = null
            )
            repository.updateProject(currentProjectState)
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

class PixelArtViewModelFactory(
    private val repository: ArtProjectRepository,
    private val projectId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PixelArtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PixelArtViewModel(repository, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}