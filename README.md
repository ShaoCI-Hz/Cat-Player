# Cat Player

Android 音乐播放器，支持 SMB/SFTP/WebDAV 直连 NAS 播放，同时支持本地音乐。

![API](https://img.shields.io/badge/API-24%2B-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![Compose](https://img.shields.io/badge/Compose-BOM-2024-blueviolet)

## 功能

### 音乐播放
- SMB/NAS 直连播放，无需挂载系统共享
- SFTP (SSH) 协议支持
- WebDAV 协议支持
- 本地音乐自动扫描，支持 mp3/flac/ogg/wav/aac/wma/opus/m4a/ape 等格式
- 无缝播放 (Gapless)，曲间 <50ms
- Hi-Res 高保真输出检测 (24bit/192kHz)
- 元数据提取：标题、艺术家、专辑、封面、歌词

### 音频效果
- 30 段图形均衡器，10 种预设
- 低音增强
- ReplayGain 响度标准化
- 声道平衡控制
- 音调/速度独立调节 (0.5x-2.0x)
- Crossfade 淡入淡出 (可配置时长)
- AB 循环
- 音频频谱可视化

### 歌词
- LRC 逐行同步歌词
- SYLT/Enhanced LRC 逐字同步歌词
- 全屏歌词页面
- 嵌入式歌词读取 (ID3 USLT/SYLT)

### 播放管理
- 播放队列管理 (拖拽排序、滑动删除)
- 多播放列表 (命名列表、持久化)
- 智能播放列表 (最近添加、随机、短歌曲、长歌曲)
- 收藏夹
- 播放历史
- 播放统计 (次数、时长、Top 10)
- 文件夹浏览模式
- 搜索 (歌曲、专辑、艺术家)
- 排序 (标题/歌手/专辑/时长/添加时间)
- 标签编辑器 (ID3 写入)
- 重复歌曲检测
- 睡眠定时器
- .m3u 导入导出

### 界面
- 暗色/浅色/跟随系统主题
- 10 种主题配色预设
- 手势操作 (封面滑动切歌/调音量)
- 首页仪表盘 (问候语、统计、推荐)
- 骨架屏加载动画
- 首次运行引导页

### 系统集成
- MediaSession + 通知栏控制
- 锁屏控制
- 桌面 Widget (4x1)
- Quick Settings Tile
- Android Auto 支持
- DLNA 设备发现
- 场景模式 (驾驶/睡眠/专注/派对)
- 分享卡片/歌词海报生成
- 数据备份/恢复

## 技术栈

| 模块 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| 音频 | Media3 ExoPlayer 1.4.1 |
| SMB | smbj 0.13.0 |
| SFTP | SSHJ 0.35.0 |
| WebDAV | OkHttp 4.12.0 |
| DI | Hilt + KSP |
| 存储 | DataStore Preferences |
| 图片 | Coil 2.7.0 |
| 元数据 | jaudiotagger 3.0.1 |
| Widget | Glance 1.1.1 |
| 构建 | AGP 8.7.3 + Gradle 8.9 |

## 架构

```
com.example.smbplayer/
├── data/
│   ├── audio/          # 音频效果、设备管理
│   ├── download/       # 离线下载
│   ├── dlna/           # DLNA 发现
│   ├── favorites/      # 收藏
│   ├── local/          # 本地音乐扫描
│   ├── lyrics/         # 歌词解析
│   ├── metadata/       # 元数据提取
│   ├── player/         # ExoPlayer 封装
│   ├── playlist/       # 播放列表
│   ├── settings/       # 设置持久化
│   ├── smb/            # SMB 连接
│   ├── sftp/           # SFTP 连接
│   ├── tag/            # 标签编辑
│   └── webdav/         # WebDAV 连接
├── domain/
│   ├── ConnectUseCase
│   ├── ManagePlaylistUseCase
│   ├── PlayAudioUseCase
│   └── ReplayGainProcessor
├── ui/
│   ├── connect/
│   ├── favorites/
│   ├── library/
│   ├── navigation/
│   ├── onboarding/
│   ├── player/
│   ├── playlist/
│   ├── settings/
│   └── theme/
├── analytics/          # 崩溃报告
├── service/            # 前台播放服务
├── smart/              # 推荐引擎、场景模式
├── share/              # 分享卡片、歌词海报
└── di/                 # Hilt 模块
```

## 编译

环境要求：
- JDK 17
- Android SDK 36
- Gradle 8.9

```bash
git clone https://github.com/ShaoCI-Hz/Cat-Player.git
cd Cat-Player
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

APK 输出: `app/build/outputs/apk/debug/app-debug.apk`

前往 [Releases](https://github.com/ShaoCI-Hz/Cat-Player/releases) 下载最新 APK。

## 更新日志

### v3.9.0 (2026-07-24) — 视觉质感升级
- 全局色彩对比度提升：surfaceVariant 与背景色层次更分明
- 圆角系统升级：small 8dp / medium 14dp / large 20dp
- 字体排版优化：增大行高，labelSmall 最小 11sp
- 播放器封面添加阴影和光晕效果
- 播放按钮添加径向渐变和阴影
- 控件层次感：prev/next 添加圆形背景，shuffle/repeat 添加状态背景
- 底栏毛玻璃效果 (alpha=0.85)
- 迷你播放条阴影增强到 8dp

### v3.8.1 (2026-07-23)
- 修复元数据竞态：旧曲目元数据不再覆盖当前曲目
- 修复 SMB 播放列表 fileSize=0 导致播放失败
- 优化列表性能：移除每项动画，减少重组开销
- 记忆化 SMB 文件列表过滤结果
- 稳定每日名言显示

### v3.8.0 (2026-07-23)
- 修复 AB 循环功能
- 修复 SMB 线程安全 (ConcurrentHashMap)
- 修复 SMB 连接竞态 (Mutex)
- 修复 ReplayGain 音量跨曲目复合问题
- 修复封面图片内存占用 (~20MB)
- 修复播放指示器实时更新

### v3.7.0 (2026-07-23)
- 首次运行引导页
- 崩溃报告框架
- 播放错误自动重试
- 手势增强 (速度感知 + 振动反馈)
- EQ 频响可视化
- ReplayGain 扫描
- 本地推荐引擎

### v3.6.0 (2026-07-23)
- Hi-Res 音频标签 + 格式详情面板
- 场景模式 (驾驶/睡眠/专注/派对)
- 分享卡片生成
- 歌词海报生成

### v3.5.0 (2026-07-23)
- 离线下载管理
- Android Auto 支持
- DLNA 设备发现

### v3.4.0 (2026-07-23)
- Crossfade 淡入淡出

### v3.3.0 (2026-07-23)
- 均衡器 UI 美化
- 骨架屏加载
- 多语言支持 (中/英)

### v3.2.0 (2026-07-23)
- 统计卡片交互
- 收藏爆炸动画

### v3.1.0 (2026-07-23)
- 封面旋转动画
- 渐变进度条
- 播放指示器美化
- 专辑封面加载
- 锁屏控制

### v3.0.0 (2026-07-22)
- 无缝播放 (Gapless)
- ReplayGain 响度标准化
- 30 段均衡器 + 参数 EQ
- 手势操作
- 10 种主题预设
- 文件夹浏览
- 智能播放列表
- 标签编辑器
- SFTP/WebDAV 协议
- 桌面 Widget
- Quick Settings Tile
- 播放统计

### v2.0.0 (2026-07-20)
- 无缝播放
- 音频设备路由
- ReplayGain
- 文件夹扫描配置
- 播放统计

### v1.0.2 (2026-07-17)
- 长按菜单
- 专辑详情页
- 艺术家页面
- 全屏歌词
- 今日推荐

### v1.0.1 (2026-07-17)
- 多项 Bug 修复
- 首页 UI 重设计
- iOS 26 风格底栏
- 性能优化

### v1.0.0 (2026-07-17)
- 首次发布

## 许可证

MIT License
