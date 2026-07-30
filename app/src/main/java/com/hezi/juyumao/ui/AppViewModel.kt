package com.hezi.juyumao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.repository.SettingsRepository
import com.hezi.juyumao.player.PlaybackStateHolder
import com.hezi.juyumao.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val playbackStateHolder: PlaybackStateHolder,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val currentSong: StateFlow<SongEntity?> = playbackStateHolder.currentSong
    val artworkUri: StateFlow<String?> = playbackStateHolder.artworkUri
    val isPlaying: StateFlow<Boolean> = playbackStateHolder.isPlaying
}
