package dev.duefor.cornered.game

/** 战斗中的状态/buff。Strength、Dexterity 永久累积，其余每次轮到其主人回合结束时 -1。 */
enum class Status(val label: String, val icon: String) {
    Strength("力量", "剋"),
    Dexterity("敏捷", "巧"),
    Vulnerable("易伤", "伤"),
    Weak("虚弱", "弱"),
    Frail("脆弱", "脆"),
    ;

    companion object {
        /** JSON 里用小写名字，例如 "vulnerable" */
        fun fromJson(name: String): Status? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

enum class Target { Enemy, AllEnemies, Self }

/** 卡牌/触发器效果的原子。所有数值都在这里，便于以后替换成数据驱动。 */
sealed interface Effect {
    data class Damage(
        val amount: Int,
        val times: Int = 1,
        val target: Target = Target.Enemy,
    ) : Effect

    data class Block(val amount: Int) : Effect

    data class Draw(val count: Int) : Effect

    data class Energy(val amount: Int) : Effect

    data class Apply(
        val status: Status,
        val stacks: Int,
        val target: Target = Target.Enemy,
    ) : Effect

    data class Heal(val amount: Int) : Effect

    /** 直接掉血，穿透格挡。 */
    data class SelfDamage(val amount: Int) : Effect
}

enum class CardType(val label: String) {
    Attack("攻击"),
    Skill("技能"),
    Power("能力"),
    ;

    companion object {
        fun fromJson(name: String): CardType? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

enum class Rarity(val label: String, val weight: Int) {
    Common("普通", 60),
    Uncommon("罕见", 30),
    Rare("稀有", 10),
    ;

    companion object {
        fun fromJson(name: String): Rarity? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/** 触发时机。能力卡与遗物共用这一套事件。 */
enum class TriggerEvent(val jsonName: String) {
    CombatStart("combatStart"),
    TurnStart("turnStart"),
    TurnEnd("turnEnd"),
    CardPlayed("cardPlayed"),
    DamageTaken("damageTaken"),
    EnemyKilled("enemyKilled"),
    ;

    companion object {
        fun fromJson(name: String): TriggerEvent? = entries.firstOrNull { it.jsonName.equals(name, ignoreCase = true) }
    }
}

/**
 * 一条「什么时候 → 做什么」。
 * [every] 表示每触发 N 次执行一次（例如「每打出 3 张攻击牌」）。
 * [cardType] 只有 [TriggerEvent.CardPlayed] 用得到，用于过滤打出的牌类型。
 */
data class TriggeredEffect(
    val on: TriggerEvent,
    val effects: List<Effect>,
    val every: Int = 1,
    val cardType: CardType? = null,
)

data class Card(
    val id: String,
    val name: String,
    val cost: Int,
    val type: CardType,
    val rarity: Rarity,
    val target: Target,
    val description: String,
    val effects: List<Effect> = emptyList(),
    val exhaust: Boolean = false,
    /** 能力牌的持续效果；普通牌留空 */
    val triggers: List<TriggeredEffect> = emptyList(),
    /** 屏障类能力：回合开始不清空格挡 */
    val keepBlock: Boolean = false,
)

/** 敌人本回合要做的事，界面上会提前亮出来。 */
sealed interface Intent {
    data class Attack(val damage: Int, val times: Int = 1) : Intent
    data class Defend(val block: Int) : Intent
    data class Buff(val status: Status, val stacks: Int) : Intent
    data class Debuff(val status: Status, val stacks: Int) : Intent
}

enum class EnemyKind(val label: String) {
    Normal("普通"),
    Elite("精英"),
    Boss("首领"),
    ;

    companion object {
        fun fromJson(name: String): EnemyKind? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

data class EnemyDef(
    val id: String,
    val name: String,
    val kind: EnemyKind,
    val minHp: Int,
    val maxHp: Int,
    val pattern: List<Intent>,
)

data class Relic(
    val id: String,
    val name: String,
    val text: String,
    val triggers: List<TriggeredEffect> = emptyList(),
)

/** 一幕：楼层数、遭遇池、精英与首领、节点权重。 */
data class Act(
    val id: String,
    val name: String,
    val floors: Int,
    val encounters: List<List<String>>,
    val elites: List<List<String>>,
    val bosses: List<List<String>>,
    val weights: Map<NodeType, Int>,
)

/**
 * 可选角色。加新角色只需要在 characters.json 里加一条数据。
 */
data class Character(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val maxHp: Int,
    val maxEnergy: Int,
    val handSize: Int,
    val starterDeck: List<Card>,
    val rewardPool: List<Card>,
)
