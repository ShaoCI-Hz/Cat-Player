package com.example.smbplayer.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smbplayer.data.lyrics.LyricLine

@Composable
fun LyricView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) return

    var lastIdx by remember { mutableIntStateOf(0) }
    val currentIndex = if (lastIdx in lyrics.indices && currentPositionMs >= lyrics[lastIdx].timestampMs) {
        while (lastIdx + 1 < lyrics.size && lyrics[lastIdx + 1].timestampMs <= currentPositionMs) lastIdx++
        lastIdx
    } else {
        lastIdx = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
        lastIdx
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex in 0 until lyrics.size) {
            listState.animateScrollToItem((currentIndex - 3).coerceIn(0, (lyrics.size - 1).coerceAtLeast(0)))
        }
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxWidth().height(120.dp)) {
        itemsIndexed(lyrics) { i, line ->
            val isCurrent = i == currentIndex
            Text(
                text = line.text,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = if (isCurrent) 16.sp else 13.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
