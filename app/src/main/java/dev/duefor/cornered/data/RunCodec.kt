package dev.duefor.cornered.data

import dev.duefor.cornered.game.Card
import dev.duefor.cornered.game.CombatEngine
import dev.duefor.cornered.game.CombatPhase
import dev.duefor.cornered.game.CombatSnapshot
import dev.duefor.cornered.game.EnemySnapshot
import dev.duefor.cornered.game.GameContent
import dev.duefor.cornered.game.GameMap
import dev.duefor.cornered.game.MapGenerator
import dev.duefor.cornered.game.Relic
import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState
import dev.duefor.cornered.game.Status
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * 存档的 JSON 编解码。不依赖 Android Context，方便单测。
 *
 * 地图不逐节点存，只存「幕 + 种子」再生成一遍；战斗完整存四个牌堆、能力与敌人状态，
 * 所以中途杀掉 App 也能从任意一步继续。
 */
object RunCodec {

    /** v2：加入幕、遗物、能力牌；v1 存档不再兼容。 */
    const val VERSION = 2

    fun encode(run: RunState): String {
        val json = JSONObject()
        json.put("version", VERSION)
        json.put("character", run.character.id)
        json.put("actIndex", run.actIndex)
        json.put("mapSeed", run.mapSeed)
        json.put("hp", run.hp)
        json.put("maxHp", run.maxHp)
        json.put("gold", run.gold)
        json.put("deepestFloor", run.deepestFloor)
        json.put("phase", run.phase.name)
        json.put("lastGoldGain", run.lastGoldGain)
        json.put("deck", cardIds(run.deck))
        json.put("relics", JSONArray(run.relics.map { it.id }))
        json.put("reachable", JSONArray(run.reachable.sorted()))
        json.put("rewards", cardIds(run.rewardCards))
        json.put("pendingRelic", run.pendingRelic?.id ?: JSONObject.NULL)
        json.put("currentNodeId", run.currentNodeId ?: JSONObject.NULL)
        run.combat?.let { json.put("combat", combatJson(it)) }
        return json.toString()
    }

    fun decode(raw: String): RunState? = runCatching {
        val json = JSONObject(raw)
        require(json.getInt("version") == VERSION) { "存档版本不匹配" }

        val character = GameContent.characterOrDefault(json.optString("character"))
        val actIndex = json.getInt("actIndex")
        val mapSeed = json.getLong("mapSeed")
        val act = GameContent.act(actIndex)
        val map: GameMap = MapGenerator.generate(act, Random(mapSeed))

        val reachableArray = json.getJSONArray("reachable")
        val reachable = (0 until reachableArray.length()).map { reachableArray.getInt(it) }.toSet()

        val nodeId = json.optInt("currentNodeId", -1).takeIf { it >= 0 }
            ?.takeIf { it in map.nodes.indices }

        val relics = json.optJSONArray("relics")?.let { array ->
            (0 until array.length()).map { index ->
                val id = array.getString(index)
                GameContent.relics.firstOrNull { it.id == id } ?: error("未知遗物：$id")
            }
        }.orEmpty()

        val pendingRelicId = json.optString("pendingRelic").takeIf { it.isNotBlank() && it != "null" }
        val pendingRelic: Relic? = pendingRelicId?.let { id ->
            GameContent.relics.firstOrNull { it.id == id } ?: error("未知遗物：$id")
        }

        val combat = json.optJSONObject("combat")?.let { combatFrom(it, mapSeed, relics) }

        RunState.restore(
            rng = Random(mapSeed xor 0x5DEECE66DL),
            mapSeed = mapSeed,
            map = map,
            character = character,
            actIndex = actIndex,
            hp = json.getInt("hp"),
            maxHp = json.getInt("maxHp"),
            gold = json.getInt("gold"),
            deck = cards(json.getJSONArray("deck")),
            relics = relics,
            deepestFloor = json.getInt("deepestFloor"),
            currentNodeId = nodeId,
            reachable = reachable,
            phase = RunPhase.valueOf(json.getString("phase")),
            combat = combat,
            rewardCards = cards(json.getJSONArray("rewards")),
            pendingRelic = pendingRelic,
            lastGoldGain = json.optInt("lastGoldGain", 0),
        )
    }.getOrNull()

