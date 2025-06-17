package com.example.pixelart.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.pixelart.data.local.Converters

@Entity(tableName = "art_projects")
@TypeConverters(Converters::class)
data class ArtProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gridState: List<List<Pixel>>,
    val palette: List<Color>,
    val thumbnail: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArtProject

        if (id != other.id) return false
        if (gridState != other.gridState) return false
        if (palette != other.palette) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + gridState.hashCode()
        result = 31 * result + palette.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }

}
