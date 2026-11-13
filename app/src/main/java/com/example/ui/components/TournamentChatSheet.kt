package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
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
import androidx.compose.ui.window.Dialog
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.data.TournamentChatMessage
import com.example.data.TournamentTeam
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TournamentChatDialog(
    tournament: FirestoreTournament,
    teams: List<TournamentTeam>,
    currentUserId: String,
    currentUserName: String,
    isAdminUser: Boolean,
    repository: FirestoreTournamentRepository,
    onDismiss: () -> Unit,
    onActionMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by repository.observeMessages(tournament.id).collectAsState(initial = emptyList())
    val mutedUserIds = remember { mutableStateListOf<String>() }

    val isOrganizer = tournament.organizerId == currentUserId
    val captainTeam = remember(teams, currentUserId) {
        teams.firstOrNull { it.captainUserId == currentUserId }
    }
    val memberTeam = remember(teams, currentUserId, captainTeam) {
        captainTeam ?: teams.firstOrNull { currentUserId in it.memberIds }
    }
    val isCaptainOrHost = isOrganizer || isAdminUser || captainTeam != null

    val currentUserRole = remember(isOrganizer, isAdminUser, captainTeam) {
        when {
            isOrganizer -> "organizer"
            isAdminUser -> "admin"
            captainTeam != null -> "captain"
            else -> "player"
        }
    }
    val currentUserTeamName = memberTeam?.name.orEmpty()

    // Default to Captains & Host channel for captains/organizers in team tournaments or when captain exists
    var selectedChannel by remember(tournament.id) {
        mutableStateOf(
            if (isCaptainOrHost && (tournament.participationType == "Team" || teams.isNotEmpty())) {
                "captains"
            } else {
                "lobby"
            }
        )
    }
    var messageInput by remember { mutableStateOf("") }
    var pinNextMessage by remember { mutableStateOf(false) }
    var lastSentMs by remember { mutableLongStateOf(0L) }
    var cooldownNotice by remember { mutableStateOf<String?>(null) }

    val visibleAllMessages = remember(messages, mutedUserIds.size) {
        messages.filterNot { it.senderId in mutedUserIds }
    }
    val lobbyMessages = remember(visibleAllMessages) {
        visibleAllMessages.filter { it.channel != "captains" }
    }
    val captainMessages = remember(visibleAllMessages) {
        visibleAllMessages.filter { it.channel == "captains" }
    }
    val activeMessages = if (selectedChannel == "captains") captainMessages else lobbyMessages
    val pinnedAnnouncement = remember(activeMessages) {
        activeMessages.lastOrNull { it.isPinned }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeMessages.size, selectedChannel) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.lastIndex)
        }
    }

    val canPostInCurrentChannel = selectedChannel == "lobby" || isCaptainOrHost

    fun dispatchMessage(raw: String, asPinned: Boolean = pinNextMessage) {
        val now = System.currentTimeMillis()
        if (now - lastSentMs < 3000L) {
            cooldownNotice = "Please wait 3 seconds between messages."
            return
        }
        val clean = raw.trim()
        if (clean.isEmpty()) return
        messageInput = ""
        cooldownNotice = null
        lastSentMs = now
        val shouldPin = asPinned && (isOrganizer || isAdminUser)
        if (asPinned) pinNextMessage = false
        scope.launch {
            val res = repository.sendChatMessage(
                tournamentId = tournament.id,
                senderName = currentUserName,
                rawText = clean,
                senderRole = currentUserRole,
                teamName = currentUserTeamName,
                channel = selectedChannel,
                isPinned = shouldPin
            )
            if (res.isFailure) {
                onActionMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to send message")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 430.dp, max = 600.dp)
                .testTag("tournament_chat_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Live Firestore indicator and User Role Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ElectricLime)
                            )
                            Text(
                                text = "TOURNAMENT LOBBY CHAT • LIVE",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = tournament.title,
                            color = HighClarityWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append("Signed in as ")
                                append(
                                    when (currentUserRole) {
                                        "organizer" -> "Tournament Organizer"
                                        "admin" -> "Admin Moderator"
                                        "captain" -> "Captain ($currentUserTeamName)"
                                        else -> "Participant"
                                    }
                                )
                            },
                            color = MutedSlate,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_chat_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = SlateSecondary)
                    }
                }

                // Channel Tabs: Captains & Host vs All Lobby
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChannelTabButton(
                        label = "Captains & Host (${captainMessages.size})",
                        selected = selectedChannel == "captains",
                        onClick = { selectedChannel = "captains" },
                        modifier = Modifier.weight(1f).testTag("chat_tab_captains")
                    )
                    ChannelTabButton(
                        label = "All Lobby (${lobbyMessages.size})",
                        selected = selectedChannel == "lobby",
                        onClick = { selectedChannel = "lobby" },
                        modifier = Modifier.weight(1f).testTag("chat_tab_lobby")
                    )
                }

                // Pinned Organizer Announcement
                pinnedAnnouncement?.let { pinned ->
                    Surface(
                        color = ElectricLime.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth().testTag("pinned_chat_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(15.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PINNED BY ${pinned.senderName.uppercase()}",
                                    color = ElectricLime,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(pinned.text, color = HighClarityWhite, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (isOrganizer || isAdminUser) {
                                IconButton(
                                    onClick = {
                                        scope.launch { repository.togglePinChatMessage(tournament.id, pinned) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Unpin message", tint = SlateSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDivider)

                // Real-time Messages Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedChannel == "captains") {
                                        "No captain/organizer messages yet. Coordinate check-ins, lineups, or room info here."
                                    } else {
                                        "No lobby messages yet. Coordinate match times cleanly!"
                                    },
                                    color = MutedSlate,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        items(activeMessages, key = { it.id }) { msg ->
                            ChatBubble(
                                message = msg,
                                isMe = msg.senderId == currentUserId,
                                canPin = isOrganizer || isAdminUser,
                                onTogglePin = {
                                    scope.launch {
                                        repository.togglePinChatMessage(tournament.id, msg)
                                    }
                                },
                                onMute = {
                                    if (msg.senderId !in mutedUserIds) {
                                        mutedUserIds.add(msg.senderId)
                                        onActionMessage("Muted ${msg.senderName} for this session.")
                                    }
                                },
                                onReport = {
                                    scope.launch {
                                        repository.reportChatMessage(tournament.id, msg, "Reported in tournament lobby chat")
                                        onActionMessage("Message reported to moderators.")
                                    }
                                }
                            )
                        }
                    }
                }

                // Quick Coordination Chips for Captains & Organizers
                if (isCaptainOrHost && canPostInCurrentChannel) {
                    val quickPrompts = if (isOrganizer) {
                        listOf("Captains check in ✅", "Bracket draw in 5m", "Room credentials updated", "Submit proof after match")
                    } else {
                        listOf("Team ready for check-in ✅", "Requesting room ID", "Roster locked", "Need organizer help")
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickPrompts) { prompt ->
                            Surface(
                                color = ObsidianContainer,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderDivider),
                                modifier = Modifier.clickable { dispatchMessage(prompt, asPinned = false) }
                            ) {
                                Text(
                                    text = prompt,
                                    color = SlateSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                cooldownNotice?.let { notice ->
                    Text(notice, color = LiveRed, fontSize = 10.sp)
                }

                if (!canPostInCurrentChannel) {
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
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Captains & Host channel is reserved for Team Captains and the Tournament Organizer. Switch to All Lobby to chat.",
                                color = MutedSlate,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isOrganizer || isAdminUser) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { pinNextMessage = !pinNextMessage }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin announcement toggle",
                                    tint = if (pinNextMessage) ElectricLime else MutedSlate,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (pinNextMessage) "Sending as Pinned Announcement" else "Pin next message as announcement",
                                    color = if (pinNextMessage) ElectricLime else MutedSlate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it.take(280) },
                                placeholder = {
                                    Text(
                                        text = if (selectedChannel == "captains") "Message captains & organizer..." else "Message tournament lobby...",
                                        color = MutedSlate,
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HighClarityWhite,
                                    unfocusedTextColor = HighClarityWhite,
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = BorderDivider
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("chat_message_input")
                            )

                            IconButton(
                                onClick = { dispatchMessage(messageInput) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ElectricLime, RoundedCornerShape(12.dp))
                                    .testTag("send_chat_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = OnElectricLime)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ElectricLime else ObsidianContainer)
            .border(1.dp, if (selected) ElectricLime else BorderDivider, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) OnElectricLime else SlateSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatBubble(
    message: TournamentChatMessage,
    isMe: Boolean,
    canPin: Boolean,
    onTogglePin: () -> Unit,
    onMute: () -> Unit,
    onReport: () -> Unit
) {
    val timeLabel = remember(message.createdAt) {
        val seconds = message.createdAt?.seconds
        if (seconds == null) "Sending..."
        else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(seconds * 1000L))
    }

    val roleBadgeText = when (message.senderRole) {
        "organizer" -> "HOST"
        "admin" -> "ADMIN"
        "captain" -> if (message.teamName.isNotBlank()) "CAPTAIN • ${message.teamName}" else "CAPTAIN"
        else -> if (message.teamName.isNotBlank()) message.teamName else null
    }
    val roleColor = when (message.senderRole) {
        "organizer" -> ElectricLime
        "admin" -> LiveRed
        "captain" -> EmeraldTertiary
        else -> SlateSecondary
    }

    Surface(
        color = if (isMe) ElectricLime.copy(alpha = 0.14f) else ObsidianContainer,
        shape = RoundedCornerShape(12.dp),
        border = if (message.isPinned) BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f)) else null,
        modifier = Modifier.fillMaxWidth().testTag("chat_message_${message.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isMe) "${message.senderName} (You)" else message.senderName,
                        color = if (isMe) ElectricLime else HighClarityWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (roleBadgeText != null) {
                        Surface(
                            color = roleColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = roleBadgeText.uppercase(),
                                color = roleColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(timeLabel, color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    if (canPin) {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (message.isPinned) "Unpin message" else "Pin message",
                                tint = if (message.isPinned) ElectricLime else MutedSlate,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (!isMe) {
                        IconButton(onClick = onMute, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute user", tint = MutedSlate, modifier = Modifier.size(13.dp))
                        }
                        IconButton(onClick = onReport, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Flag, contentDescription = "Report message", tint = if (message.isReported) LiveRed else MutedSlate, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            Text(
                text = if (message.isReported) "[Flagged for moderation] ${message.text}" else message.text,
                color = HighClarityWhite,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
