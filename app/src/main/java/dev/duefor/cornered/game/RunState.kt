package dev.duefor.cornered.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class RunPhase { Map, Combat, Reward, Rest, Treasure, ActClear, Victory, Defeat }

/**
 * 一局游戏的全部状态与流程：幕 -> 地图 -> 战斗 -> 奖励 -> 下一层 -> 幕间 -> 下一幕。
 * 纯 Kotlin，界面只负责把 [phase] 画出来。
 */
class RunState(
    val rng: Random,
    var mapSeed: Long,
    var map: GameMap,
    val character: Character,
) {
    var actIndex: Int = 0
        private set

    var maxHp: Int = character.maxHp
        private set
    var hp: Int = character.maxHp
        private set
    var gold: Int = 0
        private set

    val deck: MutableList<Card> = character.starterDeck.toMutableList()
    val relics: MutableList<Relic> = mutableListOf()

    var phase: RunPhase = RunPhase.Map
        private set
    var currentNodeId: Int? = null
        private set
    var deepestFloor: Int = 0
        private set
    var reachable: Set<Int> = map.startNodes().map { it.id }.toSet()
        private set

    var combat: CombatEngine? = null
        private set
    var rewardCards: List<Card> = emptyList()
        private set
    var pendingRelic: Relic? = null
        private set
    var lastGoldGain: Int = 0
        private set

    val act: Act get() = GameContent.act(actIndex)
    val isFinalAct: Boolean get() = actIndex >= GameContent.lastActIndex
    val currentNode: MapNode? get() = currentNodeId?.let { map.node(it) }

    fun isReachable(id: Int): Boolean = phase == RunPhase.Map && id in reachable

    fun enterNode(id: Int) {
        if (!isReachable(id)) return
        val node = map.node(id)
        currentNodeId = id
        deepestFloor = max(deepestFloor, actIndex * 100 + node.floor + 1)

        when (node.type) {
            NodeType.Combat, NodeType.Elite, NodeType.Boss -> {
                val enemies = encounterFor(node.type).map { def ->
                    EnemyState(def, rng.nextInt(def.minHp, def.maxHp + 1))
                }
                combat = CombatEngine(
                    startHp = hp,
                    maxHp = maxHp,
                    deck = deck,
                    enemies = enemies,
                    rng = rng,
                    maxEnergy = character.maxEnergy,
                    handSize = character.handSize,
                    relics = relics,
                )
                phase = RunPhase.Combat
            }

            NodeType.Rest -> phase = RunPhase.Rest

            NodeType.Treasure -> {
                lastGoldGain = 25 + rng.nextInt(26)
                gold += lastGoldGain
                phase = RunPhase.Treasure
            }
        }
    }

    /** 战斗画面在引擎进入 Victory/Defeat 后调用，结算血量与掉落。 */
    fun finishCombat() {
        val engine = combat ?: return
        hp = engine.hp
        if (engine.phase == CombatPhase.Defeat) {
            phase = RunPhase.Defeat
            return
        }
        val node = currentNode ?: return

        if (node.type == NodeType.Boss) {
            if (isFinalAct) {
                phase = RunPhase.Victory
                return
            }
            // 非最终 boss：金币 + 三选一 + 必掉遗物，然后进下一幕
            lastGoldGain = 80
            gold += lastGoldGain
            rewardCards = rewardChoices()
            pendingRelic = rollRelic(always = true)
            phase = RunPhase.ActClear
            return
        }

        lastGoldGain = if (node.type == NodeType.Elite) 45 else 15 + rng.nextInt(11)
        gold += lastGoldGain
        rewardCards = rewardChoices()
        pendingRelic = rollRelic(always = node.type == NodeType.Elite)
        phase = RunPhase.Reward
    }

    fun takeReward(card: Card?) {
        if (phase != RunPhase.Reward) return
        if (card != null) deck += card
        pendingRelic?.let { relics += it }
        pendingRelic = null
        advanceFromCurrent()
    }

    /** 幕间结算：拿牌 + 必得遗物，然后进入下一幕。 */
    fun takeActClearReward(card: Card?) {
        if (phase != RunPhase.ActClear) return
        if (card != null) deck += card
        pendingRelic?.let { relics += it }
        pendingRelic = null
        enterNextAct()
    }

    fun restHeal() {
        if (phase != RunPhase.Rest) return
        hp = min(maxHp, hp + max(10, maxHp * 3 / 10))
        advanceFromCurrent()
    }

    fun restTrain() {
        if (phase != RunPhase.Rest) return
        maxHp += 5
        hp += 5
        advanceFromCurrent()
    }

    fun leaveTreasure() {
        if (phase != RunPhase.Treasure) return
        advanceFromCurrent()
    }

    // ---------------------------------------------------------------- 内部

    private fun rewardChoices(): List<Card> =
        GameContent.cards.rewardChoices(rng, character.rewardPool)

    private fun rollRelic(always: Boolean): Relic? {
        if (!always && rng.nextInt(100) >= RELIC_CHANCE_NORMAL) return null
        return GameContent.randomUnownedRelic(relics.map { it.id }.toSet(), rng)
    }

    private fun encounterFor(type: NodeType): List<EnemyDef> {
        val current = act
        val pool = when (type) {
            NodeType.Boss -> current.bosses
            NodeType.Elite -> current.elites
            else -> current.encounters
        }
        val group = pool[rng.nextInt(pool.size)]
        return group.map { GameContent.enemies.require(it) }
    }

    private fun advanceFromCurrent() {
        val current = currentNodeId
        reachable = if (current == null) {
            map.startNodes().map { it.id }.toSet()
        } else {
            map.nextNodes(current).map { it.id }.toSet()
        }
        phase = RunPhase.Map
        combat = null
        rewardCards = emptyList()
    }

    private fun enterNextAct() {
        if (isFinalAct) {
            phase = RunPhase.Victory
            return
        }
        actIndex += 1
        mapSeed = rng.nextLong()
        map = MapGenerator.generate(act, Random(mapSeed))
        currentNodeId = null
        reachable = map.startNodes().map { it.id }.toSet()
        combat = null
        rewardCards = emptyList()
        pendingRelic = null
        phase = RunPhase.Map
    }

    companion object {
        /** 普通战斗掉遗物的概率（%） */
        const val RELIC_CHANCE_NORMAL = 12

        fun newRun(
            character: Character = GameContent.characters.first(),
            seed: Long = System.nanoTime(),
        ): RunState {
            val mapSeed = seed * 31 + 7
            return RunState(
                rng = Random(seed),
                mapSeed = mapSeed,
                map = MapGenerator.generate(GameContent.act(0), Random(mapSeed)),
                character = character,
            )
        }

        fun restore(
            rng: Random,
            mapSeed: Long,
            map: GameMap,
            character: Character,
            actIndex: Int,
            hp: Int,
            maxHp: Int,
            gold: Int,
            deck: List<Card>,
            relics: List<Relic>,
            deepestFloor: Int,
            currentNodeId: Int?,
            reachable: Set<Int>,
            phase: RunPhase,
            combat: CombatEngine?,
            rewardCards: List<Card>,
            pendingRelic: Relic?,
            lastGoldGain: Int,
        ): RunState {
            val run = RunState(rng, mapSeed, map, character)
            run.actIndex = actIndex.coerceIn(0, GameContent.lastActIndex)
            run.hp = hp.coerceIn(0, maxHp)
            run.maxHp = maxHp
            run.gold = gold
            run.deck.clear()
            run.deck.addAll(deck)
            run.relics.clear()
            run.relics.addAll(relics)
            run.deepestFloor = deepestFloor
            run.currentNodeId = currentNodeId
            run.reachable = reachable
            run.phase = phase
            run.combat = combat
            run.rewardCards = rewardCards
            run.pendingRelic = pendingRelic
            run.lastGoldGain = lastGoldGain
            return run
        }
    }
}
