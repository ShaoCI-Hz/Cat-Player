package com.hezi.juyumao.ui.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hezi.juyumao.player.audio.LyricsData
import com.hezi.juyumao.ui.lyrics.LyricsView

/**
 * 封面/歌词上下滑动切换组件
 * Salt Player 标志性交互：上下滑动在封面和歌词之间切换
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverLyricsPager(
    artworkUri: String?,
    lyricsData: LyricsData?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    lyricsFontSize: Float = 18f,
    lyricsFontBold: Boolean = true,
    onLineClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier = modifier) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (page) {
                0 -> {
                    // 第一页：专辑封面
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
                    // 第二页：歌词
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

        // 页面指示器（两个小点）
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
