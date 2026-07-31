package com.hezi.juyumao.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hezi.juyumao.ui.player.components.*

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val artworkUri by viewModel.artworkUri.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState()
    val lyricsFontBold by viewModel.lyricsFontBold.collectAsState()

    var isImmersive by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isFavorite by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ===== 背景层：封面模糊 + 主色调 + 流光 =====
        PlayerBackground(artworkUri = artworkUri)

        // ===== 内容层 =====
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶栏
            PlayerTopBar(
                onBack = onBack,
                title = if (isImmersive) "" else "正在播放",
                onImmersiveToggle = { isImmersive = !isImmersive },
                isImmersive = isImmersive,
            )

            // 封面/歌词上下滑动切换
            CoverLyricsPager(
                artworkUri = artworkUri,
                lyricsData = lyrics,
                currentPositionMs = position,
                isPlaying = isPlaying,
                showLyrics = showLyrics,
                lyricsFontSize = lyricsFontSize,
                lyricsFontBold = lyricsFontBold,
                onLineClick = { viewModel.seekTo(it) },
                modifier = Modifier.weight(1f),
            )

            // 歌曲信息（完整模式才显示）
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        currentSong?.title ?: "未知歌曲",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val subtitle = buildString {
                        append(currentSong?.artist ?: "未知艺术家")
                        if (!currentSong?.album.isNullOrEmpty() && currentSong?.album != "未知专辑") {
                            append(" · ")
                            append(currentSong!!.album)
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            PlayerSlider(
                position = position,
                duration = duration,
                onSeek = { viewModel.seekTo(it) },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮
            if (isImmersive) {
                ImmersiveControlRow(
                    isPlaying = isPlaying,
                    onPlayPause = { viewModel.togglePlay() },
                    onPrevious = { viewModel.previous() },
                    onNext = { viewModel.next() },
                )
            } else {
                FullControlRow(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onPrevious = { viewModel.previous() },
                    onPlayPause = { viewModel.togglePlay() },
                    onNext = { viewModel.next() },
                    onShuffle = {
                        shuffleEnabled = !shuffleEnabled
                        viewModel.setShuffle(shuffleEnabled)
                    },
                    onRepeat = {
                        repeatMode = (repeatMode + 1) % 3
                        viewModel.setRepeat(repeatMode)
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部功能栏（完整模式才显示）
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200)),
            ) {
                BottomFunctionBar(
                    showLyrics = showLyrics,
                    isFavorite = isFavorite,
                    onLyricsClick = { showLyrics = !showLyrics },
                    onQueueClick = { },
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onMoreClick = { },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
