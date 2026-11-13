package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.model.ParticipationType
import com.example.model.SeedingMode
import com.example.model.TournamentFormat
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CreateTournamentScreen(
    organizerName: String,
    repository: FirestoreTournamentRepository,
    onTournamentCreated: (FirestoreTournament) -> Unit,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    var highestCompletedStep by remember { mutableIntStateOf(-1) }

    // Step 1: Game & Title
    var selectedGame by remember { mutableStateOf("FC Mobile") }
    var customGameName by remember { mutableStateOf("") }
    var customFieldsText by remember { mutableStateOf("Game ID, Team Rating") }
    var tournamentTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Step 2: Format & Rules
    var selectedFormat by remember { mutableStateOf(TournamentFormat.KNOCKOUT) }
    var participationType by remember { mutableStateOf(ParticipationType.INDIVIDUAL) }
    var maxTeamSizeText by remember { mutableStateOf("4") }
    var maxParticipantsText by remember { mutableStateOf("16") }
    var selectedSeedingMode by remember { mutableStateOf(SeedingMode.RANDOM) }
    var rules by remember {
        mutableStateOf("Play fairly, capture end-match screenshot, submit results within 15 minutes.")
    }

    // Step 3: Prize Pool (Organizer-sponsored or Glory, 100% Free Entry)
    var isCashPoolMode by remember { mutableStateOf(true) }
    var prizePoolAmountText by remember { mutableStateOf("5000") }
    var prizePoolText by remember { mutableStateOf("৳5000 Sponsored Pool") }
    var prizeSplitNote by remember { mutableStateOf("1st 60% • 2nd 25% • 3rd 15%") }

    // Step 4: Registration Deadlines & Room
    var registrationDeadline by remember { mutableStateOf("Tomorrow 7:00 PM BST") }
    var startTime by remember { mutableStateOf("Tomorrow 8:00 PM BST") }
    var matchDurationText by remember { mutableStateOf("20") }
    var roomId by remember { mutableStateOf("") }
    var roomPassword by remember { mutableStateOf("") }

    // Publish State
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createdTournament by remember { mutableStateOf<FirestoreTournament?>(null) }

    fun validateStep(step: Int): String? {
        return when (step) {
            0 -> {
                if (tournamentTitle.trim().length < 3) {
                    "Please enter a tournament title (at least 3 characters)."
                } else if (selectedGame == "Custom Game" && customGameName.isBlank()) {
                    "Please specify the custom game title."
                } else null
            }
            1 -> {
                val slots = maxParticipantsText.toIntOrNull()
                val teamSize = maxTeamSizeText.toIntOrNull()
                when {
                    slots == null || slots !in 2..128 -> "Participant capacity must be between 2 and 128."
                    participationType == ParticipationType.TEAM && (teamSize == null || teamSize !in 2..10) ->
                        "Team size must be between 2 and 10 players."
                    rules.trim().length < 5 -> "Please enter match rules (at least 5 characters)."
                    else -> null
                }
            }
            2 -> {
                if (prizePoolText.trim().length < 2) {
                    "Please provide a prize pool description or select a preset."
                } else null
            }
            3 -> {
                val duration = matchDurationText.toIntOrNull()
                when {
                    registrationDeadline.isBlank() -> "Please set a registration deadline."
                    startTime.isBlank() -> "Please set a tournament start time."
                    duration == null || duration !in 5..180 -> "Match duration must be between 5 and 180 minutes."
                    else -> null
                }
            }
            else -> null
        }
    }

    fun goToStep(target: Int) {
        if (target < currentStep) {
            errorMessage = null
            currentStep = target
            return
        }
        for (s in currentStep until target) {
            val err = validateStep(s)
            if (err != null) {
                errorMessage = err
                return
            }
            highestCompletedStep = maxOf(highestCompletedStep, s)
        }
        errorMessage = null
        currentStep = target
    }

    fun publishTournament() {
        for (s in 0..3) {
            val err = validateStep(s)
            if (err != null) {
                errorMessage = err
                currentStep = s
                return
            }
        }
        val titleClean = tournamentTitle.trim()
        val maxSlots = (maxParticipantsText.toIntOrNull() ?: 16).coerceIn(2, 128)
        val teamSize = (maxTeamSizeText.toIntOrNull() ?: 4).coerceIn(2, 10)
        val duration = (matchDurationText.toIntOrNull() ?: 20).coerceIn(5, 180)
        val prizeAmount = if (isCashPoolMode) {
            (prizePoolAmountText.toIntOrNull() ?: 0).coerceIn(0, 10_000_000)
        } else 0
        val customFields = if (selectedGame == "Custom Game") {
            customFieldsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(5)
        } else emptyList()

        isSubmitting = true
        errorMessage = null
        scope.launch {
            val res = repository.createTournament(
                title = titleClean,
                game = selectedGame,
                customGameName = customGameName,
                customRequiredFields = customFields,
                description = description,
                rules = rules,
                format = selectedFormat.apiValue,
                participationType = participationType.apiValue,
                maxTeamSize = teamSize,
                maxParticipants = maxSlots,
                registrationDeadlineText = registrationDeadline,
                startTimeText = startTime,
                matchDurationMinutes = duration,
                seedingMode = selectedSeedingMode.apiValue,
                roomId = roomId,
                roomPassword = roomPassword,
                prizePoolText = prizePoolText,
                prizePoolAmount = prizeAmount,
                prizeSplitNote = prizeSplitNote,
                organizerName = organizerName
            )
            isSubmitting = false
            if (res.isSuccess) {
                createdTournament = res.getOrThrow()
            } else {
                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to save tournament configuration."
            }
        }
    }

    BackHandler(enabled = currentStep > 0) {
        errorMessage = null
        currentStep = (currentStep - 1).coerceAtLeast(0)
    }

    if (createdTournament != null) {
        val created = createdTournament!!
        val deepLink = "tanplay://join/${created.id}"
        AlertDialog(
            onDismissRequest = { onTournamentCreated(created) },
            containerColor = ObsidianContainerLow,
            title = {
                Text("Tournament Published!", color = ElectricLime, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(created.title, color = HighClarityWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Configuration saved to Firestore. Prize Pool: ${created.prizePoolText} • Deadline: ${created.registrationDeadlineText}",
                        color = SlateSecondary,
                        fontSize = 12.sp
                    )
                    Surface(color = ObsidianContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Invite Code: ${created.inviteCode}", color = ElectricLime, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Link: $deepLink", color = MutedSlate, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onTournamentCreated(created) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime)
                ) {
                    Text("Open Bracket", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Tournament Invite", "${created.inviteCode} ($deepLink)"))
                        onShowMessage("Invite code & link copied!")
                    }
                ) {
                    Text("Copy Invite", color = HighClarityWhite)
                }
            }
        )
    }

    val stepMeta = TOURNAMENT_WIZARD_STEPS[currentStep]
    val effectiveGame = if (selectedGame == "Custom Game" && customGameName.isNotBlank()) customGameName else selectedGame

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top Wizard Header & 5-Step Progress Tracker
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (currentStep > 0) {
                                    errorMessage = null
                                    currentStep -= 1
                                } else {
                                    onNavigate(TanScreen.HOME)
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SlateSecondary, modifier = Modifier.size(16.dp))
                        Text(if (currentStep > 0) "Step ${currentStep}" else "Exit Wizard", color = SlateSecondary, fontSize = 12.sp)
                    }

                    Surface(color = ElectricLime.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "STEP ${currentStep + 1} OF ${TOURNAMENT_WIZARD_STEPS.size}",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stepMeta.title,
                        color = HighClarityWhite,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stepMeta.subtitle,
                        color = MutedSlate,
                        fontSize = 12.sp
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / TOURNAMENT_WIZARD_STEPS.size.toFloat() },
                    color = ElectricLime,
                    trackColor = ObsidianContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TOURNAMENT_WIZARD_STEPS) { step ->
                        val isActive = step.stepIndex == currentStep
                        val isCompleted = step.stepIndex < currentStep || step.stepIndex <= highestCompletedStep
                        val clickableStep = step.stepIndex <= currentStep || step.stepIndex <= highestCompletedStep + 1
                        Surface(
                            color = when {
                                isActive -> ElectricLime
                                isCompleted -> ObsidianContainerHigh
                                else -> ObsidianContainerLow
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isActive -> ElectricLime
                                    isCompleted -> ElectricLime.copy(alpha = 0.45f)
                                    else -> BorderDivider
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = clickableStep) { goToStep(step.stepIndex) }
                                .testTag("wizard_step_pill_${step.stepIndex + 1}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isCompleted && !isActive) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(13.dp))
                                } else {
                                    Text(
                                        text = "0${step.stepIndex + 1}",
                                        color = if (isActive) OnElectricLime else MutedSlate,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = step.shortLabel,
                                    color = if (isActive) OnElectricLime else if (isCompleted) HighClarityWhite else SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Animated Step Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { width ->
                            if (forward) width / 5 else -width / 5
                        }) togetherWith (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { width ->
                            if (forward) -width / 6 else width / 6
                        })
                    },
                    label = "wizard_step_transition"
                ) { step ->
                    when (step) {
                        0 -> WizardGameStep(
                            selectedGame = selectedGame,
                            onSelectGame = { selectedGame = it },
                            customGameName = customGameName,
                            onCustomGameNameChange = { customGameName = it },
                            customFieldsText = customFieldsText,
                            onCustomFieldsTextChange = { customFieldsText = it },
                            tournamentTitle = tournamentTitle,
                            onTournamentTitleChange = { tournamentTitle = it },
                            description = description,
                            onDescriptionChange = { description = it }
                        )
                        1 -> WizardFormatRulesStep(
                            selectedFormat = selectedFormat,
                            onSelectFormat = { selectedFormat = it },
                            participationType = participationType,
                            onSelectParticipationType = { participationType = it },
                            maxTeamSizeText = maxTeamSizeText,
                            onMaxTeamSizeChange = { maxTeamSizeText = it },
                            maxParticipantsText = maxParticipantsText,
                            onMaxParticipantsChange = { maxParticipantsText = it },
                            selectedSeedingMode = selectedSeedingMode,
                            onSelectSeedingMode = { selectedSeedingMode = it },
                            rules = rules,
                            onRulesChange = { rules = it }
                        )
                        2 -> WizardPrizePoolStep(
                            isCashPoolMode = isCashPoolMode,
                            onModeChange = { isCashPoolMode = it },
                            prizePoolAmountText = prizePoolAmountText,
                            onPrizePoolAmountChange = { prizePoolAmountText = it },
                            prizePoolText = prizePoolText,
                            onPrizePoolTextChange = { prizePoolText = it },
                            prizeSplitNote = prizeSplitNote,
                            onPrizeSplitNoteChange = { prizeSplitNote = it }
                        )
                        3 -> WizardScheduleStep(
                            registrationDeadline = registrationDeadline,
                            onRegistrationDeadlineChange = { registrationDeadline = it },
                            startTime = startTime,
                            onStartTimeChange = { startTime = it },
                            matchDurationText = matchDurationText,
                            onMatchDurationChange = { matchDurationText = it },
                            roomId = roomId,
                            onRoomIdChange = { roomId = it },
                            roomPassword = roomPassword,
                            onRoomPasswordChange = { roomPassword = it }
                        )
                        else -> WizardReviewStep(
                            tournamentTitle = tournamentTitle,
                            effectiveGame = effectiveGame,
                            description = description,
                            format = selectedFormat,
                            participationType = participationType,
                            maxSlots = (maxParticipantsText.toIntOrNull() ?: 16).coerceIn(2, 128),
                            maxTeamSize = (maxTeamSizeText.toIntOrNull() ?: 4).coerceIn(2, 10),
                            seedingMode = selectedSeedingMode,
                            rules = rules,
                            prizePoolText = prizePoolText,
                            prizeSplitNote = prizeSplitNote,
                            registrationDeadline = registrationDeadline,
                            startTime = startTime,
                            matchDuration = (matchDurationText.toIntOrNull() ?: 20).coerceIn(5, 180),
                            roomId = roomId,
                            onEditStep = { target ->
                                errorMessage = null
                                currentStep = target
                            }
                        )
                    }
                }

                errorMessage?.let { err ->
                    Surface(
                        color = LiveRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = LiveRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // Sticky Bottom Wizard Footer Controls
            Surface(
                color = ObsidianContainerLow,
                border = BorderStroke(1.dp, BorderDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                currentStep -= 1
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderDivider),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("wizard_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back", color = HighClarityWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (currentStep < TOURNAMENT_WIZARD_STEPS.lastIndex) {
                        Button(
                            onClick = {
                                val err = validateStep(currentStep)
                                if (err != null) {
                                    errorMessage = err
                                } else {
                                    errorMessage = null
                                    highestCompletedStep = maxOf(highestCompletedStep, currentStep)
                                    currentStep += 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("wizard_next_button")
                        ) {
                            Text("Next: ${TOURNAMENT_WIZARD_STEPS[currentStep + 1].shortLabel}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                    } else {
                        Button(
                            onClick = { publishTournament() },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("publish_tournament_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnElectricLime, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Publish Tournament", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
