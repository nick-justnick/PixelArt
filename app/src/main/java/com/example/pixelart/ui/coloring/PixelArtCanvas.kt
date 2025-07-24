package com.example.pixelart.ui.coloring

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.ui.coloring.composables.GlobalProgressIndicator
import com.example.pixelart.ui.coloring.composables.PixelArtGrid
import kotlinx.coroutines.launch

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
    if (rows == 0 || cols == 0) return

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
                    var isColoring by remember { mutableStateOf(false) }
                    val currentGrid by rememberUpdatedState(grid)
                    val currentIsColoring by rememberUpdatedState(isColoring)
                    Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                if (!currentIsColoring) {
                                    val oldScale = transformState.scale
                                    val newScale = (oldScale * zoom).coerceIn(1f, (cols / 8f))

                                    val scaledGridSize =
                                        getGridRenderSize(cols, rows, viewportSize) * newScale
                                    val maxOffsetX = (scaledGridSize.width - viewportSize.width)
                                        .coerceAtLeast(0f)
                                    val maxOffsetY = (scaledGridSize.height - viewportSize.height)
                                        .coerceAtLeast(0f)

                                    val centeredCentroid =
                                        centroid - Offset(size.width / 2f, size.height / 2f)
                                    val effectiveZoom = newScale / oldScale
                                    val newOffset = centeredCentroid * (1 - effectiveZoom) +
                                            (transformState.offset + pan) * effectiveZoom

                                    transformState = TransformState(
                                        scale = newScale,
                                        offset = Offset(
                                            newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                            newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    )
                                }
                            }
                        }
                        .pointerInput(uiState.selectedColorIndex) {
                            val coloredCells = mutableSetOf<Pair<Int, Int>>()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)

                                val (row, col) = getCell(down.position) ?: return@awaitEachGesture
                                val pixel = currentGrid[row][col]
                                if (pixel.colorIndex == uiState.selectedColorIndex && !pixel.isColored) {
                                    isColoring = true
                                    onPixelTapped(row, col)
                                    coloredCells.add(row to col)
                                    down.consume()
                                }

                                if (!isColoring) return@awaitEachGesture
                                var pointer = down
                                while (pointer.pressed) {
                                    pointer = awaitPointerEvent().changes
                                        .firstOrNull { it.id == down.id } ?: break

                                    if (pointer.pressed && pointer.positionChanged()) {
                                        getCell(pointer.position)?.let { (row, col) ->
                                            if (coloredCells.add(row to col))
                                                onPixelTapped(row, col)
                                        }
                                        if (pointer.positionChange() != Offset.Zero)
                                            pointer.consume()
                                    }
                                }
                                coloredCells.clear()
                                isColoring = false
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    getCell(offset)?.let { (row, col) -> onPixelTapped(row, col) }
                                }
                            )
                        }

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
    val cols = grid.first().size

    val gridRenderSize = getGridRenderSize(cols, rows, viewportSize)
    val cellSize = gridRenderSize.width / cols

    val centeredTap = tapOffset - Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    val transformedOffset = (centeredTap - transformState.offset) / transformState.scale
    val originalTap =
        transformedOffset + Offset(gridRenderSize.width / 2f, gridRenderSize.height / 2f)

    val col = (originalTap.x / cellSize).toInt()
    val row = (originalTap.y / cellSize).toInt()

    if (row !in 0 until rows || col !in 0 until cols) return null

    if (grid[row][col].colorIndex == selectedColorIndex)
        return row to col

    val tapInCellX = originalTap.x - col * cellSize
    val tapInCellY = originalTap.y - row * cellSize
    val forgiveness = cellSize * 0.2f

    val verticalPlace = when {
        tapInCellY < forgiveness -> -1
        tapInCellY > cellSize - forgiveness -> 1
        else -> 0
    }
    val horizontalPlace = when {
        tapInCellX < forgiveness -> -1
        tapInCellX > cellSize - forgiveness -> 1
        else -> 0
    }

    if (verticalPlace == 0 && horizontalPlace == 0)
        return row to col

    val neighbors = mutableListOf<Pair<Int, Int>>()
        .apply {
            if (verticalPlace != 0)
                add(row + verticalPlace to col)
            if (horizontalPlace != 0)
                add(row to col + horizontalPlace)
            if (verticalPlace != 0 && horizontalPlace != 0)
                add(row + verticalPlace to col + horizontalPlace)
        }
        .filter { (r, c) ->
            r in 0 until rows && c in 0 until cols && grid[r][c].colorIndex == selectedColorIndex
        }

    return if (neighbors.isEmpty()) row to col else neighbors.first()
}

internal fun getGridRenderSize(
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
