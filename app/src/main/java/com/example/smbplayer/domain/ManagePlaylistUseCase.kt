package com.example.smbplayer.domain

import com.example.smbplayer.data.player.PlayMode
import com.example.smbplayer.data.player.TrackInfo
import kotlin.random.Random
import javax.inject.Inject

class ManagePlaylistUseCase @Inject constructor() {
    fun getNextIndex(
        currentIndex: Int,
        playlistSize: Int,
        playMode: PlayMode
    ): Int {
        if (playlistSize == 0) return -1
        return when (playMode) {
            PlayMode.Sequential -> {
                val next = currentIndex + 1
                if (next >= playlistSize) -1 else next
            }
            PlayMode.Random -> Random.nextInt(playlistSize)
            PlayMode.Single -> currentIndex
            PlayMode.Loop -> (currentIndex + 1) % playlistSize
        }
    }

    fun getPrevIndex(
        currentIndex: Int,
        playlistSize: Int,
        playMode: PlayMode
    ): Int {
        if (playlistSize == 0) return -1
        return when (playMode) {
            PlayMode.Sequential -> {
                val prev = currentIndex - 1
                if (prev < 0) -1 else prev
            }
            PlayMode.Random -> Random.nextInt(playlistSize)
            PlayMode.Single -> currentIndex
            PlayMode.Loop -> if (currentIndex - 1 < 0) playlistSize - 1 else currentIndex - 1
        }
    }

    fun moveTrack(
        playlist: List<TrackInfo>,
        fromIndex: Int,
        toIndex: Int
    ): List<TrackInfo> {
        val mutable = playlist.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }
}
