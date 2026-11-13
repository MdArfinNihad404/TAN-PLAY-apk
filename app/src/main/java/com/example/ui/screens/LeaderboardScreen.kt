package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BracketEngine
import com.example.data.PlayerStat
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    playerStats: List<PlayerStat>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    var selectedGame by remember { mutableStateOf("All") }
    val games = listOf("All") + BracketEngine.SUPPORTED_GAMES.filter { it != "Custom Game" }

    val filteredStats = remember(playerStats, selectedGame) {
        playerStats.filter { s ->
            selectedGame == "All" || s.primaryGame.equals(selectedGame, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "COMPETITIVE STANDINGS",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Player Rankings",
                    color = HighClarityWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ranked by competitive rating & confirmed tournament victories.",
                    color = MutedSlate,
                    fontSize = 12.sp
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(games) { game ->
                    val selected = selectedGame == game
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElectricLime else ObsidianContainerLow)
                            .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                            .clickable { selectedGame = game }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(game, color = if (selected) OnElectricLime else SlateSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filteredStats.isEmpty()) {
            item {
                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth().testTag("leaderboard_empty_state")
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(38.dp))
                        Text("No Ranked Players Yet", color = HighClarityWhite, fontWeight = FontWeight.Bold)
                        Text("Complete tournament matches to climb the seasonal rankings!", color = MutedSlate, fontSize = 12.sp)
                    }
                }
            }
        } else {
            itemsIndexed(filteredStats, key = { _, stat -> stat.userId }) { idx, stat ->
                val isMe = stat.userId == currentUserId
                val rank = idx + 1
                Surface(
                    color = if (isMe) ElectricLime.copy(alpha = 0.12f) else ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMe) ElectricLime else BorderDivider),
                    modifier = Modifier.fillMaxWidth().testTag("leaderboard_row_$rank")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (rank <= 3) ElectricLime else ObsidianContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    color = if (rank <= 3) OnElectricLime else HighClarityWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text(
                                    text = stat.gamerTag + if (isMe) " (YOU)" else "",
                                    color = if (isMe) ElectricLime else HighClarityWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${stat.primaryGame} • ${stat.wins}W / ${stat.matchesPlayed}M",
                                    color = MutedSlate,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stat.rating}",
                                color = ElectricLime,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("PTS", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
