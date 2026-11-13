package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BracketEngine
import com.example.model.ParticipationType
import com.example.model.SeedingMode
import com.example.model.TournamentFormat
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.ln

data class WizardStepMeta(
    val stepIndex: Int,
    val shortLabel: String,
    val title: String,
    val subtitle: String
)

val TOURNAMENT_WIZARD_STEPS = listOf(
    WizardStepMeta(0, "Game", "Game & Title", "Choose the game discipline and tournament identity"),
    WizardStepMeta(1, "Rules", "Format & Rules", "Configure bracket structure, slot limits, and fair-play rules"),
    WizardStepMeta(2, "Prize", "Prize Pool", "Set organizer-sponsored rewards with 100% free player entry"),
    WizardStepMeta(3, "Schedule", "Deadlines & Room", "Define registration deadlines, kickoff time, and room access"),
    WizardStepMeta(4, "Review", "Review & Publish", "Verify your blueprint and save tournament live to Firestore")
)

@Composable
fun wizardTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = HighClarityWhite,
    unfocusedTextColor = HighClarityWhite,
    focusedBorderColor = ElectricLime,
    unfocusedBorderColor = BorderDivider,
    focusedLabelColor = ElectricLime,
    unfocusedLabelColor = MutedSlate,
    focusedContainerColor = ObsidianContainer,
    unfocusedContainerColor = ObsidianContainer
)

