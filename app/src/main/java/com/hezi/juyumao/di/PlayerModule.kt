package com.hezi.juyumao.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.data.repository.SettingsRepository
import com.hezi.juyumao.player.DynamicLoadControl
import com.hezi.juyumao.player.PlaybackStateHolder
import com.hezi.juyumao.player.audio.AudioEffectsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        settingsRepository: SettingsRepository,
        dynamicLoadControl: DynamicLoadControl,
    ): ExoPlayer {
        // 扩展解码器按需启用（FLAC/WAV/MP3/AAC/OGG/OPUS 走原生；将来加入 FFmpeg 扩展自动接管不支持的格式）
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        // 用户设置的缓冲大小（KB）作为初始档位，播放时由 PlaybackController 动态更新
        val initialBufferKb = try {
            runBlocking { settingsRepository.audioBufferSize.first() }
        } catch (_: Exception) {
            256
        }
        dynamicLoadControl.updateSettings(initialBufferKb, isHiRes = false)

        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(dynamicLoadControl)
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