    // ---------------------------------------------------------------- 内部

    private fun cardIds(cards: List<Card>): JSONArray =
        JSONArray().apply { cards.forEach { put(it.id) } }

    private fun cards(array: JSONArray): List<Card> {
        val result = ArrayList<Card>(array.length())
        for (index in 0 until array.length()) {
            val id = array.getString(index)
            result += GameContent.cards.byId(id) ?: error("未知卡牌：$id")
        }
        return result
    }

    private fun statusJson(statuses: Map<Status, Int>): JSONObject =
        JSONObject().apply { statuses.forEach { (status, stacks) -> put(status.name, stacks) } }

    private fun statuses(json: JSONObject): Map<Status, Int> {
        val result = mutableMapOf<Status, Int>()
        json.keys().forEach { key ->
            val status = Status.fromJson(key) ?: return@forEach
            result[status] = json.optInt(key)
        }
        return result
    }

    private fun combatJson(engine: CombatEngine): JSONObject {
        val snap = engine.snapshot()
        return JSONObject().apply {
            put("hp", snap.hp)
            put("maxHp", snap.maxHp)
            put("maxEnergy", snap.maxEnergy)
            put("handSize", snap.handSize)
            put("block", snap.block)
            put("energy", snap.energy)
            put("turn", snap.turn)
            put("phase", snap.phase.name)
            put("statuses", statusJson(snap.statuses))
            put("drawPile", cardIds(snap.drawPile))
            put("hand", cardIds(snap.hand))
            put("discardPile", cardIds(snap.discardPile))
            put("exhaustPile", cardIds(snap.exhaustPile))
            put("powers", cardIds(snap.activePowers))

            val enemyArray = JSONArray()
            snap.enemies.forEach { enemy ->
                enemyArray.put(
                    JSONObject().apply {
                        put("id", enemy.def.id)
                        put("hp", enemy.hp)
                        put("maxHp", enemy.maxHp)
                        put("block", enemy.block)
                        put("statuses", statusJson(enemy.statuses))
                        put("moveIndex", enemy.moveIndex)
                    },
                )
            }
            put("enemies", enemyArray)
        }
    }

    private fun combatFrom(json: JSONObject, mapSeed: Long, relics: List<Relic>): CombatEngine {
        val enemyArray = json.getJSONArray("enemies")
        val enemies = ArrayList<EnemySnapshot>(enemyArray.length())
        for (index in 0 until enemyArray.length()) {
            val item = enemyArray.getJSONObject(index)
            val def = GameContent.enemies.byId(item.getString("id"))
                ?: error("未知敌人：${item.getString("id")}")
            enemies += EnemySnapshot(
                def = def,
                hp = item.getInt("hp"),
                maxHp = item.getInt("maxHp"),
                block = item.optInt("block", 0),
                statuses = statuses(item.optJSONObject("statuses") ?: JSONObject()),
                moveIndex = item.optInt("moveIndex", 0),
            )
        }

        val snapshot = CombatSnapshot(
            hp = json.getInt("hp"),
            maxHp = json.getInt("maxHp"),
            maxEnergy = json.optInt("maxEnergy", CombatEngine.DEFAULT_MAX_ENERGY),
            handSize = json.optInt("handSize", CombatEngine.DEFAULT_HAND_SIZE),
            block = json.optInt("block", 0),
            energy = json.optInt("energy", CombatEngine.DEFAULT_MAX_ENERGY),
            turn = json.optInt("turn", 1),
            phase = CombatPhase.valueOf(json.getString("phase")),
            statuses = statuses(json.optJSONObject("statuses") ?: JSONObject()),
            drawPile = cards(json.getJSONArray("drawPile")),
            hand = cards(json.getJSONArray("hand")),
            discardPile = cards(json.getJSONArray("discardPile")),
            exhaustPile = cards(json.getJSONArray("exhaustPile")),
            activePowers = json.optJSONArray("powers")?.let { cards(it) }.orEmpty(),
            enemies = enemies,
        )
        return CombatEngine.restore(snapshot, Random(mapSeed xor 0x2545F4914F6CDD1DL), relics)
    }
}
