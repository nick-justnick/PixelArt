package com.example.pixelart.ui.coloring.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import com.example.pixelart.data.model.Pixel

@Composable
fun Modifier.coloringGesture(
    grid: List<List<Pixel>>,
    selectedColorIndex: Int,
    getCellAtOffset: (Offset) -> Pair<Int, Int>?,
    onPixelTapped: (row: Int, col: Int) -> Unit
): Modifier {
    val currentGrid by rememberUpdatedState(grid)
    val currentSelectedColorIndex by rememberUpdatedState(selectedColorIndex)
    val currentGetCellAtOffset by rememberUpdatedState(getCellAtOffset)
    val currentOnPixelTapped by rememberUpdatedState(onPixelTapped)

    return this.pointerInput(Unit) {
        val coloredCells = mutableSetOf<Pair<Int, Int>>()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val initialCell = currentGetCellAtOffset(down.position)

            var isColoring = false

            if (initialCell != null) {
                val (row, col) = initialCell
                val pixel = currentGrid[row][col]
                if (pixel.colorIndex == currentSelectedColorIndex && !pixel.isColored) {
                    isColoring = true
                    coloredCells.clear()

                    currentOnPixelTapped(row, col)
                    coloredCells.add(row to col)
                    down.consume()
                }
            }

            if (isColoring) {
                var pointer = down
                while (pointer.pressed) {
                    val event = awaitPointerEvent()
                    pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (pointer.pressed && pointer.positionChanged()) {
                        currentGetCellAtOffset(pointer.position)?.let { (row, col) ->
                            if (row to col !in coloredCells) {
                                currentOnPixelTapped(row, col)
                                coloredCells.add(row to col)
                            }
                        }
                        if (pointer.positionChange() != Offset.Zero)
                            pointer.consume()
                    }
                }
                coloredCells.clear()
            }
        }
    }
}
