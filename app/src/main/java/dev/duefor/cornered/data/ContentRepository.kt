package dev.duefor.cornered.data

import android.content.Context
import dev.duefor.cornered.game.GameContent

/**
 * 从 assets/content 读取内容并装进 [GameContent]。
 * 只在 App 启动时调用一次；内容解析失败会直接抛异常（宁可开屏就发现，也别玩到一半才崩）。
 */
object ContentRepository {

    private val FILES = listOf("cards.json", "enemies.json", "acts.json", "relics.json", "characters.json")

    fun load(context: Context) {
        if (GameContent.loaded) return
        val assets = context.applicationContext.assets
        fun read(name: String): String = assets.open("content/$name").bufferedReader().use { it.readText() }

        GameContent.load(
            cardsJson = read("cards.json"),
            enemiesJson = read("enemies.json"),
            actsJson = read("acts.json"),
            relicsJson = read("relics.json"),
            charactersJson = read("characters.json"),
        )
        require(FILES.isNotEmpty()) { "" }
    }
}
