# smbj
-keep class com.hierynomus.** { *; }
-keep class com.hierynomus.smbj.** { *; }
-keep class org.bouncycastle.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Media3
-keep class androidx.media3.** { *; }
