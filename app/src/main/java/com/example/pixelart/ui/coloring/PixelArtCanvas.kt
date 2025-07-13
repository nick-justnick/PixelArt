package com.example.pixelart.ui.coloring

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.ui.coloring.composables.GlobalProgressIndicator
import com.example.pixelart.ui.coloring.composables.PixelArtGrid
import com.example.pixelart.ui.coloring.gestures.coloringGesture
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun PixelArtCanvas(
    uiState: PixelArtUiState,
    onPixelTapped: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var transformState by remember { mutableStateOf(TransformState()) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedScale = remember { Animatable(1f) }
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    val grid = uiState.grid
    val rows = grid.size
    val cols = grid.firstOrNull()?.size ?: 0

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            launch { animatedScale.animateTo(1f) }
            launch { animatedOffsetX.animateTo(0f) }
            launch { animatedOffsetY.animateTo(0f) }
        } else {
            animatedScale.snapTo(transformState.scale)
            animatedOffsetX.snapTo(transformState.offset.x)
            animatedOffsetY.snapTo(transformState.offset.y)
        }
    }

    if (uiState.isComplete) {
        transformState = TransformState(
            animatedScale.value,
            Offset(animatedOffsetX.value, animatedOffsetY.value)
        )
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (transformState.scale * zoomChange).coerceIn(1f, cols / 8f)
        val gridRenderSize = calculateGridRenderSize(cols, rows, viewportSize)
        val scaledGridSize = gridRenderSize * newScale
        val maxOffset = Offset(
            (scaledGridSize.width - viewportSize.width).coerceAtLeast(0f),
            (scaledGridSize.height - viewportSize.height).coerceAtLeast(0f)
        )
        val minOffset = -maxOffset

        transformState = transformState.copy(
            scale = newScale,
            offset = (transformState.offset + panChange).let {
                Offset(
                    it.x.coerceIn(minOffset.x, maxOffset.x),
                    it.y.coerceIn(minOffset.y, maxOffset.y)
                )
            }
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { viewportSize = it }
            .clip(RectangleShape)
            .then(
                if (!uiState.isComplete) {
                    val getCell: (Offset) -> Pair<Int, Int>? = { offset ->
                        getForgivingCellAtOffset(
                            tapOffset = offset,
                            transformState = transformState,
                            viewportSize = viewportSize,
                            grid = uiState.grid,
                            selectedColorIndex = uiState.selectedColorIndex
                        )
                    }
                    Modifier
                        .coloringGesture(
                            grid = grid,
                            selectedColorIndex = uiState.selectedColorIndex,
                            getCellAtOffset = getCell,
                            onPixelTapped = onPixelTapped
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    getCell(offset)?.let { (row, col) ->
                                        onPixelTapped(row, col)
                                    }
                                }
                            )
                        }
                        .transformable(state = transformableState)
                } else Modifier
            )
    ) {
        PixelArtGrid(
            grid = uiState.grid,
            palette = uiState.palette,
            selectedColorIndex = uiState.selectedColorIndex,
            wronglyColoredPixels = uiState.wronglyColoredPixels,
            transformState = transformState,
            viewportSize = viewportSize,
            isComplete = uiState.isComplete,
            modifier = Modifier.align(Alignment.Center)
        )
        if (!uiState.isComplete) {
            GlobalProgressIndicator(
                progress = uiState.progress.globalProgress,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

private fun getForgivingCellAtOffset(
    tapOffset: Offset,
    transformState: TransformState,
    viewportSize: IntSize,
    grid: List<List<Pixel>>,
    selectedColorIndex: Int
): Pair<Int, Int>? {
    val rows = grid.size
    if (rows == 0) return null
    val cols = grid.first().size
    if (cols == 0 || viewportSize == IntSize.Zero) return null

    val gridRenderSize = calculateGridRenderSize(cols, rows, viewportSize)
    val cellSize = gridRenderSize.width / cols

    val centeredTap = tapOffset - Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val transformedOffset = (centeredTap - transformState.offset) / transformState.scale
    val originalTap =
        transformedOffset + Offset(gridRenderSize.width / 2f, gridRenderSize.height / 2f)

    val initialCol = (originalTap.x / cellSize).toInt()
    val initialRow = (originalTap.y / cellSize).toInt()

    if (initialRow !in 0 until rows || initialCol !in 0 until cols) return null

    if (grid[initialRow][initialCol].colorIndex == selectedColorIndex)
        return initialRow to initialCol

    val tapInCellX = originalTap.x - initialCol * cellSize
    val tapInCellY = originalTap.y - initialRow * cellSize
    val forgiveness = cellSize * 0.15f

    val isTop = tapInCellY < forgiveness
    val isBottom = tapInCellY > cellSize - forgiveness
    val isLeft = tapInCellX < forgiveness
    val isRight = tapInCellX > cellSize - forgiveness

    if (!isTop && !isBottom && !isLeft && !isRight)
        return initialRow to initialCol

    val neighbors = mutableListOf<Pair<Int, Int>>().apply {
        if (isTop) add(initialRow - 1 to initialCol)
        if (isBottom) add(initialRow + 1 to initialCol)
        if (isLeft) add(initialRow to initialCol - 1)
        if (isRight) add(initialRow to initialCol + 1)
        if (isTop && isLeft) add(initialRow - 1 to initialCol - 1)
        if (isTop && isRight) add(initialRow - 1 to initialCol + 1)
        if (isBottom && isLeft) add(initialRow + 1 to initialCol - 1)
        if (isBottom && isRight) add(initialRow + 1 to initialCol + 1)
    }

    val correctNeighbors = neighbors.filter { (r, c) ->
        r in 0 until rows && c in 0 until cols && grid[r][c].colorIndex == selectedColorIndex
    }

    return when (correctNeighbors.size) {
        0 -> initialRow to initialCol
        1 -> correctNeighbors.first()
        else -> {
            correctNeighbors.minBy { (r, c) ->
                val centerX = c * cellSize + cellSize / 2
                val centerY = r * cellSize + cellSize / 2
                val dx = originalTap.x - centerX
                val dy = originalTap.y - centerY
                dx.pow(2) + dy.pow(2)
            }
        }
    }
}

internal fun calculateGridRenderSize(
    gridCols: Int,
    gridRows: Int,
    viewportSize: IntSize,
): Size {
    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()
    val gridAspectRatio = gridCols.toFloat() / gridRows.toFloat()
    val viewportAspectRatio = viewportWidth / viewportHeight

    return if (gridAspectRatio > viewportAspectRatio) {
        Size(viewportWidth, viewportWidth / gridAspectRatio)
    } else {
        Size(viewportHeight * gridAspectRatio, viewportHeight)
    }
}
