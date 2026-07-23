package com.example.smbplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives media control actions from notification buttons.
 */
@AndroidEntryPoint
class MediaControlReceiver : BroadcastReceiver() {

    @Inject lateinit var mediaSession: MediaSession

    override fun onReceive(context: Context, intent: Intent) {
        val player = mediaSession.player ?: return

        when (intent.action) {
            MusicPlaybackService.ACTION_PREV -> {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                }
            }
            MusicPlaybackService.ACTION_PLAY_PAUSE -> {
                player.playWhenReady = !player.playWhenReady
            }
            MusicPlaybackService.ACTION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                }
            }
        }
    }
}
