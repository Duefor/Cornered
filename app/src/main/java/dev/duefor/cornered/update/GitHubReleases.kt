package dev.duefor.cornered.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

/**
 * 只读访问 GitHub Releases API：
 *  - 拉取最新正式版
 *  - 下载其中的 .apk 资产
 *
 * 未登录的 API 有每小时 60 次的限额，对单人游戏完全够用。
 */
object GitHubReleases {

    private const val API_ROOT = "https://api.github.com"
    private const val USER_AGENT = "Cornered-Android"
    private const val TIMEOUT_MS = 20_000

    suspend fun latest(repo: String): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("$API_ROOT/repos/$repo/releases/latest", "application/vnd.github+json")
            val code = connection.responseCode
            try {
                when {
                    code == HttpURLConnection.HTTP_NOT_FOUND -> null // 仓库还没有发布过 Release
                    code !in 200..299 -> error("GitHub 返回 HTTP $code")
                    else -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        parse(body)
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun downloadApk(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val connection = open(url, "application/octet-stream")
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("下载失败 HTTP $code")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (total > 0 && destination.length() != total) {
                destination.delete()
                error("下载不完整")
            }
            onProgress(1f)
            destination
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, accept: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("User-Agent", USER_AGENT)
        return connection
    }

    private fun parse(body: String): ReleaseInfo? {
        val json = JSONObject(body)
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null

        val tag = json.optString("tag_name").trim()
        if (tag.isEmpty()) return null

        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                apkSize = asset.optLong("size")
                if (apkUrl != null) break
            }
        }
        val url = apkUrl ?: return null

        val versionName = tag.removePrefix("v").removePrefix("V")
        return ReleaseInfo(
            tag = tag,
            versionName = versionName,
            versionCode = Version.encodeCode(versionName),
            notes = json.optString("body").trim(),
            apkUrl = url,
            apkSize = apkSize,
        )
    }
}
