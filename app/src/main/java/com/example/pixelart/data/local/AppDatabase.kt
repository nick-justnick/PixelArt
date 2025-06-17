package com.example.pixelart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pixelart.data.model.ArtProject

@Database(entities = [ArtProject::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artProjectDao(): ArtProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixel_art_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}