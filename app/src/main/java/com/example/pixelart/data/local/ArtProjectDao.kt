package com.example.pixelart.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.pixelart.data.model.ArtProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtProjectDao {
    @Upsert
    suspend fun upsertProject(project: ArtProject): Long

    @Query("SELECT * FROM art_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ArtProject?

    @Query("SELECT * FROM art_projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<ArtProject>>
}