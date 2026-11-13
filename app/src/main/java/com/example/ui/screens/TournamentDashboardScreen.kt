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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BracketEngine
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.data.UserProfile
import com.example.ui.components.TanScreen
import com.example.ui.components.TournamentChatDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TournamentDashboardScreen(
    tournaments: List<FirestoreTournament>,
    currentUserId: String,
    userProfile: UserProfile? = null,
    isAdminUser: Boolean = false,
    repository: FirestoreTournamentRepository,
    onOpenJoinDialog: (FirestoreTournament) -> Unit,
    onOpenBracket: (String) -> Unit,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var selectedStatusTab by remember { mutableStateOf("All") } // "All", "Open", "In Progress", "Completed"
    var selectedGame by remember { mutableStateOf("All") }
    var chatTournament by remember { mutableStateOf<FirestoreTournament?>(null) }

    val gamesList = listOf("All") + BracketEngine.SUPPORTED_GAMES

    val filteredTournaments = remember(tournaments, searchQuery, selectedStatusTab, selectedGame) {
        tournaments.filter { t ->
            val matchesStatus = when (selectedStatusTab) {
                "Open" -> t.status == "Open"
                "In Progress" -> t.status == "In Progress"
                "Completed" -> t.status == "Completed"
                else -> true
            }
            val matchesGame = selectedGame == "All" || t.game.equals(selectedGame, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.game.contains(searchQuery, ignoreCase = true) ||
                t.customGameName.contains(searchQuery, ignoreCase = true) ||
                t.organizerName.contains(searchQuery, ignoreCase = true) ||
                t.inviteCode.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesGame && matchesSearch
        }
    }

    val openCount = remember(tournaments) { tournaments.count { it.status == "Open" } }
    val liveCount = remember(tournaments) { tournaments.count { it.status == "In Progress" } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bento
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FREE ARENAS DIRECTORY", color = HighClarityWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Synced live from Cloud Firestore", color = SlateSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Host Free", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = ObsidianContainerLowest, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("OPEN REGISTRATION", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("$openCount Open", color = ElectricLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(color = ObsidianContainerLowest, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("LIVE BRACKETS", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(LiveRed))
                                    Text("$liveCount Active", color = HighClarityWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Join by Invite Code / Link
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.take(40) },
                            placeholder = { Text("Enter Invite Code (e.g. TAN-...) or link", color = MutedSlate, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HighClarityWhite,
                                unfocusedTextColor = HighClarityWhite,
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = BorderDivider
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("invite_code_input")
                        )
                        Button(
                            onClick = {
                                if (inviteCodeInput.isNotBlank()) {
                                    scope.launch {
                                        val res = repository.findTournamentByInviteCode(inviteCodeInput)
                                        val found = res.getOrNull()
                                        if (found != null) {
                                            onOpenBracket(found.id)
                                        } else {
                                            onShowMessage("No tournament found for code '$inviteCodeInput'.")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianContainerHighest, contentColor = ElectricLime),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp).testTag("invite_lookup_button")
                        ) {
                            Text("Go", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search & Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tournaments, games, hosts...", color = MutedSlate, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighClarityWhite,
                        unfocusedTextColor = HighClarityWhite,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = BorderDivider,
                        focusedContainerColor = ObsidianContainerLow,
                        unfocusedContainerColor = ObsidianContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "Open", "In Progress", "Completed").forEach { tab ->
                        val selected = selectedStatusTab == tab
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { selectedStatusTab = tab }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (tab == "In Progress") "Live" else tab,
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(gamesList) { game ->
                        val selected = selectedGame == game
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ElectricLime else ObsidianContainerLow)
                                .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                                .clickable { selectedGame = game }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = game,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (filteredTournaments.isEmpty()) {
            item {
                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(36.dp))
                        Text("No tournaments match your filter", color = HighClarityWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Create a free custom tournament to get started!", color = MutedSlate, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredTournaments, key = { it.id }) { tournament ->
                FirestoreTournamentCard(
                    tournament = tournament,
                    isOrganizer = tournament.organizerId == currentUserId,
                    onJoin = { onOpenJoinDialog(tournament) },
                    onViewBracket = { onOpenBracket(tournament.id) },
                    onOpenChat = { chatTournament = tournament }
                )
            }
        }
    }

    chatTournament?.let { target ->
        val teams by repository.observeTeams(target.id).collectAsState(initial = emptyList())
        TournamentChatDialog(
            tournament = target,
            teams = teams,
            currentUserId = currentUserId,
            currentUserName = userProfile?.username?.ifBlank { userProfile.displayName } ?: "Player",
            isAdminUser = isAdminUser,
            repository = repository,
            onDismiss = { chatTournament = null },
            onActionMessage = onShowMessage
        )
    }
}

@Composable
fun FirestoreTournamentCard(
    tournament: FirestoreTournament,
    isOrganizer: Boolean,
    onJoin: () -> Unit,
    onViewBracket: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFull = tournament.participantCount >= tournament.maxParticipants
    val isLive = tournament.status == "In Progress"
    val isOpen = tournament.status == "Open"
    val effectiveGame = if (tournament.game == "Custom Game" && tournament.customGameName.isNotBlank()) {
        tournament.customGameName
    } else tournament.game

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLive) ElectricLime.copy(alpha = 0.5f) else BorderDivider),
        modifier = modifier.fillMaxWidth().testTag("tournament_card_${tournament.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = ObsidianContainerHighest, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = effectiveGame,
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text("${tournament.format} • ${tournament.participationType}", color = MutedSlate, fontSize = 10.sp)
                }

                Surface(
                    color = if (isLive) LiveRed.copy(alpha = 0.16f) else ObsidianContainerHighest,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tournament.status.uppercase(),
                        color = if (isLive) LiveRed else ElectricLime,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(tournament.title, color = HighClarityWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Deadline: ${tournament.registrationDeadlineText.ifBlank { "Before kickoff" }}",
                    color = SlateSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = if (isOrganizer) "Host: You" else "Host: ${tournament.organizerName}",
                    color = MutedSlate,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${tournament.prizePoolText.ifBlank { "Glory & Badge" }} • FREE",
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${tournament.participantCount}/${tournament.maxParticipants} slots • ${tournament.inviteCode}",
                        color = MutedSlate,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenChat != null) {
                        IconButton(
                            onClick = onOpenChat,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianContainerHigh)
                                .testTag("dashboard_chat_${tournament.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Open Tournament Lobby Chat",
                                tint = ElectricLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onViewBracket,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Bracket", color = HighClarityWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (isOpen) {
                        Button(
                            onClick = onJoin,
                            enabled = !isFull,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isFull) "Full" else "Join Free", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
