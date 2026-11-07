package dev.duefor.cornered.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class CombatPhase { PlayerTurn, EnemyTurn, Victory, Defeat }

class EnemyState(val def: EnemyDef, hp: Int) {
    var hp: Int = hp
    var maxHp: Int = hp
    var block: Int = 0
    val statuses = mutableMapOf<Status, Int>()
    var moveIndex: Int = 0

    val alive: Boolean get() = hp > 0
    val intent: Intent get() = def.pattern[moveIndex % def.pattern.size]

    fun statusOf(status: Status): Int = statuses[status] ?: 0
}

/** 战斗的完整快照，用于存档/读档。 */
data class CombatSnapshot(
    val hp: Int,
    val maxHp: Int,
    val block: Int,
    val energy: Int,
    val turn: Int,
    val phase: CombatPhase,
    val statuses: Map<Status, Int>,
    val drawPile: List<Card>,
    val hand: List<Card>,
    val discardPile: List<Card>,
    val exhaustPile: List<Card>,
    val enemies: List<EnemySnapshot>,
)

data class EnemySnapshot(
    val def: EnemyDef,
    val hp: Int,
    val maxHp: Int,
    val block: Int,
    val statuses: Map<Status, Int>,
    val moveIndex: Int,
)

/**
 * 一回合制战斗的全部规则，纯 Kotlin、无 Android 依赖，可以直接单测。
 *
 * 回合流程：玩家出牌 -> 结束回合 -> 弃手牌/自身负面状态 -1 -> 敌人依次行动 -> 玩家回合开始
 * （格挡清零、能量回满、抽 5 张）。
 */
