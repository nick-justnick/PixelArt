package com.example.pixelart.ui.coloring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixelart.data.model.ArtProject
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.ProgressInfo
import com.example.pixelart.data.model.ProgressState
import com.example.pixelart.data.repository.ArtProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var isDirty = false
    private var autoSaveJob: Job? = null

    init {
        if (projectId != -1L)
            loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = repository.getProjectById(projectId)
            if (project != null) {
                val (info, state) = calculateInitialProgress(
                    project.gridState,
                    project.palette.size
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        grid = project.gridState,
                        palette = project.palette,
                        progress = state,
                        progressInfo = info,
                        selectedColorIndex = -1
                    )
                }
            }
        }
    }

    fun saveProgress() {
        if (!isDirty || projectId == -1L || _uiState.value.grid.isEmpty()) return

        viewModelScope.launch {
            val currentProjectState = ArtProject(
                id = projectId,
                gridState = _uiState.value.grid,
                palette = _uiState.value.palette,
                thumbnail = null
            )
            repository.updateProject(currentProjectState)
            isDirty = false
        }
    }

    fun onPixelTapped(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.selectedColorIndex == -1) return

        val grid = currentState.grid
        if (row !in grid.indices || col !in grid.first().indices) return

        val pixel = grid[row][col]
        if (pixel.isColored) return

        val isCorrectColor = pixel.colorIndex == currentState.selectedColorIndex
        val cellKey = row to col

        if (isCorrectColor) {
            updateProgressAndGrid(pixel, row, col)
            if (cellKey in currentState.wronglyColoredPixels) {
                _uiState.update {
                    it.copy(wronglyColoredPixels = it.wronglyColoredPixels - cellKey)
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    wronglyColoredPixels =
                        it.wronglyColoredPixels + (cellKey to it.selectedColorIndex)
                )
            }
            isDirty = true
            triggerAutoSave()
        }
    }

    private fun updateProgressAndGrid(pixel: Pixel, row: Int, col: Int) {
        val info = _uiState.value.progressInfo
        val colorIndex = pixel.colorIndex

        val newPerColorColored = info.perColorColored.toMutableList().apply {
            this[colorIndex] = this[colorIndex] + 1
        }.toList()

        val newInfo = ProgressInfo(
            totalPixels = info.totalPixels,
            totalColored = info.totalColored + 1,
            perColorTotal = info.perColorTotal,
            perColorColored = newPerColorColored
        )

        val newGlobalProgress = newInfo.totalColored.toFloat() / newInfo.totalPixels
        val newPerColorProgress =
            newInfo.perColorColored[colorIndex].toFloat() / newInfo.perColorTotal[colorIndex]
        val isColorDone = newPerColorProgress >= 1.0f

        _uiState.update { state ->
            state.copy(
                grid = state.grid.toMutableList().apply {
                    this[row] = state.grid[row].toMutableList().apply {
                        this[col] = pixel.copy(isColored = true)
                    }.toList()
                }.toList(),
                progress = state.progress.copy(
                    globalProgress = newGlobalProgress,
                    perColorProgress = state.progress.perColorProgress.toMutableList().apply {
                        this[colorIndex] = newPerColorProgress
                    }
                ),
                progressInfo = newInfo,
                selectedColorIndex = if (isColorDone) -1 else state.selectedColorIndex
            )
        }
        isDirty = true
        triggerAutoSave()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(3000L)
            saveProgress()
        }
    }

    fun selectColor(index: Int) {
        _uiState.update { it.copy(selectedColorIndex = index) }
    }

    private fun calculateInitialProgress(
        grid: List<List<Pixel>>,
        paletteSize: Int
    ): Pair<ProgressInfo, ProgressState> {
        if (grid.isEmpty() || paletteSize == 0) return Pair(ProgressInfo(), ProgressState())

        val totalPixels = grid.size * grid.first().size
        val perColorTotal = IntArray(paletteSize)
        val perColorColored = IntArray(paletteSize)

        for (row in grid) {
            for (pixel in row) {
                perColorTotal[pixel.colorIndex]++
                if (pixel.isColored) {
                    perColorColored[pixel.colorIndex]++
                }
            }
        }
        val totalColored = perColorColored.sum()

        val info = ProgressInfo(
            totalPixels = totalPixels,
            perColorTotal = perColorTotal.toList(),
            perColorColored = perColorColored.toMutableList(),
            totalColored = totalColored
        )
        val state = ProgressState(
            globalProgress = if (totalPixels > 0) totalColored.toFloat() / totalPixels else 0f,
            perColorProgress = List(paletteSize) { i ->
                if (perColorTotal[i] == 0) 1.0f else perColorColored[i].toFloat() / perColorTotal[i]
            }
        )
        return Pair(info, state)
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