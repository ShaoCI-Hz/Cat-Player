---
feature: juyumao-player
status: designed
updated: 2026-07-30
---

# 局域猫播放器 (JuYuMao Player)

## Report

## [S1] Problem

用户在局域网内有 NAS 存储了大量音乐文件（含 Hi-Res 无损格式），需要一款手机端音乐播放器能够：
1. 自动发现并连接局域网内的 SMB/NAS 设备，无需手动配置
2. 直接串流播放 NAS 上的音乐文件，体验类似云端播放
3. 提供丰富动画、流畅交互的现代播放体验
4. 支持全格式音频解码（含 DSD/APE/WAVPACK 等冷门格式）

上一版存在的问题：SMB 连接不可用、页面卡顿，根因是 SMB 操作阻塞主线程 + LazyColumn 未做性能优化。

## [S2] Design

### S2.1 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| UI | Jetpack Compose | 声明式 UI，动画 API 丰富 (spring/AnimatedVisibility/SharedElement/Canvas) |
| 架构 | MVVM | ViewModel + StateFlow + Repository |
| DI | Hilt | Google 官方，与 ViewModel 深度集成 |
| 导航 | Compose Navigation | Type-safe routes |
| SMB | smbj | 纯 Java SMB2/3 客户端，无需 NDK |
| 音频播放 | Media3 ExoPlayer | Google 官方播放器，支持扩展 |
| 音频解码 | ExoPlayer FFmpeg 扩展 | 支持 DSD/APE/WAVPACK 全格式 |
| 网络发现 | mDNS/NetBIOS | 自动发现局域网 SMB 设备 |
| 数据持久化 | Room | 本地数据库，缓存 NAS 文件索引 |
| 图片加载 | Coil | Compose 原生支持，加载专辑封面 |
| 异步 | Kotlin Coroutines + Flow | 全链路响应式 |

### S2.2 性能架构 (防卡顿)

**核心原则：UI 线程零阻塞**

1. **SMB I/O 隔离**
   - 所有 SMB 操作在 `Dispatchers.IO` 独立线程池执行
   - SMB 连接池管理：最大 3 个并发连接，空闲超时 60s 自动回收
   - 文件元数据缓存到 Room，UI 读缓存不走 SMB
   - 音频流式传输：边下边播，不等整个文件下载完

2. **列表性能**
   - `LazyColumn` 强制使用 `key` 参数（文件路径 hash）
   - 列表项 Composable 标记 `@Stable`，避免不必要重组
   - 大列表用 Paging3 分页加载（每页 50 条）
   - 列表项类型分离 (`contentType`) 优化回收池

3. **Compose 稳定性**
   - 所有数据类标记 `@Immutable` 或 `@Stable`
   - 使用 `derivedStateOf` 减少派生状态重组
   - 使用 `snapshotFlow` 桥接 Compose 和 Coroutine
   - 编译器指标检查：确保跳过率 > 90%

4. **内存管理**
   - 封面图片：LRU 缓存 (最大 50MB)，大图下采样
   - 音频缓冲：ExoPlayer 自适应缓冲策略
   - SMB 文件句柄：用完即关，避免泄漏

### S2.3 项目结构

