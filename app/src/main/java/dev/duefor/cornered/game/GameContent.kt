package dev.duefor.cornered.game

/**
 * 全部内容（卡牌 / 敌人 / 幕 / 遗物 / 角色）的注册表。
 *
 * 加内容只改 assets/content 下的 JSON，不需要动任何 Kotlin 代码；
 * [load] 会做一遍交叉校验，引用了不存在的 id 会立刻报错（单测也会跑这套校验）。
 */
object GameContent {

    var cards: CardCatalog = CardCatalog(emptyList())
        private set
    var enemies: EnemyCatalog = EnemyCatalog(emptyList())
        private set
    var acts: List<Act> = emptyList()
        private set
    var relics: List<Relic> = emptyList()
        private set
    var characters: List<Character> = emptyList()
        private set

    var loaded: Boolean = false
        private set

    fun load(
        cardsJson: String,
        enemiesJson: String,
        actsJson: String,
        relicsJson: String,
        charactersJson: String,
    ) {
        val cardCatalog = CardCatalog(ContentCodec.cards(cardsJson))
        val enemyCatalog = EnemyCatalog(ContentCodec.enemies(enemiesJson))
        val actList = ContentCodec.acts(actsJson)
        val relicList = ContentCodec.relics(relicsJson)
        val characterList = ContentCodec.characters(charactersJson, cardCatalog.asMap)

        validate(actList, enemyCatalog, relicList, characterList)

        cards = cardCatalog
        enemies = enemyCatalog
        acts = actList
        relics = relicList
        characters = characterList
        loaded = true
    }

    fun characterOrDefault(id: String?): Character =
        characters.firstOrNull { it.id == id } ?: characters.first()

    fun act(index: Int): Act = acts[index.coerceIn(0, acts.lastIndex)]

    val lastActIndex: Int get() = acts.lastIndex

    /** 还没拥有的遗物里随机一个；全都有了就返回 null。 */
    fun randomUnownedRelic(owned: Set<String>, rng: kotlin.random.Random): Relic? {
        val pool = relics.filter { it.id !in owned }
        if (pool.isEmpty()) return null
        return pool[rng.nextInt(pool.size)]
    }

    private fun validate(
        actList: List<Act>,
        enemyCatalog: EnemyCatalog,
        relicList: List<Relic>,
        characterList: List<Character>,
    ) {
        require(actList.isNotEmpty()) { "acts.json 至少要有一幕" }
        require(characterList.isNotEmpty()) { "characters.json 至少要有一个角色" }

        val relicDupes = relicList.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(relicDupes.isEmpty()) { "遗物 id 重复：$relicDupes" }

        actList.forEach { act ->
            require(act.floors in 5..30) { "幕 ${act.id} 的楼层数不合理：${act.floors}" }
            require(act.encounters.isNotEmpty()) { "幕 ${act.id} 没有普通遭遇" }
            require(act.elites.isNotEmpty()) { "幕 ${act.id} 没有精英" }
            require(act.bosses.isNotEmpty()) { "幕 ${act.id} 没有首领" }
            require(act.weights.isNotEmpty()) { "幕 ${act.id} 没有节点权重" }

            (act.encounters + act.elites + act.bosses).forEach { group ->
                require(group.isNotEmpty()) { "幕 ${act.id} 有一个空遭遇" }
                group.forEach { id ->
                    require(enemyCatalog.byId(id) != null) { "幕 ${act.id} 引用了不存在的敌人：$id" }
                }
            }
        }

        characterList.forEach { character ->
            require(character.starterDeck.size >= 8) { "角色 ${character.id} 起始牌组太少" }
            require(character.rewardPool.isNotEmpty()) { "角色 ${character.id} 奖励池为空" }
            require(character.handSize in 3..8) { "角色 ${character.id} 手牌数不合理" }
        }
    }
}

class CardCatalog(val all: List<Card>) {
    private val map: Map<String, Card>

    init {
        val dupes = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "卡牌 id 重复：$dupes" }
        map = all.associateBy { it.id }
    }

    val asMap: Map<String, Card> get() = map

    fun byId(id: String): Card? = map[id]

    /** 按稀有度权重抽 [count] 张不重复的卡。 */
    fun rewardChoices(rng: kotlin.random.Random, source: List<Card>, count: Int = 3): List<Card> {
        val pool = source.toMutableList()
        val picked = mutableListOf<Card>()
        repeat(count.coerceAtMost(pool.size)) {
            val totalWeight = pool.sumOf { it.rarity.weight }
            var roll = rng.nextInt(totalWeight)
            var chosen = pool.first()
            for (card in pool) {
                roll -= card.rarity.weight
                if (roll < 0) {
                    chosen = card
                    break
                }
            }
            picked += chosen
            pool.remove(chosen)
        }
        return picked
    }
}

class EnemyCatalog(val all: List<EnemyDef>) {
    private val map: Map<String, EnemyDef>

    init {
        val dupes = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "敌人 id 重复：$dupes" }
        map = all.associateBy { it.id }
    }

    fun byId(id: String): EnemyDef? = map[id]

    fun require(id: String): EnemyDef = map[id] ?: error("未知敌人：$id")
}
