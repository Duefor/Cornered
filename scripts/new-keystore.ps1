#Requires -Version 5.1
<#
.SYNOPSIS
    生成 Cornered 的发布签名（release.jks），并输出 GitHub Secrets 需要的内容。

.DESCRIPTION
    只需要跑一次。生成后：
      - release.jks          发布密钥，请离线备份，丢了就没法给老用户推更新
      - keystore.properties  本地构建自动读取，已在 .gitignore 中
    两者都不要提交到仓库。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/new-keystore.ps1
#>
param(
    [string]$Alias = 'cornered',
    [string]$OutFile = 'release.jks',
    [string]$DName = 'CN=Cornered, OU=Cornered, O=Duefor, L=Shanghai, C=CN',
    [int]$ValidityDays = 10000
)

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$keystorePath = if ([System.IO.Path]::IsPathRooted($OutFile)) { $OutFile } else { Join-Path $repoRoot $OutFile }

if (Test-Path $keystorePath) {
    throw "已存在 $keystorePath。发布签名不能随便覆盖：覆盖后老用户将无法覆盖安装更新。确认要重来请先手动删除并接受后果。"
}

$keytool = $null
$candidates = @(
    (Join-Path $repoRoot '.toolchain\jdk\bin\keytool.exe'),
    (Join-Path $env:JAVA_HOME 'bin\keytool.exe')
)
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path $candidate)) { $keytool = $candidate; break }
}
if (-not $keytool) {
    $found = Get-Command keytool -ErrorAction SilentlyContinue
    if ($found) { $keytool = $found.Source }
}
if (-not $keytool) {
    throw "找不到 keytool。先运行 scripts/setup-toolchain.ps1，或安装 JDK 17（Android Studio 自带）。"
}

# 生成一个足够强的口令，避免用户自己想
$alphabet = (48..57) + (65..90) + (97..122)
$password = -join ($alphabet | Get-Random -Count 28 | ForEach-Object { [char]$_ })

Write-Host "[keytool] $keytool"
Write-Host "[gen    ] 生成 $keystorePath（别名 $Alias，有效期 $ValidityDays 天）"

& $keytool -genkeypair -v `
    -keystore $keystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity $ValidityDays `
    -storetype PKCS12 `
    -storepass $password `
    -keypass $password `
    -dname $DName
if ($LASTEXITCODE -ne 0) { throw "keytool 执行失败（退出码 $LASTEXITCODE）" }

# 本地构建用的配置
$props = @(
    "storeFile=$OutFile",
    "storePassword=$password",
    "keyAlias=$Alias",
    "keyPassword=$password"
) -join "`n"
$propsPath = Join-Path $repoRoot 'keystore.properties'
[System.IO.File]::WriteAllText($propsPath, "$props`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[ok     ] 已写入 $propsPath（已 gitignore）"

$base64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($keystorePath))

Write-Host ''
Write-Host '============================================================'
Write-Host ' 把下面四项加到 GitHub 仓库 Settings -> Secrets and variables'
Write-Host ' -> Actions，名称大小写必须一致：'
Write-Host '============================================================'
Write-Host ''
Write-Host 'KEYSTORE_BASE64 ='
Write-Host $base64
Write-Host ''
Write-Host "KEYSTORE_PASSWORD = $password"
Write-Host "KEY_ALIAS         = $Alias"
Write-Host "KEY_PASSWORD      = $password"
Write-Host ''
Write-Host '============================================================'
Write-Host " 重要：现在就备份 $keystorePath 到两个离线位置。"
Write-Host ' 丢失后无法给已安装的用户推送更新，只能换包名重来。'
Write-Host '============================================================'
