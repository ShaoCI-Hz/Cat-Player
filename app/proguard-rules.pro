# smbj
-keep class com.hierynomus.** { *; }
-keep class com.hierynomus.smbj.** { *; }
-keep class org.bouncycastle.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Media3
-keep class androidx.media3.** { *; }

# Media3 FFmpeg 扩展解码器
-keep class androidx.media3.decoder.ffmpeg.** { *; }

# jmdns (mDNS discovery, uses reflection)
-keep class javax.jmdns.** { *; }
-keep class org.jmdns.** { *; }

# jaudiotagger (audio metadata, uses reflection)
-keep class org.jaudiotagger.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.** { *; }
