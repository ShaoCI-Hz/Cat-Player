# Cat Player 快速编译脚本
# 用法: . D:\Cat-Player\build.ps1

# 配置环境
$env:JAVA_HOME = "D:\jdk17\jdk-17.0.19+10"
$env:ANDROID_HOME = "C:\Users\CDHW088\AppData\Local\BeeWare\briefcase\Cache\tools\android_sdk"
$env:GRADLE_USER_HOME = "D:\Cat-Player\.gradle-cache"

Set-Location D:\Cat-Player

Write-Host "开始编译..." -ForegroundColor Cyan
$gradlePath = "D:\Cat-Player\.gradle-cache\wrapper\dists\gradle-8.9-bin-temp\gradle-8.9\bin\gradle.bat"

& $gradlePath assembleDebug --no-daemon --console=plain

if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
    Write-Host "`n编译成功!" -ForegroundColor Green
    Write-Host "APK: D:\Cat-Player\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Yellow
} else {
    Write-Host "`n编译失败" -ForegroundColor Red
}
