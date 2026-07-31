package com.hezi.juyumao.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.player.PlaybackStateHolder
import com.hezi.juyumao.player.audio.AudioEffectsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        playbackStateHolder: PlaybackStateHolder,
        audioEffectsManager: AudioEffectsManager,
    ): ExoPlayer {
        val player = ExoPlayer.Builder(context)
            // CRITICAL-3: 音频焦点处理（来电暂停、其他 app 播放时暂停）
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            // MEDIUM: SMB 流式播放防休眠
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        playbackStateHolder.bindPlayer(player)
        audioEffectsManager.attachToPlayer(player)
        return player
    }
}
