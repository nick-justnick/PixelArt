package com.example.pixelart.domain.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.pixelart.data.model.Pixel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageExporter {
    private const val MAX_DIMENSION = 1200

    private suspend fun createSourceBitmap(
        grid: List<List<Pixel>>,
        palette: List<Color>
    ) = withContext(Dispatchers.Default) {
        val rows = grid.size
        val cols = grid.first().size

        val bitmap = createBitmap(cols, rows)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        grid.forEachIndexed { row, pixelRow ->
            pixelRow.forEachIndexed { col, pixel ->
                paint.color = palette[pixel.colorIndex].toArgb()
                canvas.drawPoint(col.toFloat(), row.toFloat(), paint)
            }
        }
        bitmap
    }

    private suspend fun upscaleBitmap(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val aspectRatio = source.width.toFloat() / source.height.toFloat()

        val (outWidth, outHeight) = if (aspectRatio >= 1)
            MAX_DIMENSION to (MAX_DIMENSION / aspectRatio).toInt()
        else
            (MAX_DIMENSION * aspectRatio).toInt() to MAX_DIMENSION

        source.scale(outWidth, outHeight, false)
    }

    suspend fun saveToDevice(
        context: Context,
        grid: List<List<Pixel>>,
        palette: List<Color>
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceBitmap = createSourceBitmap(grid, palette)
        val finalBitmap = upscaleBitmap(sourceBitmap)
        sourceBitmap.recycle()

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PixelArt_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, contentValues) ?: return@withContext false

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            false
        } finally {
            finalBitmap.recycle()
        }
    }

    suspend fun getShareableUri(
        context: Context,
        grid: List<List<Pixel>>,
        palette: List<Color>
    ): Uri? = withContext(Dispatchers.IO) {
        val sourceBitmap = createSourceBitmap(grid, palette)
        val finalBitmap = upscaleBitmap(sourceBitmap)
        sourceBitmap.recycle()

        return@withContext try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "image.png")
            FileOutputStream(imageFile).use { stream ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            finalBitmap.recycle()
        }
    }
}