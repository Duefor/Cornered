package dev.duefor.cornered.game

import kotlin.random.Random

/**
 * 敌人模板。pattern 是一个循环的出招表，[EnemyState.moveIndex] 每次行动后 +1。
 * 内容很少，但结构是照着「多个敌人、不同出招表」设计的。
 */
object EnemyPool {

    val worm = EnemyDef(
        id = "worm",
        name = "腐蚀蠕虫",
        kind = EnemyKind.Normal,
        minHp = 12,
        maxHp = 16,
        pattern = listOf(
            Intent.Attack(7),
            Intent.Defend(6),
            Intent.Attack(9),
        ),
    )

    val guard = EnemyDef(
        id = "guard",
        name = "铁面守卫",
        kind = EnemyKind.Normal,
        minHp = 18,
        maxHp = 22,
        pattern = listOf(
            Intent.Attack(5, times = 2),
            Intent.Buff(Status.Strength, 2),
            Intent.Attack(9),
        ),
    )

    val bandit = EnemyDef(
        id = "bandit",
        name = "盗匪",
        kind = EnemyKind.Normal,
        minHp = 14,
        maxHp = 18,
        pattern = listOf(
            Intent.Attack(8),
            Intent.Debuff(Status.Weak, 1),
            Intent.Attack(6),
        ),
    )

    val colossus = EnemyDef(
        id = "colossus",
        name = "岩石巨像",
        kind = EnemyKind.Elite,
        minHp = 44,
        maxHp = 48,
        pattern = listOf(
            Intent.Attack(12),
            Intent.Defend(10),
            Intent.Buff(Status.Strength, 3),
            Intent.Attack(6, times = 2),
        ),
    )

    val eye = EnemyDef(
        id = "eye",
        name = "深渊之眼",
        kind = EnemyKind.Boss,
        minHp = 85,
        maxHp = 95,
        pattern = listOf(
            Intent.Attack(14),
            Intent.Debuff(Status.Vulnerable, 2),
            Intent.Attack(8, times = 2),
            Intent.Defend(12),
            Intent.Buff(Status.Strength, 3),
        ),
    )

    /** 按 id 找敌人：存档/读档用。 */
    fun byId(id: String): EnemyDef? = allById[id]

    private val allById: Map<String, EnemyDef> by lazy {
        listOf(worm, guard, bandit, colossus, eye).associateBy { it.id }
    }
    private val early = listOf(worm, bandit)
    private val normal = listOf(worm, guard, bandit)

    fun forNode(type: NodeType, floor: Int, rng: Random): List<EnemyDef> = when (type) {
        NodeType.Boss -> listOf(eye)
        NodeType.Elite -> listOf(colossus)
        NodeType.Combat, NodeType.Rest, NodeType.Treasure -> {
            val pool = if (floor < 3) early else normal
            val count = when {
                floor < 2 -> 1
                floor < 5 -> if (rng.nextBoolean()) 1 else 2
                else -> 2
            }
            List(count) { pool.random(rng) }
        }
    }
}
