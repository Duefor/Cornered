# 发版指南

## 0. 前置：发布签名（只做一次）

```powershell
powershell -ExecutionPolicy Bypass -File scripts/new-keystore.ps1
```

把打印出来的四项填进仓库的 `Settings → Secrets and variables → Actions`：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

**`release.jks` 与 `keystore.properties` 都在 `.gitignore` 里，永远不要提交。** 请把 `release.jks` 备份到至少两个离线位置。

> 覆盖安装（`adb install -r` 或应用内更新）要求包名相同且签名相同。keystore 一旦丢失，老用户就只能卸载重装。

## 1. 版本号规则

版本号不写在代码里，由 git 标签推导：

| 标签 | versionName | versionCode |
| --- | --- | --- |
| `v1.0.0` | 1.0.0 | 10000 |
| `v1.0.1` | 1.0.1 | 10001 |
| `v1.2.0` | 1.2.0 | 10200 |
| `v2.0.0` | 2.0.0 | 20000 |

规则：`主 × 10000 + 次 × 100 + 修订`。**versionCode 必须严格递增**，否则应用内更新会被判定为「不更新」而且系统拒绝覆盖安装。

标签格式必须是三段数字，`v1.0-beta` 这类标签构建脚本解析不了。

## 2. 发版

```powershell
git add -A
git commit -m "feat: 新增冲刺残留特效"
git push origin main

git tag v1.0.1
git push origin v1.0.1
```

推标签即触发 `.github/workflows/release.yml`：

1. checkout 该标签
2. 装 JDK 17 + Gradle
3. 从标签解析 `versionName` / `versionCode`
4. 解码 `KEYSTORE_BASE64` 到临时目录
5. `./gradlew :app:assembleRelease -Pcornered.versionName=… -Pcornered.versionCode=…`
6. 产物重命名为 `Cornered-1.0.1.apk` 并上传构建产物
7. `softprops/action-gh-release` 创建 Release、自动生成更新日志、挂上 APK

## 3. 重新发布同一个版本

标签已经存在但需要重跑构建（比如 Secrets 之前没配好）：

```powershell
# Actions 页面 Run workflow，填 tag = v1.0.1
# 或本地
gh workflow run release.yml -f tag=v1.0.1
```

`softprops/action-gh-release` 会更新已存在的 Release 并替换同名资产。

## 4. 验证发布效果

- Releases 页面能看到版本、APK 与更新说明
- `https://api.github.com/repos/Duefor/Cornered/releases/latest` 返回 `tag_name` 正确、`assets` 里有 `.apk`
- 手机装上一版，打开游戏应弹出版本更新框

## 5. 排错

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 更新框下载完提示安装失败 | 签名不一致或 versionCode 未递增 | 检查是否换过 keystore；确认标签递增 |
| 日志出现 `::warning::仓库没有配置 KEYSTORE_BASE64` | Secrets 缺失 | 补齐四项后重跑 |
| Release 里没有 APK 资产 | `fail_on_unmatched_files` 触发，构建未产出 | 看构建日志 |
| 游戏里提示「检查更新失败：HTTP 403」 | API 限流 | 等一小时，或减少调试频率 |
| 应用内更新没有提示 | Release 被标记为 prerelease / draft | 发布时不要勾选 |

## 6. 首次建仓（如果还没建）

仓库页 → New repository，名字 `Cornered`，**不要**勾选初始化 README（本地已有）。然后：

```powershell
cd D:\Users\Desktop\winner
git init
git add -A
git commit -m "chore: 初始化 Cornered 工程"
git branch -M main
git remote add origin https://github.com/Duefor/Cornered.git
git push -u origin main
```

推送时会弹出浏览器登录，或用 Personal Access Token 作为密码。
