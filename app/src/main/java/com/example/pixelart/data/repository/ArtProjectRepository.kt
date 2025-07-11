package com.example.pixelart.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.example.pixelart.data.local.ArtProjectDao
import com.example.pixelart.data.model.ArtProject
import com.example.pixelart.data.model.Pixel
import com.example.pixelart.domain.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ArtProjectRepository(private val artProjectDao: ArtProjectDao) {

    fun getAllProjects() = artProjectDao.getAllProjects()

    suspend fun getProjectById(id: Long) = artProjectDao.getProjectById(id)

    suspend fun createNewProject(
        context: Context,
        imageUri: Uri,
        width: Int,
        colorCount: Int
    ): Long {
        val processedArt = ImageProcessor.imageToPixelArt(context, imageUri, width, colorCount)
        val thumbnail = generateThumbnail(processedArt.grid, processedArt.palette)
        val newProject = ArtProject(
            gridState = processedArt.grid,
            palette = processedArt.palette,
            thumbnail = thumbnail
        )
        return artProjectDao.upsertProject(newProject)
    }

    suspend fun updateProject(project: ArtProject) {
        val updatedThumbnail = generateThumbnail(project.gridState, project.palette)
        artProjectDao.upsertProject(project.copy(thumbnail = updatedThumbnail))
    }

    private suspend fun generateThumbnail(
        grid: List<List<Pixel>>,
        palette: List<Color>
    ): ByteArray = withContext(Dispatchers.Default) {
        val rows = grid.size
        val cols = grid.first().size

        val bitmap = createBitmap(cols, rows)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val pixel = grid[row][col]
                val color = palette[pixel.colorIndex]

                paint.color = if (pixel.isColored) {
                    color.toArgb()
                } else {
                    val gray = color.luminance()
                    Color(gray, gray, gray).toArgb()
                }
                canvas.drawPoint(col.toFloat(), row.toFloat(), paint)
            }
        }

        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }
}