package dev.duefor.cornered.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.duefor.cornered.game.CombatEngine
import dev.duefor.cornered.game.EnemyState
import dev.duefor.cornered.game.Intent
import dev.duefor.cornered.game.Status
import dev.duefor.cornered.game.Target
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.AccentRed
import dev.duefor.cornered.ui.theme.ArenaSurface
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

/**
 * 引擎状态是普通 var，不是 Compose State，所以只把「值」传给子组件。
 *
 * 布局：敌人区 -> 出牌预览（选中时）-> 能力/遗物 -> 手牌 -> 底部状态条
 */
@Composable
fun CombatScreen(
    engine: CombatEngine,
    revision: Int,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    var local by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(revision, local) {
        if (engine.isOver) onFinished()
    }

    val aliveEnemies = engine.aliveEnemies
    val selectedCard = selectedIndex?.let { engine.hand.getOrNull(it) }
    // 单体卡需要指定目标；自身 / 全体牌点「确认」即可
    val needsTarget = selectedCard != null && selectedCard.target == Target.Enemy

    fun play(index: Int, target: Int) {
        engine.playCard(index, target)
        selectedIndex = null
        local++
    }

    fun tapCard(index: Int) {
        if (!engine.canPlay(index)) return
        selectedIndex = if (selectedIndex == index) null else index
        local++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("第 ${engine.turn} 回合", fontSize = 12.sp, color = InkMuted)
            Text(
                "抽 ${engine.drawPile.size} · 弃 ${engine.discardPile.size}",
                fontSize = 11.sp,
                color = InkMuted,
            )
            TextButton(onClick = onExit) { Text("退出", fontSize = 12.sp, color = InkMuted) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            engine.enemies.forEach { enemy ->
                val aliveIndex = aliveEnemies.indexOf(enemy)
                EnemyPanel(
                    name = enemy.def.name,
                    intentText = intentLabel(engine, enemy),
                    hp = enemy.hp,
                    maxHp = enemy.maxHp,
                    block = enemy.block,
                    statuses = statusList(enemy.statuses),
                    alive = enemy.alive,
                    highlight = needsTarget && enemy.alive,
                    onClick = {
                        val index = selectedIndex
                        if (index != null && enemy.alive) play(index, aliveIndex)
                    },
                )
            }
        }

        if (selectedCard != null) {
            CardPreview(
                name = selectedCard.name,
                cost = selectedCard.cost,
                description = selectedCard.description,
                accent = selectedCard.type.accentColor(),
                needsTarget = needsTarget,
                onConfirm = { selectedIndex?.let { play(it, 0) } },
                onCancel = {
                    selectedIndex = null
                    local++
                },
            )
        }

        AbilityRow(engine)

        HandRow(
            engine = engine,
            selectedIndex = selectedIndex,
            onCardClick = { tapCard(it) },
        )

        BottomStatusBar(
            hp = engine.hp,
            maxHp = engine.maxHp,
            block = engine.block,
            statuses = statusList(engine.statuses),
            energy = engine.energy,
            maxEnergy = engine.maxEnergy,
            onEndTurn = {
                engine.endTurn()
                selectedIndex = null
                local++
            },
        )
    }
}

@Composable
private fun CardPreview(
    name: String,
    cost: Int,
    description: String,
    accent: Color,
    needsTarget: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.5.dp, AccentCyan, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("$cost", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B0F14))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(description, fontSize = 11.sp, color = InkMuted)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (needsTarget) {
                Text("点敌人", fontSize = 11.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = Color(0xFF06231F),
                    ),
                ) {
                    Text("确认", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onCancel) {
                Text("取消", fontSize = 10.sp, color = InkMuted)
            }
        }
    }
}

