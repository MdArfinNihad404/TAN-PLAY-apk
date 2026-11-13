package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.BracketEngine
import com.example.data.FirestoreMatch
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MatchLobbyDialog(
    tournament: FirestoreTournament,
    match: FirestoreMatch,
    currentUserId: String,
    isAdminOrOrganizer: Boolean,
    repository: FirestoreTournamentRepository,
    onDismiss: () -> Unit,
    onActionMessage: (String) -> Unit,
    onOpenTournamentChat: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isP1 = currentUserId == match.participant1UserId
    val isP2 = currentUserId == match.participant2UserId
    val isContender = isP1 || isP2
    val bothReady = match.participant1Ready && match.participant2Ready
    val revealFullIds = isAdminOrOrganizer || (isContender && bothReady)

    val myReady = (isP1 && match.participant1Ready) || (isP2 && match.participant2Ready)
    val opponentName = if (isP1) match.participant2Name else match.participant1Name
    val opponentGameId = if (isP1) match.participant2GameId else match.participant1GameId

    var score1Text by remember(match.id, match.score1) { mutableStateOf((match.score1 ?: 0).toString()) }
    var score2Text by remember(match.id, match.score2) { mutableStateOf((match.score2 ?: 0).toString()) }
    var proofNote by remember(match.id, match.proofNote) { mutableStateOf(match.proofNote) }
    var pickedProofUri by remember(match.id, match.proofImageUri) {
        mutableStateOf<Uri?>(match.proofImageUri.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) })
    }

    var showDisputeBox by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("Score mismatch / Opponent did not show up") }
    var isBusy by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedProofUri = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("match_lobby_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${match.roundName.uppercase()} • MATCH #${match.matchNumber}",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = tournament.title,
                            color = HighClarityWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${match.status} • ${match.scheduledTimeText.ifBlank { "Scheduled" }}",
                            color = MutedSlate,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Match Lobby", tint = SlateSecondary)
                    }
                }

                // Contenders Card
                Surface(
                    color = ObsidianContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LobbyPlayerRow(
                            slotLabel = "SLOT 1",
                            name = match.participant1Name.ifBlank { "TBD" },
                            stat = match.participant1Stat,
                            rawId = match.participant1GameId,
                            revealId = revealFullIds || isP1,
                            isReady = match.participant1Ready,
                            score = match.score1,
                            isWinner = match.status == "Completed" && match.winnerParticipantId == match.participant1Id,
                            onCopyId = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Game ID", match.participant1GameId))
                                onActionMessage("Copied Game ID: ${match.participant1GameId}")
                            }
                        )
                        HorizontalDivider(color = BorderDivider)
                        LobbyPlayerRow(
                            slotLabel = "SLOT 2",
                            name = match.participant2Name.ifBlank { "TBD" },
                            stat = match.participant2Stat,
                            rawId = match.participant2GameId,
                            revealId = revealFullIds || isP2,
                            isReady = match.participant2Ready,
                            score = match.score2,
                            isWinner = match.status == "Completed" && match.winnerParticipantId == match.participant2Id,
                            onCopyId = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Game ID", match.participant2GameId))
                                onActionMessage("Copied Game ID: ${match.participant2GameId}")
                            }
                        )
                    }
                }

                // Ready Gate & Honest Manual Gameplay Instructions
                if (match.status in listOf("Scheduled", "Ready", "Pending") &&
                    match.participant1Id.isNotEmpty() && match.participant2Id.isNotEmpty()
                ) {
                    Surface(
                        color = ObsidianContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!bothReady) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Both players must click Ready to unlock opponent's full Game ID and start the match.",
                                        color = SlateSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isContender && !myReady) {
                                    Button(
                                        onClick = {
                                            isBusy = true
                                            scope.launch {
                                                val res = repository.markPlayerReady(tournament.id, match)
                                                isBusy = false
                                                if (res.isSuccess) onActionMessage("Marked Ready! Waiting for opponent.")
                                                else localError = res.exceptionOrNull()?.localizedMessage
                                            }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("match_ready_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("I'm Ready to Play", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "MATCH READY — MANUAL LAUNCH INSTRUCTIONS",
                                        color = ElectricLime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                val gameLabel = if (tournament.game == "Custom Game" && tournament.customGameName.isNotBlank()) {
                                    tournament.customGameName
                                } else tournament.game
                                Text(
                                    text = "1. Open $gameLabel on your device.\n2. Add $opponentName using ID: ${opponentGameId.ifBlank { "Check Room ID" }}.\n3. Play your match (${tournament.matchDurationMinutes}m limit), capture final scoreboard screenshot, and submit score below.",
                                    color = HighClarityWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                                if (match.roomId.isNotBlank()) {
                                    Surface(color = ObsidianContainerLowest, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Room ID: ${match.roomId}  |  Pass: ${match.roomPassword.ifBlank { "None" }}",
                                            color = ElectricLime,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Result Submission / Confirm / Dispute Controls
                if ((isContender || isAdminOrOrganizer) &&
                    match.status !in listOf("Completed", "Cancelled") &&
                    match.participant1Id.isNotEmpty() && match.participant2Id.isNotEmpty()
                ) {
                    Surface(
                        color = ObsidianContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SCORE SUBMISSION & PROOF",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = score1Text,
                                    onValueChange = { score1Text = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text(match.participant1Name.take(14), fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = HighClarityWhite,
                                        unfocusedTextColor = HighClarityWhite,
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = BorderDivider
                                    ),
                                    modifier = Modifier.weight(1f).testTag("score1_input")
                                )
                                OutlinedTextField(
                                    value = score2Text,
                                    onValueChange = { score2Text = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text(match.participant2Name.take(14), fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = HighClarityWhite,
                                        unfocusedTextColor = HighClarityWhite,
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = BorderDivider
                                    ),
                                    modifier = Modifier.weight(1f).testTag("score2_input")
                                )
                            }

                            OutlinedTextField(
                                value = proofNote,
                                onValueChange = { proofNote = it.take(200) },
                                label = { Text("Match note / proof details", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HighClarityWhite,
                                    unfocusedTextColor = HighClarityWhite,
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = BorderDivider
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pick_proof_image_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ElectricLime)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (pickedProofUri != null) "Change Screenshot Proof ✓" else "Attach Match Screenshot (Photo Picker)",
                                    color = HighClarityWhite,
                                    fontSize = 12.sp
                                )
                            }

                            pickedProofUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected match proof screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderDivider, RoundedCornerShape(10.dp))
                                )
                            }

                            // Action Buttons based on current match state
                            if (match.status == "Awaiting Results" && match.submittedByUserId != currentUserId && isContender) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showDisputeBox = !showDisputeBox },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("dispute_result_button")
                                    ) {
                                        Text("Dispute", color = LiveRed, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            isBusy = true
                                            scope.launch {
                                                val res = repository.confirmMatchResultAndAdvance(tournament, match)
                                                isBusy = false
                                                if (res.isSuccess) {
                                                    onActionMessage("Result confirmed & winner advanced!")
                                                    onDismiss()
                                                } else {
                                                    localError = res.exceptionOrNull()?.localizedMessage
                                                }
                                            }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("confirm_result_button")
                                    ) {
                                        Text("Confirm & Advance", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (isContender) {
                                Button(
                                    onClick = {
                                        val s1 = score1Text.toIntOrNull()
                                        val s2 = score2Text.toIntOrNull()
                                        if (s1 == null || s2 == null || s1 == s2) {
                                            localError = "Enter valid non-tied scores to determine winner."
                                            return@Button
                                        }
                                        isBusy = true
                                        localError = null
                                        scope.launch {
                                            val res = repository.submitMatchResult(
                                                tournamentId = tournament.id,
                                                match = match,
                                                score1 = s1,
                                                score2 = s2,
                                                proofImageUri = pickedProofUri?.toString().orEmpty(),
                                                proofNote = proofNote.ifBlank { "Score submitted via Match Lobby" }
                                            )
                                            isBusy = false
                                            if (res.isSuccess) {
                                                onActionMessage("Score submitted! Waiting for opponent confirmation.")
                                                onDismiss()
                                            } else {
                                                localError = res.exceptionOrNull()?.localizedMessage
                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_score_button")
                                ) {
                                    Text("Submit Result for Confirmation", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Organizer / Admin Official Override
                            if (isAdminOrOrganizer) {
                                HorizontalDivider(color = BorderDivider)
                                Text(
                                    text = "ORGANIZER / ADMIN OFFICIAL RULING",
                                    color = ElectricLime,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val s1 = score1Text.toIntOrNull() ?: 1
                                            val s2 = score2Text.toIntOrNull() ?: 0
                                            isBusy = true
                                            scope.launch {
                                                val res = repository.adminOrOrganizerOverrideMatch(
                                                    tournament = tournament,
                                                    match = match,
                                                    winnerSlot = 1,
                                                    score1 = maxOf(s1, s2 + 1),
                                                    score2 = s2,
                                                    resolutionNote = "Official ruling"
                                                )
                                                isBusy = false
                                                if (res.isSuccess) {
                                                    onActionMessage("${match.participant1Name} advanced!")
                                                    onDismiss()
                                                } else localError = res.exceptionOrNull()?.localizedMessage
                                            }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianContainerHighest, contentColor = ElectricLime),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Advance ${match.participant1Name.take(8)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            val s1 = score1Text.toIntOrNull() ?: 0
                                            val s2 = score2Text.toIntOrNull() ?: 1
                                            isBusy = true
                                            scope.launch {
                                                val res = repository.adminOrOrganizerOverrideMatch(
                                                    tournament = tournament,
                                                    match = match,
                                                    winnerSlot = 2,
                                                    score1 = s1,
                                                    score2 = maxOf(s2, s1 + 1),
                                                    resolutionNote = "Official ruling"
                                                )
                                                isBusy = false
                                                if (res.isSuccess) {
                                                    onActionMessage("${match.participant2Name} advanced!")
                                                    onDismiss()
                                                } else localError = res.exceptionOrNull()?.localizedMessage
                                            }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianContainerHighest, contentColor = ElectricLime),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Advance ${match.participant2Name.take(8)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Jump to Tournament Lobby Chat (Captains & Organizer coordination)
                if (onOpenTournamentChat != null) {
                    OutlinedButton(
                        onClick = onOpenTournamentChat,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("match_lobby_open_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tournament Lobby Chat (Captains & Host)", color = HighClarityWhite, fontSize = 12.sp)
                    }
                }

                // Report Problem / Dispute Box (accessible anytime)
                if (isContender && match.status !in listOf("Completed", "Cancelled")) {
                    if (!showDisputeBox) {
                        OutlinedButton(
                            onClick = { showDisputeBox = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = LiveRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Report Problem / Call Referee", color = SlateSecondary, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = disputeReason,
                                onValueChange = { disputeReason = it.take(250) },
                                label = { Text("Describe the issue / dispute", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HighClarityWhite,
                                    unfocusedTextColor = HighClarityWhite,
                                    focusedBorderColor = LiveRed,
                                    unfocusedBorderColor = BorderDivider
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    isBusy = true
                                    scope.launch {
                                        val res = repository.disputeMatchResult(tournament, match, disputeReason)
                                        isBusy = false
                                        if (res.isSuccess) {
                                            onActionMessage("Dispute submitted to Organizer & Admin queue.")
                                            onDismiss()
                                        } else {
                                            localError = res.exceptionOrNull()?.localizedMessage
                                        }
                                    }
                                },
                                enabled = !isBusy && disputeReason.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = HighClarityWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Escalate Dispute to Referee", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Audit Log
                if (match.auditLog.isNotEmpty()) {
                    Surface(color = ObsidianContainerLowest, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("MATCH AUDIT LOG", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            match.auditLog.forEach { entry ->
                                Text("• $entry", color = SlateSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                localError?.let { err ->
                    Text(err, color = LiveRed, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun LobbyPlayerRow(
    slotLabel: String,
    name: String,
    stat: String,
    rawId: String,
    revealId: Boolean,
    isReady: Boolean,
    score: Int?,
    isWinner: Boolean,
    onCopyId: () -> Unit
) {
    val shownId = when {
        rawId.isEmpty() -> "No ID"
        revealId -> rawId
        else -> BracketEngine.maskGameId(rawId)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isReady) ElectricLime else MutedSlate)
                )
                Text(
                    text = name,
                    color = if (isWinner) ElectricLime else HighClarityWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isReady) {
                    Text("READY", color = ElectricLime, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
            Text(
                text = listOfNotNull(slotLabel, stat.takeIf { it.isNotEmpty() }, "ID: $shownId").joinToString(" • "),
                color = MutedSlate,
                fontSize = 10.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (revealId && rawId.isNotEmpty()) {
                IconButton(onClick = onCopyId, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Game ID", tint = ElectricLime, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = score?.toString() ?: "-",
                color = if (isWinner) ElectricLime else HighClarityWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
