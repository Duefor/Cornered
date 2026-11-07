#Requires -Version 5.1
<#
.SYNOPSIS
    在仓库内准备一套可删除的 Android 构建工具链（JDK 17 + Gradle 8.9 + Android SDK）。

.DESCRIPTION
    所有内容都写进 <repo>/.toolchain（已在 .gitignore 中），不需要管理员权限，
    不改系统环境变量。删除 .toolchain 目录即可完全回滚。

    默认优先国内镜像（清华 TUNA / 腾讯云），失败自动回退官方源；
    国内网络下官方源通常只有几十 KB/s，镜像能到 2-3 MB/s。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/setup-toolchain.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/setup-toolchain.ps1 -OfficialOnly
#>
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [switch]$OfficialOnly
)

# 原生程序（java / sdkmanager / tar）会把正常信息写到 stderr，Stop 会误判成致命错误；
# 这里改为 Continue，并对每个原生调用显式检查 $LASTEXITCODE。
$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$toolchain = Join-Path $RepoRoot '.toolchain'
$jdkDir    = Join-Path $toolchain 'jdk'
$gradleDir = Join-Path $toolchain 'gradle'
$sdkDir    = Join-Path $toolchain 'android-sdk'
$dlDir     = Join-Path $toolchain 'downloads'

New-Item -ItemType Directory -Force -Path $toolchain, $dlDir | Out-Null

function Get-File([string[]]$Urls, [string]$OutFile, [long]$MinBytes = 1MB) {
    if ((Test-Path $OutFile) -and ((Get-Item $OutFile).Length -ge $MinBytes)) {
        Write-Host "[skip] $([System.IO.Path]::GetFileName($OutFile)) 已存在"
        return
    }
    foreach ($url in $Urls) {
        Write-Host "[get ] $url"
        & curl.exe -L --fail --silent --show-error --retry 1 --connect-timeout 20 --max-time 1800 -o $OutFile $url
        if ($LASTEXITCODE -eq 0 -and (Test-Path $OutFile) -and ((Get-Item $OutFile).Length -ge $MinBytes)) {
            Write-Host "[ok  ] $([math]::Round((Get-Item $OutFile).Length / 1MB, 1)) MB"
            return
        }
        Write-Host '[fail] 换下一个源'
        if (Test-Path $OutFile) { Remove-Item $OutFile -Force }
    }
    throw "所有下载源都失败：$($Urls -join ' | ')"
}

if ($OfficialOnly) {
    $jdkUrls = @('https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk')
    $gradleUrls = @('https://services.gradle.org/distributions/gradle-8.9-bin.zip')
} else {
    $jdkUrls = @(
        'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.21_9.zip',
        'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
    )
    $gradleUrls = @(
        'https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip',
        'https://services.gradle.org/distributions/gradle-8.9-bin.zip'
    )
}

function Expand-ZipInto([string]$Zip, [string]$Target, [string]$InnerDirName) {
    $tmp = Join-Path $toolchain '_extract'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    & tar.exe -xf $Zip -C $tmp
    if ($LASTEXITCODE -ne 0) { throw "解压失败: $Zip" }

    $source = if ($InnerDirName) { Join-Path $tmp $InnerDirName } else {
        (Get-ChildItem $tmp -Directory | Select-Object -First 1).FullName
    }
    if (-not (Test-Path $source)) { throw "压缩包结构异常: $Zip" }
    if (Test-Path $Target) { Remove-Item $Target -Recurse -Force }
    Move-Item $source $Target
    Remove-Item $tmp -Recurse -Force
}

# ---------- 1. JDK 17 ----------
if (-not (Test-Path (Join-Path $jdkDir 'bin\java.exe'))) {
    $jdkZip = Join-Path $dlDir 'jdk17.zip'
    Get-File $jdkUrls $jdkZip 50MB
    Write-Host '[unzip] JDK 17'
    Expand-ZipInto $jdkZip $jdkDir ''
}
$env:JAVA_HOME = $jdkDir
$env:Path = "$jdkDir\bin;$env:Path"
Write-Host "[ok  ] JDK: $(& java -version 2>&1 | Select-Object -First 1)"

# ---------- 2. Gradle 8.9 ----------
if (-not (Test-Path (Join-Path $gradleDir 'bin\gradle.bat'))) {
    $gradleZip = Join-Path $dlDir 'gradle-8.9-bin.zip'
    Get-File $gradleUrls $gradleZip 50MB
    Write-Host '[unzip] Gradle 8.9'
    Expand-ZipInto $gradleZip $gradleDir 'gradle-8.9'
}
$env:GRADLE_HOME = $gradleDir

# ---------- 3. Android cmdline-tools ----------
$sdkManager = Join-Path $sdkDir 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path $sdkManager)) {
    $clZip = Join-Path $dlDir 'cmdline-tools.zip'
    Get-File @('https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip') $clZip 10MB
    Write-Host '[unzip] cmdline-tools'
    $latest = Join-Path $sdkDir 'cmdline-tools\latest'
    New-Item -ItemType Directory -Force -Path (Split-Path $latest) | Out-Null
    $tmp = Join-Path $toolchain '_extract'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    & tar.exe -xf $clZip -C $tmp
    if ($LASTEXITCODE -ne 0) { throw "解压失败: $clZip" }
    if (Test-Path $latest) { Remove-Item $latest -Recurse -Force }
    Move-Item (Join-Path $tmp 'cmdline-tools') $latest
    Remove-Item $tmp -Recurse -Force
}

# ---------- 4. SDK 组件 ----------
Write-Host '[sdk ] 接受许可协议'
$yes = (1..200 | ForEach-Object { 'y' }) -join "`n"
$yes | & $sdkManager --sdk_root=$sdkDir --licenses | Out-Null

Write-Host '[sdk ] 安装 platform-tools / platforms;android-35 / build-tools;35.0.0'
& $sdkManager --sdk_root=$sdkDir 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'sdkmanager 安装失败' }

# ---------- 5. local.properties ----------
$localProps = Join-Path $RepoRoot 'local.properties'
$sdkPath = $sdkDir -replace '\\', '/'
[System.IO.File]::WriteAllText($localProps, "sdk.dir=$sdkPath`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[ok  ] local.properties -> sdk.dir=$sdkPath"

Write-Host ''
Write-Host '工具链就绪：'
Write-Host "  `$env:JAVA_HOME='$jdkDir'"
Write-Host "  `$env:Path=`"$jdkDir\bin;`$env:Path`""
Write-Host "  & '$gradleDir\bin\gradle.bat' :app:assembleDebug"
Write-Host ''
Write-Host '也可以直接用仓库自带的 wrapper：./gradlew :app:assembleDebug'
Write-Host '（wrapper 首次会从 services.gradle.org 下载发行版，国内可能很慢）'
