package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BracketEngine
import com.example.data.FirestoreTournamentRepository
import com.example.data.UserProfile
import com.example.ui.components.TanScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    isAdminUser: Boolean,
    repository: FirestoreTournamentRepository,
    onNavigate: (TanScreen) -> Unit,
    onAdminToggle: () -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    val displayUsername = userProfile?.username?.ifBlank { null }
        ?: userDisplayName?.ifBlank { null }
        ?: "Player_${currentUserId.take(5)}"
    val avatarInitial = (userProfile?.avatarInitial?.firstOrNull() ?: displayUsername.firstOrNull() ?: 'T')
        .uppercaseChar().toString()
    val roleLabel = if (isAdminUser) "ADMIN" else (userProfile?.role ?: "player").uppercase()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Identity Card
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_identity_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarInitial,
                                color = ElectricLime,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(color = ObsidianContainerHighest, shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = ElectricLime,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = roleLabel,
                                        color = ElectricLime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Surface(color = ObsidianContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = userProfile?.primaryGame ?: "FC Mobile",
                                    color = SlateSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = displayUsername,
                            color = HighClarityWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!userProfile?.displayName.isNullOrBlank() && userProfile?.displayName != displayUsername) {
                            Text(
                                text = userProfile?.displayName.orEmpty(),
                                color = SlateSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userProfile?.email?.ifBlank { userEmail.orEmpty() } ?: userEmail.orEmpty(),
                                color = MutedSlate,
                                fontSize = 12.sp
                            )
                            Text("•", color = MutedSlate, fontSize = 10.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Player UID", currentUserId))
                                        onShowMessage("Account UID copied")
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("copy_uid_button")
                            ) {
                                Text(
                                    text = "UID: ${currentUserId.take(8)}",
                                    color = SlateSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Account UID",
                                    tint = ElectricLime,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile & Game IDs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Saved Game IDs Overview (masked for privacy)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SAVED GAME IDS (AUTO-FILLS TOURNAMENTS)",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SavedGameIdRow("Free Fire", userProfile?.freeFireUid.orEmpty(), userProfile?.freeFireIgn.orEmpty())
                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)
                        SavedGameIdRow("FC Mobile", userProfile?.fcMobileUid.orEmpty(), userProfile?.fcMobileOvr?.takeIf { it.isNotBlank() }?.let { "OVR $it" }.orEmpty())
                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)
                        SavedGameIdRow("eFootball", userProfile?.efootballId.orEmpty(), userProfile?.efootballPlatform.orEmpty())
                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)
                        SavedGameIdRow("DLS", userProfile?.dlsTeamName.orEmpty(), userProfile?.dlsTeamRating?.takeIf { it.isNotBlank() }?.let { "Rating $it" }.orEmpty())
                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)
                        SavedGameIdRow("PUBG Mobile", userProfile?.pubgPlayerId.orEmpty(), userProfile?.pubgNickname.orEmpty())
                    }
                }
            }
        }

        // Privacy, Legal & Data Portability Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIVACY, TERMS & DATA CONTROLS",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("privacy_terms_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Privacy Policy & Terms", color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("100% Free Play • Masked Public Game IDs", color = MutedSlate, fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = if (userProfile?.termsAccepted != false) "Accepted" else "Review",
                                color = ElectricLime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isWorking) {
                                    isWorking = true
                                    scope.launch {
                                        val res = repository.exportUserDataJson(currentUserId)
                                        isWorking = false
                                        res.onSuccess { json -> exportedJsonText = json }
                                        res.onFailure { onShowMessage(it.localizedMessage ?: "Export failed") }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("export_data_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Export My Data (JSON)", color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Download profile, competitive rating & alerts", color = MutedSlate, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedSlate)
                        }

                        HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isWorking) { showDeleteConfirmDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("delete_data_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LiveRed, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Delete My Account Data", color = LiveRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Erase profile, saved game IDs & alerts", color = MutedSlate, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LiveRed)
                        }
                    }
                }
            }
        }

        // Admin Console Access Button (strictly role-gated)
        if (isAdminUser) {
            item {
                Button(
                    onClick = onAdminToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_admin_console_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianContainerHigh,
                        contentColor = ElectricLime
                    )
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Admin & Moderation Console", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Sign Out Button
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialProfile = userProfile ?: UserProfile(
                userId = currentUserId,
                username = displayUsername,
                email = userEmail.orEmpty(),
                displayName = userDisplayName ?: displayUsername
            ),
            repository = repository,
            onDismiss = { showEditProfileDialog = false },
            onSaved = {
                showEditProfileDialog = false
                onShowMessage("Profile & Game IDs saved")
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            termsAccepted = userProfile?.termsAccepted ?: true,
            onAcceptAndSave = { accepted ->
                scope.launch {
                    val base = userProfile ?: UserProfile(
                        userId = currentUserId,
                        username = displayUsername,
                        email = userEmail.orEmpty()
                    )
                    repository.updateUserProfileDetails(base.copy(termsAccepted = accepted))
                    showPrivacyDialog = false
                    onShowMessage("Privacy preferences updated")
                }
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    exportedJsonText?.let { json ->
        Dialog(onDismissRequest = { exportedJsonText = null }) {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EXPORTED ACCOUNT DATA (JSON)", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Surface(
                        color = ObsidianContainerLowest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        Text(
                            text = json,
                            color = HighClarityWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportedJsonText = null },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Close", color = SlateSecondary)
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TAN PLAY Data", json))
                                onShowMessage("JSON copied to clipboard")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Copy JSON", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ObsidianContainerLow,
            title = { Text("Delete Account Data?", color = LiveRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This deletes your profile, saved Game IDs, competitive leaderboard stats, and alerts from TAN PLAY, then signs you out.",
                    color = SlateSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isWorking = true
                        scope.launch {
                            val res = repository.deleteAccountData(currentUserId)
                            isWorking = false
                            if (res.isSuccess) {
                                onShowMessage("Account data deleted")
                                onSignOut()
                            } else {
                                onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Delete failed")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = HighClarityWhite),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete & Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }
}

@Composable
private fun SavedGameIdRow(game: String, idValue: String, secondaryValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(game, color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (secondaryValue.isNotBlank()) {
                Text(secondaryValue, color = MutedSlate, fontSize = 11.sp)
            }
        }
        Text(
            text = if (idValue.isBlank()) "Not set" else BracketEngine.maskGameId(idValue),
            color = if (idValue.isBlank()) MutedSlate else ElectricLime,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialProfile: UserProfile,
    repository: FirestoreTournamentRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(initialProfile.username) }
    var displayName by remember { mutableStateOf(initialProfile.displayName) }
    var contactInfo by remember { mutableStateOf(initialProfile.contactInfo) }
    var primaryGame by remember { mutableStateOf(initialProfile.primaryGame.ifBlank { "FC Mobile" }) }
    var freeFireUid by remember { mutableStateOf(initialProfile.freeFireUid) }
    var freeFireIgn by remember { mutableStateOf(initialProfile.freeFireIgn) }
    var freeFireLevel by remember { mutableStateOf(initialProfile.freeFireLevel) }
    var fcMobileUid by remember { mutableStateOf(initialProfile.fcMobileUid) }
    var fcMobileOvr by remember { mutableStateOf(initialProfile.fcMobileOvr) }
    var efootballId by remember { mutableStateOf(initialProfile.efootballId) }
    var efootballPlatform by remember { mutableStateOf(initialProfile.efootballPlatform.ifBlank { "Android" }) }
    var dlsTeamName by remember { mutableStateOf(initialProfile.dlsTeamName) }
    var dlsTeamRating by remember { mutableStateOf(initialProfile.dlsTeamRating) }
    var pubgPlayerId by remember { mutableStateOf(initialProfile.pubgPlayerId) }
    var pubgNickname by remember { mutableStateOf(initialProfile.pubgNickname) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EDIT PROFILE & GAME IDS", color = ElectricLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateSecondary)
                    }
                }

                ProfileTextField("Gamer Tag / Username", username, { username = it.take(30) }, testTag = "edit_username_input")
                ProfileTextField("Display Name", displayName, { displayName = it.take(40) })
                ProfileTextField("Discord / Contact Handle (optional)", contactInfo, { contactInfo = it.take(40) })

                Text("PRIMARY GAME", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("FC Mobile", "Free Fire", "eFootball").forEach { g ->
                        val selected = primaryGame == g
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ElectricLime else ObsidianContainer)
                                .clickable { primaryGame = g }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(g, color = if (selected) OnElectricLime else SlateSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = BorderDivider)
                Text("FREE FIRE", color = ElectricLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                ProfileTextField("Free Fire UID (numeric)", freeFireUid, { freeFireUid = it.filter(Char::isDigit).take(16) }, numeric = true)
                ProfileTextField("Free Fire In-Game Name (IGN)", freeFireIgn, { freeFireIgn = it.take(30) })
                ProfileTextField("Free Fire Level", freeFireLevel, { freeFireLevel = it.filter(Char::isDigit).take(3) }, numeric = true)

                Text("FC MOBILE", color = ElectricLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                ProfileTextField("FC Mobile Account UID", fcMobileUid, { fcMobileUid = it.filter(Char::isDigit).take(16) }, numeric = true)
                ProfileTextField("Team OVR", fcMobileOvr, { fcMobileOvr = it.filter(Char::isDigit).take(3) }, numeric = true)

                Text("EFOOTBALL", color = ElectricLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                ProfileTextField("eFootball Online ID", efootballId, { efootballId = it.take(24) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Android", "iOS", "Console").forEach { plat ->
                        val sel = efootballPlatform == plat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) ElectricLime else ObsidianContainer)
                                .border(1.dp, if (sel) ElectricLime else BorderDivider, RoundedCornerShape(8.dp))
                                .clickable { efootballPlatform = plat }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(plat, color = if (sel) OnElectricLime else SlateSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("DLS & PUBG MOBILE", color = ElectricLime, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                ProfileTextField("DLS Team Name", dlsTeamName, { dlsTeamName = it.take(30) })
                ProfileTextField("DLS Team Rating", dlsTeamRating, { dlsTeamRating = it.filter(Char::isDigit).take(3) }, numeric = true)
                ProfileTextField("PUBG Mobile Player ID", pubgPlayerId, { pubgPlayerId = it.filter(Char::isDigit).take(16) }, numeric = true)
                ProfileTextField("PUBG Nickname", pubgNickname, { pubgNickname = it.take(30) })

                errorMsg?.let { Text(it, color = LiveRed, fontSize = 11.sp) }

                Button(
                    onClick = {
                        if (username.isBlank()) {
                            errorMsg = "Gamer Tag cannot be empty."
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val updated = initialProfile.copy(
                                username = username.trim(),
                                displayName = displayName.trim().ifEmpty { username.trim() },
                                contactInfo = contactInfo.trim(),
                                primaryGame = primaryGame,
                                freeFireUid = freeFireUid.trim(),
                                freeFireIgn = freeFireIgn.trim(),
                                freeFireLevel = freeFireLevel.trim(),
                                fcMobileUid = fcMobileUid.trim(),
                                fcMobileOvr = fcMobileOvr.trim(),
                                efootballId = efootballId.trim(),
                                efootballPlatform = efootballPlatform,
                                dlsTeamName = dlsTeamName.trim(),
                                dlsTeamRating = dlsTeamRating.trim(),
                                pubgPlayerId = pubgPlayerId.trim(),
                                pubgNickname = pubgNickname.trim()
                            )
                            val res = repository.updateUserProfileDetails(updated)
                            isSaving = false
                            if (res.isSuccess) onSaved() else errorMsg = res.exceptionOrNull()?.localizedMessage ?: "Failed to save"
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnElectricLime, strokeWidth = 2.dp)
                    } else {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean = false,
    testTag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HighClarityWhite,
            unfocusedTextColor = HighClarityWhite,
            focusedBorderColor = ElectricLime,
            unfocusedBorderColor = BorderDivider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
private fun PrivacyPolicyDialog(
    termsAccepted: Boolean,
    onAcceptAndSave: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var accepted by remember { mutableStateOf(termsAccepted) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("PRIVACY NOTICE & TERMS", color = ElectricLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(
                    text = "• 100% Free Tournaments: TAN PLAY is strictly for skill-based free eSports brackets. No entry fees, wallets, gambling, or monetary escrow exist.\n\n" +
                        "• Game ID Protection: Sensitive game IDs (Free Fire UID, FC Mobile UID, eFootball Online ID, PUBG Player ID) are masked publicly (••••1234) and revealed only inside the Match Lobby once both opponents click Ready.\n\n" +
                        "• Audit & Fair Play: Participant edits before deadline and match score confirmations maintain an immutable audit trail. Disputes can be reviewed by tournament organizers or platform admins.\n\n" +
                        "• Data Rights: You can export your account data as JSON or delete your account data at any time from Profile.",
                    color = SlateSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { accepted = !accepted }
                ) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = { accepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricLime, checkmarkColor = OnElectricLime)
                    )
                    Text("I accept TAN PLAY's Privacy Notice & Fair-Play Terms", color = HighClarityWhite, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Close", color = SlateSecondary)
                    }
                    Button(
                        onClick = { onAcceptAndSave(accepted) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
