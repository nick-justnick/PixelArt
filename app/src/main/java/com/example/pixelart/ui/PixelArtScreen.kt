package com.example.pixelart.ui

import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelart.data.Pixel
import com.example.pixelart.viewmodel.PixelArtViewModel

@Composable
fun PixelArtScreen(
    modifier: Modifier = Modifier,
    viewModel: PixelArtViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri: Uri? -> viewModel.onImageSelected(uri) }
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
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
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PixelArtGrid(
                        grid = uiState.grid,
                        palette = uiState.palette,
                        onPixelTapped = viewModel::onPixelTapped,
                    )
                }
                ColorPalette(
                    palette = uiState.palette,
                    selectedColorIndex = uiState.selectedColorIndex,
                    onColorSelected = viewModel::selectColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else if (!uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Create New Pixel Art")
                    }
                }
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

        if (uiState.showSettingsDialog) {
            SettingsDialog(
                onDismiss = viewModel::dismissDialogue,
                onConfirm = { width, colorCount ->
                    viewModel.createPixelArt(context, width, colorCount)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (width: Int, colorCount: Int) -> Unit,
) {
    var width by remember { mutableStateOf("64") }
    var colorCount by remember { mutableStateOf("16") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Artwork") },
        text = {
            Column {
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Grid Width") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = colorCount,
                    onValueChange = { colorCount = it },
                    label = { Text("Number of Colors") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val widthInt = width.toIntOrNull() ?: 64
                val colorsInt = colorCount.toIntOrNull() ?: 16
                onConfirm(widthInt, colorsInt)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PixelArtGrid(
    grid: List<List<Pixel>>,
    palette: List<Color>,
    onPixelTapped: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = grid.size
    val cols = grid.first().size
    val aspectRatio = cols.toFloat() / rows.toFloat()

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellWidth = size.width / cols
                    val cellHeight = size.height / rows
                    val col = (offset.x / cellWidth).toInt().coerceIn(0, cols - 1)
                    val row = (offset.y / cellHeight).toInt().coerceIn(0, rows - 1)
                    onPixelTapped(row, col)
                }
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

@Composable
fun ColorPalette(
    palette: List<Color>,
    selectedColorIndex: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(palette) { index, color ->
            val textColor = if (color.luminance() > 0.5) Color.Black else Color.White
            val isSelected = (index == selectedColorIndex)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent
                    )
                    .clickable { onColorSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}