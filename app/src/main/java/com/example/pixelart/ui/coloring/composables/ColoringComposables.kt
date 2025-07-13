package com.example.pixelart.ui.coloring.composables

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.example.pixelart.R
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.ProgressState
import com.example.pixelart.ui.coloring.TransformState
import com.example.pixelart.ui.coloring.calculateGridRenderSize

@Composable
fun GlobalProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PixelArtGrid(
    grid: List<List<Pixel>>,
    palette: List<Color>,
    selectedColorIndex: Int,
    wronglyColoredPixels: Map<Pair<Int, Int>, Int>,
    transformState: TransformState,
    viewportSize: IntSize,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val rows = grid.size
    val cols = grid.first().size
    val aspectRatio = cols.toFloat() / rows.toFloat()

    var mainBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var highlightBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(grid) {
        if (cols == 0 || rows == 0) return@LaunchedEffect
        val bitmap = createBitmap(cols, rows)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        grid.forEachIndexed { row, pixelRow ->
            pixelRow.forEachIndexed { col, pixel ->
                if (pixel.isColored) {
                    paint.color = palette[pixel.colorIndex].toArgb()
                    canvas.drawPoint(col.toFloat(), row.toFloat(), paint)
                }
            }
        }
        mainBitmap = bitmap.asImageBitmap()
    }

    LaunchedEffect(wronglyColoredPixels, selectedColorIndex) {
        if (cols == 0 || rows == 0) return@LaunchedEffect
        val bitmap = createBitmap(cols, rows)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        if (selectedColorIndex != -1) {
            paint.color = Color.Gray.copy(alpha = 0.4f).toArgb()
            grid.forEachIndexed { row, pixelRow ->
                pixelRow.forEachIndexed { col, pixel ->
                    if (!pixel.isColored && pixel.colorIndex == selectedColorIndex) {
                        canvas.drawPoint(col.toFloat(), row.toFloat(), paint)
                    }
                }
            }
        }
        wronglyColoredPixels.forEach { (pos, colorIndex) ->
            paint.color = palette[colorIndex].copy(alpha = 0.5f).toArgb()
            canvas.drawPoint(pos.second.toFloat(), pos.first.toFloat(), paint)
        }
        highlightBitmap = bitmap.asImageBitmap()
    }

    Box(modifier = modifier.aspectRatio(aspectRatio)) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = transformState.scale
                    scaleY = transformState.scale
                    translationX = transformState.offset.x
                    translationY = transformState.offset.y
                }
        ) {
            val scale = transformState.scale
            val offset = transformState.offset
            val cellSize = size.width / cols

            drawRect(Color.White, size = size)

            val destinationSize = IntSize(size.width.toInt(), size.height.toInt())

            highlightBitmap?.let {
                drawImage(
                    image = it,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None
                )
            }

            if (!isComplete) {
                val strokeWidth = 1.dp.toPx() / scale
                for (i in 0..cols) {
                    val x = i * cellSize
                    drawLine(
                        Color.LightGray,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                for (i in 0..rows) {
                    val y = i * cellSize
                    drawLine(
                        Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            }

            mainBitmap?.let {
                drawImage(
                    image = it,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None
                )
            }

            val onScreenCellSize = cellSize * scale
            if (!isComplete && onScreenCellSize > 10.dp.toPx()) {
                fun screenToGridCoordinates(screenOffset: Offset): Offset {
                    val gridRenderSize = calculateGridRenderSize(cols, rows, viewportSize)
                    val gridRenderWidth = gridRenderSize.width
                    val gridRenderHeight = gridRenderSize.height

                    val centeredTap =
                        screenOffset - Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val transformedOffset = (centeredTap - offset) / scale
                    return transformedOffset + Offset(gridRenderWidth / 2f, gridRenderHeight / 2f)
                }

                val topLeftOnGrid = screenToGridCoordinates(Offset.Zero)
                val bottomRightOnGrid = screenToGridCoordinates(
                    Offset(
                        viewportSize.width.toFloat(),
                        viewportSize.height.toFloat()
                    )
                )

                val buffer = 2
                val firstVisibleRow =
                    ((topLeftOnGrid.y / cellSize) - buffer).toInt().coerceIn(0, rows - 1)
                val lastVisibleRow =
                    ((bottomRightOnGrid.y / cellSize) + buffer).toInt().coerceIn(0, rows - 1)
                val firstVisibleCol =
                    ((topLeftOnGrid.x / cellSize) - buffer).toInt().coerceIn(0, cols - 1)
                val lastVisibleCol =
                    ((bottomRightOnGrid.x / cellSize) + buffer).toInt().coerceIn(0, cols - 1)

                val textStyle = TextStyle(
                    color = Color.DarkGray,
                    fontSize = (cellSize / 2).toSp()
                )

                for (row in firstVisibleRow..lastVisibleRow) {
                    for (col in firstVisibleCol..lastVisibleCol) {
                        val pixel = grid[row][col]
                        if (!pixel.isColored) {
                            val textLayoutResult = textMeasurer.measure(
                                text = (pixel.colorIndex + 1).toString(),
                                style = textStyle
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    x = (col * cellSize) + (cellSize - textLayoutResult.size.width) / 2,
                                    y = (row * cellSize) + (cellSize - textLayoutResult.size.height) / 2
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPalette(
    palette: List<Color>,
    selectedColorIndex: Int,
    progress: ProgressState,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(palette) { index, color ->
            val isSelected = (index == selectedColorIndex)
            val colorProgress = progress.perColorProgress.getOrNull(index) ?: 0f
            val isDone = (colorProgress == 1f)
            val textColor = if (color.luminance() > 0.5) Color.Black else Color.White

            val cellModifier = if (isDone) {
                Modifier.size(48.dp)
            } else {
                Modifier
                    .size(48.dp)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent
                    )
                    .clickable { onColorSelected(index) }
            }

            Box(
                modifier = cellModifier.background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.done),
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { colorProgress },
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(32.dp),
                                color = color.copy(alpha = 0.8f),
                                trackColor = textColor.copy(alpha = 0.5f),
                                strokeCap = StrokeCap.Round,
                                drawStopIndicator = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionButtons(
    onDone: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            .safeDrawingPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onSave) {
                Icon(ImageVector.vectorResource(R.drawable.save), "Save")
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, "Share")
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
            Button(onClick = onDone) {
                Icon(Icons.Default.Done, "Done")
                Spacer(Modifier.width(8.dp))
                Text("Done")
            }
        }
    }
}
