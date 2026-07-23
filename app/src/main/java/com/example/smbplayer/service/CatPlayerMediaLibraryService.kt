package com.example.smbplayer.service

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Android Auto media library service.
 * Provides media browsing tree for car display.
 */
@AndroidEntryPoint
class CatPlayerMediaLibraryService : MediaLibraryService() {

    @Inject lateinit var mediaSession: MediaSession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return null // Will be implemented with MediaLibrarySession
    }

    override fun onDestroy() {
        mediaSession.player?.stop()
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        // Root node IDs for Android Auto browsing
        const val ROOT_ID = "root"
        const val LOCAL_MUSIC_ID = "local_music"
        const val SMB_MUSIC_ID = "smb_music"
        const val FAVORITES_ID = "favorites"
        const val RECENT_ID = "recent"
    }
}
