package dev.duefor.cornered.game

import kotlin.random.Random

/**
 * 卡池。所有卡都是纯数据，加新卡只需要在 [rewards] 里加一条。
 * 卡面描述写死成文字，先保证可读；数值改动时记得同步 description。
 */
object CardPool {

    // ---------------------------------------------------------------- 起始牌组
    val strike = Card(
        id = "strike",
        name = "打击",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.Enemy,
        description = "6 点伤害",
        effects = listOf(Effect.Damage(6)),
    )

    val defend = Card(
        id = "defend",
        name = "防御",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Common,
        target = Target.Self,
        description = "5 点格挡",
        effects = listOf(Effect.Block(5)),
    )

    val bash = Card(
        id = "bash",
        name = "重击",
        cost = 2,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.Enemy,
        description = "8 伤害 + 2 易伤",
        effects = listOf(Effect.Damage(8), Effect.Apply(Status.Vulnerable, 2)),
    )

    // ---------------------------------------------------------------- 奖励池
    val cleave = Card(
        id = "cleave",
        name = "横扫",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.AllEnemies,
        description = "全体 8 伤害",
        effects = listOf(Effect.Damage(8, target = Target.AllEnemies)),
    )

    val twinStrike = Card(
        id = "twin_strike",
        name = "连击",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.Enemy,
        description = "5 伤害 ×2",
        effects = listOf(Effect.Damage(5, times = 2)),
    )

    val ironWall = Card(
        id = "iron_wall",
        name = "铁壁",
        cost = 2,
        type = CardType.Skill,
        rarity = Rarity.Common,
        target = Target.Self,
        description = "12 点格挡",
        effects = listOf(Effect.Block(12)),
    )

    val quickStep = Card(
        id = "quick_step",
        name = "疾行",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Common,
        target = Target.Self,
        description = "抽 2 张",
        effects = listOf(Effect.Draw(2)),
    )

    val guard = Card(
        id = "guard",
        name = "护心",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Common,
        target = Target.Self,
        description = "8 格挡 + 抽 1",
        effects = listOf(Effect.Block(8), Effect.Draw(1)),
    )

    val rend = Card(
        id = "rend",
        name = "破甲",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Uncommon,
        target = Target.Enemy,
        description = "5 伤害 + 3 易伤",
        effects = listOf(Effect.Damage(5), Effect.Apply(Status.Vulnerable, 3)),
    )

    val intimidate = Card(
        id = "intimidate",
        name = "恐吓",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Uncommon,
        target = Target.AllEnemies,
        description = "全体 2 虚弱",
        effects = listOf(Effect.Apply(Status.Weak, 2, Target.AllEnemies)),
    )

    val flex = Card(
        id = "flex",
        name = "蓄力",
        cost = 0,
        type = CardType.Skill,
        rarity = Rarity.Uncommon,
        target = Target.Self,
        description = "+2 力量",
        effects = listOf(Effect.Apply(Status.Strength, 2, Target.Self)),
    )

    val heavyBlow = Card(
        id = "heavy_blow",
        name = "重锤",
        cost = 2,
        type = CardType.Attack,
        rarity = Rarity.Uncommon,
        target = Target.Enemy,
        description = "14 点伤害",
        effects = listOf(Effect.Damage(14)),
    )

    val lifesteal = Card(
        id = "lifesteal",
        name = "吸血打击",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Uncommon,
        target = Target.Enemy,
        description = "6 伤害 + 回 3",
        effects = listOf(Effect.Damage(6), Effect.Heal(3)),
    )

    val battleTrance = Card(
        id = "battle_trance",
        name = "战意",
        cost = 0,
        type = CardType.Skill,
        rarity = Rarity.Uncommon,
        target = Target.Self,
        description = "抽 3 张 · 消耗",
        effects = listOf(Effect.Draw(3)),
        exhaust = true,
    )

    val recharge = Card(
        id = "recharge",
        name = "蓄能",
        cost = 0,
        type = CardType.Skill,
        rarity = Rarity.Uncommon,
        target = Target.Self,
        description = "+2 能量 · 消耗",
        effects = listOf(Effect.Energy(2)),
        exhaust = true,
    )

    val berserk = Card(
        id = "berserk",
        name = "狂暴",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Rare,
        target = Target.Self,
        description = "+3 力量 · -4 生命",
        effects = listOf(Effect.Apply(Status.Strength, 3, Target.Self), Effect.SelfDamage(4)),
    )

    val whirlwind = Card(
        id = "whirlwind",
        name = "漩涡",
        cost = 2,
        type = CardType.Attack,
        rarity = Rarity.Rare,
        target = Target.AllEnemies,
        description = "全体 5 伤害 ×3",
        effects = listOf(Effect.Damage(5, times = 3, target = Target.AllEnemies)),
    )

    val impervious = Card(
        id = "impervious",
        name = "不灭壁垒",
        cost = 2,
        type = CardType.Skill,
        rarity = Rarity.Rare,
        target = Target.Self,
        description = "24 格挡 · 消耗",
        effects = listOf(Effect.Block(24)),
        exhaust = true,
    )

    // ---------------------------------------------------------------- 影刃起始牌
    val dagger = Card(
        id = "dagger",
        name = "匕首",
        cost = 0,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.Enemy,
        description = "3 点伤害",
        effects = listOf(Effect.Damage(3)),
    )

    val dodge = Card(
        id = "dodge",
        name = "闪避",
        cost = 1,
        type = CardType.Skill,
        rarity = Rarity.Common,
        target = Target.Self,
        description = "4 点格挡",
        effects = listOf(Effect.Block(4)),
    )

    val shadowStrike = Card(
        id = "shadow_strike",
        name = "影袭",
        cost = 1,
        type = CardType.Attack,
        rarity = Rarity.Common,
        target = Target.Enemy,
        description = "4 伤害 + 抽 1",
        effects = listOf(Effect.Damage(4), Effect.Draw(1)),
    )

    // ---------------------------------------------------------------- 角色卡池
    val ironcladDeck: List<Card> =
        List(5) { strike } + List(4) { defend } + listOf(bash)

    val shadowDeck: List<Card> =
        List(4) { dagger } + List(4) { dodge } + List(2) { shadowStrike }

    val ironcladRewards: List<Card> = listOf(
        cleave, twinStrike, ironWall, quickStep, guard,
        rend, intimidate, flex, heavyBlow, lifesteal, battleTrance, recharge,
        berserk, whirlwind, impervious,
    )

    val shadowRewards: List<Card> = listOf(
        dagger, shadowStrike, twinStrike, cleave, quickStep, guard,
        rend, intimidate, flex, battleTrance, recharge, lifesteal,
        berserk, whirlwind, impervious,
    )

    private val allCards: List<Card> =
        (ironcladDeck + ironcladRewards + shadowDeck + shadowRewards).distinctBy { it.id }

    /** 按 id 找卡：存档/读档用。 */
    fun byId(id: String): Card? = allById[id]

    private val allById: Map<String, Card> by lazy { allCards.associateBy { it.id } }

    /** 按稀有度权重从指定卡池抽 [count] 张不重复的卡作为战后奖励。 */
    fun rewardChoices(rng: Random, source: List<Card>, count: Int = 3): List<Card> {
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
