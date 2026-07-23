package com.example.smbplayer.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * First-run onboarding screen with 4 pages.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.MusicNote,
            title = "欢迎使用 Cat Player",
            description = "一款强大的 Android 音乐播放器，支持 SMB/SFTP/WebDAV 直连 NAS 播放音乐。",
            color = Color(0xFF1ED760)
        ),
        OnboardingPage(
            icon = Icons.Filled.FolderOpen,
            title = "访问您的音乐",
            description = "授予存储权限以扫描本地音乐，或直接连接网络服务器播放。",
            color = Color(0xFF4A90FF)
        ),
        OnboardingPage(
            icon = Icons.Filled.Cloud,
            title = "连接 NAS 服务器",
            description = "支持 SMB、SFTP、WebDAV 三种协议，直连您的网络存储设备。",
            color = Color(0xFFFF9800)
        ),
        OnboardingPage(
            icon = Icons.Filled.Palette,
            title = "个性化您的体验",
            description = "多种主题、均衡器、场景模式，打造专属音乐体验。",
            color = Color(0xFFBB86FC)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF1A1A2E))
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Bottom navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            // Page indicators
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val color = if (pagerState.currentPage == index) {
                        pages[index].color
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    }
                    val width = if (pagerState.currentPage == index) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button
                TextButton(onClick = onComplete) {
                    Text("跳过", color = Color.White.copy(alpha = 0.6f))
                }

                // Next / Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[pagerState.currentPage].color
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "开始使用" else "下一步",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(page.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                null,
                modifier = Modifier.size(64.dp),
                tint = page.color
            )
        }

        Spacer(Modifier.height(48.dp))

        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)
