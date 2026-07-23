# Cat Player 开发环境配置
# 用法: . D:\Cat-Player\setup-env.ps1

$env:JAVA_HOME = "D:\jdk17\jdk-17.0.19+10"
$env:ANDROID_HOME = "C:\Users\CDHW088\AppData\Local\BeeWare\briefcase\Cache\tools\android_sdk"
$env:GRADLE_USER_HOME = "D:\Cat-Player\.gradle-cache"
$env:PATH = "$env:JAVA_HOME\bin;$env:GRADLE_USER_HOME\wrapper\dists\gradle-8.9-bin-temp\gradle-8.9\bin;D:\Git\cmd;D:\GitHub-CLI;$env:PATH"

Write-Host "Cat Player 环境已配置" -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "项目目录: D:\Cat-Player"

# 切换到项目目录
Set-Location D:\Cat-Player
