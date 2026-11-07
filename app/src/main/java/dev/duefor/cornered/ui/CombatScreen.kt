package dev.duefor.cornered.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.Card
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

@Composable
fun CombatScreen(
    engine: CombatEngine,
    revision: Int,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    var local by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // revision 来自外层（进入战斗、重新开始），local 来自本屏操作；
    // 两者任一变化都重新检查一次胜负。
    LaunchedEffect(revision, local) {
        if (engine.isOver) onFinished()
    }

    val aliveEnemies = engine.aliveEnemies
    val selectedCard = selectedIndex?.let { engine.hand.getOrNull(it) }
    val awaitingTarget = selectedCard != null &&
        selectedCard.target == Target.Enemy &&
        aliveEnemies.size > 1

    fun play(index: Int, target: Int) {
        engine.playCard(index, target)
        selectedIndex = null
        local++
    }

    fun tapCard(index: Int) {
        val card = engine.hand.getOrNull(index) ?: return
        if (!engine.canPlay(index)) return
        if (card.target == Target.Enemy && aliveEnemies.size > 1) {
            selectedIndex = if (selectedIndex == index) null else index
        } else {
            play(index, 0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("第 ${engine.turn} 回合", fontSize = 12.sp, color = InkMuted)
            Text(
                "抽牌 ${engine.drawPile.size} · 弃牌 ${engine.discardPile.size}",
                fontSize = 11.sp,
                color = InkMuted,
            )
            TextButton(onClick = onExit) { Text("退出", fontSize = 12.sp, color = InkMuted) }
        }

        PlayerBar(engine)
        Spacer(Modifier.height(10.dp))

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
                    engine = engine,
                    enemy = enemy,
                    highlight = awaitingTarget && enemy.alive,
                    onClick = {
                        val index = selectedIndex
                        if (index != null && enemy.alive) play(index, aliveIndex)
                    },
                )
            }
        }

        if (awaitingTarget) {
            Text(
                text = "选择目标",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = AccentCyan,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            engine.hand.toList().forEachIndexed { index, card ->
                CardView(
                    card = card,
                    playable = engine.canPlay(index),
                    selected = selectedIndex == index,
                    onClick = { tapCard(index) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EnergyDisplay(engine)
            Button(
                onClick = {
                    engine.endTurn()
                    selectedIndex = null
                    local++
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color(0xFF06231F),
                ),
            ) {
                Text("结束回合", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerBar(engine: CombatEngine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ArenaSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "生命 ${engine.hp}/${engine.maxHp}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            if (engine.block > 0) {
                Text("格挡 ${engine.block}", fontSize = 12.sp, color = Color(0xFF7FC8F8))
            }
        }
        Spacer(Modifier.height(5.dp))
        HpBar(engine.hp, engine.maxHp, AccentCyan)
        if (engine.statuses.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            StatusChips(engine.statuses)
        }
    }
}

@Composable
private fun EnergyDisplay(engine: CombatEngine) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(CombatEngine.MAX_ENERGY) { index ->
            Box(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(if (index < engine.energy) Color(0xFFE9B44C) else Color(0xFF243040)),
            )
        }
        Text(
            text = "${engine.energy}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE9B44C),
        )
    }
}

@Composable
private fun EnemyPanel(
    engine: CombatEngine,
    enemy: EnemyState,
    highlight: Boolean,
    onClick: () -> Unit,
) {
    val intentText = when (val intent = enemy.intent) {
        is Intent.Attack -> {
            val damage = engine.intentDamagePreview(enemy)
            if (intent.times > 1) "攻击 $damage×${intent.times}" else "攻击 $damage"
        }
        is Intent.Defend -> "防御 ${intent.block}"
        is Intent.Buff -> "${intent.status.label} +${intent.stacks}"
        is Intent.Debuff -> "施加${intent.status.label}"
    }

    Column(
        modifier = Modifier
            .width(114.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) AccentRed.copy(alpha = 0.16f) else ArenaSurface)
            .border(
                width = 1.dp,
                color = if (highlight) AccentRed else Color(0xFF243040),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enemy.alive, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = intentText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (enemy.alive) AccentRed else InkMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (enemy.alive) Color(0xFF231A20) else Color(0xFF15191E)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (enemy.alive) enemy.def.name.take(1) else "×",
                fontSize = 20.sp,
                color = if (enemy.alive) AccentRed else InkMuted,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(enemy.def.name, fontSize = 10.sp, color = Ink, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        HpBar(enemy.hp, enemy.maxHp, AccentRed)
        Text("${enemy.hp}/${enemy.maxHp}", fontSize = 9.sp, color = InkMuted)
        if (enemy.block > 0) {
            Spacer(Modifier.height(2.dp))
            Text("格挡 ${enemy.block}", fontSize = 9.sp, color = Color(0xFF7FC8F8))
        }
        if (enemy.statuses.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            StatusChips(enemy.statuses)
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
private fun StatusChips(statuses: Map<Status, Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        statuses.forEach { (status, stacks) ->
            if (stacks > 0) {
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
}

@Suppress("unused")
private fun combatHint(card: Card): String = when (card.target) {
    Target.Enemy -> "点敌人选择目标"
    Target.AllEnemies -> "影响全体敌人"
    Target.Self -> "影响自己"
}
