package com.example.smbplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.smbplayer.MainActivity
import com.example.smbplayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundWhenRequired: Boolean) {
        val player = session.player ?: return
        val mediaItem = player.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: getString(R.string.app_name)
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentIntent)
            .setOngoing(player.playWhenReady)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            // Previous button
            .addAction(
                R.drawable.ic_skip_previous,
                "上一首",
                buildPendingIntent(ACTION_PREV)
            )
            // Play/Pause button
            .addAction(
                if (player.playWhenReady) R.drawable.ic_pause else R.drawable.ic_play,
                if (player.playWhenReady) "暂停" else "播放",
                buildPendingIntent(ACTION_PLAY_PAUSE)
            )
            // Next button
            .addAction(
                R.drawable.ic_skip_next,
                "下一首",
                buildPendingIntent(ACTION_NEXT)
            )
            .build()

        if (startInForegroundWhenRequired) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaControlReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        mediaSession.player?.stop()
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示当前播放曲目和控制按钮"
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "smb_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PREV = "com.example.smbplayer.PREV"
        const val ACTION_PLAY_PAUSE = "com.example.smbplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.smbplayer.NEXT"
    }
}
