package com.example.smbplayer.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smbplayer.data.favorites.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _favoritePaths = MutableStateFlow<List<String>>(emptyList())
    val favoritePaths: StateFlow<List<String>> = _favoritePaths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { paths ->
                _favoritePaths.value = paths
            }
        }
    }

    fun toggleFavorite(path: String) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(path) }
    }

    fun removeFavorite(path: String) {
        viewModelScope.launch { favoritesRepository.removeFavorite(path) }
    }
}
