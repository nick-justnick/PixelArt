package com.example.pixelart.ui.gallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixelart.data.repository.ArtProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(private val repository: ArtProjectRepository) : ViewModel() {
    val projects = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewProject(
        context: Context,
        uri: Uri,
        width: Int,
        colorCount: Int,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newId = repository.createNewProject(context, uri, width, colorCount)
            onComplete(newId)
        }
    }
}

class GalleryViewModelFactory(private val repository: ArtProjectRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}