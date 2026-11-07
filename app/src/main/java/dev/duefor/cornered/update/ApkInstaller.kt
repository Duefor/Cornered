package dev.duefor.cornered.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import dev.duefor.cornered.BuildConfig
import java.io.File

/**
 * 通过 PackageInstaller 安装下载好的 APK。
 * 相比 ACTION_VIEW + FileProvider，这条路径不需要暴露 FileProvider，
 * 且由系统安装器接管确认弹窗。
 */
object ApkInstaller {

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** 跳到「安装未知应用」授权页，让用户为本应用打开开关。 */
    fun openUnknownSourcesSetting(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun install(context: Context, apk: File) {
        val packageManager = context.packageManager

        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("APK 解析失败，文件可能已损坏")
        if (archive.packageName != context.packageName) {
            error("APK 包名不匹配：${archive.packageName}")
        }
        @Suppress("DEPRECATION")
        val archiveVersionCode = archive.versionCode
        if (archiveVersionCode <= BuildConfig.VERSION_CODE) {
            error("该安装包版本不高于当前版本")
        }

        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)

        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val callback = Intent(context, InstallResultReceiver::class.java)
                    .setAction(InstallResultReceiver.ACTION_INSTALL_RESULT)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callback,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (t: Throwable) {
            runCatching { installer.abandonSession(sessionId) }
            throw t
        }
    }
}
