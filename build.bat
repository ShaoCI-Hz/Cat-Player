@echo off
set "JAVA_HOME=C:\Users\CDHW088\AppData\Local\BeeWare\briefcase\Cache\tools\java17"
set "ANDROID_HOME=C:\Users\CDHW088\AppData\Local\BeeWare\briefcase\Cache\tools\android_sdk"
set "GRADLE_HOME=C:\Users\CDHW088\.gradle\wrapper\dists\gradle-8.7-all\aan3ydargesu18aqyqjwhr3pc\gradle-8.7"

set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%GRADLE_HOME%\bin;%PATH%"

echo === 编译环境 ===
java -version
echo.
echo ANDROID_HOME=%ANDROID_HOME%
echo.
echo === 开始编译 ===
cd /d C:\Users\CDHW088\smb-music-player-android
gradle.bat assembleDebug --no-daemon --console=plain 2>&1

echo.
echo === 编译完成 ===
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK 已生成: app\build\outputs\apk\debug\app-debug.apk
    dir "app\build\outputs\apk\debug\app-debug.apk"
) else (
    echo 编译失败，请检查上方错误信息
)
pause
