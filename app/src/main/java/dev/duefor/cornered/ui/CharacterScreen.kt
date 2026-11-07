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
import androidx.compose.runtime.getValue
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
import dev.duefor.cornered.game.Character
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

fun characterColor(id: String): Color = when (id) {
    "shadow" -> Color(0xFF9E7BE8)
    else -> AccentCyan
}

@Composable
fun CharacterScreen(
    characters: List<Character>,
    onStart: (Character) -> Unit,
    onBack: () -> Unit,
) {
    var selectedId by remember { mutableStateOf(characters.firstOrNull()?.id) }
    val selected = characters.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("选择角色", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${characters.size} 位可选", fontSize = 11.sp, color = InkMuted)
            }
            TextButton(onClick = onBack) { Text("返回", fontSize = 12.sp, color = InkMuted) }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            characters.forEach { character ->
                CharacterCard(
                    character = character,
                    selected = character.id == selectedId,
                    onClick = { selectedId = character.id },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = selected?.description ?: "",
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { selected?.let(onStart) },
            enabled = selected != null,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = selected?.let { characterColor(it.id) } ?: AccentCyan,
                contentColor = Color(0xFF06231F),
            ),
        ) {
            Text(
                text = selected?.let { "以「${it.name}」开始" } ?: "选择一个角色",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun CharacterCard(
    character: Character,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = characterColor(character.id)
    val shape = RoundedCornerShape(16.dp)
    val deckCounts = remember(character.id) {
        character.starterDeck.groupingBy { it.name }.eachCount().entries.toList()
    }

    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else Color(0xFF111A22))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color(0xFF243040),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = character.name.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0F14),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(character.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(character.subtitle, fontSize = 9.sp, color = accent)
            }
        }

        Spacer(Modifier.height(12.dp))

        StatLine("生命", "${character.maxHp}")
        StatLine("每回合能量", "${character.maxEnergy}")
        StatLine("每回合抽牌", "${character.handSize}")

        Spacer(Modifier.height(12.dp))
        Text("起始牌组 · ${character.starterDeck.size} 张", fontSize = 10.sp, color = InkMuted)

        Spacer(Modifier.height(6.dp))
        deckCounts.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(entry.key, fontSize = 11.sp, color = Ink)
                Text("×${entry.value}", fontSize = 11.sp, color = InkMuted)
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 10.sp, color = InkMuted)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}