@Composable
fun WizardGameStep(
    selectedGame: String,
    onSelectGame: (String) -> Unit,
    customGameName: String,
    onCustomGameNameChange: (String) -> Unit,
    customFieldsText: String,
    onCustomFieldsTextChange: (String) -> Unit,
    tournamentTitle: String,
    onTournamentTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    val gameDescriptors = mapOf(
        "FC Mobile" to Pair(Icons.Default.SportsSoccer, "UID + Team OVR"),
        "eFootball" to Pair(Icons.Default.SportsSoccer, "Owner ID + Platform"),
        "Free Fire" to Pair(Icons.Default.Whatshot, "UID + IGN + Level"),
        "PUBG Mobile" to Pair(Icons.Default.GpsFixed, "Player ID + Nickname"),
        "Dream League Soccer" to Pair(Icons.Default.Stadium, "Team Name + Rating"),
        "Custom Game" to Pair(Icons.Default.Tune, "Custom Join Fields")
    )
    val titlePresets = listOf(
        "Weekend Open Knockout",
        "Dhaka Champions Cup",
        "Pro Community Series"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "TARGET GAME TITLE",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                val games = BracketEngine.SUPPORTED_GAMES
                for (rowGames in games.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGames.forEach { game ->
                            val selected = selectedGame == game
                            val meta = gameDescriptors[game] ?: Pair(Icons.Default.SportsEsports, "Standard")
                            Surface(
                                color = if (selected) ObsidianContainerHigh else ObsidianContainer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) ElectricLime else BorderDivider
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectGame(game) }
                                    .testTag("select_game_${game.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ElectricLime else ObsidianContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = meta.first,
                                            contentDescription = null,
                                            tint = if (selected) OnElectricLime else SlateSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = game,
                                            color = if (selected) HighClarityWhite else SlateSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = meta.second,
                                            color = if (selected) ElectricLime else MutedSlate,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (rowGames.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (selectedGame == "Custom Game") {
                    OutlinedTextField(
                        value = customGameName,
                        onValueChange = { onCustomGameNameChange(it.take(60)) },
                        label = { Text("Custom Game Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Call of Duty Mobile, Brawl Stars", color = MutedSlate, fontSize = 12.sp) },
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("wizard_custom_game_input")
                    )
                    OutlinedTextField(
                        value = customFieldsText,
                        onValueChange = { onCustomFieldsTextChange(it.take(150)) },
                        label = { Text("Required Join Fields (comma-separated, max 5)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Player UID, Server, Rank Rating", color = MutedSlate, fontSize = 11.sp) },
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TOURNAMENT IDENTITY",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = tournamentTitle,
                    onValueChange = { onTournamentTitleChange(it.take(120)) },
                    label = { Text("Official Tournament Title *", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Friday Night FC Mobile Open Cup", color = MutedSlate, fontSize = 12.sp) },
                    singleLine = true,
                    colors = wizardTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_title_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(titlePresets) { preset ->
                        val suggested = "${if (selectedGame == "Custom Game") customGameName.ifBlank { "Arena" } else selectedGame} $preset"
                        Surface(
                            color = ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderDivider),
                            modifier = Modifier.clickable { onTournamentTitleChange(suggested.take(120)) }
                        ) {
                            Text(
                                text = "+ $preset",
                                color = SlateSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { onDescriptionChange(it.take(400)) },
                    label = { Text("Overview & Highlights (Optional)", fontSize = 11.sp) },
                    placeholder = { Text("Describe format highlights, check-in expectations, and community focus", color = MutedSlate, fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 4,
                    colors = wizardTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_description_input")
                )
            }
        }
    }
}

@Composable
fun WizardFormatRulesStep(
    selectedFormat: TournamentFormat,
    onSelectFormat: (TournamentFormat) -> Unit,
    participationType: ParticipationType,
    onSelectParticipationType: (ParticipationType) -> Unit,
    maxTeamSizeText: String,
    onMaxTeamSizeChange: (String) -> Unit,
    maxParticipantsText: String,
    onMaxParticipantsChange: (String) -> Unit,
    selectedSeedingMode: SeedingMode,
    onSelectSeedingMode: (SeedingMode) -> Unit,
    rules: String,
    onRulesChange: (String) -> Unit
) {
    val slotCount = maxParticipantsText.toIntOrNull()?.coerceIn(2, 128) ?: 16
    val estimatedRounds = ceil(ln(slotCount.toDouble()) / ln(2.0)).toInt().coerceAtLeast(1)
    val ruleClauses = listOf(
        "End-match screenshot proof required within 15 mins.",
        "15-minute lobby check-in window; no-shows forfeit.",
        "Fair play enforced — zero emulators/scripts allowed.",
        "Disconnect before 15' triggers rematch with same score."
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "BRACKET & CAPACITY",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentFormat.values().forEach { fmt ->
                        val selected = selectedFormat == fmt
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectFormat(fmt) }
                                .testTag("wizard_format_${fmt.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = fmt.displayName,
                                    color = if (selected) OnElectricLime else HighClarityWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (fmt) {
                                        TournamentFormat.KNOCKOUT -> "Sudden death"
                                        TournamentFormat.DOUBLE_ELIM -> "Upper/Lower"
                                        TournamentFormat.ROUND_ROBIN -> "Group league"
                                    },
                                    color = if (selected) OnElectricLime.copy(alpha = 0.8f) else MutedSlate,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text("PARTICIPATION MODE", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParticipationType.values().forEach { pType ->
                        val selected = participationType == pType
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectParticipationType(pType) }
                                .testTag("wizard_participation_${pType.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (pType == ParticipationType.INDIVIDUAL) Icons.Default.Person else Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = if (selected) OnElectricLime else SlateSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = pType.displayName,
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text("SLOTS CAP ($slotCount SLOTS • ~$estimatedRounds ROUNDS)", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 8, 16, 32, 64).forEach { cap ->
                        val selected = maxParticipantsText == cap.toString()
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMaxParticipantsChange(cap.toString()) }
                                .testTag("wizard_slots_$cap")
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = cap.toString(),
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxParticipantsText,
                        onValueChange = { onMaxParticipantsChange(it.filter { c -> c.isDigit() }.take(3)) },
                        label = { Text("Custom Slots (2-128)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    if (participationType == ParticipationType.TEAM) {
                        OutlinedTextField(
                            value = maxTeamSizeText,
                            onValueChange = { onMaxTeamSizeChange(it.filter { c -> c.isDigit() }.take(2)) },
                            label = { Text("Team Size (2-10)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = wizardTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("SEEDING ENGINE", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SeedingMode.values().forEach { mode ->
                        val selected = selectedSeedingMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ElectricLime else ObsidianContainer)
                                .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                                .clickable { onSelectSeedingMode(mode) }
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
                Text(selectedSeedingMode.description, color = MutedSlate, fontSize = 11.sp)
            }
        }

        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TOURNAMENT RULES *",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text("TAP TO APPEND QUICK CLAUSES", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ruleClauses) { clause ->
                        val alreadyIncluded = rules.contains(clause, ignoreCase = true)
                        Surface(
                            color = if (alreadyIncluded) ElectricLime.copy(alpha = 0.14f) else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (alreadyIncluded) ElectricLime else BorderDivider),
                            modifier = Modifier.clickable {
                                if (!alreadyIncluded) {
                                    val updated = if (rules.isBlank()) clause else "${rules.trim()}\n• $clause"
                                    onRulesChange(updated.take(1200))
                                }
                            }
                        ) {
                            Text(
                                text = if (alreadyIncluded) "✓ ${clause.take(26)}…" else "+ ${clause.take(26)}…",
                                color = if (alreadyIncluded) ElectricLime else SlateSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rules,
                    onValueChange = { onRulesChange(it.take(1200)) },
                    label = { Text("Official Match & Fair-Play Rules", fontSize = 11.sp) },
                    minLines = 3,
                    maxLines = 6,
                    colors = wizardTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_rules_input")
                )
            }
        }
    }
}

@Composable
fun WizardPrizePoolStep(
    isCashPoolMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    prizePoolAmountText: String,
    onPrizePoolAmountChange: (String) -> Unit,
    prizePoolText: String,
    onPrizePoolTextChange: (String) -> Unit,
    prizeSplitNote: String,
    onPrizeSplitNoteChange: (String) -> Unit
) {
    val numericPool = prizePoolAmountText.toIntOrNull()?.coerceIn(0, 10_000_000) ?: 0
    val cashPresets = listOf(500, 1000, 2500, 5000, 10000)
    val rewardPresets = listOf(
        "Glory & Champion Badge",
        "1,000 Diamonds + Trophy",
        "Club Jersey & Winner Medal",
        "Pro League Qualifier Spot"
    )
    val splitPresets = listOf(
        "1st 60% • 2nd 25% • 3rd 15%",
        "1st 70% • 2nd 30%",
        "Winner Takes All (100%)",
        "Top 4 Equal Recognition"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Free Entry Guarantee
        Surface(
            color = ElectricLime.copy(alpha = 0.1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(20.dp))
                Column {
                    Text("100% Free Entry • Zero Player Fees", color = HighClarityWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Prize pools are organizer-sponsored or non-cash honors. Players always join free (৳0).", color = SlateSecondary, fontSize = 11.sp)
                }
            }
        }

        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SPONSORED PRIZE POOL",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (isCashPoolMode) ElectricLime else ObsidianContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isCashPoolMode) ElectricLime else BorderDivider),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onModeChange(true)
                                val amt = prizePoolAmountText.toIntOrNull()?.takeIf { it > 0 } ?: 5000
                                onPrizePoolAmountChange(amt.toString())
                                onPrizePoolTextChange("৳$amt Sponsored Pool")
                            }
                            .testTag("wizard_prize_mode_sponsored")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sponsored Pool (৳)",
                                color = if (isCashPoolMode) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = if (!isCashPoolMode) ElectricLime else ObsidianContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!isCashPoolMode) ElectricLime else BorderDivider),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onModeChange(false)
                                onPrizePoolAmountChange("0")
                                if (prizePoolText.startsWith("৳")) {
                                    onPrizePoolTextChange("Glory & Champion Badge")
                                }
                            }
                            .testTag("wizard_prize_mode_glory")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Trophy / Custom",
                                color = if (!isCashPoolMode) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isCashPoolMode) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cashPresets) { amount ->
                            val selected = numericPool == amount
                            Surface(
                                color = if (selected) ElectricLime else ObsidianContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                                modifier = Modifier
                                    .clickable {
                                        onPrizePoolAmountChange(amount.toString())
                                        onPrizePoolTextChange("৳$amount Sponsored Pool")
                                    }
                                    .testTag("wizard_prize_preset_$amount")
                            ) {
                                Text(
                                    text = "৳$amount",
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Slider(
                        value = numericPool.coerceIn(0, 25000).toFloat(),
                        onValueChange = { sliderVal ->
                            val snapped = ((sliderVal / 500f).toInt() * 500).coerceIn(0, 25000)
                            onPrizePoolAmountChange(snapped.toString())
                            onPrizePoolTextChange(if (snapped > 0) "৳$snapped Sponsored Pool" else "Glory & Champion Badge")
                        },
                        valueRange = 0f..25000f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricLime,
                            activeTrackColor = ElectricLime,
                            inactiveTrackColor = ObsidianContainerHighest
                        )
                    )

                    OutlinedTextField(
                        value = prizePoolAmountText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(7)
                            onPrizePoolAmountChange(clean)
                            val num = clean.toIntOrNull() ?: 0
                            onPrizePoolTextChange(if (num > 0) "৳$num Sponsored Pool" else "Glory & Champion Badge")
                        },
                        label = { Text("Sponsored Pool Amount (৳ BDT)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_prize_amount_input")
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(rewardPresets) { preset ->
                            val selected = prizePoolText == preset
                            Surface(
                                color = if (selected) ElectricLime else ObsidianContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                                modifier = Modifier.clickable { onPrizePoolTextChange(preset) }
                            ) {
                                Text(
                                    text = preset,
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = prizePoolText,
                    onValueChange = { onPrizePoolTextChange(it.take(80)) },
                    label = { Text("Displayed Prize Pool Title", fontSize = 11.sp) },
                    placeholder = { Text("e.g. ৳5,000 + Champion Badge", color = MutedSlate, fontSize = 12.sp) },
                    singleLine = true,
                    colors = wizardTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_prize_pool_input")
                )

                Text("DISTRIBUTION SPLIT", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(splitPresets) { split ->
                        val selected = prizeSplitNote == split
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier.clickable { onPrizeSplitNoteChange(split) }
                        ) {
                            Text(
                                text = split,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Live distribution breakdown preview
                if (isCashPoolMode && numericPool > 0) {
                    val (p1, p2, p3) = when {
                        prizeSplitNote.contains("70%") -> Triple(70, 30, 0)
                        prizeSplitNote.contains("100%") -> Triple(100, 0, 0)
                        else -> Triple(60, 25, 15)
                    }
                    Surface(
                        color = ObsidianContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrizePlaceCard("1st ($p1%)", "৳${(numericPool * p1) / 100}", ElectricLime, Modifier.weight(1f))
                            if (p2 > 0) {
                                PrizePlaceCard("2nd ($p2%)", "৳${(numericPool * p2) / 100}", SlateSecondary, Modifier.weight(1f))
                            }
                            if (p3 > 0) {
                                PrizePlaceCard("3rd ($p3%)", "৳${(numericPool * p3) / 100}", MutedSlate, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizePlaceCard(
    place: String,
    amount: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(color = ObsidianContainer, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(place, color = accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(amount, color = HighClarityWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScheduleStep(
    registrationDeadline: String,
    onRegistrationDeadlineChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    matchDurationText: String,
    onMatchDurationChange: (String) -> Unit,
    roomId: String,
    onRoomIdChange: (String) -> Unit,
    roomPassword: String,
    onRoomPasswordChange: (String) -> Unit
) {
    var pickingForDeadline by remember { mutableStateOf<Boolean?>(null) }

    pickingForDeadline?.let { isDeadline ->
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingForDeadline = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val formatted = fmt.format(Date(millis))
                            if (isDeadline) {
                                onRegistrationDeadlineChange("$formatted • 7:00 PM BST")
                            } else {
                                onStartTimeChange("$formatted • 8:00 PM BST")
                            }
                        }
                        pickingForDeadline = null
                    }
                ) {
                    Text("Apply Date", color = ElectricLime, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingForDeadline = null }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val deadlinePresets = listOf(
        "Tonight 7:00 PM BST",
        "Tomorrow 7:00 PM BST",
        "Friday 8:00 PM BST",
        "48 Hours Before Start"
    )
    val startPresets = listOf(
        "Tonight 8:00 PM BST",
        "Tomorrow 8:00 PM BST",
        "Friday 9:00 PM BST",
        "Weekend 6:00 PM BST"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REGISTRATION DEADLINE *",
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = { pickingForDeadline = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("wizard_pick_deadline_date_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Date", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(deadlinePresets) { preset ->
                        val selected = registrationDeadline == preset
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier.clickable { onRegistrationDeadlineChange(preset) }
                        ) {
                            Text(
                                text = preset,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = registrationDeadline,
                    onValueChange = { onRegistrationDeadlineChange(it.take(60)) },
                    label = { Text("Registration Cut-off Date & Timezone", fontSize = 11.sp) },
                    singleLine = true,
                    colors = wizardTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_deadline_input")
                )

                HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KICKOFF SCHEDULE & DURATION *",
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = { pickingForDeadline = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("wizard_pick_start_date_button")
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Date", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(startPresets) { preset ->
                        val selected = startTime == preset
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricLime else BorderDivider),
                            modifier = Modifier.clickable { onStartTimeChange(preset) }
                        ) {
                            Text(
                                text = preset,
                                color = if (selected) OnElectricLime else SlateSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { onStartTimeChange(it.take(60)) },
                        label = { Text("Tournament Start Time", fontSize = 11.sp) },
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("wizard_start_time_input")
                    )
                    OutlinedTextField(
                        value = matchDurationText,
                        onValueChange = { onMatchDurationChange(it.filter { c -> c.isDigit() }.take(3)) },
                        label = { Text("Mins/Match", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 20, 30, 45).forEach { mins ->
                        val selected = matchDurationText == mins.toString()
                        Surface(
                            color = if (selected) ElectricLime else ObsidianContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMatchDurationChange(mins.toString()) }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${mins}m",
                                    color = if (selected) OnElectricLime else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "OPTIONAL PRE-MATCH ROOM CREDENTIALS",
                    color = SlateSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Share initial custom room credentials now, or add them later per match inside the Lobby.",
                    color = MutedSlate,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomId,
                        onValueChange = { onRoomIdChange(it.take(40)) },
                        label = { Text("Room ID (Optional)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("wizard_room_id_input")
                    )
                    OutlinedTextField(
                        value = roomPassword,
                        onValueChange = { onRoomPasswordChange(it.take(40)) },
                        label = { Text("Password (Optional)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = wizardTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("wizard_room_pass_input")
                    )
                }
            }
        }
    }
}

@Composable
fun WizardReviewStep(
    tournamentTitle: String,
    effectiveGame: String,
    description: String,
    format: TournamentFormat,
    participationType: ParticipationType,
    maxSlots: Int,
    maxTeamSize: Int,
    seedingMode: SeedingMode,
    rules: String,
    prizePoolText: String,
    prizeSplitNote: String,
    registrationDeadline: String,
    startTime: String,
    matchDuration: Int,
    roomId: String,
    onEditStep: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = ElectricLime.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(20.dp))
                Column {
                    Text("Ready to Save Configuration to Firestore", color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Tap Edit on any section below to adjust before publishing.", color = SlateSecondary, fontSize = 11.sp)
                }
            }
        }

        ReviewSectionCard(
            stepLabel = "01 • GAME & TITLE",
            icon = Icons.Default.SportsEsports,
            onEdit = { onEditStep(0) }
        ) {
            Text(tournamentTitle.ifBlank { "Untitled Tournament" }, color = HighClarityWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Discipline: $effectiveGame", color = ElectricLime, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            if (description.isNotBlank()) {
                Text(description, color = SlateSecondary, fontSize = 11.sp)
            }
        }

        ReviewSectionCard(
            stepLabel = "02 • FORMAT & RULES",
            icon = Icons.Default.AccountTree,
            onEdit = { onEditStep(1) }
        ) {
            val squadInfo = if (participationType == ParticipationType.TEAM) " (Squad of $maxTeamSize)" else ""
            Text(
                text = "${format.apiValue} • ${participationType.apiValue}$squadInfo • $maxSlots Slots • ${seedingMode.apiValue} Draw",
                color = HighClarityWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(rules.ifBlank { "Standard fair play rules" }, color = SlateSecondary, fontSize = 11.sp)
        }

        ReviewSectionCard(
            stepLabel = "03 • PRIZE POOL",
            icon = Icons.Default.EmojiEvents,
            onEdit = { onEditStep(2) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(prizePoolText.ifBlank { "Glory & Champion Badge" }, color = ElectricLime, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Surface(color = ObsidianContainerHighest, shape = RoundedCornerShape(6.dp)) {
                    Text("FREE ENTRY", color = ElectricLime, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Text("Split: $prizeSplitNote", color = SlateSecondary, fontSize = 11.sp)
        }

        ReviewSectionCard(
            stepLabel = "04 • DEADLINES & ROOM",
            icon = Icons.Default.Schedule,
            onEdit = { onEditStep(3) }
        ) {
            Text("Registration Deadline: $registrationDeadline", color = HighClarityWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Kickoff: $startTime • $matchDuration mins/match", color = SlateSecondary, fontSize = 11.sp)
            if (roomId.isNotBlank()) {
                Text("Pre-set Room ID: $roomId", color = ElectricLime, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ReviewSectionCard(
    stepLabel: String,
    icon: ImageVector,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = ObsidianContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(15.dp))
                    Text(stepLabel, color = MutedSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = "Edit",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onEdit() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            content()
        }
    }
}
