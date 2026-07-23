@echo off
set "JAVA_HOME=D:\jdk17\jdk-17.0.19+10"
set "ANDROID_HOME=C:\Users\CDHW088\AppData\Local\BeeWare\briefcase\Cache\tools\android_sdk"
set "GRADLE_USER_HOME=D:\Cat-Player\.gradle-cache"
set "GRADLE_HOME=D:\Cat-Player\.gradle-cache\wrapper\dists\gradle-8.9-bin-temp\gradle-8.9"

set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%GRADLE_HOME%\bin;%PATH%"

echo === 编译 Release APK ===
cd /d D:\Cat-Player
gradle.bat assembleRelease --no-daemon --console=plain 2>&1

echo.
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo Release APK 已生成
    copy "app\build\outputs\apk\debug\app-debug.apk" "Cat-Player-v3.0.0.apk" /Y
    echo 已复制为 Cat-Player-v3.0.0.apk
) else (
    echo 编译失败
)
pause
