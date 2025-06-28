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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pixelart.PixelArtApplication
import com.example.pixelart.R
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.ProgressState
import com.example.pixelart.data.repository.ArtProjectRepository

@Composable
fun PixelArtScreen(
    navController: NavController,
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
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                ) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        PixelArtGrid(
                            grid = uiState.grid,
                            palette = uiState.palette,
                            selectedColorIndex = uiState.selectedColorIndex,
                            onPixelTapped = viewModel::onPixelTapped,
                        )
                    }
                    GlobalProgressIndicator(
                        progress = uiState.progress.globalProgress,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
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
//                Button(onClick = viewModel::colorAllPixels) {
//                    Icon(
//                        Icons.Filled.Add,
//                        contentDescription = null
//                    )
//                }
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
    onPixelTapped: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = grid.size
    if (rows == 0) return
    val cols = grid.first().size
    if (cols == 0) return
    val aspectRatio = cols.toFloat() / rows.toFloat()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, cols / 8f)

        val canvasSize = Size(layoutSize.width.toFloat(), layoutSize.height.toFloat())
        val gridSize = canvasSize * newScale
        val maxOffset = Offset(
            x = (gridSize.width - canvasSize.width).coerceAtLeast(0f) / 2f,
            y = (gridSize.height - canvasSize.height).coerceAtLeast(0f) / 2f
        )
        val minOffset = -maxOffset

        offset = (offset + panChange).let {
            Offset(
                x = it.x.coerceIn(minOffset.x, maxOffset.x),
                y = it.y.coerceIn(minOffset.y, maxOffset.y)
            )
        }
        scale = newScale
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio)
            .onSizeChanged { layoutSize = it }
            .pointerInput(grid) {
                detectTapGestures { tapOffset ->
                    val centeredTap = tapOffset - Offset(size.width / 2f, size.height / 2f)
                    val transformedOffset = (centeredTap - offset) / scale
                    val originalTap = transformedOffset + Offset(size.width / 2f, size.height / 2f)
                    val cellWidth = size.width / cols
                    val cellHeight = size.height / rows
                    val col = (originalTap.x / cellWidth).toInt().coerceIn(0, cols - 1)
                    val row = (originalTap.y / cellHeight).toInt().coerceIn(0, rows - 1)
                    onPixelTapped(row, col)
                }
            }
            .transformable(state = transformableState)
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            val cellWidth = size.width / cols
            val cellHeight = size.height / rows
            val cellSize = Size(cellWidth, cellHeight)
            textPaint.textSize = minOf(cellWidth, cellHeight) * 0.5f

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val pixel = grid[row][col]
                    val topLeft = Offset(col * cellWidth, row * cellHeight)

                    if (pixel.isColored) {
                        drawRect(
                            color = palette[pixel.colorIndex],
                            topLeft = topLeft,
                            size = cellSize
                        )
                    } else {
                        drawRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = cellSize
                        )
                        if (pixel.colorIndex == selectedColorIndex) {
                            drawRect(
                                color = Color.DarkGray.copy(alpha = 0.5f),
                                topLeft = topLeft,
                                size = cellSize
                            )
                        }
                        drawRect(
                            color = Color.LightGray,
                            topLeft = topLeft,
                            size = cellSize,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                (pixel.colorIndex + 1).toString(),
                                topLeft.x + cellWidth / 2,
                                topLeft.y + cellHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2,
                                textPaint
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