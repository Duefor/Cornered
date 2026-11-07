package dev.duefor.cornered.game

/** 战斗中的状态/buff。Strength、Dexterity 永久累积，其余每次轮到其主人回合结束时 -1。 */
enum class Status(val label: String, val icon: String, val description: String) {
    Strength("力量", "剋", "每点力量让攻击伤害 +1"),
    Dexterity("敏捷", "巧", "每点敏捷让获得格挡 +1"),
    Vulnerable("易伤", "伤", "受到的攻击伤害 +50%，回合结束 -1"),
    Weak("虚弱", "弱", "造成的攻击伤害 -25%，回合结束 -1"),
    Frail("脆弱", "脆", "获得的格挡 -25%，回合结束 -1"),
}

/** 效果的作用对象。 */
enum class Target { Enemy, AllEnemies, Self }

/** 卡牌/敌人行动的效果原子，所有数值都在这里，便于以后替换成数据驱动。 */
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
}

enum class Rarity(val label: String, val weight: Int) {
    Common("普通", 60),
    Uncommon("罕见", 30),
    Rare("稀有", 10),
}

data class Card(
    val id: String,
    val name: String,
    val cost: Int,
    val type: CardType,
    val rarity: Rarity,
    val target: Target,
    val description: String,
    val effects: List<Effect>,
    val exhaust: Boolean = false,
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
}

data class EnemyDef(
    val id: String,
    val name: String,
    val kind: EnemyKind,
    val minHp: Int,
    val maxHp: Int,
    val pattern: List<Intent>,
)
