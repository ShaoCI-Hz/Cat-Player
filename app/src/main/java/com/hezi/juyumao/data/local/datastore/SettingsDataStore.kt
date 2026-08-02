package com.hezi.juyumao.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SMB_AUTO_CONNECT = booleanPreferencesKey("smb_auto_connect")
        private val SMB_CONNECTION_TIMEOUT = intPreferencesKey("smb_connection_timeout")
        private val AUDIO_BUFFER_SIZE = intPreferencesKey("audio_buffer_size")
        private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        private val LYRICS_FONT_SIZE = floatPreferencesKey("lyrics_font_size")
        private val LYRICS_FONT_BOLD = booleanPreferencesKey("lyrics_font_bold")
        private val CACHE_THREADS = intPreferencesKey("cache_threads")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val smbAutoConnect: Flow<Boolean> = context.dataStore.data.map { it[SMB_AUTO_CONNECT] ?: true }
    val smbConnectionTimeout: Flow<Int> = context.dataStore.data.map { it[SMB_CONNECTION_TIMEOUT] ?: 30 }
    val audioBufferSize: Flow<Int> = context.dataStore.data.map { it[AUDIO_BUFFER_SIZE] ?: 256 }
    val gaplessPlayback: Flow<Boolean> = context.dataStore.data.map { it[GAPLESS_PLAYBACK] ?: false }
    val lyricsFontSize: Flow<Float> = context.dataStore.data.map { it[LYRICS_FONT_SIZE] ?: 18f }
    val lyricsFontBold: Flow<Boolean> = context.dataStore.data.map { it[LYRICS_FONT_BOLD] ?: true }
    val cacheThreads: Flow<Int> = context.dataStore.data.map { it[CACHE_THREADS] ?: 4 }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setSmbAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[SMB_AUTO_CONNECT] = enabled }
    }

    suspend fun setSmbConnectionTimeout(seconds: Int) {
        context.dataStore.edit { it[SMB_CONNECTION_TIMEOUT] = seconds }
    }

    suspend fun setAudioBufferSize(kb: Int) {
        context.dataStore.edit { it[AUDIO_BUFFER_SIZE] = kb }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setLyricsFontSize(size: Float) {
        context.dataStore.edit { it[LYRICS_FONT_SIZE] = size }
    }

    suspend fun setLyricsFontBold(bold: Boolean) {
        context.dataStore.edit { it[LYRICS_FONT_BOLD] = bold }
    }

    suspend fun setCacheThreads(threads: Int) {
        context.dataStore.edit { it[CACHE_THREADS] = threads }
    }
}
