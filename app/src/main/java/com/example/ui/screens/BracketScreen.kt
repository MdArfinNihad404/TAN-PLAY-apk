package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.model.SeedingMode
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BracketScreen(
    tournaments: List<FirestoreTournament>,
    selectedTournamentId: String?,
    onSelectTournament: (String) -> Unit,
    currentUserId: String,
    userProfile: UserProfile?,
    isAdminUser: Boolean,
    repository: FirestoreTournamentRepository,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeTournament = remember(tournaments, selectedTournamentId) {
        tournaments.find { it.id == selectedTournamentId } ?: tournaments.firstOrNull()
    }

    if (activeTournament == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBase)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(48.dp))
                Text("No Tournaments Yet", color = HighClarityWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Create a free custom tournament or join via an invite code to view live interactive brackets.",
                    color = MutedSlate,
                    fontSize = 13.sp
                )
                Button(
                    onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Create Free Tournament", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val participants by repository.observeParticipants(activeTournament.id).collectAsState(initial = emptyList())
    val matches by repository.observeMatches(activeTournament.id).collectAsState(initial = emptyList())
    val teams by repository.observeTeams(activeTournament.id).collectAsState(initial = emptyList())

    val isOrganizerOrAdmin = isAdminUser || activeTournament.organizerId == currentUserId
    val myParticipation = remember(participants, currentUserId) {
        participants.find { it.userId == currentUserId }
    }

    var drawMode by remember(activeTournament.id) {
        mutableStateOf(
            SeedingMode.values().find { it.apiValue == activeTournament.seedingMode } ?: SeedingMode.RANDOM
        )
    }
    var isDrawing by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedMatchForLobby by remember { mutableStateOf<FirestoreMatch?>(null) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showManualPairingDialog by remember { mutableStateOf(false) }

    // Keep lobby match live-updated from Firestore stream
    val liveLobbyMatch = remember(selectedMatchForLobby, matches) {
        selectedMatchForLobby?.let { sel -> matches.find { it.id == sel.id } ?: sel }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Horizontal Tournament Switcher
        if (tournaments.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tournaments, key = { it.id }) { tourn ->
                        val isSelected = tourn.id == activeTournament.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricLime else ObsidianContainerLow)
                                .border(1.dp, if (isSelected) ElectricLime else BorderDivider, RoundedCornerShape(10.dp))
                                .clickable { onSelectTournament(tourn.id) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = tourn.title,
                                color = if (isSelected) OnElectricLime else SlateSecondary,
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

        // Tournament Header + Invite & Chat Actions
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val gameDisplay = if (activeTournament.game == "Custom Game" && activeTournament.customGameName.isNotBlank()) {
                            activeTournament.customGameName
                        } else activeTournament.game
                        Surface(color = ObsidianContainerHighest, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "$gameDisplay • ${activeTournament.format}",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = if (activeTournament.status == "In Progress") ElectricLime.copy(alpha = 0.16f) else ObsidianContainerHighest,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = activeTournament.status.uppercase(),
                                color = if (activeTournament.status == "In Progress") ElectricLime else SlateSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = activeTournament.title,
                        color = HighClarityWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Free Entry • ${participants.size}/${activeTournament.maxParticipants} Players • Host: ${activeTournament.organizerName}",
                        color = MutedSlate,
                        fontSize = 11.sp
                    )

                    Surface(color = ObsidianContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Prize Pool: ${activeTournament.prizePoolText.ifBlank { "Glory & Champion Badge" }} (${activeTournament.prizeSplitNote.ifBlank { "Top 3" }})",
                                color = ElectricLime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeTournament.registrationDeadlineText.isNotBlank()) {
                                Text(
                                    text = "Reg. Deadline: ${activeTournament.registrationDeadlineText} • Start: ${activeTournament.startTimeText}",
                                    color = SlateSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (activeTournament.rules.isNotBlank()) {
                                Text(
                                    text = "Rules: ${activeTournament.rules}",
                                    color = MutedSlate,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val link = "tanplay://join/${activeTournament.id}"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Invite", "${activeTournament.inviteCode} | $link"))
                                onShowMessage("Copied invite ${activeTournament.inviteCode}")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Invite", tint = ElectricLime, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(activeTournament.inviteCode, color = HighClarityWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { showChatDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("open_tournament_chat_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Tournament Lobby Chat", tint = ElectricLime, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lobby Chat", color = HighClarityWhite, fontSize = 11.sp)
                        }
                    }

                    if (activeTournament.status == "Open") {
                        Button(
                            onClick = { showJoinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("bracket_join_edit_button")
                        ) {
                            Text(
                                text = if (myParticipation != null) "Edit My Registration (Before Deadline)" else "Join Tournament Free",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Organizer Draw & Seeding Engine Controls (Visible before bracket starts)
        if (isOrganizerOrAdmin && activeTournament.status == "Open" && matches.isEmpty()) {
            item {
                Surface(
                    color = ObsidianContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("organizer_draw_panel")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ORGANIZER DRAW & SEEDING ENGINE",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SeedingMode.values().forEach { mode ->
                                val selected = drawMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) ElectricLime else ObsidianContainerLowest)
                                        .clickable { drawMode = mode }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.apiValue,
                                        color = if (selected) OnElectricLime else SlateSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Fairness explanation
                        Surface(color = ObsidianContainerLowest, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = drawMode.description,
                                color = SlateSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (participants.size < 2) {
                                    onShowMessage("At least 2 players must join before drawing the bracket.")
                                    return@Button
                                }
                                if (drawMode == SeedingMode.MANUAL && activeTournament.format != "Round Robin") {
                                    showManualPairingDialog = true
                                } else {
                                    isDrawing = true
                                    scope.launch {
                                        val res = repository.startTournamentAndDrawBracket(
                                            tournament = activeTournament,
                                            participants = participants,
                                            seedingMode = drawMode.apiValue
                                        )
                                        isDrawing = false
                                        if (res.isSuccess) {
                                            onShowMessage("Bracket drawn (${drawMode.apiValue}) & published!")
                                        } else {
                                            onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Draw failed")
                                        }
                                    }
                                }
                            },
                            enabled = !isDrawing,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("draw_bracket_button")
                        ) {
                            if (isDrawing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnElectricLime, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (drawMode == SeedingMode.MANUAL) "Configure Manual Pairings" else "Lock Registration & Draw Bracket (${participants.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visual Tournament Bracket Tree (Mapped to Firestore Matches)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE FIRESTORE BRACKET",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tap any match to open Lobby →",
                        color = ElectricLime,
                        fontSize = 10.sp
                    )
                }

                VisualBracketTree(
                    tournament = activeTournament,
                    matches = matches,
                    participants = participants,
                    currentUserId = currentUserId,
                    isOrganizerOrAdmin = isOrganizerOrAdmin,
                    onMatchClick = { match -> selectedMatchForLobby = match }
                )
            }
        }

        // Registered Participants List (Masked IDs publicly)
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "REGISTERED ROSTER (${participants.size})",
                        color = ElectricLime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (participants.isEmpty()) {
                        Text("No players registered yet. Share ${activeTournament.inviteCode} to invite contenders.", color = MutedSlate, fontSize = 12.sp)
                    } else {
                        participants.forEachIndexed { idx, p ->
                            val idDisplay = if (isOrganizerOrAdmin || p.userId == currentUserId) {
                                p.sensitiveIdField
                            } else {
                                BracketEngine.maskGameId(p.sensitiveIdField)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${if (p.seedNumber > 0) "#${p.seedNumber} " else "${idx + 1}. "}${p.displayName}",
                                        color = if (p.userId == currentUserId) ElectricLime else HighClarityWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = listOfNotNull(p.publicStatLabel.takeIf { it.isNotEmpty() }, "ID: $idDisplay").joinToString(" • "),
                                        color = MutedSlate,
                                        fontSize = 10.sp
                                    )
                                }
                                if (p.teamName.isNotEmpty()) {
                                    Text(p.teamName.substringBefore(" ("), color = SlateSecondary, fontSize = 10.sp)
                                }
                            }
                            if (idx < participants.lastIndex) HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinTournamentDialog(
            tournament = activeTournament,
            userProfile = userProfile,
            existingParticipant = myParticipation,
            teams = teams,
            repository = repository,
            onDismiss = { showJoinDialog = false },
            onSuccess = { msg ->
                showJoinDialog = false
                onShowMessage(msg)
            }
        )
    }

    if (liveLobbyMatch != null) {
        MatchLobbyDialog(
            tournament = activeTournament,
            match = liveLobbyMatch,
            currentUserId = currentUserId,
            isAdminOrOrganizer = isOrganizerOrAdmin,
            repository = repository,
            onDismiss = { selectedMatchForLobby = null },
            onActionMessage = onShowMessage,
            onOpenTournamentChat = {
                selectedMatchForLobby = null
                showChatDialog = true
            }
        )
    }

    if (showChatDialog) {
        TournamentChatDialog(
            tournament = activeTournament,
            teams = teams,
            currentUserId = currentUserId,
            currentUserName = userProfile?.username?.ifBlank { userProfile.displayName } ?: "Player",
            isAdminUser = isAdminUser,
            repository = repository,
            onDismiss = { showChatDialog = false },
            onActionMessage = onShowMessage
        )
    }

    if (showManualPairingDialog) {
        ManualPairingDialog(
            participants = participants,
            onDismiss = { showManualPairingDialog = false },
            onConfirmPairs = { pairs ->
                showManualPairingDialog = false
                isDrawing = true
                scope.launch {
                    val res = repository.startTournamentAndDrawBracket(
                        tournament = activeTournament,
                        participants = participants,
                        seedingMode = SeedingMode.MANUAL.apiValue,
                        manualPairs = pairs
                    )
                    isDrawing = false
                    if (res.isSuccess) onShowMessage("Manual bracket published!")
                    else onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Manual draw failed")
                }
            }
        )
    }
}

@Composable
private fun ManualPairingDialog(
    participants: List<TournamentParticipant>,
    onDismiss: () -> Unit,
    onConfirmPairs: (List<Pair<TournamentParticipant, TournamentParticipant?>>) -> Unit
) {
    val ordered = remember(participants) { mutableStateListOf<TournamentParticipant>().apply { addAll(participants) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MANUAL ROUND 1 PAIRINGS", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Reorder players to adjust Round 1 matchups (1 vs 2, 3 vs 4, etc.).", color = MutedSlate, fontSize = 11.sp)

                ordered.chunked(2).forEachIndexed { idx, pair ->
                    Surface(color = ObsidianContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "M${idx + 1}: ${pair[0].displayName} vs ${pair.getOrNull(1)?.displayName ?: "BYE"}",
                                color = HighClarityWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            val pos = idx * 2
                            if (pos + 1 < ordered.size) {
                                IconButton(
                                    onClick = {
                                        val nextIdx = (pos + 2) % ordered.size
                                        val tmp = ordered[pos + 1]
                                        ordered[pos + 1] = ordered[nextIdx]
                                        ordered[nextIdx] = tmp
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.SwapVert, contentDescription = "Swap Opponent", tint = ElectricLime, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = SlateSecondary)
                    }
                    Button(
                        onClick = {
                            val pairs = ordered.chunked(2).map { it[0] to it.getOrNull(1) }
                            onConfirmPairs(pairs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lock & Draw", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
