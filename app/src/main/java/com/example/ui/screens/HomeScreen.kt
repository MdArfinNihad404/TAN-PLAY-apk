package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.FirestoreTournamentRepository
import com.example.data.UserProfile
import com.example.ui.components.TanScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tournaments: List<FirestoreTournament>,
    userProfile: UserProfile?,
    currentUserId: String,
    repository: FirestoreTournamentRepository,
    onOpenJoinDialog: (FirestoreTournament) -> Unit,
    onOpenBracket: (String) -> Unit,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedGameFilter by remember { mutableStateOf("All") }
    var quickInviteCode by remember { mutableStateOf("") }

    val games = listOf("All") + BracketEngine.SUPPORTED_GAMES

    val filteredTournaments = remember(tournaments, selectedGameFilter) {
        tournaments.filter { t ->
            selectedGameFilter == "All" || t.game.equals(selectedGameFilter, ignoreCase = true)
        }
    }

    val openTournaments = remember(tournaments) { tournaments.count { it.status == "Open" } }
    val liveTournaments = remember(tournaments) { tournaments.count { it.status == "In Progress" } }
    val myHostedCount = remember(tournaments, currentUserId) { tournaments.count { it.organizerId == currentUserId } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Player Greeting Banner
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                modifier = Modifier.fillMaxWidth().testTag("home_greeting_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ElectricLime))
                            Text(
                                text = "WELCOME TO TAN PLAY",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = userProfile?.username?.ifBlank { userProfile.displayName } ?: "Competitor",
                            color = HighClarityWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% Free eSports • ${userProfile?.primaryGame ?: "FC Mobile"}",
                            color = MutedSlate,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp).testTag("home_create_tournament_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Quick KPI Strip
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("OPEN", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$openTournaments", color = ElectricLime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LIVE", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$liveTournaments", color = LiveRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("HOSTED", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$myHostedCount", color = HighClarityWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Join by Invite Code
        item {
            Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickInviteCode,
                        onValueChange = { quickInviteCode = it.take(40) },
                        placeholder = { Text("Paste Invite Code (TAN-...) or Link", color = MutedSlate, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HighClarityWhite,
                            unfocusedTextColor = HighClarityWhite,
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = BorderDivider
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (quickInviteCode.isNotBlank()) {
                                scope.launch {
                                    val found = repository.findTournamentByInviteCode(quickInviteCode).getOrNull()
                                    if (found != null) {
                                        onOpenBracket(found.id)
                                    } else {
                                        onShowMessage("Tournament not found for '$quickInviteCode'")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianContainerHighest, contentColor = ElectricLime),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Open", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Game Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { game ->
                    val selected = selectedGameFilter == game
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) ElectricLime else ObsidianContainerLow)
                            .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(10.dp))
                            .clickable { selectedGameFilter = game }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = game,
                            color = if (selected) OnElectricLime else SlateSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tournaments Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOURNAMENTS FEED",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Explore All →",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(TanScreen.EXPLORE) }
                )
            }
        }

        if (filteredTournaments.isEmpty()) {
            item {
                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(40.dp))
                        Text("No Free Tournaments Found", color = HighClarityWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Host your own custom tournament in seconds — invite players with a shareable code.",
                            color = MutedSlate,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Host First Tournament", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredTournaments, key = { it.id }) { tournament ->
                FirestoreTournamentCard(
                    tournament = tournament,
                    isOrganizer = tournament.organizerId == currentUserId,
                    onJoin = { onOpenJoinDialog(tournament) },
                    onViewBracket = { onOpenBracket(tournament.id) }
                )
            }
        }
    }
}