```
app/src/main/java/com/hezi/juyumao/
├── JuYuMaoApplication.kt          # Hilt Application
├── MainActivity.kt                 # 单 Activity
├── di/                              # Hilt 模块
│   ├── AppModule.kt                # 全局单例
│   ├── SmbModule.kt               # SMB 相关依赖
│   └── PlayerModule.kt            # 播放器相关依赖
├── data/                            # 数据层
│   ├── local/
│   │   ├── db/
│   │   │   ├── JuYuMaoDatabase.kt
│   │   │   ├── dao/               # SongDao, PlaylistDao, ServerDao
│   │   │   └── entity/            # SongEntity, PlaylistEntity, ServerEntity
│   │   └── datastore/
│   │       └── SettingsDataStore.kt
│   ├── remote/
│   │   ├── smb/
│   │   │   ├── SmbClient.kt       # smbj 封装
│   │   │   ├── SmbConnectionPool.kt
│   │   │   ├── SmbFileScanner.kt   # 文件扫描+元数据提取
│   │   │   └── SmbStreamSource.kt  # 流式音频源
│   │   └── discovery/
│   │       ├── SmbDiscovery.kt     # mDNS 自动发现
│   │       └── NetworkMonitor.kt   # 网络状态监听
│   └── repository/
│       ├── MusicRepository.kt      # 音乐数据统一入口
│       ├── SmbRepository.kt        # SMB 连接管理
│       └── SettingsRepository.kt
├── domain/                          # 领域层
│   ├── model/
│   │   ├── Song.kt
│   │   ├── Album.kt
│   │   ├── Playlist.kt
│   │   ├── SmbServer.kt
│   │   └── PlaybackState.kt
│   └── usecase/
│       ├── ScanMusicUseCase.kt
│       ├── ConnectSmbUseCase.kt
│       └── SearchMusicUseCase.kt
├── player/                          # 播放引擎
│   ├── MusicPlayerService.kt       # Media3 MediaSessionService
│   ├── PlaybackQueue.kt           # 播放队列管理
│   ├── AudioEffectsManager.kt      # 均衡器+音效
│   └── SmbMediaSource.kt          # 自定义 MediaSource for SMB
├── ui/                              # UI 层
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt               # Dark + Light 双主题
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt              # Type-safe routes
│   ├── components/                  # 通用组件
│   │   ├── GlassMorphism.kt       # 毛玻璃效果
│   │   ├── AnimatedIconButton.kt
│   │   ├── PulsingGlow.kt         # 脉冲光晕
│   │   ├── RotatingAlbumArt.kt    # 旋转封面
│   │   ├── PremiumBottomNavBar.kt
│   │   └── MiniPlayerBar.kt       # 底部迷你播放条
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── browse/
│   │   ├── BrowseScreen.kt
│   │   ├── BrowseViewModel.kt
│   │   ├── FolderBrowserScreen.kt
│   │   └── AlbumBrowserScreen.kt
│   ├── player/
│   │   ├── PlayerScreen.kt        # 全屏播放器
│   │   ├── PlayerViewModel.kt
│   │   └── LyricsView.kt         # 歌词显示
│   ├── queue/
│   │   ├── QueueScreen.kt
│   │   └── QueueViewModel.kt
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   ├── smb/
│   │   ├── SmbConnectScreen.kt    # SMB 连接管理
│   │   ├── SmbViewModel.kt
│   │   └── SmbDiscoverySheet.kt   # 自动发现弹窗
│   ├── equalizer/
│   │   ├── EqualizerScreen.kt
│   │   └── EqualizerViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── sleep/
│   │   └── SleepTimerSheet.kt
│   └── widget/
│       └── JuYuMaoWidget.kt       # Glance Widget
└── util/
    ├── AudioMetadata.kt           # 音频元数据提取
    ├── FormatUtils.kt
    └── NetworkUtils.kt
```

### S2.4 UI 设计规范

基于设计稿 v3.27.0，以下为核心规范：

#### 色彩系统

| 名称 | 暗色模式 | 浅色模式 | 用途 |
|------|----------|----------|------|
| Primary | #1ED760 | #1ED760 | 强调色、选中态、播放按钮 |
| Background | #0A0A0A | #FFFFFF | 全局背景 |
| Surface | #121212 | #F5F5F5 | 卡片、底栏 |
| Card | #252525 | #E8E8E8 | 列表项 |
| OnSurface | #FFFFFF | #1F1F1F | 主文字 |
| OnSurfaceVariant | #B3B3B3 | #666666 | 次要文字 |
| HiRes Gold | #FFD700 | #FFD700 | Hi-Res 标签 |

#### 动画规范 (关键)

| 场景 | 类型 | 参数 | 说明 |
|------|------|------|------|
| 封面旋转 | 无限动画 | 20s/圈, Linear | 播放时旋转，暂停停止 |
| 封面缩放 | Spring | damping=0.6, stiffness=300 | 暂停 0.95x → 播放 1.0x |
| 脉冲光晕 | 无限动画 | 2s/周期, Linear | 呼吸效果 |
| 播放按钮 | Spring | damping=0.4, stiffness=600 | 快速弹跳 |
| Tab 切换 | Spring | damping=0.65, stiffness=420 | 平滑过渡 |
| 图标缩放 | Spring | damping=0.35, stiffness=500 | 明显弹跳 |
| 底栏指示器 | Spring | damping=0.5, stiffness=500 | 滑动跟随 |
| 列表项进入 | AnimatedVisibility | fadeIn + slideInVertically | 交错动画 |
| 页面转场 | SharedElement | 共享封面+标题 | 列表→播放器 |

