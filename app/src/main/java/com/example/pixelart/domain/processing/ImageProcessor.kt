package com.example.pixelart.domain.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.data.model.PixelArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

object ImageProcessor {
    suspend fun imageToPixelArt(
        context: Context,
        imageUri: Uri,
        targetWidth: Int,
        colorCount: Int,
    ) = withContext(Dispatchers.Default) {
        val originalBitmap = loadBitmap(context, imageUri)
        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val targetHeight = (targetWidth / aspectRatio).toInt()

        val scaledBitmap = originalBitmap.scale(targetWidth, targetHeight)

        val pixels = IntArray(targetWidth * targetHeight)
        scaledBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        val outLab = DoubleArray(3)
        val labPixels = pixels.map {
            ColorUtils.colorToLAB(it, outLab)
            outLab.copyOf()
        }.toMutableList()

        val labPalette = medianCut(labPixels, colorCount)
        val rgbPalette = labPalette.map { Color(ColorUtils.LABToColor(it[0], it[1], it[2])) }

        val grid = List(targetHeight) { row ->
            List(targetWidth) { col ->
                val pixelColor = labPixels[row * targetWidth + col]
                val closestPaletteIndex = findClosestPaletteIndex(pixelColor, labPalette)
                Pixel(colorIndex = closestPaletteIndex)
            }
        }
        PixelArt(grid, rgbPalette)
    }

    private suspend fun loadBitmap(context: Context, imageUri: Uri): Bitmap {
        val loader = ImageLoader(context)
        val request =
            ImageRequest.Builder(context).data(imageUri).size(Size.Companion.ORIGINAL)
                .allowHardware(false)
                .build()
        val result = loader.execute(request)
        return when (result) {
            is SuccessResult -> {
                val drawable = result.image.asDrawable(context.resources)
                when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    else -> throw IllegalStateException("Unsupported drawable type")
                }
            }

            else -> throw IllegalStateException("Failed to load image")
        }
    }

    private fun medianCut(labPixels: MutableList<DoubleArray>, colorCount: Int): List<DoubleArray> {
        val buckets = mutableListOf(labPixels.map { it.copyOf() }.toMutableList())
        while (buckets.size < colorCount) {
            val bucketToSplit = buckets.maxByOrNull { bucket ->
                val minLAB = DoubleArray(3) { i -> bucket.minOf { it[i] } }
                val maxLAB = DoubleArray(3) { i -> bucket.maxOf { it[i] } }
                maxLAB.sum() - minLAB.sum()
            } ?: break
            buckets.remove(bucketToSplit)

            val minLAB = DoubleArray(3) { i -> bucketToSplit.minOf { it[i] } }
            val maxLAB = DoubleArray(3) { i -> bucketToSplit.maxOf { it[i] } }
            val rangeLAB = List(3) { i -> maxLAB[i] - minLAB[i] }

            val axisToSort = rangeLAB.indexOf(rangeLAB.maxOrNull())
            bucketToSplit.sortBy { it[axisToSort] }

            val medianIndex = bucketToSplit.size / 2
            buckets.add(bucketToSplit.subList(0, medianIndex))
            buckets.add(bucketToSplit.subList(medianIndex, bucketToSplit.size))
        }
        return buckets.map { bucket ->
            DoubleArray(3) { i -> bucket.sumOf { it[i] } / bucket.size }
        }
    }

    private fun findClosestPaletteIndex(labPixel: DoubleArray, palette: List<DoubleArray>): Int {
        val minIndex = palette.indices.minBy { i ->
            (labPixel[0] - palette[i][0]).pow(2) +
                    (labPixel[1] - palette[i][1]).pow(2) +
                    (labPixel[2] - palette[i][2]).pow(2)
        }
        return minIndex
    }
}