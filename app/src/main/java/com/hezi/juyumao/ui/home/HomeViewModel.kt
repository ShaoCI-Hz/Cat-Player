package com.hezi.juyumao.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hezi.juyumao.data.local.db.entity.SongEntity
import com.hezi.juyumao.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DailyCardData(
    val greeting: String,
    val dateText: String,
    val quote: String,
    val quoteAuthor: String,
    val weatherText: String? = null,
)

data class HomeUiState(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val playCount: Long = 0,
    val totalSize: Long = 0L,
    val isScanning: Boolean = false,
    val scanMessage: String = "",
    val recentlyPlayed: List<SongEntity> = emptyList(),
    val dailyCard: DailyCardData = generateDailyCard(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                musicRepository.getSongCount(),
                musicRepository.getTotalSize(),
                musicRepository.getAlbumCount(),
                musicRepository.getArtistCount(),
            ) { count, size, albums, artists ->
                _uiState.value.copy(
                    songCount = count, totalSize = size ?: 0L,
                    albumCount = albums, artistCount = artists,
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch {
            musicRepository.getRecentlyPlayed().collect { songs ->
                _uiState.value = _uiState.value.copy(recentlyPlayed = songs)
            }
        }
        // 异步获取天气
        refreshWeather()
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanMessage = "扫描中...")
            val result = musicRepository.scanLocalMusic()
            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(isScanning = false, scanMessage = "扫描完成，找到 $count 首歌曲")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isScanning = false, scanMessage = "扫描失败: ${e.message}")
                },
            )
        }
    }

    private fun refreshWeather() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val conn = java.net.URL("https://wttr.in/?format=%t+%C&lang=zh").openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "curl/7.64.1")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val weather = conn.inputStream.bufferedReader().readText().trim()
                conn.disconnect()
                if (weather.isNotEmpty() && !weather.contains("Unknown") && !weather.contains("ERROR")) {
                    _uiState.value = _uiState.value.copy(
                        dailyCard = _uiState.value.dailyCard.copy(weatherText = weather)
                    )
                }
            } catch (_: Exception) {}
        }
    }
}

private val musicQuotes = listOf(
    "音乐是灵魂的语言，它能说出语言无法表达的东西。" to "贝多芬",
    "没有音乐，生命是一个错误。" to "尼采",
    "音乐是比一切智慧、一切哲学更高的启示。" to "贝多芬",
    "当我听到音乐时，我便忘记了自己。" to "玛丽莲·梦露",
    "音乐是思维着的声音。" to "雨果",
    "最好的音乐是在你最需要的时候听到的。" to "爱默生",
    "音乐是人类的通用语言。" to "朗费罗",
    "音乐是生活中最美好的一面。" to "丘吉尔",
    "没有音乐的世界是不完整的。" to "莫扎特",
    "音乐能抚慰野蛮的胸膛，能软化坚硬的石头。" to "莎士比亚",
    "音乐是唯一可以纵情而不会损害道德和宗教观念的享受。" to "爱迪生",
    "音乐中蕴藏着如此悦耳的催人奋进的力量。" to "弥尔顿",
    "音乐是建筑在音响基础上的艺术。" to "海顿",
    "音乐使一个民族的气质更高贵。" to "福楼拜",
    "音乐是开启人类智慧宝库的钥匙。" to "雨果",
    "音乐是耳朵的眼睛。" to "塞万提斯",
    "没有热情，就不可能创造出任何真正的艺术作品。" to "舒曼",
    "音乐是心灵的进发。" to "柏辽兹",
    "音乐是上天给人类最伟大的礼物。" to "肖邦",
    "音乐表达的是无法用语言说出的东西。" to "雨果",
    "音乐是不假任何外力，直接沁人心脾的最纯的感情火焰。" to "李斯特",
    "音乐用理想的纽带把人类结合在一起。" to "瓦格纳",
    "音乐是人生的艺术。" to "施特劳斯",
    "没有早期音乐教育，干什么事我都会一事无成。" to "爱因斯坦",
    "音乐应当使人类的精神爆发出火花。" to "贝多芬",
    "此曲只应天上有，人间能得几回闻。" to "杜甫",
    "嘈嘈切切错杂弹，大珠小珠落玉盘。" to "白居易",
    "清风吹歌入空去，歌曲自绕行云飞。" to "李白",
    "音乐，是人生最大的快乐；音乐，是生活中的一股清泉。" to "冼星海",
    "真正创作音乐的是人民，作曲家只不过把它们编成曲子而已。" to "格林卡",
    "不爱音乐不配做人。" to "黑格尔",
    "要尊崇过去的遗产，但也要一片至诚地迎接新的萌芽。" to "舒曼",
    "技术只有到了高尚的手中，才会变得像歌唱一样优美。" to "李斯特",
    "假如我的音乐只能使人愉快，那我很遗憾，我的目的是使人高尚。" to "亨德尔",
    "通过音乐并在音乐中教育我们的孩子。" to "海伦·凯勒",
    "对美的感知和理解是审美教育的核心。" to "苏霍姆林斯基",
    "音乐教育并不是音乐家的教育，而首先是人的教育。" to "苏霍姆林斯基",
    "一字新声一颗珠，转喉疑是击珊瑚。" to "薛能",
)

fun generateDailyCard(): DailyCardData {
    val now = LocalDateTime.now()
    val greeting = when (now.hour) {
        in 0..5 -> "夜深了"
        in 6..8 -> "早上好"
        in 9..11 -> "上午好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..21 -> "晚上好"
        else -> "夜深了"
    }
    val weekDay = arrayOf("星期一","星期二","星期三","星期四","星期五","星期六","星期日")[now.dayOfWeek.value - 1]
    val dateText = "${now.monthValue}月${now.dayOfMonth}日 $weekDay"
    val seed = now.toLocalDate().toEpochDay().toInt()
    val (quote, author) = musicQuotes[seed % musicQuotes.size]
    return DailyCardData(greeting = greeting, dateText = dateText, quote = quote, quoteAuthor = author)
}
