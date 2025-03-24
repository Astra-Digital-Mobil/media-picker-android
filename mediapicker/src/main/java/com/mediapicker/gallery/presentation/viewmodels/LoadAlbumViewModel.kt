package com.mediapicker.gallery.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.repositories.GalleryRepository
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class LoadAlbumViewModel constructor(private val galleryRepository: GalleryRepository) : ViewModel(){

    private val albumLiveData = MutableLiveData<HashSet<PhotoAlbum>>()
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun getAlbums() = albumLiveData

    fun loadAlbums() {
        viewModelScope.launch(singleThreadDispatcher) {
            albumLiveData.postValue(galleryRepository.getAlbums())
        }
    }

    override fun onCleared() {
        super.onCleared()
        singleThreadDispatcher.close()
    }
}
