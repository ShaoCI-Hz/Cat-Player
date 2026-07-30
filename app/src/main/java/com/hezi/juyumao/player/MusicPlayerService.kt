package com.hezi.juyumao.player

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 音乐播放服务
 * MediaSessionService 自动管理通知栏控件（上一首/播放暂停/下一首）
 * 不需要手动创建通知
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(MediaSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        // 通知栏控件的回调由系统自动处理
        // play/pause/next/prev 都通过 MediaSession 的 Player 接口自动映射
    }
}
