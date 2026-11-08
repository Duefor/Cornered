package dev.duefor.cornered.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.Card
import dev.duefor.cornered.game.Relic
import dev.duefor.cornered.game.RunState
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

@Composable
fun RewardScreen(
    run: RunState,
    revision: Int,
    onTake: (Card?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("战斗胜利", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
        Spacer(Modifier.height(8.dp))
        Text("获得 ${run.lastGoldGain} 金币", fontSize = 13.sp, color = Color(0xFFE9B44C))

        run.pendingRelic?.let { relic ->
            Spacer(Modifier.height(16.dp))
            RelicBadge(relic)
        }

        Spacer(Modifier.height(22.dp))
        Text("选择一张牌加入牌组", fontSize = 12.sp, color = InkMuted)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            run.rewardCards.forEach { card ->
                CardView(
                    card = card,
                    width = 88.dp,
                    onClick = { onTake(card) },
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        TextButton(onClick = { onTake(null) }) {
            Text("跳过，不加牌", color = InkMuted, fontSize = 13.sp)
        }
    }
}

/** 幕间结算：金币 + 三选一 + 必得遗物 */
@Composable
fun ActClearScreen(
    run: RunState,
    onTake: (Card?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("幕间", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AccentCyan)
        Spacer(Modifier.height(6.dp))
        Text("${run.act.name} 已通过", fontSize = 12.sp, color = InkMuted)
        Spacer(Modifier.height(10.dp))
        Text("获得 ${run.lastGoldGain} 金币", fontSize = 13.sp, color = Color(0xFFE9B44C))

        run.pendingRelic?.let { relic ->
            Spacer(Modifier.height(16.dp))
            RelicBadge(relic)
        }

        Spacer(Modifier.height(22.dp))
        Text("选择一张牌进入下一幕", fontSize = 12.sp, color = InkMuted)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            run.rewardCards.forEach { card ->
                CardView(card = card, width = 88.dp, onClick = { onTake(card) })
            }
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { onTake(null) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = Color(0xFF06231F),
            ),
        ) {
            Text("进入下一幕", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RelicBadge(relic: Relic) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2E2618))
            .border(1.dp, Color(0xFFE9B44C), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("获得遗物", fontSize = 10.sp, color = Color(0xFFE9B44C))
        Spacer(Modifier.height(4.dp))
        Text(relic.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(relic.text, fontSize = 10.sp, color = InkMuted, textAlign = TextAlign.Center)
    }
}

@Composable
fun TreasureScreen(
    run: RunState,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("宝箱", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9B44C))
            Spacer(Modifier.height(10.dp))
            Text(
                text = "获得 ${run.lastGoldGain} 金币",
                fontSize = 15.sp,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text("当前金币 ${run.gold}", fontSize = 11.sp, color = InkMuted)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color(0xFF06231F),
                ),
            ) {
                Text("继续前进", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RestScreen(
    run: RunState,
    onRest: () -> Unit,
    onTrain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("营地", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6FCF97))
        Spacer(Modifier.height(6.dp))
        Text("生命 ${run.hp}/${run.maxHp}", fontSize = 12.sp, color = InkMuted)
        Spacer(Modifier.height(30.dp))

        RestOption(
            title = "休息",
            detail = if (run.hp < run.maxHp) "回复 30% 最大生命" else "生命已满",
            enabled = run.hp < run.maxHp,
            onClick = onRest,
        )
        Spacer(Modifier.height(14.dp))
        RestOption(
            title = "锻炼",
            detail = "最大生命 +5，并回复等量生命",
            onClick = onTrain,
        )
    }
}

@Composable
private fun RestOption(title: String, detail: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF16212B),
            contentColor = Ink,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 11.sp, color = InkMuted.copy(alpha = if (enabled) 1f else 0.5f))
        }
    }
}
