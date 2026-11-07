package dev.duefor.cornered.update

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.duefor.cornered.BuildConfig
import java.io.File

/**
 * 应用内更新的状态机：检查 -> 下载 -> 交给系统安装器。
 * 所有可变字段都是 Compose state，UI 直接读取即可。
 */
class UpdateController(context: Context) {

    private val appContext = context.applicationContext

    var checking by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set
    var release by mutableStateOf<ReleaseInfo?>(null)
    var message by mutableStateOf<String?>(null)

    val currentVersion: String = BuildConfig.VERSION_NAME

    suspend fun check(silent: Boolean) {
        if (checking || downloading) return
        checking = true
        try {
            GitHubReleases.latest(BuildConfig.UPDATE_REPO)
                .onSuccess { info ->
                    if (info != null && Version.isNewer(info.versionName, currentVersion)) {
                        release = info
                    } else {
                        release = null
                        if (!silent) message = "已是最新版本 v$currentVersion"
                    }
                }
                .onFailure { error ->
                    if (!silent) message = "检查更新失败：${error.message ?: "网络异常"}"
                }
        } finally {
            checking = false
        }
    }

    suspend fun downloadAndInstall() {
        val info = release ?: return
        if (downloading) return

        if (!ApkInstaller.canInstall(appContext)) {
            ApkInstaller.openUnknownSourcesSetting(appContext)
            message = "请允许 Cornered 安装未知来源应用，然后回来再点一次"
            return
        }

        downloading = true
        progress = 0f
        try {
            val target = File(appContext.cacheDir, "cornered-${info.versionName}.apk")
            GitHubReleases.downloadApk(info.apkUrl, target) { value -> progress = value }
            ApkInstaller.install(appContext, target)
            message = "安装器已打开，按提示完成更新"
        } catch (t: Throwable) {
            message = "更新失败：${t.message ?: t.javaClass.simpleName}"
        } finally {
            downloading = false
        }
    }

    fun dismiss() {
        release = null
    }
}
