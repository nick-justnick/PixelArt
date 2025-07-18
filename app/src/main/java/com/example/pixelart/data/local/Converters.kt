package com.example.pixelart.data.local

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import com.example.pixelart.data.model.Pixel
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromGridJson(jsonString: String): List<List<Pixel>> {
        return Json.decodeFromString(jsonString)
    }

    @TypeConverter
    fun toGridJson(grid: List<List<Pixel>>): String {
        return Json.encodeToString(grid)
    }

    @TypeConverter
    fun fromPaletteJson(jsonString: String): List<Color> {
        val intList = Json.decodeFromString<List<Int>>(jsonString)
        return intList.map { Color(it) }
    }

    @TypeConverter
    fun toPaletteJson(palette: List<Color>): String {
        val intList = palette.map { it.toArgb() }
        return Json.encodeToString(intList)
    }
}