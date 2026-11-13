package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Dynamic Game-Specific Tournament Join & Edit-Before-Deadline Dialog.
 * Supports Free Fire, eFootball, FC Mobile, DLS, PUBG Mobile, and Custom Games,
 * plus Team creation/join flows when participationType == "Team".
 */
@Composable
fun JoinTournamentDialog(
    tournament: FirestoreTournament,
    userProfile: UserProfile?,
    existingParticipant: TournamentParticipant?,
    teams: List<TournamentTeam>,
    repository: FirestoreTournamentRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val effectiveGame = if (tournament.game == "Custom Game" && tournament.customGameName.isNotBlank()) {
        tournament.customGameName
    } else {
        tournament.game
    }
    val specs = remember(effectiveGame, tournament.customRequiredFields) {
        BracketEngine.getFieldSpecs(effectiveGame, tournament.customRequiredFields)
    }

    var displayName by remember(existingParticipant, userProfile) {
        mutableStateOf(
            existingParticipant?.displayName
                ?: userProfile?.username?.takeIf { it.isNotBlank() }
                ?: userProfile?.displayName
                ?: ""
        )
    }

    val fieldStates = remember(tournament.id, existingParticipant) {
        mutableStateMapOf<String, String>().apply {
            specs.forEach { spec ->
                val saved = existingParticipant?.gameFields?.get(spec.key)
                    ?: when {
                        tournament.game == "Free Fire" && spec.key == "uid" -> userProfile?.freeFireUid
                        tournament.game == "Free Fire" && spec.key == "ign" -> userProfile?.freeFireIgn
                        tournament.game == "Free Fire" && spec.key == "level" -> userProfile?.freeFireLevel
                        tournament.game == "FC Mobile" && spec.key == "uid" -> userProfile?.fcMobileUid
                        tournament.game == "FC Mobile" && spec.key == "ovr" -> userProfile?.fcMobileOvr
                        tournament.game == "eFootball" && spec.key == "onlineId" -> userProfile?.efootballId
                        tournament.game == "eFootball" && spec.key == "platform" -> userProfile?.efootballPlatform?.ifBlank { "Android" }
                        (tournament.game == "DLS" || tournament.game == "DLS 24") && spec.key == "teamName" -> userProfile?.dlsTeamName
                        (tournament.game == "DLS" || tournament.game == "DLS 24") && spec.key == "teamRating" -> userProfile?.dlsTeamRating
                        tournament.game == "PUBG Mobile" && spec.key == "playerId" -> userProfile?.pubgPlayerId
                        tournament.game == "PUBG Mobile" && spec.key == "nickname" -> userProfile?.pubgNickname
                        spec.options.isNotEmpty() -> spec.options.first()
                        else -> ""
                    }.orEmpty()
                put(spec.key, saved)
            }
        }
    }

    // Team Flow State
    var teamMode by remember { mutableStateOf("Create") } // "Create" or "Join"
    var newTeamName by remember(existingParticipant) { mutableStateOf(existingParticipant?.teamName.orEmpty()) }
    var joinTeamCode by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_tournament_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingParticipant != null) "EDIT REGISTRATION" else "FREE TOURNAMENT ENTRY",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = tournament.title,
                            color = HighClarityWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$effectiveGame • ${tournament.format} • Free Entry",
                            color = MutedSlate,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateSecondary)
                    }
                }

                // Privacy Notice for sensitive game IDs
                Surface(
                    color = ObsidianContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Privacy Protection: Your game ID/UID is masked publicly (••••1234) and only shared with your match opponent after both click Ready.",
                            color = MutedSlate,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    label = { Text("Player / Bracket Display Name", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighClarityWhite,
                        unfocusedTextColor = HighClarityWhite,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = BorderDivider
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_display_name_input")
                )

                // Dynamic Game-Specific Fields
                specs.forEach { spec ->
                    if (spec.options.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(spec.label.uppercase(), color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                spec.options.forEach { option ->
                                    val selected = fieldStates[spec.key] == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ElectricLime else ObsidianContainer)
                                            .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                                            .clickable { fieldStates[spec.key] = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = if (selected) OnElectricLime else SlateSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = fieldStates[spec.key].orEmpty(),
                            onValueChange = { input ->
                                fieldStates[spec.key] = if (spec.isNumeric) {
                                    input.filter { it.isDigit() }.take(18)
                                } else {
                                    input.take(40)
                                }
                            },
                            label = { Text(spec.label, fontSize = 11.sp) },
                            placeholder = { Text(spec.placeholder, color = MutedSlate, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (spec.isNumeric) KeyboardType.Number else KeyboardType.Text
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HighClarityWhite,
                                unfocusedTextColor = HighClarityWhite,
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = BorderDivider
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_field_${spec.key}")
                        )
                    }
                }

                // Team Section if Team Tournament
                if (tournament.participationType == "Team") {
                    Surface(
                        color = ObsidianContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "TEAM / SQUAD ENTRY (MAX ${tournament.maxTeamSize} PLAYERS)",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Create", "Join").forEach { mode ->
                                    val selected = teamMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ElectricLime else ObsidianContainerLowest)
                                            .clickable { teamMode = mode }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (mode == "Create") "Create Team" else "Join by Code",
                                            color = if (selected) OnElectricLime else SlateSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (teamMode == "Create") {
                                OutlinedTextField(
                                    value = newTeamName,
                                    onValueChange = { newTeamName = it.take(36) },
                                    label = { Text("Team / Clan Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = HighClarityWhite,
                                        unfocusedTextColor = HighClarityWhite,
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = BorderDivider
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = joinTeamCode,
                                    onValueChange = { joinTeamCode = it.uppercase().take(12) },
                                    label = { Text("Enter Team Join Code", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = HighClarityWhite,
                                        unfocusedTextColor = HighClarityWhite,
                                        focusedBorderColor = ElectricLime,
                                        unfocusedBorderColor = BorderDivider
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (teams.isNotEmpty()) {
                                    Text(
                                        text = "Open Teams: " + teams.filter { it.status == "Open" }
                                            .joinToString(", ") { "${it.name} (${it.teamCode})" }
                                            .ifEmpty { "None open" },
                                        color = MutedSlate,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Edit Audit Trail if updating before deadline
                if (existingParticipant != null && existingParticipant.auditLog.isNotEmpty()) {
                    Text(
                        text = "Audit Log: ${existingParticipant.auditLog.last()}",
                        color = MutedSlate,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = LiveRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        if (displayName.isBlank()) {
                            errorMessage = "Display name is required."
                            return@Button
                        }
                        val cleanMap = fieldStates.toMap()
                        val err = BracketEngine.validateGameFields(effectiveGame, tournament.customRequiredFields, cleanMap)
                        if (err != null) {
                            errorMessage = err
                            return@Button
                        }

                        isSubmitting = true
                        errorMessage = null
                        scope.launch {
                            var resolvedTeamId = existingParticipant?.teamId.orEmpty()
                            var resolvedTeamName = existingParticipant?.teamName.orEmpty()
                            val strength = BracketEngine.extractRankingMetric(effectiveGame, tournament.customRequiredFields, cleanMap)

                            if (tournament.participationType == "Team" && existingParticipant == null) {
                                if (teamMode == "Create") {
                                    if (newTeamName.isBlank()) {
                                        isSubmitting = false
                                        errorMessage = "Enter a team name or switch to Join by Code."
                                        return@launch
                                    }
                                    val tmRes = repository.createTeam(
                                        tournamentId = tournament.id,
                                        teamName = newTeamName,
                                        captainDisplayName = displayName,
                                        strength = strength
                                    )
                                    if (tmRes.isFailure) {
                                        isSubmitting = false
                                        errorMessage = tmRes.exceptionOrNull()?.localizedMessage ?: "Failed to create team"
                                        return@launch
                                    }
                                    val createdTeam = tmRes.getOrThrow()
                                    resolvedTeamId = createdTeam.id
                                    resolvedTeamName = "${createdTeam.name} (${createdTeam.teamCode})"
                                } else {
                                    if (joinTeamCode.isBlank()) {
                                        isSubmitting = false
                                        errorMessage = "Enter a valid team code."
                                        return@launch
                                    }
                                    val joinRes = repository.joinTeamByCode(
                                        tournamentId = tournament.id,
                                        teamCode = joinTeamCode,
                                        memberDisplayName = displayName,
                                        memberStrength = strength,
                                        maxTeamSize = tournament.maxTeamSize
                                    )
                                    if (joinRes.isFailure) {
                                        isSubmitting = false
                                        errorMessage = joinRes.exceptionOrNull()?.localizedMessage ?: "Failed to join team"
                                        return@launch
                                    }
                                    val joinedTeam = joinRes.getOrThrow()
                                    resolvedTeamId = joinedTeam.id
                                    resolvedTeamName = "${joinedTeam.name} (${joinedTeam.teamCode})"
                                }
                            }

                            val finalDisplayName = if (resolvedTeamName.isNotEmpty() && !displayName.contains(resolvedTeamName)) {
                                "${displayName.trim()} [${resolvedTeamName.substringBefore(" (")}]"
                            } else {
                                displayName.trim()
                            }

                            val res = repository.joinOrUpdateParticipant(
                                tournament = tournament,
                                displayName = finalDisplayName,
                                gameFields = cleanMap,
                                teamId = resolvedTeamId,
                                teamName = resolvedTeamName
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                onSuccess(
                                    if (existingParticipant != null) "Registration updated & logged!"
                                    else "Successfully joined ${tournament.title}!"
                                )
                            } else {
                                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Registration failed"
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_join_tournament_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnElectricLime, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (existingParticipant != null) "Save Changes" else "Confirm Free Entry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
