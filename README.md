# Cornered

> 一款爬塔式卡牌肉鸽（deckbuilder roguelike）：组牌、打怪、往上爬，别死在半路。

用 **Kotlin + Jetpack Compose** 手写，没有第三方游戏引擎：战斗规则是纯 Kotlin 的单机逻辑层，界面是 Compose 画出来的，构建与发版交给 GitHub Actions。

[![Release](https://img.shields.io/github/v/release/Duefor/Cornered?label=latest&color=4DECC8)](https://github.com/Duefor/Cornered/releases/latest)
[![Build](https://github.com/Duefor/Cornered/actions/workflows/ci.yml/badge.svg)](https://github.com/Duefor/Cornered/actions/workflows/ci.yml)

---

## 关于本项目的开发方式

**Cornered 是基于 DSH（DeepSeek Harness）开发的。**

工程骨架、战斗引擎、卡牌与敌人内容、Compose 界面、单元测试到 CI 发版流水线，都是在 DSH 的会话里逐步搭起来并本地编译验证的。版本通过 GitHub Release 分发，应用内可以直接拉取新版本。

## 玩法

一局 = 选一位角色，在 11 层的地图上往上爬，最后打首领「深渊之眼」。

**角色**

| 角色 | 生命 | 每回合 | 起始牌组 | 特点 |
| --- | --- | --- | --- | --- |
| 铁卫 | 80 | 3 能量 / 抽 5 | 打击×5 防御×4 重击×1 | 血厚，重击叠易伤，格挡换回合 |
| 影刃 | 68 | 3 能量 / 抽 6 | 匕首×4 闪避×4 影袭×2 | 血薄但手牌多，匕首 0 费，靠出牌数堆伤害 |

新角色只需要在 `game/Character.kt` 里加一条数据（生命、能量、每回合抽牌数、起始牌组、奖励卡池），存档、地图、战斗都不用改。

**地图节点**

| 节点 | 说明 |
| --- | --- |
| 战斗 | 普通遭遇，打 1-2 个敌人 |
| 精英 | 单个强敌，金币更多 |
| 营地 | 二选一：回复 30% 生命，或最大生命 +5 |
| 宝箱 | 白拿 25-50 金币 |
| 首领 | 第 11 层的最终战 |

每层有 2-4 条岔路，连线决定你下一步能去哪。

**战斗规则**

- 每回合 **3 点能量**、**抽 5 张牌**，打不完的牌回合结束进弃牌堆
- 格挡在自己回合开始时清零，所以「这回合挡多少」是要算的
- 敌人头顶会提前显示下回合意图：攻击 N / 防御 N / 加力量 / 给你上状态
- 抽牌堆空了会自动把弃牌堆洗回去
- 战斗胜利后：拿金币 + **三选一**加一张牌进牌组（也可以跳过）
- 单体卡**永远要点两下**：先点牌进入「选择目标」，再点敌人确认（只有一个敌人时也一样，手滑点错还能再点一次取消）

**界面与存档**

- 主菜单有「继续游戏」：每次操作后自动存档（连战斗中途的手牌、牌堆、敌人血量都存），杀掉进程也能从原处接着打
- 地图右上角「牌库 N」可以查看当前牌组，按类型分组、组内按费用排序

**状态效果**

| 状态 | 效果 | 持续 |
| --- | --- | --- |
| 力量 | 攻击伤害 +N | 整场战斗 |
| 敏捷 | 获得格挡 +N | 整场战斗 |
| 易伤 | 受到的攻击伤害 +50% | 回合结束 -1 |
| 虚弱 | 造成的攻击伤害 -25% | 回合结束 -1 |
| 脆弱 | 获得的格挡 -25% | 回合结束 -1 |

**卡牌**

共 21 张卡，每个角色有各自的起始牌组和奖励池：

| 卡 | 类型 | 费用 | 效果 |
| --- | --- | --- | --- |
| 打击 | 攻击 | 1 | 造成 6 点伤害 |
| 防御 | 技能 | 1 | 获得 5 点格挡 |
| 重击 | 攻击 | 2 | 8 点伤害 + 2 层易伤 |
| 横扫 | 攻击 | 1 | 对全体造成 8 点伤害 |
| 连击 | 攻击 | 1 | 5 点伤害两次 |
| 铁壁 | 技能 | 2 | 获得 12 点格挡 |
| 疾行 | 技能 | 1 | 抽 2 张牌 |
| 护心 | 技能 | 1 | 8 点格挡 + 抽 1 张 |
| 破甲 | 攻击 | 1 | 5 点伤害 + 3 层易伤 |
| 恐吓 | 技能 | 1 | 全体施加 2 层虚弱 |
| 蓄力 | 技能 | 0 | 获得 2 点力量 |
| 重锤 | 攻击 | 2 | 造成 14 点伤害 |
| 吸血打击 | 攻击 | 1 | 6 点伤害 + 回复 3 点生命 |
| 战意 | 技能 | 0 | 抽 3 张牌（消耗） |
| 蓄能 | 技能 | 0 | 获得 2 点能量（消耗） |
| 狂暴 | 技能 | 1 | +3 力量，失去 4 点生命 |
| 漩涡 | 攻击 | 2 | 对全体造成 5 点伤害三次 |
| 不灭壁垒 | 技能 | 2 | 获得 24 点格挡（消耗） |
| 匕首 | 攻击 | 0 | 3 点伤害（影刃起始） |
| 闪避 | 技能 | 1 | 4 点格挡（影刃起始） |
| 影袭 | 攻击 | 1 | 4 点伤害 + 抽 1（影刃起始） |

**敌人**

| 敌人 | 血量 | 特点 |
| --- | --- | --- |
| 腐蚀蠕虫 | 12-16 | 攻击与防御交替 |
| 铁面守卫 | 18-22 | 连击、自己叠力量 |
| 盗匪 | 14-18 | 高单发，会施加虚弱 |
| 岩石巨像（精英） | 44-48 | 重击、格挡、叠力量 |
| 深渊之眼（首领） | 85-95 | 五段循环，会给你上易伤 |

## 已经做了 / 还没做

**已实现**：地图生成与分支、完整战斗循环、21 张卡、**2 名可选角色**（不同生命/抽牌/起始牌组/奖励池）、5 种敌人、状态系统、战后三选一、营地、宝箱、胜负结算、跨局记录（最深楼层/通关次数）、应用内更新、自动存档与继续游戏、牌库查看。

**路线图**：卡牌升级（营地打铁）、遗物、药水、商店、随机事件、多幕（Act）、战斗中的卡牌拖拽、弃牌堆/抽牌堆查看。

## 下载与安装

到 [Releases](https://github.com/Duefor/Cornered/releases/latest) 下载 `Cornered-<版本>.apk`，手机上允许「安装未知来源应用」即可。需要 Android 8.0（API 26）及以上。

## 应用内更新

每次启动会静默查一次 GitHub 最新正式版，有新版本就弹窗；主菜单也有「检查更新」。点「下载并安装」后：

1. 读 `https://api.github.com/repos/Duefor/Cornered/releases/latest`
2. 把 tag 的 `versionName` 和当前 `BuildConfig.VERSION_NAME` 逐段比数字
3. 从 Release 资产里挑第一个 `.apk` 下载到缓存目录，带进度
4. 校验包名与 `versionCode` 后交给系统 `PackageInstaller` 弹安装确认

覆盖安装的前提是**包名相同 + 签名相同**，所以发布签名一旦定下来就不能换。

## 仓库结构

```
app/src/main/java/dev/duefor/cornered/
├── MainActivity.kt              入口：主页 / 对局切换、更新提示
├── game/                        ← 纯 Kotlin，不依赖 Android，可直接单测
│   ├── Model.kt                 卡牌、效果、状态、敌人意图等基础类型
│   ├── Cards.kt                 卡池与起始牌组
│   ├── Enemies.kt               敌人模板与按层数遭遇表
│   ├── CombatEngine.kt          战斗规则：抽牌/能量/格挡/伤害结算/敌人回合
│   ├── GameMap.kt               地图节点与分叉生成
│   └── RunState.kt              一局的流程状态机
├── ui/                          ← Compose 界面
│   ├── MenuScreen.kt            主页
│   ├── MapScreen.kt             Canvas 画的地图 + 点击选路
│   ├── CombatScreen.kt          战斗：敌我状态、意图、手牌、结束回合
│   ├── CardView.kt              卡面组件
│   ├── RewardScreen.kt          三选一 / 宝箱
│   ├── RestScreen.kt            营地
│   ├── EndScreen.kt             胜负结算
│   └── RunScreen.kt             按阶段切换上面这些界面
├── update/                      GitHub Release 检查、下载、安装
└── data/RunRecords.kt           跨局记录

app/src/test/java/...            战斗与地图的单元测试（20 个）
```

设计上刻意把**规则**和**表现**分开：`game/` 里没有一个 import 是 `android.*` 或 `androidx.*` 的，加新卡、新敌人、新状态都只动数据层；界面只读状态、发指令。

## 本地开发

需要 JDK 17 和 Android SDK Platform 35。用 Android Studio 直接打开即可。

纯命令行环境可以用脚本在仓库内装一套可删除的工具链（装在 `.toolchain/`，已 gitignore，删目录即完全回滚；默认走国内镜像，失败回退官方源）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup-toolchain.ps1
$env:JAVA_HOME = "$PWD\.toolchain\jdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./gradlew :app:assembleDebug
```

跑测试：

```powershell
./gradlew :app:testDebugUnitTest
```

产物在 `app/build/outputs/apk/debug/`。

### 在模拟器里跑

模拟器需要硬件加速。Windows 上装 Google 的 AEHD 驱动（`sdkmanager "extras;google;Android_Emulator_Hypervisor_Driver"`，再以管理员运行其中的 `silent_install.bat`），macOS/Linux 用系统自带虚拟化。装好后：

```powershell
emulator -avd <你的AVD名> -no-window -gpu swiftshader_indirect
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.duefor.cornered.debug/dev.duefor.cornered.MainActivity
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png
```

调试包的应用 ID 带 `.debug` 后缀，和正式包可以同时安装。

## 发版流程

版本号由 **git 标签**决定，不在代码里手改：标签 `v1.2.3` → `versionName=1.2.3`、`versionCode=10203`（主×10000 + 次×100 + 修订）。

```powershell
git add -A
git commit -m "feat: 新增遗物系统"
git push origin main

git tag v1.0.1
git push origin v1.0.1
```

Actions 会自动构建签名 APK、创建 Release、挂上 `Cornered-1.0.1.apk`，游戏内随即能收到更新。细节见 [docs/RELEASING.md](docs/RELEASING.md)。

## 一次性配置：发布签名

**必须在第一次发版前做完，之后不能换。**

```powershell
powershell -ExecutionPolicy Bypass -File scripts/new-keystore.ps1
```

生成 `release.jks` 与本地 `keystore.properties`（都在 gitignore 里），并打印要填进 GitHub Secrets 的四项：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。**`release.jks` 请离线备份**，丢了就再也无法给已安装的用户推更新。

## 常见问题

**更新下载完装不上？** 签名不一致或 `versionCode` 没递增，两个条件缺一不可。

**Release 用了 debug 签名？** Actions 日志里有 `::warning::仓库没有配置 KEYSTORE_BASE64`，补齐 Secrets 后重跑。

## 许可

[MIT](LICENSE) © Duefor