@Composable
private fun AbilityRow(engine: CombatEngine) {
    if (engine.activePowers.isEmpty() && engine.relics.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        engine.activePowers.forEach { power ->
            Text(
                text = power.name,
                fontSize = 9.sp,
                color = Color(0xFFB07CE8),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2036))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
        engine.relics.forEach { relic ->
            Text(
                text = relic.name,
                fontSize = 9.sp,
                color = Color(0xFFE9B44C),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2E2618))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun HandRow(
    engine: CombatEngine,
    selectedIndex: Int?,
    onCardClick: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(top = 6.dp),
    ) {
        val cardWidth = 84.dp
        val cards = engine.hand.toList()
        val step = if (cards.size <= 1) 0.dp else minOf(
            cardWidth + 6.dp,
            (maxWidth - cardWidth) / (cards.size - 1),
        )
        val totalWidth = cardWidth + step * (cards.size - 1)
        val startX = ((maxWidth - totalWidth) / 2).coerceAtLeast(0.dp)

        cards.forEachIndexed { index, card ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .offset(x = startX + step * index, y = if (isSelected) 0.dp else 8.dp)
                    .zIndex(if (isSelected) 1f else index.toFloat()),
            ) {
                CardView(
                    card = card,
                    width = cardWidth,
                    playable = engine.canPlay(index),
                    selected = isSelected,
                    onClick = { onCardClick(index) },
                )
            }
        }
    }
}

@Composable
private fun BottomStatusBar(
    hp: Int,
    maxHp: Int,
    block: Int,
    statuses: List<Pair<Status, Int>>,
    energy: Int,
    maxEnergy: Int,
    onEndTurn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ArenaSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "生命 $hp/$maxHp",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                if (block > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("格挡 $block", fontSize = 11.sp, color = Color(0xFF7FC8F8))
                }
            }
            Spacer(Modifier.height(4.dp))
            HpBar(hp, maxHp, AccentCyan)
            if (statuses.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                StatusChips(statuses)
            }
        }

        Spacer(Modifier.width(10.dp))
        EnergyDisplay(energy = energy, max = maxEnergy)
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onEndTurn,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = Color(0xFF06231F),
            ),
        ) {
            Text("结束回合", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusList(map: Map<Status, Int>): List<Pair<Status, Int>> =
    map.entries.filter { it.value > 0 }.map { it.key to it.value }

private fun intentLabel(engine: CombatEngine, enemy: EnemyState): String =
    when (val intent = enemy.intent) {
        is Intent.Attack -> {
            val damage = engine.intentDamagePreview(enemy)
            if (intent.times > 1) "攻击 $damage×${intent.times}" else "攻击 $damage"
        }
        is Intent.Defend -> "防御 ${intent.block}"
        is Intent.Buff -> "${intent.status.label} +${intent.stacks}"
        is Intent.Debuff -> "施加${intent.status.label}"
    }

@Composable
private fun EnergyDisplay(energy: Int, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { index ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (index < energy) Color(0xFFE9B44C) else Color(0xFF243040)),
            )
        }
        Text(
            text = "$energy",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE9B44C),
        )
    }
}

@Composable
private fun EnemyPanel(
    name: String,
    intentText: String,
    hp: Int,
    maxHp: Int,
    block: Int,
    statuses: List<Pair<Status, Int>>,
    alive: Boolean,
    highlight: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) AccentRed.copy(alpha = 0.16f) else ArenaSurface)
            .border(
                width = if (highlight) 2.dp else 1.dp,
                color = if (highlight) AccentRed else Color(0xFF243040),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = alive, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = intentText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (alive) AccentRed else InkMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (alive) Color(0xFF231A20) else Color(0xFF15191E)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (alive) name.take(1) else "×",
                fontSize = 20.sp,
                color = if (alive) AccentRed else InkMuted,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 10.sp, color = Ink, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        HpBar(hp, maxHp, AccentRed)
        Text("$hp/$maxHp", fontSize = 9.sp, color = InkMuted)
        if (block > 0) {
            Spacer(Modifier.height(2.dp))
            Text("格挡 $block", fontSize = 9.sp, color = Color(0xFF7FC8F8))
        }
        if (statuses.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            StatusChips(statuses)
        }
    }
}

@Composable
private fun HpBar(current: Int, max: Int, color: Color) {
    val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF202A34)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(color),
        )
    }
}

@Composable
private fun StatusChips(statuses: List<Pair<Status, Int>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        statuses.forEach { (status, stacks) ->
            Text(
                text = "${status.icon}$stacks",
                fontSize = 9.sp,
                color = Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A3946))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}
