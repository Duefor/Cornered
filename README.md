# Cornered

> 四面合围，别让角落吞掉你。

一个用 **Kotlin + Jetpack Compose Canvas** 写的安卓小游戏：场地不断收缩，方块敌人从四周涌来，你只能拖动躲避、靠冲刺脱身。活得越久分越高。

[![Release](https://img.shields.io/github/v/release/Duefor/Cornered?label=latest&color=4DECC8)](https://github.com/Duefor/Cornered/releases/latest)
[![Build](https://github.com/Duefor/Cornered/actions/workflows/ci.yml/badge.svg)](https://github.com/Duefor/Cornered/actions/workflows/ci.yml)

---

## 关于本项目的开发方式

**Cornered 是基于 DSH（DeepSeek Harness）开发的。**

从工程骨架、游戏循环、更新模块到 CI 发版流水线，都是在 DSH 的会话里逐步搭起来并验证的：代码由 DSH 编写，构建与打包交给 GitHub Actions，版本通过 GitHub Release 分发。README、提交信息和发版说明也一并维护在仓库里，方便回看每次改动是怎么来的。

## 玩法

| 操作 | 效果 |
| --- | --- |
| 按住并拖动 | 朝拖动方向移动，幅度越大速度越快 |
| 快速甩动（220ms 内） | 朝甩动方向冲刺，冷却 2.4 秒，右下角圆环是冷却进度 |
| 目标 | 尽可能久地不被红色方块碰到 |

场地（青色边框）会随时间向内收缩，敌人从屏幕四周生成并向你靠拢、彼此还会互相挤开。分数 = 存活秒数，最高分存在本地。

## 下载与安装

到 [Releases](https://github.com/Duefor/Cornered/releases/latest) 下载 `Cornered-<版本>.apk`，手机上允许「安装未知来源应用」即可安装。

> Android 8.0（API 26）及以上。

## 应用内更新（不用每次手动下载）

游戏每次启动都会静默查一次 GitHub 最新正式版，发现新版本时弹出更新框；主菜单也有「检查更新」按钮手动触发。点「下载并安装」后：

1. 读 `https://api.github.com/repos/Duefor/Cornered/releases/latest`
2. 用 `versionName` 与当前 `BuildConfig.VERSION_NAME` 比较（`v1.2.0` → `1.2.0` 逐段比数字）
3. 从 Release 资产里挑第一个 `.apk` 下载到应用缓存目录，带进度回调
4. 校验包名与 `versionCode` 后，交给系统 `PackageInstaller` 弹出安装确认

首次使用会要求「允许 Cornered 安装未知来源应用」，直接跳到系统授权页。

**为什么能覆盖安装？** 必须满足两条：包名相同（都是 `dev.duefor.cornered`）、**签名相同**。所以发布签名一旦定下来就不能换，见下面的[签名配置](#一次性配置发布签名)。

## 仓库结构

```
.
├── app/src/main/java/dev/duefor/cornered/
│   ├── MainActivity.kt          应用入口、主页/游戏页切换、提示条
│   ├── game/GameEngine.kt       纯逻辑：收缩场地、敌人生成与追踪、冲刺、碰撞
│   ├── ui/GameScreen.kt         Canvas 渲染 + 手势（拖动/甩动）+ HUD
│   ├── ui/MenuScreen.kt         主菜单
│   ├── update/                  GitHub Release 检查、下载、PackageInstaller 安装
│   └── data/ScoreStore.kt       最高分
├── .github/workflows/
│   ├── release.yml              打 v* 标签 -> 构建签名 APK -> 发布 Release
│   └── ci.yml                   推 main / PR 时编译 debug 包
├── scripts/
│   ├── setup-toolchain.ps1      在仓库内装一份可删除的 JDK + Android SDK
│   └── new-keystore.ps1         生成发布签名并输出 GitHub Secrets 需要的 base64
└── docs/RELEASING.md            发版与签名的完整清单
```

游戏逻辑（`GameEngine`）不依赖任何 Android API，尺寸全部按 dp 书写再按屏幕密度换算，所以不同分辨率的手机手感一致，也方便以后加测试。

## 本地开发

需要 JDK 17 和 Android SDK Platform 35。用 Android Studio 直接打开仓库即可。

纯粹的 Windows 命令行环境可以用脚本在仓库内准备一套工具链（装在 `.toolchain/`，已 gitignore，删掉目录就完全回滚，不动系统环境变量）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-toolchain.ps1
$env:JAVA_HOME = "$PWD\.toolchain\jdk"
$env:Path = "$PWD\.toolchain\jdk\bin;$env:Path"
./gradlew :app:assembleDebug
```

产物在 `app/build/outputs/apk/debug/`。

跑单元测试（版本比较逻辑）：

```powershell
./gradlew :app:testDebugUnitTest
```

## 发版流程

版本号由 **git 标签**决定，不在代码里手改：标签 `v1.2.3` → `versionName=1.2.3`、`versionCode=10203`（主×10000 + 次×100 + 修订）。

```powershell
# 1. 改完代码，正常提交
git add -A
git commit -m "feat: 新增 xxx"

# 2. 打标签并推送（推标签就会触发 Release 工作流）
git tag v1.0.1
git push origin main
git push origin v1.0.1
```

Actions 跑完后会自动：构建 release APK → 上传构建产物 → 创建 `v1.0.1` 的 Release（正文带版本号与签名来源）→ 挂上 `Cornered-1.0.1.apk`。已装旧版的手机打开游戏就能收到更新提示。

细节和排错见 [docs/RELEASING.md](docs/RELEASING.md)。

## 一次性配置：发布签名

**必须在第一次发版前做完，之后不能换。** debug 签名每次构建都不一样，用它发布的包无法覆盖安装，应用内更新会一直失败。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/new-keystore.ps1
```

脚本会生成 `release.jks`、写入本地 `keystore.properties`（两者都在 gitignore 里），并打印出需要填进 GitHub Secrets 的四项：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | `release.jks` 的 base64 整行文本 |
| `KEYSTORE_PASSWORD` | keystore 口令 |
| `KEY_ALIAS` | 密钥别名（默认 `cornered`） |
| `KEY_PASSWORD` | 密钥口令 |

到 `Settings → Secrets and variables → Actions` 里逐个添加。**`release.jks` 请离线备份**，丢了就再也无法给已安装的用户推送更新，只能换包名重新上架。

## 常见问题

**更新提示下载完却装不上？**
签名不一致（换过 keystore）或 `versionCode` 没有递增。覆盖安装两个条件缺一不可。

**Release 构建用了 debug 签名？**
Actions 日志里会有一行 `::warning::仓库没有配置 KEYSTORE_BASE64`。补齐 Secrets 后重新跑一次工作流。

**API 限流？**
未登录的 GitHub API 每小时 60 次，只查 `releases/latest` 完全够用。

## 许可

[MIT](LICENSE) © Duefor
