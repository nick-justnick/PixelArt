package com.example.pixelart.ui.create

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.example.pixelart.data.repository.ArtProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CreateArtUiState(
    val sourceBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val longestSide: Int = 80,
    val width: Int = 80,
    val height: Int = 80,
    val useFilter: Boolean = true,
    val colorCount: Int = 64,
    val colorSliderPosition: Float = 0.5f,
    val colorRange: IntRange = 50..80
)

class CreateArtViewModel(
    application: Application,
    private val repository: ArtProjectRepository,
    private val imageUri: Uri
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CreateArtUiState())
    val uiState = _uiState.asStateFlow()

    private val colorFactor = 0.83f

    private var previewUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            loadSourceBitmap()
            _uiState.value.sourceBitmap?.let {
                onLongestSideChange(_uiState.value.longestSide)
            }
            triggerPreviewUpdate()
        }
    }

    private suspend fun loadSourceBitmap() {
        val context = getApplication<Application>()
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(imageUri).allowHardware(false).build()
        val result = (loader.execute(request) as? SuccessResult)?.image
        _uiState.update {
            it.copy(
                sourceBitmap = result?.let { bmp ->
                    (bmp.asDrawable(context.resources) as BitmapDrawable).bitmap
                }
            )
        }
    }

    fun onLongestSideChange(newLongestSide: Int) {
        val source = _uiState.value.sourceBitmap ?: return
        val aspectRatio = source.width.toFloat() / source.height.toFloat()

        val (newWidth, newHeight) = if (aspectRatio >= 1) {
            newLongestSide to (newLongestSide / aspectRatio).roundToInt()
        } else {
            (newLongestSide * aspectRatio).roundToInt() to newLongestSide
        }

        val recommended = calculateRecommendedColorCount(newWidth, newHeight)
        val oldPosition = _uiState.value.colorSliderPosition

        val rangeStart = roundDownTo5((recommended * 0.8f).roundToInt())
        val rangeEnd = roundUpTo5((recommended * 1.2f).roundToInt())

        val newColorCount = (rangeStart + (rangeEnd - rangeStart) * oldPosition).roundToInt()

        _uiState.update {
            it.copy(
                longestSide = newLongestSide,
                width = newWidth,
                height = newHeight,
                colorCount = newColorCount,
                colorRange = rangeStart..rangeEnd
            )
        }
        triggerPreviewUpdate()
    }

    fun onColorCountChange(newColorCount: Int) {
        val state = _uiState.value
        val rangeStart = state.colorRange.first
        val rangeEnd = state.colorRange.last
        val range = rangeEnd - rangeStart
        val newPosition = if (range > 0) (newColorCount - rangeStart).toFloat() / range else 0.5f

        _uiState.update {
            it.copy(
                colorCount = newColorCount,
                colorSliderPosition = newPosition
            )
        }
    }

    fun onFilterChange(useFilter: Boolean) {
        _uiState.update { it.copy(useFilter = useFilter) }
    }

    private fun triggerPreviewUpdate() {
        previewUpdateJob?.cancel()
        previewUpdateJob = viewModelScope.launch {
            updatePreview()
        }
    }

    private fun updatePreview() {
        val source = _uiState.value.sourceBitmap ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val aspectRatio = source.width.toFloat() / source.height.toFloat()
            val height = (state.width / aspectRatio).toInt()

            val scaled = source.scale(state.width, height, state.useFilter)

            val grayscaleBitmap = createBitmap(scaled.width, scaled.height)
            val canvas = Canvas(grayscaleBitmap)
            val paint = Paint()
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            _uiState.update { it.copy(previewBitmap = grayscaleBitmap, isLoading = false) }
        }
    }

    private fun calculateRecommendedColorCount(width: Int, height: Int): Int {
        val totalPixels = width * height
        return (colorFactor * sqrt(totalPixels.toFloat())).roundToInt().coerceIn(8, 256)
    }

    fun createProject(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val state = _uiState.value
            val newProjectId = repository.createNewProject(
                context = getApplication(),
                imageUri = imageUri,
                width = state.width,
                colorCount = state.colorCount,
                useFilter = state.useFilter
            )
            onComplete(newProjectId)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private fun roundDownTo5(n: Int): Int = (n / 5) * 5
    private fun roundUpTo5(n: Int): Int = ((n + 4) / 5) * 5
}

class CreateArtViewModelFactory(
    private val application: Application,
    private val repository: ArtProjectRepository,
    private val imageUri: Uri
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateArtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateArtViewModel(application, repository, imageUri) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}