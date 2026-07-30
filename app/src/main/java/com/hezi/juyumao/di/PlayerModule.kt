package com.hezi.juyumao.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.hezi.juyumao.player.PlaybackStateHolder
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
    ): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        // 绑定到全局状态持有者，让进度条、通知栏等共享同一个播放器状态
        playbackStateHolder.bindPlayer(player)
        return player
    }
}
