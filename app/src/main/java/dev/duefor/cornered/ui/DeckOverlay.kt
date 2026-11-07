package dev.duefor.cornered.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.Card
import dev.duefor.cornered.game.CardType
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.ArenaSurface
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

/** 牌库查看器：按类型分组，组内按费用排序。 */
@Composable
fun DeckOverlay(cards: List<Card>, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("牌库", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("${cards.size} 张", fontSize = 11.sp, color = InkMuted)
                }
                TextButton(onClick = onClose) {
                    Text("关闭", color = AccentCyan, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                CardType.entries.forEach { type ->
                    val group = cards
                        .filter { it.type == type }
                        .sortedWith(compareBy({ it.cost }, { it.name }))
                    if (group.isNotEmpty()) {
                        Text(
                            text = "${type.label} · ${group.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = type.accentColor(),
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                        group.forEach { card -> DeckRow(card) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(card: Card) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ArenaSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(card.type.accentColor()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${card.cost}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B0F14),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(card.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(card.description, fontSize = 10.sp, color = InkMuted)
        }
        if (card.exhaust) {
            Text("消耗", fontSize = 9.sp, color = InkMuted)
        }
    }
}
