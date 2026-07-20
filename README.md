# 🐱 Cat Player

> 一款面向 Android 的原生音乐播放器，通过 SMB 协议直连网络共享（NAS）中的音乐文件，支持本地音乐播放。

![API](https://img.shields.io/badge/API-26%2B-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![Compose](https://img.shields.io/badge/Compose-BOM-2024-blueviolet)

## ✨ 功能特性

### 🎵 音乐播放
- **SMB/NAS 直连** — 无需挂载系统共享，应用内直连 SMB 服务器播放音乐
- **本地音乐** — 自动扫描本地音乐库，支持所有常见音频格式
- **Media3 ExoPlayer** — 基于 Google 最新播放引擎，流畅稳定
- **元数据提取** — 自动读取标题、艺术家、专辑、封面、歌词
- **无缝播放** — 支持无缝切换下一首

### 🎨 界面设计
- **Spotify 暗色主题** — 纯黑背景 + 绿色强调色，沉浸式视觉体验
- **iOS 26 风格底栏** — 胶囊悬浮导航，弹性动画，图标+文字
- **全屏播放器** — 大封面展示 + 进度条 + 歌词同步
- **首页仪表盘** — 问候语、统计卡片、每日名言、最近播放

### 🎛️ 音频效果
- **均衡器** — 5 段可视化均衡器，预设模式切换
- **低音增强** — 一键开启低音增强
- **播放速度** — 0.5x ~ 2.0x 变速播放
- **AB 循环** — A 点到 B 点循环播放

### 📋 播放管理
- **播放队列** — 拖拽排序，滑动删除
- **多播放列表** — 自定义收藏、最近播放
- **睡眠定时器** — 定时停止播放
- **收藏夹** — 一键收藏喜欢的歌曲
- **播放历史** — 自动记录，持久化保存

### ⚙️ 其他
- **MediaSession + 通知栏** — 系统级播放控制
- **记住密码** — SMB 连接信息自动保存
- **搜索功能** — 歌曲、专辑、艺术家全局搜索
- **数据备份** — 设置导出/导入

## 📸 截图

> 应用内截图请查看 `screenshots/` 目录

## 🛠️ 技术栈

| 模块 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI 框架 | Jetpack Compose + Material3 |
| 音频引擎 | Media3 ExoPlayer 1.4.1 |
| SMB 客户端 | smbj 0.13.0 |
| 依赖注入 | Hilt + KSP |
| 数据存储 | DataStore Preferences |
| 图片加载 | Coil 2.7.0 |
| 元数据 | jaudiotagger 2.2.6 |
| 构建工具 | AGP 8.7.3 + Gradle 8.9 |

## 🏗️ 架构

```
com.example.smbplayer/
├── data/                    # 数据层
│   ├── local/              # 本地音乐扫描
│   ├── metadata/           # 元数据提取
│   ├── player/             # ExoPlayer 封装
│   ├── settings/           # 设置持久化
│   ├── smb/                # SMB 连接管理
│   ├── audio/              # 音频效果
│   ├── favorites/          # 收藏夹
│   └── lyrics/             # LRC 歌词解析
├── domain/                  # 业务层
│   ├── ConnectUseCase      # 连接逻辑
│   ├── ManagePlaylistUseCase # 播放列表管理
│   └── PlayAudioUseCase    # 播放编排
├── ui/                      # 界面层
│   ├── library/            # 音乐库首页
│   ├── player/             # 播放器页面
│   ├── connect/            # SMB 连接
│   ├── favorites/          # 收藏夹
│   ├── playlist/           # 播放队列
│   ├── settings/           # 设置页面
│   ├── navigation/         # 导航 + 底栏
│   └── theme/              # 主题配置
├── di/                      # Hilt 依赖注入
└── service/                 # 前台播放服务
```

## 🚀 编译运行

### 环境要求
- JDK 17
- Android SDK 36
- Gradle 8.9

### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/ShaoCI-Hz/Cat-Player.git
cd Cat-Player

# 设置环境变量
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk

# 编译 Debug APK
./gradlew assembleDebug

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

### 直接下载

前往 [Releases](https://github.com/ShaoCI-Hz/Cat-Player/releases) 下载最新 APK。

## 📝 更新日志

### v1.0.2 (2026-07-17)
- 🆕 **长按菜单**：歌曲行长按弹出播放/添加到队列
- 🆕 **专辑详情页**：点专辑→全屏封面+曲目列表+全部播放
- 🆕 **艺人页面**：按歌手分组，圆形头像展示
- 🆕 **大歌词页**：Canvas 流光动效背景+居中24sp歌词滚动
- 🆕 **今日推荐**：首页横向卡片，播放历史随机推荐
- 🆕 **确认对话框**：清空播放队列前弹窗确认
- 🎨 **卡片系统统一**：收藏夹+播放队列全部卡片化
- 🎨 **底栏振动反馈**：选Tab时触感反馈
- ⚡ **播放按钮振动**：播放/暂停按钮触觉反馈
- 🔧 **内外R角统一**：底栏胶囊内外均为40dp纯圆

### v1.0.1 (2026-07-17)
- 🔧 修复浏览 Tab 闪退（Compose 重复 key 崩溃）
- 🔧 修复暂停播放时自动退出全屏播放器
- 🔧 修复滑动删除播放队列动画异常
- 🔧 修复星期显示偏差（每一天都错位一天）
- 🔧 修复播放历史重启后丢失（持久化到 DataStore）
- 🔧 修复 SMB 连接超时后资源泄漏（CancellationException 未捕获）
- 🔧 修复播放器状态卡死（STATE_IDLE 未更新 Idle 状态）
- 🔧 修复封面取色导致全屏重组卡顿（添加 remember 缓存）
- 🎨 首页 UI 模块化重设计：问候语+时间+统计卡片+每日名言+最近播放+全部歌曲
- 🎨 最近播放默认加载显示，无需先播放歌曲
- 🎨 收藏夹和歌曲列表全中文化
- 🎨 iOS 26 风格悬浮底栏：胶囊造型 + 弹性动画
- 🎨 底栏内外圆角统一 40dp 纯胶囊设计
- ⚡ 歌词滚动 O(n) → O(1) 摊还优化
- ⚡ 封面图片降采样防 OOM
- ⚡ 搜索过滤 remember 缓存防重复计算
- ⚡ 密码输入框遮罩显示

### v1.0.0 (2026-07-17)
- 🎉 首次发布
- SMB/NAS 音乐直连播放
- 本地音乐自动扫描
- Spotify 暗色主题
- iOS 26 风格悬浮底栏
- 均衡器 + 低音增强
- 歌词同步显示
- 播放队列管理
- 睡眠定时器
- 收藏夹 + 播放历史
- MediaSession 通知栏控制

## 📄 许可证

MIT License - 自由使用和修改

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

*Built with ❤️ using Kotlin + Jetpack Compose*
