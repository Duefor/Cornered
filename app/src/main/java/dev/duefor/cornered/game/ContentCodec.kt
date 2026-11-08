package dev.duefor.cornered.game

import org.json.JSONArray
import org.json.JSONObject

/**
 * 把 assets/content 下的 JSON 解析成模型对象。
 * 解析全程「严格」：字段缺失或取值不认识会直接抛异常并带上卡牌/敌人 id，
 * 这样加内容写错时能立刻定位，而不是进游戏才发现。
 */
object ContentCodec {

    fun cards(json: String): List<Card> {
        val root = JSONObject(json)
        val array = root.getJSONArray("cards")
        val result = ArrayList<Card>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            result += Card(
                id = id,
                name = item.getString("name"),
                cost = item.getInt("cost"),
                type = CardType.fromJson(item.getString("type"))
                    ?: error("卡牌 $id 的类型不认识：${item.getString("type")}"),
                rarity = Rarity.fromJson(item.getString("rarity"))
                    ?: error("卡牌 $id 的稀有度不认识：${item.getString("rarity")}"),
                target = target(item.optString("target", "enemy"), id),
                description = item.optString("text", ""),
                effects = effects(item.optJSONArray("effects"), id),
                exhaust = item.optBoolean("exhaust", false),
                triggers = triggers(item.optJSONArray("triggers"), id),
                keepBlock = item.optBoolean("keepBlock", false),
            )
        }
        return result
    }

    fun enemies(json: String): List<EnemyDef> {
        val root = JSONObject(json)
        val array = root.getJSONArray("enemies")
        val result = ArrayList<EnemyDef>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            result += EnemyDef(
                id = id,
                name = item.getString("name"),
                kind = EnemyKind.fromJson(item.getString("kind"))
                    ?: error("敌人 $id 的种类不认识：${item.getString("kind")}"),
                minHp = item.getInt("minHp"),
                maxHp = item.getInt("maxHp"),
                pattern = pattern(item.getJSONArray("pattern"), id),
            )
        }
        return result
    }

    fun acts(json: String): List<Act> {
        val root = JSONObject(json)
        val array = root.getJSONArray("acts")
        val result = ArrayList<Act>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            val weightsJson = item.getJSONObject("weights")
            val weights = buildMap {
                NodeType.entries.forEach { type ->
                    val key = when (type) {
                        NodeType.Combat -> "combat"
                        NodeType.Elite -> "elite"
                        NodeType.Rest -> "rest"
                        NodeType.Treasure -> "treasure"
                        NodeType.Boss -> "boss"
                    }
                    if (weightsJson.has(key)) put(type, weightsJson.getInt(key))
                }
            }
            result += Act(
                id = id,
                name = item.getString("name"),
                floors = item.getInt("floors"),
                encounters = groupIds(item.getJSONArray("encounters"), id),
                elites = groupIds(item.getJSONArray("elites"), id),
                bosses = groupIds(item.getJSONArray("bosses"), id),
                weights = weights,
            )
        }
        return result
    }

    fun relics(json: String): List<Relic> {
        val root = JSONObject(json)
        val array = root.getJSONArray("relics")
        val result = ArrayList<Relic>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            result += Relic(
                id = id,
                name = item.getString("name"),
                text = item.optString("text", ""),
                triggers = triggers(item.optJSONArray("triggers"), id),
            )
        }
        return result
    }

    fun characters(json: String, cards: Map<String, Card>): List<Character> {
        val root = JSONObject(json)
        val array = root.getJSONArray("characters")
        val result = ArrayList<Character>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getString("id")

            val starter = ArrayList<Card>()
            val deckArray = item.getJSONArray("starterDeck")
            for (i in 0 until deckArray.length()) {
                val entry = deckArray.getJSONObject(i)
                val cardId = entry.getString("card")
                val card = cards[cardId] ?: error("角色 $id 的起始牌组引用了不存在的卡：$cardId")
                repeat(entry.getInt("count")) { starter += card }
            }

            val poolArray = item.getJSONArray("rewardPool")
            val pool = ArrayList<Card>(poolArray.length())
            for (i in 0 until poolArray.length()) {
                val cardId = poolArray.getString(i)
                pool += cards[cardId] ?: error("角色 $id 的奖励池引用了不存在的卡：$cardId")
            }

            result += Character(
                id = id,
                name = item.getString("name"),
                subtitle = item.optString("subtitle", ""),
                description = item.optString("description", ""),
                maxHp = item.getInt("maxHp"),
                maxEnergy = item.getInt("maxEnergy"),
                handSize = item.getInt("handSize"),
                starterDeck = starter,
                rewardPool = pool,
            )
        }
        return result
    }

    // ---------------------------------------------------------------- 内部

    private fun target(name: String, owner: String): Target = when (name.lowercase()) {
        "enemy" -> Target.Enemy
        "all", "allenemies" -> Target.AllEnemies
        "self" -> Target.Self
        else -> error("$owner 的目标类型不认识：$name")
    }

    private fun effects(array: JSONArray?, owner: String): List<Effect> {
        if (array == null) return emptyList()
        val result = ArrayList<Effect>(array.length())
        for (index in 0 until array.length()) {
            result += effect(array.getJSONObject(index), owner)
        }
        return result
    }

    private fun effect(json: JSONObject, owner: String): Effect = when (val kind = json.getString("kind")) {
        "damage" -> Effect.Damage(
            amount = json.getInt("amount"),
            times = json.optInt("times", 1),
            target = target(json.optString("target", "enemy"), owner),
        )
        "block" -> Effect.Block(json.getInt("amount"))
        "draw" -> Effect.Draw(json.getInt("count"))
        "energy" -> Effect.Energy(json.getInt("amount"))
        "apply" -> Effect.Apply(
            status = Status.fromJson(json.getString("status"))
                ?: error("$owner 的状态不认识：${json.getString("status")}"),
            stacks = json.getInt("stacks"),
            target = target(json.optString("target", "enemy"), owner),
        )
        "heal" -> Effect.Heal(json.getInt("amount"))
        "selfDamage" -> Effect.SelfDamage(json.getInt("amount"))
        else -> error("$owner 的效果类型不认识：$kind")
    }

    private fun triggers(array: JSONArray?, owner: String): List<TriggeredEffect> {
        if (array == null) return emptyList()
        val result = ArrayList<TriggeredEffect>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val eventName = item.getString("on")
            val event = TriggerEvent.fromJson(eventName)
                ?: error("$owner 的触发时机不认识：$eventName")
            result += TriggeredEffect(
                on = event,
                effects = effects(item.optJSONArray("effects"), owner),
                every = item.optInt("every", 1).coerceAtLeast(1),
                cardType = item.optString("cardType").takeIf { it.isNotBlank() }?.let { name ->
                    CardType.fromJson(name) ?: error("$owner 的 cardType 不认识：$name")
                },
            )
        }
        return result
    }

    private fun pattern(array: JSONArray, owner: String): List<Intent> {
        val result = ArrayList<Intent>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result += when (val kind = item.getString("kind")) {
                "attack" -> Intent.Attack(item.getInt("damage"), item.optInt("times", 1))
                "defend" -> Intent.Defend(item.getInt("block"))
                "buff" -> Intent.Buff(
                    Status.fromJson(item.getString("status")) ?: error("$owner 的状态不认识"),
                    item.getInt("stacks"),
                )
                "debuff" -> Intent.Debuff(
                    Status.fromJson(item.getString("status")) ?: error("$owner 的状态不认识"),
                    item.getInt("stacks"),
                )
                else -> error("$owner 的意图类型不认识：$kind")
            }
        }
        require(result.isNotEmpty()) { "$owner 的出招表不能为空" }
        return result
    }

    private fun groupIds(array: JSONArray, owner: String): List<List<String>> {
        val result = ArrayList<List<String>>(array.length())
        for (index in 0 until array.length()) {
            val group = array.getJSONArray(index)
            result += (0 until group.length()).map { group.getString(it) }
        }
        return result
    }
}
