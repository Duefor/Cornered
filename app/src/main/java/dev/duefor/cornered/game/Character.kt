package dev.duefor.cornered.game

/**
 * 可选角色。加新角色只需要在这里加一条数据：
 * 起始生命、每回合能量、每回合抽牌数、起始牌组、战后奖励卡池。
 * 其余系统（存档、地图、战斗）都不需要改动。
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

object Characters {

    val ironclad = Character(
        id = "ironclad",
        name = "铁卫",
        subtitle = "稳健的重击者",
        description = "生命厚实，靠重击叠易伤、用格挡换回合。上手最简单。",
        maxHp = 80,
        maxEnergy = 3,
        handSize = 5,
        starterDeck = CardPool.ironcladDeck,
        rewardPool = CardPool.ironcladRewards,
    )

    val shadow = Character(
        id = "shadow",
        name = "影刃",
        subtitle = "快攻的连击者",
        description = "血薄，但每回合多抽一张牌；匕首不耗能量，靠出牌数量堆伤害。",
        maxHp = 68,
        maxEnergy = 3,
        handSize = 6,
        starterDeck = CardPool.shadowDeck,
        rewardPool = CardPool.shadowRewards,
    )

    val all: List<Character> = listOf(ironclad, shadow)

    val default: Character get() = ironclad

    fun byId(id: String): Character? = all.firstOrNull { it.id == id }
}