#### 圆角系统

| 名称 | 值 | 用途 |
|------|-----|------|
| extraSmall | 6dp | 小标签 |
| small | 8dp | 按钮、缩略图 |
| medium | 14dp | 卡片 |
| large | 20dp | 弹窗 |
| extraLarge | 28dp | 底栏胶囊 |

#### 间距系统

| 名称 | 值 | 用途 |
|------|-----|------|
| screenPadding | 16dp | 屏幕水平边距 |
| cardPadding | 12dp | 卡片内边距 |
| itemPadding | 10dp | 列表项内边距 |
| spacingSmall | 8dp | 小间距 |
| spacingMedium | 12dp | 中间距 |
| spacingLarge | 16dp | 大间距 |
| spacingXLarge | 24dp | 特大间距 |

### S2.5 页面设计

#### 1. 首页 (HomeScreen)

- **统计卡片区**：4 个卡片横排显示（歌曲数、专辑数、播放次数、存储容量），每个卡片带图标 + 数字
- **最近播放**：水平滚动的专辑封面卡片，带播放动画
- **快捷入口**：SMB 连接状态、均衡器、定时关闭
- **迷你播放条**：底部常驻，显示封面缩略图 + 标题 + 播放/暂停按钮

#### 2. 音乐浏览 (BrowseScreen)

- **Tab 切换**：文件夹 / 专辑 / 艺术家 / 流派，带弹性指示器动画
- **文件夹浏览**：树形结构，支持面包屑导航
- **专辑网格**：2 列网格，封面带渐变遮罩 + 专辑名
- **列表模式**：48dp 缩略图 + 标题 + 副标题 + 时长，当前播放项左侧绿色竖条

#### 3. 播放器 (PlayerScreen)

- **封面**：240dp 圆形，20dp 阴影，播放时旋转，暂停时缩放+停止旋转
- **光晕**：封面背后的脉冲呼吸光晕，颜色跟随封面主色
- **进度条**：3dp 高，白色渐变，触摸区域 24dp，支持拖拽 seek
- **控件**：播放(72dp 绿色渐变)、上/下一首(56dp)、Shuffle/Repeat(44dp)
- **歌词**：逐行高亮滚动，支持逐字显示
- **手势**：下滑关闭播放器（带弹性动画）

#### 4. SMB 连接 (SmbConnectScreen)

- **自动发现**：打开页面自动扫描局域网 SMB 设备，显示在列表中
- **手动添加**：输入 IP、端口、用户名、密码、共享名
- **连接状态**：实时显示连接状态（连接中/已连接/失败）
- **已保存服务器**：记住上次连接的服务器，支持自动重连
- **文件路径选择**：连接后浏览选择音乐文件夹

#### 5. 搜索 (SearchScreen)

- **实时搜索**：输入即搜，300ms 防抖
- **搜索范围**：标题、艺术家、专辑、文件名
- **结果分类**：按歌曲/专辑/艺术家分组展示
- **高亮匹配**：搜索关键词高亮

#### 6. 播放队列 (QueueScreen)

- **当前播放**：顶部高亮当前曲目
- **拖拽排序**：长按拖拽调整顺序
- **滑动删除**：左滑移除
- **清空队列**：一键清空

#### 7. 均衡器 (EqualizerScreen)

- **预设模式**：普通/摇滚/流行/古典/爵士/自定义
- **频段调节**：5-10 频段滑块，带弹性动画
- **可视化**：频谱实时显示

#### 8. 设置 (SettingsScreen)

- **主题切换**：深色/浅色/跟随系统
- **SMB 设置**：连接超时、并发数、缓存策略
- **音频设置**：输出格式、缓冲大小、无缝播放
- **关于**：版本号、开源许可

#### 9. 桌面 Widget (JuYuMaoWidget)

