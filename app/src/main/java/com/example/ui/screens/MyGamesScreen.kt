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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreMatch
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.data.UserProfile
import com.example.ui.components.BracketFirestoreMatchCard
import com.example.ui.components.MatchLobbyDialog
import com.example.ui.components.TanScreen
import com.example.ui.components.TournamentChatDialog
import com.example.ui.theme.*

@Composable
fun MyGamesScreen(
    tournaments: List<FirestoreTournament>,
    selectedTournamentId: String?,
    onSelectTournament: (String) -> Unit,
    currentUserId: String,
    userProfile: UserProfile? = null,
    isAdminUser: Boolean,
    repository: FirestoreTournamentRepository,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTournament = remember(tournaments, selectedTournamentId) {
        tournaments.find { it.id == selectedTournamentId }
            ?: tournaments.firstOrNull { it.status == "In Progress" }
            ?: tournaments.firstOrNull()
    }

    if (activeTournament == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBase)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(44.dp))
                Text("No Active Matches", color = HighClarityWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Join a free tournament or host one to open your Match Lobby.", color = MutedSlate, fontSize = 13.sp)
                Button(
                    onClick = { onNavigate(TanScreen.TOURNAMENT_DASHBOARD) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Browse Tournaments", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val matches by repository.observeMatches(activeTournament.id).collectAsState(initial = emptyList())
    val teams by repository.observeTeams(activeTournament.id).collectAsState(initial = emptyList())
    val myMatches = remember(matches, currentUserId) {
        matches.filter { it.participant1UserId == currentUserId || it.participant2UserId == currentUserId }
    }
    var selectedTab by remember { mutableStateOf("My Matches") }
    var activeLobbyMatch by remember { mutableStateOf<FirestoreMatch?>(null) }
    var showTournamentChat by remember { mutableStateOf(false) }

    val liveLobbyMatch = remember(activeLobbyMatch, matches) {
        activeLobbyMatch?.let { sel -> matches.find { it.id == sel.id } ?: sel }
    }

    val shownMatches = remember(selectedTab, myMatches, matches) {
        when (selectedTab) {
            "My Matches" -> myMatches
            "Pending" -> matches.filter { it.status in listOf("Scheduled", "Ready", "Awaiting Results", "Disputed") }
            else -> matches.filter { it.status == "Completed" }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MATCH HUB & LOBBY",
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = activeTournament.title,
                        color = HighClarityWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Click any match to mark Ready, unlock opponent Game ID, submit proof, or confirm results.",
                        color = MutedSlate,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showTournamentChat = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(44.dp).testTag("my_games_lobby_chat_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open Tournament Lobby Chat", tint = ElectricLime, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lobby Chat", color = HighClarityWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tournaments.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tournaments, key = { it.id }) { t ->
                        val selected = t.id == activeTournament.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ElectricLime else ObsidianContainerLow)
                                .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                                .clickable { onSelectTournament(t.id) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = t.title,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("My Matches", "Pending", "Completed").forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        color = if (selected) ElectricLime else ObsidianContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).clickable { selectedTab = tab }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = tab,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (shownMatches.isEmpty()) {
            item {
                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (matches.isEmpty()) "Bracket not drawn yet" else "No matches in '$selectedTab'",
                            color = HighClarityWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        OutlinedButton(onClick = { onNavigate(TanScreen.BRACKET) }) {
                            Text("Open Bracket View", color = ElectricLime)
                        }
                    }
                }
            }
        } else {
            items(shownMatches, key = { it.id }) { match ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${match.roundName.uppercase()} • ${match.scheduledTimeText.ifBlank { "Scheduled" }}",
                        color = MutedSlate,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    BracketFirestoreMatchCard(
                        match = match,
                        currentUserId = currentUserId,
                        isOrganizerOrAdmin = isAdminUser || activeTournament.organizerId == currentUserId,
                        onClick = { activeLobbyMatch = match },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .testTag("my_games_match_${match.id}")
                    )
                }
            }
        }
    }

    if (liveLobbyMatch != null) {
        MatchLobbyDialog(
            tournament = activeTournament,
            match = liveLobbyMatch,
            currentUserId = currentUserId,
            isAdminOrOrganizer = isAdminUser || activeTournament.organizerId == currentUserId,
            repository = repository,
            onDismiss = { activeLobbyMatch = null },
            onActionMessage = onShowMessage,
            onOpenTournamentChat = {
                activeLobbyMatch = null
                showTournamentChat = true
            }
        )
    }

    if (showTournamentChat) {
        TournamentChatDialog(
            tournament = activeTournament,
            teams = teams,
            currentUserId = currentUserId,
            currentUserName = userProfile?.username?.ifBlank { userProfile.displayName } ?: "Player",
            isAdminUser = isAdminUser,
            repository = repository,
            onDismiss = { showTournamentChat = false },
            onActionMessage = onShowMessage
        )
    }
}
