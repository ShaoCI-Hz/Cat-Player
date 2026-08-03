# JuYuMao 发布脚本
# 用法: .\release.ps1 -Version "4.1.0" -ReleaseNotes "修复了xxx"
# 功能: 编译 debug APK → 打包到桌面 → 更新版本号 → 提交 → 推送 → 创建 GitHub Release(附 APK + 更新日志)

param(
    [Parameter(Mandatory = $true)]
    [string]$Version,
    [Parameter(Mandatory = $true)]
    [string]$ReleaseNotes
)

$ErrorActionPreference = "Stop"
$repo = "D:\RuanJian\KaiFa\Juyumao"
$github = "ShaoCI-Hz/JuYuMao"
$desktop = [Environment]::GetFolderPath('Desktop')
$apkName = "JuYuMao-v$Version.apk"
$apkPath = Join-Path $desktop $apkName
$tokenFile = Join-Path $repo ".gh_token"

# ── 0. 读取 token ──
if (-not (Test-Path $tokenFile)) {
    Write-Host "缺少 token 文件 .gh_token（首次运行请创建，内容为 GitHub PAT）" -ForegroundColor Red
    exit 1
}
$token = (Get-Content $tokenFile -Raw).Trim()

# ── 1. 环境 ──
$env:JAVA_HOME = "D:\RuanJian\Java"
$env:ANDROID_HOME = "D:\RuanJian\Android tool\Android SDK"
$env:GITHUB_TOKEN = $token
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# ── 2. 更新版本号 ──
$gradleFile = Join-Path $repo "app\build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw -Encoding UTF8
$versionCode = ($gradleContent -match 'versionCode = (\d+)') | Out-Null
$newCode = [int]$Matches[1] + 1
$gradleContent = $gradleContent -replace 'versionCode = \d+', "versionCode = $newCode"
$gradleContent = $gradleContent -replace 'versionName = "[\d.]+"', "versionName = `"$Version`""
[System.IO.File]::WriteAllText($gradleFile, $gradleContent, [System.Text.UTF8Encoding]::new($false))
Write-Host "版本更新: versionCode=$newCode, versionName=$Version" -ForegroundColor Green

# ── 3. 更新 README 更新日志 ──
$readmeFile = Join-Path $repo "README.md"
$today = Get-Date -Format "yyyy-MM-dd"
$logEntry = @"

### v$Version ($today) — $ReleaseNotes
"@
$readme = Get-Content $readmeFile -Raw -Encoding UTF8
$readme = $readme -replace '^## 更新日志\r?\n', "## 更新日志`n$logEntry`n"
[System.IO.File]::WriteAllText($readmeFile, $readme, [System.Text.UTF8Encoding]::new($false))
Write-Host "README 更新日志已追加" -ForegroundColor Green

# ── 4. 编译 ──
Write-Host "编译中..." -ForegroundColor Cyan
Push-Location $repo
& "D:\RuanJian\Android tool\Gradle\gradle-8.11.1\bin\gradle.bat" assembleDebug 2>&1 | Out-Null
$buildOk = $LASTEXITCODE -eq 0
Pop-Location
if (-not $buildOk) {
    Write-Host "编译失败，中止发布" -ForegroundColor Red
    exit 1
}
Copy-Item (Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk") $apkPath -Force
Write-Host "APK 已打包: $apkPath" -ForegroundColor Green

# ── 5. 提交 + 推送 ──
Push-Location $repo
git add -A
git -c user.name="JuYuMao" -c user.email="juyumao@local" commit -m "release: v$Version - $ReleaseNotes" 2>&1 | Out-Null
git -c http.sslVerify=false push origin master:main 2>&1 | Out-Null
Pop-Location
Write-Host "代码已推送" -ForegroundColor Green

# ── 6. 创建 GitHub Release ──
$body = @{
    tag_name = "v$Version"
    name = "v$Version - $ReleaseNotes"
    body = $ReleaseNotes
    draft = $false
    prerelease = $false
} | ConvertTo-Json

$pyScript = @"
import json, urllib.request, os
token = os.environ.get("GITHUB_TOKEN")
payload = json.loads(r'''$($body -replace "'", "\u0027")''')
req = urllib.request.Request(
    "https://api.github.com/repos/$github/releases",
    data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
    headers={"Authorization": f"token {token}", "Content-Type": "application/json"},
    method="POST",
)
try:
    with urllib.request.urlopen(req) as resp:
        print(json.loads(resp.read().decode())["id"])
except urllib.error.HTTPError as e:
    print("ERR:" + e.read().decode()[:300]); exit(1)
"@
$pyFile = Join-Path $repo "_release_tmp.py"
[System.IO.File]::WriteAllText($pyFile, $pyScript, [System.Text.UTF8Encoding]::new($false))
$releaseId = python $pyFile
Remove-Item $pyFile -Force

if ($releaseId -match '^\d+$') {
    # 上传 APK
    $upScript = @"
import json, urllib.request, os
token = os.environ.get("GITHUB_TOKEN")
url = "https://uploads.github.com/repos/$github/releases/$releaseId/assets?name=$apkName"
with open(r"$apkPath", "rb") as f: data = f.read()
req = urllib.request.Request(url, data=data, headers={
    "Authorization": f"token {token}",
    "Content-Type": "application/vnd.android.package-archive",
}, method="POST")
try:
    with urllib.request.urlopen(req) as resp:
        print(json.loads(resp.read().decode())["browser_download_url"])
except urllib.error.HTTPError as e:
    print("ERR:" + e.read().decode()[:300]); exit(1)
"@
    $upFile = Join-Path $repo "_upload_tmp.py"
    [System.IO.File]::WriteAllText($upFile, $upScript, [System.Text.UTF8Encoding]::new($false))
    $dlUrl = python $upFile
    Remove-Item $upFile -Force
    Write-Host "发布完成!" -ForegroundColor Green
    Write-Host "Release: https://github.com/$github/releases/tag/v$Version"
    Write-Host "APK 下载: $dlUrl"
} else {
    Write-Host "Release 创建失败: $releaseId" -ForegroundColor Red
    exit 1
}
