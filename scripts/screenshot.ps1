#Requires -Version 5.1
<#
.SYNOPSIS
    截一张当前模拟器画面，并附上最近的应用日志，方便描述 bug。

.DESCRIPTION
    输出到 screenshots\report-<时间>\ 下：
      screen.png   当前画面
      logcat.txt   最近 300 行日志（含崩溃栈）

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/screenshot.ps1
#>
param(
    [string]$OutDir = ''
)

$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sdk = Join-Path $repo '.toolchain\android-sdk'
$adb = if (Test-Path $sdk) { Join-Path $sdk 'platform-tools\adb.exe' } else { 'adb' }

$device = (& $adb devices 2>&1 | Select-Object -Skip 1 | Where-Object { $_ -match '\sdevice$' } | Select-Object -First 1)
if (-not $device) { throw '没有在线设备，先运行 scripts/run-emulator.ps1' }
$serial = ($device -replace '\s+device$', '').Trim()

if (-not $OutDir) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutDir = Join-Path $repo "screenshots\report-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$png = Join-Path $OutDir 'screen.png'
$log = Join-Path $OutDir 'logcat.txt'

& $adb -s $serial shell screencap -p /sdcard/_shot.png | Out-Null
& $adb -s $serial pull /sdcard/_shot.png $png | Out-Null
& $adb -s $serial logcat -d -t 300 > $log 2>&1

Write-Host "截图: $png"
Write-Host "日志: $log"
Write-Host ''
Write-Host '把这两个文件路径发给我，再说一句你点在哪、期望什么、实际看到什么，我就能定位。'
