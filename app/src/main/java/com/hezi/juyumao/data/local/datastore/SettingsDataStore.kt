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
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "dark" }
    val smbAutoConnect: Flow<Boolean> = context.dataStore.data.map { it[SMB_AUTO_CONNECT] ?: true }
    val smbConnectionTimeout: Flow<Int> = context.dataStore.data.map { it[SMB_CONNECTION_TIMEOUT] ?: 30 }
    val audioBufferSize: Flow<Int> = context.dataStore.data.map { it[AUDIO_BUFFER_SIZE] ?: 256 }
    val gaplessPlayback: Flow<Boolean> = context.dataStore.data.map { it[GAPLESS_PLAYBACK] ?: false }

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
}
