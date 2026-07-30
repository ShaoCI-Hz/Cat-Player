package com.hezi.juyumao.data.repository

import com.hezi.juyumao.data.local.datastore.SettingsDataStore
import com.hezi.juyumao.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) {
    val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode.map {
        when (it) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    val smbAutoConnect: Flow<Boolean> = settingsDataStore.smbAutoConnect
    val smbConnectionTimeout: Flow<Int> = settingsDataStore.smbConnectionTimeout
    val audioBufferSize: Flow<Int> = settingsDataStore.audioBufferSize
    val gaplessPlayback: Flow<Boolean> = settingsDataStore.gaplessPlayback

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsDataStore.setThemeMode(
            when (mode) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
            }
        )
    }

    suspend fun setSmbAutoConnect(enabled: Boolean) = settingsDataStore.setSmbAutoConnect(enabled)
    suspend fun setSmbConnectionTimeout(seconds: Int) = settingsDataStore.setSmbConnectionTimeout(seconds)
    suspend fun setAudioBufferSize(kb: Int) = settingsDataStore.setAudioBufferSize(kb)
    suspend fun setGaplessPlayback(enabled: Boolean) = settingsDataStore.setGaplessPlayback(enabled)
}
