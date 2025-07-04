package com.example.pixelart.ui.coloring

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelart.PixelArtApplication
import com.example.pixelart.R
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.ProgressState
import com.example.pixelart.data.repository.ArtProjectRepository

@Composable
fun PixelArtScreen(
    projectId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember {
        ArtProjectRepository(
            (context.applicationContext as PixelArtApplication).database.artProjectDao()
        )
    }

    val viewModel: PixelArtViewModel =
        viewModel(factory = PixelArtViewModelFactory(repository, projectId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE)
                viewModel.saveProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var transformState by remember { mutableStateOf(TransformState()) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    val grid = uiState.grid
    val rows = grid.size
    if (rows == 0) return
    val cols = grid.first().size
    if (cols == 0) return

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (transformState.scale * zoomChange).coerceIn(1f, cols / 8f)

        val gridAspectRatio = cols.toFloat() / rows
        val viewportAspectRatio = viewportSize.width.toFloat() / viewportSize.height

        val gridRenderSize = if (gridAspectRatio > viewportAspectRatio) {
            Size(viewportSize.width.toFloat(), viewportSize.width / gridAspectRatio)
        } else {
            Size(viewportSize.height * gridAspectRatio, viewportSize.height.toFloat())
        }

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.grid.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportSize = it }
                            .clip(RectangleShape)
                            .pointerInput(grid) {
                                detectTapGestures { tapOffset ->
                                    val gridAspectRatio = cols.toFloat() / rows
                                    val viewportAspectRatio = size.width.toFloat() / size.height

                                    val gridRenderWidth: Float
                                    val gridRenderHeight: Float
                                    if (gridAspectRatio > viewportAspectRatio) {
                                        gridRenderWidth = size.width.toFloat()
                                        gridRenderHeight = gridRenderWidth / gridAspectRatio
                                    } else {
                                        gridRenderHeight = size.height.toFloat()
                                        gridRenderWidth = gridRenderHeight * gridAspectRatio
                                    }

                                    val centeredTap =
                                        tapOffset - Offset(size.width / 2f, size.height / 2f)
                                    val transformedOffset =
                                        (centeredTap - transformState.offset) / transformState.scale
                                    val originalTap = transformedOffset + Offset(
                                        gridRenderWidth / 2f,
                                        gridRenderHeight / 2f
                                    )
                                    val cellWidth = gridRenderWidth / cols.toFloat()
                                    val cellHeight = gridRenderHeight / rows.toFloat()

                                    val col = (originalTap.x / cellWidth).toInt()
                                    val row = (originalTap.y / cellHeight).toInt()
                                    viewModel.onPixelTapped(row, col)
                                }
                            }
                            .transformable(state = transformableState)
                    ) {
                        PixelArtGrid(
                            grid = uiState.grid,
                            palette = uiState.palette,
                            selectedColorIndex = uiState.selectedColorIndex,
                            wronglyColoredPixels = uiState.wronglyColoredPixels,
                            transformState = transformState,
                            viewportSize = viewportSize,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    GlobalProgressIndicator(
                        progress = uiState.progress.globalProgress,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
                ColorPalette(
                    palette = uiState.palette,
                    selectedColorIndex = uiState.selectedColorIndex,
                    progress = uiState.progress,
                    onColorSelected = viewModel::selectColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

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
    modifier: Modifier = Modifier
) {
    val rows = grid.size
    val cols = grid.first().size
    val aspectRatio = cols.toFloat() / rows.toFloat()

    var mainBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var overlayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(grid, layoutSize) {
        if (layoutSize == IntSize.Zero) return@LaunchedEffect
        val bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint()
        val cellWidth = layoutSize.width.toFloat() / cols
        val cellHeight = layoutSize.height.toFloat() / rows

        grid.forEachIndexed { row, pixelRow ->
            pixelRow.forEachIndexed { col, pixel ->
                if (pixel.isColored) {
                    paint.color = palette[pixel.colorIndex].toArgb()
                    canvas.drawRect(
                        col * cellWidth,
                        row * cellHeight,
                        (col + 1) * cellWidth,
                        (row + 1) * cellHeight,
                        paint
                    )
                }
            }
        }
        mainBitmap = bitmap.asImageBitmap()
    }

    LaunchedEffect(wronglyColoredPixels, selectedColorIndex, layoutSize) {
        if (layoutSize == IntSize.Zero) return@LaunchedEffect
        val bitmap = createBitmap(layoutSize.width, layoutSize.height)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint()
        val cellWidth = layoutSize.width.toFloat() / cols
        val cellHeight = layoutSize.height.toFloat() / rows

        if (selectedColorIndex != -1) {
            paint.color = Color.Gray.copy(alpha = 0.4f).toArgb()
            grid.forEachIndexed { row, pixelRow ->
                pixelRow.forEachIndexed { col, pixel ->
                    if (!pixel.isColored && pixel.colorIndex == selectedColorIndex) {
                        canvas.drawRect(
                            col * cellWidth,
                            row * cellHeight,
                            (col + 1) * cellWidth,
                            (row + 1) * cellHeight,
                            paint
                        )
                    }
                }
            }
        }
        wronglyColoredPixels.forEach { (pos, colorIndex) ->
            paint.color = palette[colorIndex].copy(alpha = 0.5f).toArgb()
            canvas.drawRect(
                pos.second * cellWidth,
                pos.first * cellHeight,
                (pos.second + 1) * cellWidth,
                (pos.first + 1) * cellHeight,
                paint
            )
        }
        overlayBitmap = bitmap.asImageBitmap()
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .onSizeChanged { layoutSize = it }
    ) {
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
            val cellWidth = size.width / cols
            val cellHeight = size.height / rows

            drawRect(Color.White, size = size)

            overlayBitmap?.let {
                drawImage(it, filterQuality = FilterQuality.None)
            }

            val strokeWidth = 1.dp.toPx() / scale
            for (i in 0..cols) {
                val x = i * cellWidth
                drawLine(
                    Color.LightGray,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
            }
            for (i in 0..rows) {
                val y = i * cellHeight
                drawLine(
                    Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }

            mainBitmap?.let {
                drawImage(it, filterQuality = FilterQuality.None)
            }

            val onScreenCellWidth = cellWidth * scale
            if (onScreenCellWidth > 10.dp.toPx()) {
                fun screenToGridCoordinates(screenOffset: Offset): Offset {
                    val viewportAspectRatio = viewportSize.width.toFloat() / viewportSize.height
                    val gridRenderWidth: Float
                    val gridRenderHeight: Float
                    if (aspectRatio > viewportAspectRatio) {
                        gridRenderWidth = viewportSize.width.toFloat()
                        gridRenderHeight = gridRenderWidth / aspectRatio
                    } else {
                        gridRenderHeight = viewportSize.height.toFloat()
                        gridRenderWidth = gridRenderHeight * aspectRatio
                    }

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
                    ((topLeftOnGrid.y / cellHeight) - buffer).toInt().coerceIn(0, rows - 1)
                val lastVisibleRow =
                    ((bottomRightOnGrid.y / cellHeight) + buffer).toInt().coerceIn(0, rows - 1)
                val firstVisibleCol =
                    ((topLeftOnGrid.x / cellWidth) - buffer).toInt().coerceIn(0, cols - 1)
                val lastVisibleCol =
                    ((bottomRightOnGrid.x / cellWidth) + buffer).toInt().coerceIn(0, cols - 1)

                val textStyle = TextStyle(
                    color = Color.DarkGray,
                    fontSize = (minOf(cellWidth, cellHeight) / 2).toSp()
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
                                    x = (col * cellWidth) + (cellWidth - textLayoutResult.size.width) / 2,
                                    y = (row * cellHeight) + (cellHeight - textLayoutResult.size.height) / 2
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