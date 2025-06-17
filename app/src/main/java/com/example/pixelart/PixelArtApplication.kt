package com.example.pixelart

import android.app.Application
import com.example.pixelart.data.local.AppDatabase

class PixelArtApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}