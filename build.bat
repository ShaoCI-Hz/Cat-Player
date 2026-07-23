@echo off
:: Cat Player 编译脚本
:: 环境变量已在系统中配置，此脚本自动使用

echo === Cat Player 编译环境 ===
echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_HOME=%ANDROID_HOME%
echo GRADLE_USER_HOME=%GRADLE_USER_HOME%
echo.

cd /d D:\Cat-Player

echo === 开始编译 Debug APK ===
call gradle assembleDebug --no-daemon --console=plain

echo.
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo === 编译成功 ===
    echo APK: app\build\outputs\apk\debug\app-debug.apk
    for %%A in (app\build\outputs\apk\debug\app-debug.apk) do echo 大小: %%~zA bytes
) else (
    echo === 编译失败 ===
)
pause
