package com.example.smbplayer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.TrackInfo
import com.example.smbplayer.data.player.TrackSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val PLAY_MODE_KEY = stringPreferencesKey("play_mode")
        private val LAST_HOST_KEY = stringPreferencesKey("last_host")
        private val LAST_SHARE_KEY = stringPreferencesKey("last_share")
        private val LAST_USER_KEY = stringPreferencesKey("last_user")
        private val LAST_PASSWORD_KEY = stringPreferencesKey("last_password")
        private val LAST_PLAYLIST_KEY = stringPreferencesKey("last_playlist")
        private val LAST_INDEX_KEY = stringPreferencesKey("last_index")
        private val LAST_POSITION_KEY = stringPreferencesKey("last_position")
        private val LAST_VOLUME_KEY = stringPreferencesKey("last_volume")
        private val LAST_HISTORY_KEY = stringPreferencesKey("last_history")
        private val EXCLUDED_FOLDERS_KEY = stringPreferencesKey("excluded_folders")
        private val MIN_DURATION_KEY = stringPreferencesKey("min_duration_sec")
        private val PLAY_STATS_KEY = stringPreferencesKey("play_stats")
        private val TOTAL_PLAY_TIME_KEY = stringPreferencesKey("total_play_time_ms")
        private val SAVED_PLAYLISTS_KEY = stringPreferencesKey("saved_playlists")
        private val SMB_BOOKMARKS_KEY = stringPreferencesKey("smb_bookmarks")
    }

    val themeMode: Flow<String> = dataStore.data.map { p -> p[THEME_MODE_KEY] ?: "system" }
    val playMode: Flow<PlayMode> = dataStore.data.map { p -> when(p[PLAY_MODE_KEY]) { "random" -> PlayMode.Random; "single" -> PlayMode.Single; "loop" -> PlayMode.Loop; else -> PlayMode.Sequential } }
    val lastConnection: Flow<Triple<String,String,String>?> = dataStore.data.map { p -> val h=p[LAST_HOST_KEY] ?: return@map null; val s=p[LAST_SHARE_KEY] ?: return@map null; val u=p[LAST_USER_KEY] ?: return@map null; Triple(h,s,u) }
    val savedPlaylist: Flow<String> = dataStore.data.map { p -> p[LAST_PLAYLIST_KEY] ?: "" }
    val savedIndex: Flow<Int> = dataStore.data.map { p -> (p[LAST_INDEX_KEY] ?: "-1").toIntOrNull() ?: -1 }
    val savedPosition: Flow<Long> = dataStore.data.map { p -> (p[LAST_POSITION_KEY] ?: "0").toLongOrNull() ?: 0 }
    val savedVolume: Flow<Float> = dataStore.data.map { p -> (p[LAST_VOLUME_KEY] ?: "0.8").toFloatOrNull() ?: 0.8f }
    val savedPlayHistory: Flow<String> = dataStore.data.map { p -> p[LAST_HISTORY_KEY] ?: "" }
    val lastPassword: Flow<String> = dataStore.data.map { p -> p[LAST_PASSWORD_KEY] ?: "" }
    val excludedFolders: Flow<Set<String>> = dataStore.data.map { p ->
        val raw = p[EXCLUDED_FOLDERS_KEY] ?: ""
        if (raw.isEmpty()) emptySet() else raw.split("|").toSet()
    }
    val minDurationSeconds: Flow<Int> = dataStore.data.map { p ->
        (p[MIN_DURATION_KEY] ?: "60").toIntOrNull() ?: 60
    }
    val totalPlayTimeMs: Flow<Long> = dataStore.data.map { p ->
        (p[TOTAL_PLAY_TIME_KEY] ?: "0").toLongOrNull() ?: 0L
    }

    suspend fun setThemeMode(mode: String) { dataStore.edit { p -> p[THEME_MODE_KEY] = mode } }
    suspend fun setPlayMode(mode: PlayMode) { dataStore.edit { p -> p[PLAY_MODE_KEY] = when(mode) { PlayMode.Random->"random"; PlayMode.Single->"single"; PlayMode.Loop->"loop"; PlayMode.Sequential->"sequential" } } }
    suspend fun saveLastConnection(host: String, share: String, user: String) { dataStore.edit { p -> p[LAST_HOST_KEY]=host; p[LAST_SHARE_KEY]=share; p[LAST_USER_KEY]=user } }
    suspend fun saveLastConnectionWithPassword(host: String, share: String, user: String, password: String) { dataStore.edit { p -> p[LAST_HOST_KEY]=host; p[LAST_SHARE_KEY]=share; p[LAST_USER_KEY]=user; p[LAST_PASSWORD_KEY]=password } }

    suspend fun savePlaybackState(playlist: List<TrackInfo>, currentIndex: Int, positionMs: Long, volume: Float) {
        val s = playlist.joinToString("\n") { t -> "${t.source.name}|${esc(t.title)}|${esc(t.artist)}|${esc(t.album)}|${t.durationMs}|${t.localUri?:""}|${esc(t.smbPath)}|${t.fileSize}" }
        dataStore.edit { p -> p[LAST_PLAYLIST_KEY]=s; p[LAST_INDEX_KEY]=currentIndex.toString(); p[LAST_POSITION_KEY]=positionMs.toString(); p[LAST_VOLUME_KEY]=volume.toString() }
    }

    suspend fun savePlayHistory(history: List<TrackInfo>) {
        val s = history.take(20).joinToString("\n") { t -> "${t.source.name}|${esc(t.title)}|${esc(t.artist)}|${esc(t.album)}|${t.durationMs}|${t.localUri?:""}|${esc(t.smbPath)}|${t.fileSize}" }
        dataStore.edit { p -> p[LAST_HISTORY_KEY] = s }
    }

    fun parsePlaylist(raw: String): List<TrackInfo> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { l ->
            val parts = l.split("|", limit=8)
            if (parts.size < 8) return@mapNotNull null
            TrackInfo(source=runCatching{TrackSource.valueOf(parts[0])}.getOrDefault(TrackSource.SMB), title=unesc(parts[1]), artist=unesc(parts[2]), album=unesc(parts[3]), durationMs=parts[4].toLongOrNull()?:0, localUri=parts[5].ifEmpty{null}, smbPath=unesc(parts[6]), fileSize=parts[7].toLongOrNull()?:0)
        }
    }

    private fun esc(s: String) = s.replace("|", "\\p").replace("\n", "\\n")
    private fun unesc(s: String) = s.replace("\\p", "|").replace("\\n", "\n")

    suspend fun setExcludedFolders(folders: Set<String>) {
        dataStore.edit { p -> p[EXCLUDED_FOLDERS_KEY] = folders.joinToString("|") }
    }
    suspend fun setMinDurationSeconds(seconds: Int) {
        dataStore.edit { p -> p[MIN_DURATION_KEY] = seconds.toString() }
    }
    suspend fun addPlayTime(ms: Long) {
        dataStore.edit { p ->
            val current = (p[TOTAL_PLAY_TIME_KEY] ?: "0").toLongOrNull() ?: 0L
            p[TOTAL_PLAY_TIME_KEY] = (current + ms).toString()
        }
    }
    suspend fun incrementPlayCount(trackId: String) {
        dataStore.edit { p ->
            val raw = p[PLAY_STATS_KEY] ?: ""
            val stats = if (raw.isEmpty()) mutableMapOf() else raw.split("|").associate {
                val parts = it.split(":", limit = 2)
                (parts.getOrNull(0) ?: "") to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }.toMutableMap()
            stats[trackId] = (stats[trackId] ?: 0) + 1
            p[PLAY_STATS_KEY] = stats.entries.joinToString("|") { "${it.key}:${it.value}" }
        }
    }
    suspend fun getPlayStats(): Map<String, Int> {
        val raw = dataStore.data.map { p -> p[PLAY_STATS_KEY] ?: "" }.first()
        if (raw.isEmpty()) return emptyMap()
        return raw.split("|").associate {
            val parts = it.split(":", limit = 2)
            (parts.getOrNull(0) ?: "") to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
    }

    // Multi-playlist management (F3)
    suspend fun savePlaylists(playlists: Map<String, List<TrackInfo>>) {
        val raw = playlists.entries.joinToString("\n---\n") { (name, tracks) ->
            val trackData = tracks.joinToString("\n") { t ->
                "${t.source.name}|${esc(t.title)}|${esc(t.artist)}|${esc(t.album)}|${t.durationMs}|${t.localUri?:""}|${esc(t.smbPath)}|${t.fileSize}"
            }
            "$name\n$trackData"
        }
        dataStore.edit { p -> p[SAVED_PLAYLISTS_KEY] = raw }
    }

    suspend fun loadPlaylists(): Map<String, List<TrackInfo>> {
        val raw = dataStore.data.map { p -> p[SAVED_PLAYLISTS_KEY] ?: "" }.first()
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, List<TrackInfo>>()
        raw.split("\n---\n").forEach { block ->
            val lines = block.lines()
            if (lines.isNotEmpty()) {
                val name = lines[0]
                val tracks = lines.drop(1).mapNotNull { l ->
                    val parts = l.split("|", limit=8)
                    if (parts.size < 8) return@mapNotNull null
                    TrackInfo(source=runCatching{TrackSource.valueOf(parts[0])}.getOrDefault(TrackSource.SMB), title=unesc(parts[1]), artist=unesc(parts[2]), album=unesc(parts[3]), durationMs=parts[4].toLongOrNull()?:0, localUri=parts[5].ifEmpty{null}, smbPath=unesc(parts[6]), fileSize=parts[7].toLongOrNull()?:0)
                }
                result[name] = tracks
            }
        }
        return result
    }

    // SMB Bookmarks (D1)
    suspend fun saveSmbBookmarks(bookmarks: List<SmbBookmark>) {
        val raw = bookmarks.joinToString("\n") { "${esc(it.host)}|${esc(it.share)}|${esc(it.username)}|${esc(it.password)}|${esc(it.displayName)}" }
        dataStore.edit { p -> p[SMB_BOOKMARKS_KEY] = raw }
    }

    suspend fun loadSmbBookmarks(): List<SmbBookmark> {
        val raw = dataStore.data.map { p -> p[SMB_BOOKMARKS_KEY] ?: "" }.first()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { l ->
            val parts = l.split("|", limit=5)
            if (parts.size < 5) return@mapNotNull null
            SmbBookmark(host=unesc(parts[0]), share=unesc(parts[1]), username=unesc(parts[2]), password=unesc(parts[3]), displayName=unesc(parts[4]))
        }
    }
}

data class SmbBookmark(
    val host: String,
    val share: String,
    val username: String = "Guest",
    val password: String = "",
    val displayName: String = ""
)