class CombatEngine private constructor(
    val maxHp: Int,
    val enemies: List<EnemyState>,
    private val rng: Random,
) {
    var hp: Int = 0
        private set
    var block: Int = 0
        private set
    var energy: Int = MAX_ENERGY
        private set
    var turn: Int = 1
        private set
    var phase: CombatPhase = CombatPhase.PlayerTurn
        private set

    val statuses = mutableMapOf<Status, Int>()

    val drawPile = ArrayDeque<Card>()
    val hand = mutableListOf<Card>()
    val discardPile = mutableListOf<Card>()
    val exhaustPile = mutableListOf<Card>()
    val log = mutableListOf<String>()

    constructor(
        startHp: Int,
        maxHp: Int,
        deck: List<Card>,
        enemies: List<EnemyState>,
        rng: Random,
    ) : this(maxHp, enemies, rng) {
        hp = startHp.coerceIn(0, maxHp)
        drawPile.addAll(deck)
        drawPile.shuffle(rng)
        draw(HAND_SIZE)
    }

    val aliveEnemies: List<EnemyState> get() = enemies.filter { it.alive }
    val isOver: Boolean get() = phase == CombatPhase.Victory || phase == CombatPhase.Defeat

    fun statusOf(status: Status): Int = statuses[status] ?: 0
    fun hasStatus(status: Status): Boolean = statusOf(status) > 0

    fun canPlay(index: Int): Boolean =
        phase == CombatPhase.PlayerTurn &&
            index in hand.indices &&
            hand[index].cost <= energy

    /**
     * @param targetIndex 指向 [aliveEnemies] 的下标，只有需要单体目标的卡才用得到
     */
    fun playCard(index: Int, targetIndex: Int = 0): Boolean {
        if (!canPlay(index)) return false
        val card = hand[index]

        val targets = when (card.target) {
            Target.Self -> emptyList()
            Target.AllEnemies -> aliveEnemies
            Target.Enemy -> listOfNotNull(aliveEnemies.getOrNull(targetIndex))
        }
        if (card.target == Target.Enemy && targets.isEmpty()) return false

        energy -= card.cost
        hand.removeAt(index)
        log.add("打出「${card.name}」")
        applyEffects(card, targets)
        if (card.exhaust) exhaustPile.add(card) else discardPile.add(card)
        checkEnd()
        return true
    }

    fun endTurn() {
        if (phase != CombatPhase.PlayerTurn) return
        discardPile.addAll(hand)
        hand.clear()
        tickDurations(statuses)

        phase = CombatPhase.EnemyTurn
        runEnemyTurn()
        if (hp <= 0) {
            phase = CombatPhase.Defeat
            return
        }
        if (aliveEnemies.isEmpty()) {
            phase = CombatPhase.Victory
            return
        }
        startPlayerTurn()
    }

    /** 敌人这回合会打出多少伤害（已计入力量/虚弱/易伤），给界面显示用。 */
    fun intentDamagePreview(enemy: EnemyState): Int {
        val intent = enemy.intent
        if (intent !is Intent.Attack) return 0
        return computeDamage(
            base = intent.damage,
            strength = enemy.statusOf(Status.Strength),
            weak = enemy.statusOf(Status.Weak) > 0,
            vulnerable = hasStatus(Status.Vulnerable),
        )
    }

    fun snapshot(): CombatSnapshot = CombatSnapshot(
        hp = hp,
        maxHp = maxHp,
        block = block,
        energy = energy,
        turn = turn,
        phase = phase,
        statuses = statuses.toMap(),
        drawPile = drawPile.toList(),
        hand = hand.toList(),
        discardPile = discardPile.toList(),
        exhaustPile = exhaustPile.toList(),
        enemies = enemies.map {
            EnemySnapshot(it.def, it.hp, it.maxHp, it.block, it.statuses.toMap(), it.moveIndex)
        },
    )

    // ---------------------------------------------------------------- 内部

    private fun startPlayerTurn() {
        turn++
        block = 0
        energy = MAX_ENERGY
        phase = CombatPhase.PlayerTurn
        draw(HAND_SIZE)
    }

    private fun runEnemyTurn() {
        for (enemy in enemies) {
            if (!enemy.alive) continue
            enemy.block = 0
            when (val intent = enemy.intent) {
                is Intent.Attack -> repeat(intent.times) {
                    if (hp > 0) hitPlayer(intent.damage, enemy)
                }
                is Intent.Defend -> enemy.block += intent.block + enemy.statusOf(Status.Dexterity)
                is Intent.Buff -> addStatus(enemy.statuses, intent.status, intent.stacks)
                is Intent.Debuff -> addStatus(statuses, intent.status, intent.stacks)
            }
            enemy.moveIndex++
            if (hp <= 0) return
        }
        enemies.filter { it.alive }.forEach { tickDurations(it.statuses) }
    }

    private fun applyEffects(card: Card, targets: List<EnemyState>) {
        for (effect in card.effects) {
            when (effect) {
                is Effect.Damage -> resolveDamage(effect, card.target, targets)
                is Effect.Block -> gainBlock(effect.amount)
                is Effect.Draw -> draw(effect.count)
                is Effect.Energy -> energy += effect.amount
                is Effect.Apply -> resolveApply(effect, card.target, targets)
                is Effect.Heal -> hp = min(maxHp, hp + effect.amount)
                is Effect.SelfDamage -> hp = max(0, hp - effect.amount)
            }
        }
    }

    private fun resolveDamage(effect: Effect.Damage, cardTarget: Target, targets: List<EnemyState>) {
        val hit = when (effect.target) {
            Target.Self -> emptyList()
            Target.AllEnemies -> aliveEnemies
            Target.Enemy -> if (cardTarget == Target.AllEnemies) aliveEnemies else targets
        }
        for (enemy in hit) {
            repeat(effect.times) {
                if (enemy.alive) hitEnemy(enemy, effect.amount)
            }
        }
    }

    private fun resolveApply(effect: Effect.Apply, cardTarget: Target, targets: List<EnemyState>) {
        when (effect.target) {
            Target.Self -> addStatus(statuses, effect.status, effect.stacks)
            Target.AllEnemies -> aliveEnemies.forEach { addStatus(it.statuses, effect.status, effect.stacks) }
            Target.Enemy -> {
                val hit = if (cardTarget == Target.AllEnemies) aliveEnemies else targets
                hit.forEach { addStatus(it.statuses, effect.status, effect.stacks) }
            }
        }
    }

    private fun hitEnemy(enemy: EnemyState, base: Int) {
        val damage = computeDamage(
            base = base,
            strength = statusOf(Status.Strength),
            weak = hasStatus(Status.Weak),
            vulnerable = enemy.statusOf(Status.Vulnerable) > 0,
        )
        val absorbed = min(enemy.block, damage)
        enemy.block -= absorbed
        enemy.hp = max(0, enemy.hp - (damage - absorbed))
    }

    private fun hitPlayer(base: Int, enemy: EnemyState) {
        val damage = computeDamage(
            base = base,
            strength = enemy.statusOf(Status.Strength),
            weak = enemy.statusOf(Status.Weak) > 0,
            vulnerable = hasStatus(Status.Vulnerable),
        )
        val absorbed = min(block, damage)
        block -= absorbed
        hp = max(0, hp - (damage - absorbed))
    }

    private fun gainBlock(amount: Int) {
        var value = amount + statusOf(Status.Dexterity)
        if (hasStatus(Status.Frail)) value = (value * 0.75f).toInt()
        block += max(0, value)
    }

    private fun draw(count: Int) {
        repeat(count) {
            if (drawPile.isEmpty()) reshuffleDiscardIntoDraw()
            if (drawPile.isEmpty()) return
            hand.add(drawPile.removeFirst())
        }
    }

    private fun reshuffleDiscardIntoDraw() {
        if (discardPile.isEmpty()) return
        drawPile.addAll(discardPile)
        discardPile.clear()
        drawPile.shuffle(rng)
        log.add("弃牌堆洗回抽牌堆")
    }

    private fun addStatus(target: MutableMap<Status, Int>, status: Status, stacks: Int) {
        if (stacks == 0) return
        val next = (target[status] ?: 0) + stacks
        if (next <= 0) target.remove(status) else target[status] = next
    }

    private fun tickDurations(target: MutableMap<Status, Int>) {
        for (status in DURATION_STATUSES) {
            val current = target[status] ?: continue
            if (current <= 1) target.remove(status) else target[status] = current - 1
        }
    }

    private fun checkEnd() {
        if (hp <= 0) {
            phase = CombatPhase.Defeat
            return
        }
        if (aliveEnemies.isEmpty()) phase = CombatPhase.Victory
    }

    private fun computeDamage(base: Int, strength: Int, weak: Boolean, vulnerable: Boolean): Int {
        var value = max(0, base + strength)
        if (weak) value = (value * 0.75f).toInt()
        if (vulnerable) value = (value * 1.5f).toInt()
        return value
    }

    companion object {
        const val MAX_ENERGY = 3
        const val HAND_SIZE = 5

        private val DURATION_STATUSES = listOf(Status.Vulnerable, Status.Weak, Status.Frail)

        /** 从存档快照重建一场战斗（不重新抽牌）。 */
        fun restore(snapshot: CombatSnapshot, rng: Random): CombatEngine {
            val enemies = snapshot.enemies.map { save ->
                EnemyState(save.def, save.hp).apply {
                    maxHp = save.maxHp
                    block = save.block
                    statuses.putAll(save.statuses)
                    moveIndex = save.moveIndex
                }
            }
            val engine = CombatEngine(snapshot.maxHp, enemies, rng)
            engine.hp = snapshot.hp
            engine.block = snapshot.block
            engine.energy = snapshot.energy
            engine.turn = snapshot.turn
            engine.phase = snapshot.phase
            engine.statuses.putAll(snapshot.statuses)
            engine.drawPile.addAll(snapshot.drawPile)
            engine.hand.addAll(snapshot.hand)
            engine.discardPile.addAll(snapshot.discardPile)
            engine.exhaustPile.addAll(snapshot.exhaustPile)
            return engine
        }
    }
}
