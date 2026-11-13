package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
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
import com.example.data.FirestoreTournament
import com.example.ui.components.TanScreen
import com.example.ui.theme.*

@Composable
fun ExploreScreen(
    tournaments: List<FirestoreTournament>,
    currentUserId: String,
    onOpenJoinDialog: (FirestoreTournament) -> Unit,
    onOpenBracket: (String) -> Unit,
    onNavigate: (TanScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("All") }
    var selectedFormat by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }

    val games = listOf("All") + BracketEngine.SUPPORTED_GAMES
    val formats = listOf("All", "Single Elimination", "Double Elimination", "Round Robin")
    val statuses = listOf("All", "Open", "In Progress", "Completed")

    val filtered = remember(tournaments, searchQuery, selectedGame, selectedFormat, selectedStatus) {
        tournaments.filter { t ->
            val matchQuery = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.game.contains(searchQuery, ignoreCase = true) ||
                t.customGameName.contains(searchQuery, ignoreCase = true) ||
                t.inviteCode.contains(searchQuery, ignoreCase = true)
            val matchGame = selectedGame == "All" || t.game.equals(selectedGame, ignoreCase = true)
            val matchFmt = selectedFormat == "All" || t.format.equals(selectedFormat, ignoreCase = true)
            val matchStatus = selectedStatus == "All" || t.status.equals(selectedStatus, ignoreCase = true)
            matchQuery && matchGame && matchFmt && matchStatus
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search custom titles, games, invite codes...", color = MutedSlate, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricLime) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HighClarityWhite,
                    unfocusedTextColor = HighClarityWhite,
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = BorderDivider,
                    focusedContainerColor = ObsidianContainerLow,
                    unfocusedContainerColor = ObsidianContainerLow
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("explore_search_input")
            )
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

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(formats) { fmt ->
                    val selected = selectedFormat == fmt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ObsidianContainerHighest else ObsidianContainerLow)
                            .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                            .clickable { selectedFormat = fmt }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(fmt, color = if (selected) ElectricLime else MutedSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                statuses.forEach { st ->
                    val selected = selectedStatus == st
                    Surface(
                        color = if (selected) ElectricLime else ObsidianContainerLow,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).clickable { selectedStatus = st }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (st == "In Progress") "Live" else st,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "${filtered.size} FREE TOURNAMENTS FOUND",
                color = MutedSlate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (filtered.isEmpty()) {
            item {
                Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(36.dp))
                        Text("No matching tournaments", color = HighClarityWhite, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime)
                        ) {
                            Text("Create One Free", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { tourn ->
                FirestoreTournamentCard(
                    tournament = tourn,
                    isOrganizer = tourn.organizerId == currentUserId,
                    onJoin = { onOpenJoinDialog(tourn) },
                    onViewBracket = { onOpenBracket(tourn.id) }
                )
            }
        }
    }
}
