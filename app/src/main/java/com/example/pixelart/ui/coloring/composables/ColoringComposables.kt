package com.example.pixelart.ui.coloring.composables

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.ui.coloring.TransformState
import com.example.pixelart.ui.coloring.getGridRenderSize
import com.example.pixelart.ui.theme.MontserratFamily

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
            fontWeight = FontWeight.Bold,
            fontFamily = MontserratFamily
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
    var grayscaleBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val baseTextStyle = MaterialTheme.typography.labelMedium

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

    LaunchedEffect(Unit) {
        if (cols == 0 || rows == 0) return@LaunchedEffect
        val bitmap = createBitmap(cols, rows)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        grid.forEachIndexed { row, pixelRow ->
            pixelRow.forEachIndexed { col, pixel ->
                val originalColor = palette[pixel.colorIndex]
                val lightenedLuminance = (originalColor.luminance() * 0.4f) + 0.5f
                val grayColor = Color(lightenedLuminance, lightenedLuminance, lightenedLuminance)
                paint.color = grayColor.toArgb()
                canvas.drawPoint(col.toFloat(), row.toFloat(), paint)
            }
        }
        grayscaleBitmap = bitmap.asImageBitmap()
    }

    LaunchedEffect(wronglyColoredPixels, selectedColorIndex) {
        if (cols == 0 || rows == 0) return@LaunchedEffect
        val bitmap = createBitmap(cols, rows)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        if (selectedColorIndex != -1) {
            paint.color = Color.Gray.copy(alpha = 0.6f).toArgb()
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
            val cellSize = size.width / cols
            val onScreenCellSize = cellSize * scale

            val lowerThresholdPx = with(density) { 5.dp.toPx() }
            val middleThresholdPx = with(density) { 7.dp.toPx() }
            val upperThresholdPx = with(density) { 10.dp.toPx() }

            val transitionProgress =
                ((onScreenCellSize - lowerThresholdPx) / (upperThresholdPx - lowerThresholdPx))
                    .coerceIn(0f, 1f)

            val (grayscaleAlpha, numbersAlpha, showGridLines) = when {
                isComplete -> Triple(0f, 0f, false)
                onScreenCellSize < lowerThresholdPx -> Triple(1f, 0f, false)
                onScreenCellSize < middleThresholdPx ->
                    Triple(1f - transitionProgress, transitionProgress, false)

                onScreenCellSize < upperThresholdPx ->
                    Triple(1f - transitionProgress, transitionProgress, true)

                else -> Triple(0f, 1f, true)
            }

            val destinationSize = IntSize(size.width.toInt(), size.height.toInt())

            drawRect(Color.White, size = size)

            if (grayscaleAlpha > 0.01f) {
                grayscaleBitmap?.let {
                    drawImage(
                        image = it,
                        dstSize = destinationSize,
                        filterQuality = FilterQuality.None,
                        alpha = grayscaleAlpha
                    )
                }
            }

            highlightBitmap?.let {
                drawImage(
                    image = it,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None
                )
            }

            if (showGridLines) {
                val lineColor = Color.DarkGray.copy(alpha = 0.7f)
                for (i in 0..cols) {
                    val x = i * cellSize
                    drawLine(lineColor, start = Offset(x, 0f), end = Offset(x, size.height))
                }
                for (i in 0..rows) {
                    val y = i * cellSize
                    drawLine(lineColor, start = Offset(0f, y), end = Offset(size.width, y))
                }
            }

            mainBitmap?.let {
                drawImage(
                    image = it,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None
                )
            }

            if (numbersAlpha > 0.01f) {
                fun screenToGridCoordinates(screenOffset: Offset): Offset {
                    val gridRenderSize = getGridRenderSize(cols, rows, viewportSize)
                    val gridRenderWidth = gridRenderSize.width
                    val gridRenderHeight = gridRenderSize.height

                    val centeredTap =
                        screenOffset - Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val transformedOffset = (centeredTap - transformState.offset) / scale
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

                val textStyle = baseTextStyle.copy(
                    color = Color.DarkGray.copy(alpha = numbersAlpha),
                    fontSize = (cellSize * 0.45f).toSp()
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
                Icon(Icons.Default.SaveAlt, "Save")
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
