package com.example.pixelart.ui.coloring.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelart.data.model.ProgressState

private const val COLORS_PER_PAGE = 10
private const val PALETTE_COLUMNS = 5

@Composable
fun PagedColorPalette(
    palette: List<Color>,
    selectedColorIndex: Int,
    progress: ProgressState,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val chunkedPalette = palette.withIndex().chunked(COLORS_PER_PAGE)
    val pagerState = rememberPagerState { chunkedPalette.size }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            PalettePage(
                pageColors = chunkedPalette[pageIndex],
                selectedColorIndex = selectedColorIndex,
                progress = progress,
                onColorSelected = onColorSelected
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.Gray else Color.LightGray
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun PalettePage(
    pageColors: List<IndexedValue<Color>>,
    selectedColorIndex: Int,
    progress: ProgressState,
    onColorSelected: (Int) -> Unit
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            pageColors.forEach { (originalIndex, color) ->
                ColorTile(
                    color = color,
                    index = originalIndex,
                    isSelected = (originalIndex == selectedColorIndex),
                    isDone = progress.perColorProgress.getOrElse(originalIndex) { 0f } == 1f,
                    progress = progress.perColorProgress.getOrElse(originalIndex) { 0f },
                    onColorSelected = onColorSelected
                )
            }
        }
    ) { measurables, constraints ->
        val tileSize = constraints.maxWidth / PALETTE_COLUMNS
        val tileConstraints = Constraints.fixed(width = tileSize, height = tileSize)

        val placeables = measurables.map { it.measure(tileConstraints) }

        val pageHeight = if (placeables.size > PALETTE_COLUMNS) tileSize * 2 else tileSize
        val pageWidth = constraints.maxWidth

        layout(width = pageWidth, height = pageHeight) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / PALETTE_COLUMNS
                val col = index % PALETTE_COLUMNS
                placeable.placeRelative(x = col * tileSize, y = row * tileSize)
            }
        }
    }
}

@Composable
private fun ColorTile(
    color: Color,
    index: Int,
    isSelected: Boolean,
    isDone: Boolean,
    progress: Float,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (color.luminance() > 0.5) Color.Black else Color.White

    Box(
        modifier = modifier
            .background(color)
            .clickable { onColorSelected(index) },
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Done",
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = if (isSelected) 32.sp else 24.sp
                    )
                )
                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .width(40.dp)
                            .height(8.dp)
                    ) {
                        val trackWidth = 8.dp.toPx()
                        val progressWidth = 6.dp.toPx()
                        drawLine(
                            color = textColor,
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = trackWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = color,
                            start = Offset(0f, center.y),
                            end = Offset(size.width * progress, center.y),
                            strokeWidth = progressWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}