- **信息显示**：封面缩略图 + 标题 + 艺术家
- **控制按钮**：上一首/播放暂停/下一首
- **尺寸**：4x1 或 4x2

#### 10. 通知栏控制

- **MediaStyle 通知**：封面 + 标题 + 控制按钮
- **锁屏控制**：锁屏界面显示播放信息和控制

### S2.6 SMB 核心设计

#### 连接流程

```
1. App 启动 → 检查网络状态 (WiFi?)
2. mDNS 扫描 `_smb._tcp` 服务 → 发现 NAS
3. 用户选择/手动输入服务器
4. smbj 建立 SMB2/3 连接 (Dispatchers.IO)
5. 浏览共享文件夹 → 用户选择音乐目录
6. 扫描目录 → 提取元数据 → 缓存到 Room
7. 用户播放 → SmbMediaSource 流式读取 → ExoPlayer 解码
```

#### 流式播放架构

```
NAS (SMB Share)
    ↓ smbj SMBRead (chunk 64KB)
SmbStreamSource (BufferedSource)
    ↓
SmbMediaSource (Custom MediaSource)
    ↓
ExoPlayer (解码 + 音效)
    ↓
AudioTrack (输出)
```

#### 关键设计决策

1. **不下载整个文件**：使用 smbj 的 `InputStream` 读取，边读边播
2. **预缓冲**：开始播放前预读 256KB，避免卡顿
3. **Seek 支持**：通过 SMB 的 `seek()` 方法实现，不重新下载
4. **断线重连**：网络切换时自动重连，播放位置记忆
5. **多 NAS 支持**：可同时连接多个 NAS，统一浏览

### S2.7 音频解码方案

| 格式 | 解码方案 | 说明 |
|------|----------|------|
| MP3 | ExoPlayer 原生 | DefaultRenderersFactory |
| AAC | ExoPlayer 原生 | DefaultRenderersFactory |
| FLAC | ExoPlayer 原生 | DefaultRenderersFactory |
| WAV | ExoPlayer 原生 | DefaultRenderersFactory |
| OGG | ExoPlayer 原生 | DefaultRenderersFactory |
| DSD (DSF/DFF) | FFmpeg 扩展 | 需编译 FFmpeg so 库 |
| APE | FFmpeg 扩展 | 需编译 FFmpeg so 库 |
| WavPack | FFmpeg 扩展 | 需编译 FFmpeg so 库 |
| OPUS | ExoPlayer 原生 | DefaultRenderersFactory |

### S2.8 错误处理

| 场景 | 处理方式 |
|------|----------|
| SMB 连接失败 | 显示重试按钮 + 手动输入选项 |
| SMB 连接超时 | 30s 超时，提示检查网络 |
| 文件读取错误 | 跳到下一首 + Toast 提示 |
| 网络断开 | 暂停播放 + 提示重连 |
| 不支持的格式 | 提示格式不支持 + 跳过 |
| 权限被拒 | 引导用户授权 |

## [S3] Out of Scope

- iOS 版本（仅 Android）
- 在线流媒体服务集成（Spotify/Apple Music 等）
- 音乐下载到本地功能（仅串流播放）
- 社交功能（分享、评论）
- 多语言国际化（第一版仅中文）
- 蓝牙设备管理（使用系统默认）
- 横屏/平板适配（第一版仅竖屏手机）
- 云同步（跨设备同步播放列表）

## Tasks

### 基础设施

- [ ] T1: 项目初始化 — 创建 Android 项目，配置 Gradle (Compose BOM, Hilt, Media3, smbj, Room, Coil, Navigation)，设置 minSdk=29, targetSdk=35 (covers: S2.1, S2.3)
- [ ] T2: 主题系统 — 实现 Color.kt/Theme.kt/Type.kt/Shape.kt，支持 Dark+Light 双主题切换 (covers: S2.4)
- [ ] T3: 导航框架 — 实现 NavGraph.kt + Screen.kt，配置底部导航栏路由 (covers: S2.3, S2.5)

### 核心组件

- [ ] T4: 通用 UI 组件 — 实现 GlassMorphism, AnimatedIconButton, PulsingGlow, RotatingAlbumArt, PremiumBottomNavBar, MiniPlayerBar (covers: S2.4, S2.5)
- [ ] T5: 数据库层 — 实现 Room 数据库 (SongEntity, PlaylistEntity, ServerEntity) + DAO + Migration (covers: S2.3)
- [ ] T6: Settings DataStore — 实现主题/音频/SMB 配置的持久化存储 (covers: S2.5)

