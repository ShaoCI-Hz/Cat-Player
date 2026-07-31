package com.hezi.juyumao.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hezi.juyumao.player.audio.LyricsData
import com.hezi.juyumao.ui.lyrics.LyricsView
import kotlinx.coroutines.launch

@Composable
fun CoverLyricsPager(
    artworkUri: String?,
    lyricsData: LyricsData?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    showLyrics: Boolean = false,
    lyricsFontSize: Float = 18f,
    lyricsFontBold: Boolean = true,
    onLineClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // 当外部 showLyrics 变化时，切换到歌词页
    LaunchedEffect(showLyrics) {
        if (showLyrics && pagerState.currentPage == 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(1) }
        } else if (!showLyrics && pagerState.currentPage == 1) {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Column(modifier = modifier) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (page) {
                0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AlbumArtPager(
                            artworkUri = artworkUri,
                            isPlaying = isPlaying,
                            isRound = false,
                            size = 280.dp,
                        )
                    }
                }
                1 -> {
                    LyricsView(
                        lyricsData = lyricsData,
                        currentPositionMs = currentPositionMs,
                        fontSize = lyricsFontSize,
                        fontBold = lyricsFontBold,
                        onLineClick = onLineClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // 页面指示器
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .background(
                            color = if (pagerState.currentPage == index)
                                Color.White else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}
