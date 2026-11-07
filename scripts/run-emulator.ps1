#Requires -Version 5.1
<#
.SYNOPSIS
    启动 Android 模拟器（带窗口）、装好调试包并打开游戏。

.DESCRIPTION
    默认带窗口，可以直接用鼠标玩：
      - 如果模拟器已经在跑，就跳过启动，只重装并重开 App
      - 没有 AVD 时会提示先跑 scripts/setup-toolchain.ps1

    窗口是 qemu 进程持有的，脚本退出后窗口仍然保留。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/run-emulator.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/run-emulator.ps1 -Build
    先重新编译，再装到模拟器上

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/run-emulator.ps1 -Headless -NoInstall
    只起一个无窗口的模拟器，方便我用 adb 截图
#>
param(
    [string]$Avd = 'cornered',
    [switch]$Headless,
    [switch]$Build,
    [switch]$NoInstall
)

$ErrorActionPreference = 'Continue'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sdk = Join-Path $repo '.toolchain\android-sdk'

if (-not (Test-Path $sdk)) {
    if ($env:ANDROID_HOME) { $sdk = $env:ANDROID_HOME }
    else { throw "找不到 Android SDK：先运行 scripts/setup-toolchain.ps1" }
}

$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
if (-not (Test-Path $adb)) { throw "找不到 adb：$adb" }
if (-not (Test-Path $emulator)) { throw "找不到 emulator：$emulator" }

$env:ANDROID_SDK_ROOT = $sdk
$env:ANDROID_HOME = $sdk
$env:ANDROID_AVD_HOME = Join-Path $env:USERPROFILE '.android\avd'

# ---------------------------------------------------------------- 编译
if ($Build) {
    $jdk = Join-Path $repo '.toolchain\jdk'
    $gradle = Join-Path $repo '.toolchain\gradle\bin\gradle.bat'
    if (-not (Test-Path $gradle)) { throw "找不到 Gradle：先运行 scripts/setup-toolchain.ps1" }
    $env:JAVA_HOME = $jdk
    $env:Path = "$jdk\bin;$env:Path"
    Write-Host '[build] assembleDebug ...'
    & $gradle -p $repo :app:assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw '编译失败，先修代码再启动模拟器' }
}

# ---------------------------------------------------------------- 模拟器
function Get-OnlineDevice {
    $lines = & $adb devices 2>&1 | Select-Object -Skip 1
    ($lines | Where-Object { $_ -match '\sdevice$' } | Select-Object -First 1) -replace '\s+device$', ''
}

$device = Get-OnlineDevice
if (-not $device) {
    if (-not (Test-Path (Join-Path $env:ANDROID_AVD_HOME "$Avd.avd"))) {
        throw "找不到 AVD「$Avd」：先运行 scripts/setup-toolchain.ps1 并用 avdmanager 创建"
    }

    $args = @('-avd', $Avd, '-no-audio', '-no-boot-anim', '-no-snapshot',
              '-gpu', 'angle_indirect', '-memory', '2048', '-cores', '4', '-no-metrics')
    if ($Headless) { $args += '-no-window' }

    $log = Join-Path $repo '.toolchain\emulator-window.log'
    Write-Host "[emu  ] 启动 $Avd$(if ($Headless) { '（无窗口）' } else { '（带窗口，稍等几秒会弹出手机界面）' })"
    Start-Process -FilePath $emulator -ArgumentList $args -RedirectStandardOutput $log -RedirectStandardError "$log.err" -WindowStyle Hidden | Out-Null

    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        $online = Get-OnlineDevice
        if ($online) {
            $boot = (& $adb -s $online shell getprop sys.boot_completed 2>&1) -join ''
            if ($boot -match '1') { $device = $online; break }
        }
    }
    if (-not $device) { throw "模拟器 5 分钟内没启动完成，看 $log" }
    Write-Host "[emu  ] 已就绪：$device"
} else {
    Write-Host "[emu  ] 复用已在运行的设备：$device"
}

# ---------------------------------------------------------------- 装包并启动
$apk = Join-Path $repo 'app\build\outputs\apk\debug\app-debug.apk'
if (-not $NoInstall) {
    if (-not (Test-Path $apk)) { throw "没有安装包：$apk（加 -Build 或先手动编译）" }
    Write-Host '[install] 覆盖安装调试包'
    & $adb -s $device install -r $apk | Select-Object -Last 1
    & $adb -s $device shell am force-stop dev.duefor.cornered.debug | Out-Null
}

Write-Host '[start] 打开游戏'
& $adb -s $device shell am start -n dev.duefor.cornered.debug/dev.duefor.cornered.MainActivity | Out-Null
Write-Host ''
Write-Host '完成。模拟器窗口里可以直接用鼠标玩：'
Write-Host '  - 点击 = 轻触，按住拖动 = 滑动'
Write-Host '  - 关掉模拟器窗口即可结束'
Write-Host '  - 发现哪里不对，截图给我：powershell -ExecutionPolicy Bypass -File scripts/screenshot.ps1'
