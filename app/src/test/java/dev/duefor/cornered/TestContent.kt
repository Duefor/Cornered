package dev.duefor.cornered

import dev.duefor.cornered.game.Card
import dev.duefor.cornered.game.GameContent
import java.io.File

/**
 * 单测用的内容加载：直接读 app/src/main/assets/content 下的真实 JSON，
 * 这样「json 写错了」会被单测直接抓到。
 */
object TestContent {

    private var loaded = false

    fun ensure() {
        if (loaded) return
        val dir = File("src/main/assets/content")
        require(dir.exists()) { "找不到内容目录：${dir.absolutePath}" }
        GameContent.load(
            cardsJson = File(dir, "cards.json").readText(),
            enemiesJson = File(dir, "enemies.json").readText(),
            actsJson = File(dir, "acts.json").readText(),
            relicsJson = File(dir, "relics.json").readText(),
            charactersJson = File(dir, "characters.json").readText(),
        )
        loaded = true
    }

    fun card(id: String): Card {
        ensure()
        return GameContent.cards.byId(id) ?: error("测试卡不存在：$id")
    }
}