### SMB 模块

- [ ] T7: SMB 客户端封装 — 基于 smbj 实现 SmbClient，封装连接/认证/文件浏览/流式读取 (covers: S2.6)
- [ ] T8: SMB 连接池 — 实现 SmbConnectionPool，管理多连接、超时回收、并发控制 (covers: S2.6, S2.2)
- [ ] T9: SMB 自动发现 — 基于 mDNS 实现局域网 SMB 设备自动扫描 (covers: S2.6)
- [ ] T10: SMB 连接 UI — 实现 SmbConnectScreen + SmbDiscoverySheet + SmbViewModel (covers: S2.5)
- [ ] T11: 网络状态监听 — 实现 NetworkMonitor，WiFi 切换/断开时自动处理连接 (covers: S2.6, S2.8)

### 音频引擎

- [ ] T12: ExoPlayer 集成 — 配置 Media3 ExoPlayer + FFmpeg 扩展，支持全格式解码 (covers: S2.7)
- [ ] T13: SMB MediaSource — 实现自定义 MediaSource，从 SMB 流式读取音频数据 (covers: S2.6)
- [ ] T14: 播放队列 — 实现 PlaybackQueue，管理队列/随机/循环模式 (covers: S2.5)
- [ ] T15: MediaSession 服务 — 实现 MusicPlayerService，支持通知栏/锁屏控制 (covers: S2.5)
- [ ] T16: 均衡器 — 实现 AudioEffectsManager + EqualizerScreen，预设+自定义频段 (covers: S2.5)

### 数据层

- [ ] T17: 文件扫描器 — 实现 SmbFileScanner，递归扫描目录提取音频元数据 (covers: S2.6)
- [ ] T18: MusicRepository — 实现统一数据入口，合并本地缓存和 SMB 数据 (covers: S2.3)
- [ ] T19: Paging 集成 — 实现 PagingSource，大列表分页加载 (covers: S2.2)

### 页面实现

- [ ] T20: 首页 — 实现 HomeScreen + HomeViewModel，统计卡片+最近播放+快捷入口 (covers: S2.5)
- [ ] T21: 音乐浏览 — 实现 BrowseScreen，文件夹/专辑/艺术家/流派 Tab 浏览 (covers: S2.5)
- [ ] T22: 播放器页面 — 实现 PlayerScreen，封面旋转+光晕+进度条+控件+手势 (covers: S2.5)
- [ ] T23: 歌词显示 — 实现 LyricsView，逐行/逐字高亮滚动 (covers: S2.5)
- [ ] T24: 搜索 — 实现 SearchScreen + SearchViewModel，实时搜索+结果分组 (covers: S2.5)
- [ ] T25: 播放队列页面 — 实现 QueueScreen，拖拽排序+滑动删除 (covers: S2.5)
- [ ] T26: 设置页面 — 实现 SettingsScreen，主题/SMB/音频/关于配置 (covers: S2.5)

### 扩展功能

- [ ] T27: 定时关闭 — 实现 SleepTimerSheet，倒计时停止播放 (covers: S2.5)
- [ ] T28: 桌面 Widget — 基于 Glance 实现 JuYuMaoWidget (covers: S2.5)
- [ ] T29: 歌词获取 — 实现本地 .lrc 文件解析和匹配 (covers: S2.5)

### 测试

- [ ] T30: 单元测试 — SMB 连接、播放队列、Repository、ViewModel 测试 (covers: S2.6, S2.2)
- [ ] T31: 集成测试 — 完整播放流程、SMB 连接流程测试 (covers: S2.6, S2.7)

### 依赖关系

```
T1 → T2, T3, T5, T7, T12
T2 → T4
T3 → T20, T21, T22, T24, T25, T26
T4 → T20, T21, T22
T5 → T17, T18
T7 → T8, T9, T13, T17
T8 → T10
T9 → T10
T12 → T13, T16
T13 → T14
T14 → T15
T18 → T19
T19 → T20, T21
T22 → T23
T14 → T25
```
