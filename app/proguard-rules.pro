# Add project specific ProGuard rules here.

# smbj
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# Media3
-keep class androidx.media3.** { *; }

# MIUI X
-keep class top.yukonga.miuix.** { *; }

# SSHJ
-keep class net.schmizz.** { *; }
-keep class com.hierynomus.sshj.** { *; }
-dontwarn net.schmizz.**

# BouncyCastle (used by SSHJ)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# javax.el (referenced by smbj/mbassy)
-dontwarn javax.el.**
-keep class javax.el.** { *; }

# mbassy (used by smbj)
-keep class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**

# EdDSA (used by SSHJ)
-dontwarn sun.security.x509.**
-dontwarn net.i2p.crypto.eddsa.**
-keep class net.i2p.crypto.eddsa.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

# jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Glance Widget
-keep class androidx.glance.** { *; }

# ReplayGain / metadata
-keep class com.example.smbplayer.domain.ReplayGainProcessor$ReplayGainInfo { *; }
-keep class com.example.smbplayer.data.metadata.AudioMetadata { *; }
