package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.auth.FirebaseAuthService
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.data.UserAlert
import com.example.ui.components.AiCoachDialog
import com.example.ui.components.JoinTournamentDialog
import com.example.ui.components.TanPlayBottomNav
import com.example.ui.components.TanPlayTopBar
import com.example.ui.components.TanScreen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TanPlayTheme {
                MainAuthGate()
            }
        }
    }
}

@Composable
fun MainAuthGate() {
    val context = LocalContext.current
    val authService = remember { FirebaseAuthService(context) }
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        SignInScreen(
            authService = authService,
            onSignInSuccess = { user ->
                currentUser = user
            }
        )
    } else {
        val user = currentUser!!
        key(user.uid) {
            TanPlayApp(
                currentUser = user,
                onSignOut = {
                    authService.signOut(scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
fun TanPlayApp(
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FirestoreTournamentRepository(context) }

    val tournaments by repository.observeTournaments().collectAsState(initial = emptyList())
    val userProfile by repository.observeUserProfile(currentUser.uid).collectAsState(initial = null)
    val alerts by repository.observeAlerts(currentUser.uid).collectAsState(initial = emptyList())
    val playerStats by repository.observePlayerStats().collectAsState(initial = emptyList())

    val isAdminUser = remember(currentUser.email, userProfile?.role) {
        currentUser.email.equals("nmd634384@gmail.com", ignoreCase = true) ||
            userProfile?.role == "admin"
    }
    val unreadAlertsCount = remember(alerts) { alerts.count { !it.isRead } }

    var navigationStack by remember { mutableStateOf(listOf(TanScreen.HOME)) }
    val currentScreen = navigationStack.lastOrNull() ?: TanScreen.HOME

    var isAdminMode by remember { mutableStateOf(false) }
    var selectedTournamentId by remember { mutableStateOf<String?>(null) }
    var tournamentToJoin by remember { mutableStateOf<FirestoreTournament?>(null) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showAiCoachDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    fun navigateTo(screen: TanScreen) {
        if (screen == TanScreen.ADMIN_PANEL && !isAdminUser) {
            showMessage("Admin role required")
            return
        }
        isAdminMode = (screen == TanScreen.ADMIN_PANEL)
        navigationStack = if (screen == TanScreen.HOME) {
            listOf(TanScreen.HOME)
        } else {
            navigationStack.filterNot { it == screen } + screen
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
            isAdminMode = (navigationStack.lastOrNull() == TanScreen.ADMIN_PANEL)
        }
    }

    Scaffold(
        containerColor = ObsidianBase,
        topBar = {
            TanPlayTopBar(
                screenTitle = currentScreen.title,
                unreadAlertsCount = unreadAlertsCount,
                isAdminUser = isAdminUser,
                isAdminMode = isAdminMode,
                onAlertsClick = { showAlertsDialog = true },
                onLeaderboardClick = { navigateTo(TanScreen.LEADERBOARD) },
                onAdminToggle = {
                    if (!isAdminUser) return@TanPlayTopBar
                    if (isAdminMode || currentScreen == TanScreen.ADMIN_PANEL) {
                        isAdminMode = false
                        navigateTo(TanScreen.HOME)
                    } else {
                        isAdminMode = true
                        navigateTo(TanScreen.ADMIN_PANEL)
                    }
                },
                onAiCoachClick = { showAiCoachDialog = true }
            )
        },
        bottomBar = {
            TanPlayBottomNav(
                currentScreen = currentScreen,
                hasActiveMatch = tournaments.any { it.status == "In Progress" },
                onNavigate = { navigateTo(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E2428),
                    contentColor = Color.White,
                    actionColor = ElectricLime
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBase)
        ) {
            when (currentScreen) {
                TanScreen.HOME -> HomeScreen(
                    tournaments = tournaments,
                    userProfile = userProfile,
                    currentUserId = currentUser.uid,
                    repository = repository,
                    onOpenJoinDialog = { t -> tournamentToJoin = t },
                    onOpenBracket = { id -> selectedTournamentId = id },
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )

                TanScreen.TOURNAMENT_DASHBOARD -> TournamentDashboardScreen(
                    tournaments = tournaments,
                    currentUserId = currentUser.uid,
                    userProfile = userProfile,
                    isAdminUser = isAdminUser,
                    repository = repository,
                    onOpenJoinDialog = { t -> tournamentToJoin = t },
                    onOpenBracket = { id -> selectedTournamentId = id },
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )

                TanScreen.EXPLORE -> ExploreScreen(
                    tournaments = tournaments,
                    currentUserId = currentUser.uid,
                    onOpenJoinDialog = { t -> tournamentToJoin = t },
                    onOpenBracket = { id -> selectedTournamentId = id },
                    onNavigate = { navigateTo(it) }
                )

                TanScreen.MY_GAMES -> MyGamesScreen(
                    tournaments = tournaments,
                    selectedTournamentId = selectedTournamentId,
                    onSelectTournament = { selectedTournamentId = it },
                    currentUserId = currentUser.uid,
                    userProfile = userProfile,
                    isAdminUser = isAdminUser,
                    repository = repository,
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )

                TanScreen.LEADERBOARD -> LeaderboardScreen(
                    playerStats = playerStats,
                    currentUserId = currentUser.uid
                )

                TanScreen.PROFILE -> ProfileScreen(
                    userProfile = userProfile,
                    currentUserId = currentUser.uid,
                    userDisplayName = currentUser.displayName,
                    userEmail = currentUser.email,
                    isAdminUser = isAdminUser,
                    repository = repository,
                    onNavigate = { navigateTo(it) },
                    onAdminToggle = {
                        if (isAdminUser) navigateTo(TanScreen.ADMIN_PANEL)
                    },
                    onSignOut = onSignOut,
                    onShowMessage = ::showMessage
                )

                TanScreen.BRACKET -> BracketScreen(
                    tournaments = tournaments,
                    selectedTournamentId = selectedTournamentId,
                    onSelectTournament = { selectedTournamentId = it },
                    currentUserId = currentUser.uid,
                    userProfile = userProfile,
                    isAdminUser = isAdminUser,
                    repository = repository,
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )

                TanScreen.CREATE_TOURNAMENT -> CreateTournamentScreen(
                    organizerName = userProfile?.username?.ifBlank { null }
                        ?: currentUser.displayName
                        ?: "Organizer",
                    repository = repository,
                    onTournamentCreated = { created ->
                        selectedTournamentId = created.id
                        showMessage("Created ${created.title} (${created.inviteCode})")
                        navigateTo(TanScreen.BRACKET)
                    },
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )

                TanScreen.ADMIN_PANEL -> AdminDashboardScreen(
                    tournaments = tournaments,
                    currentUserId = currentUser.uid,
                    isAdminUser = isAdminUser,
                    repository = repository,
                    onOpenBracket = { id -> selectedTournamentId = id },
                    onNavigate = { navigateTo(it) },
                    onShowMessage = ::showMessage
                )
            }

            // Global Join Tournament Dialog (from Home / Dashboard / Explore)
            tournamentToJoin?.let { targetTournament ->
                val participants by repository.observeParticipants(targetTournament.id).collectAsState(initial = emptyList())
                val teams by repository.observeTeams(targetTournament.id).collectAsState(initial = emptyList())
                val existing = participants.find { it.userId == currentUser.uid }
                JoinTournamentDialog(
                    tournament = targetTournament,
                    userProfile = userProfile,
                    existingParticipant = existing,
                    teams = teams,
                    repository = repository,
                    onDismiss = { tournamentToJoin = null },
                    onSuccess = { msg ->
                        tournamentToJoin = null
                        selectedTournamentId = targetTournament.id
                        showMessage(msg)
                        navigateTo(TanScreen.BRACKET)
                    }
                )
            }

            // In-App Alerts Dialog
            if (showAlertsDialog) {
                InAppAlertsDialog(
                    alerts = alerts,
                    onAlertClick = { alert ->
                        scope.launch {
                            repository.markAlertRead(currentUser.uid, alert.id)
                        }
                        if (alert.tournamentId.isNotBlank()) {
                            selectedTournamentId = alert.tournamentId
                            showAlertsDialog = false
                            navigateTo(TanScreen.BRACKET)
                        }
                    },
                    onDismiss = { showAlertsDialog = false }
                )
            }

            // AI Strategy Coach & Rules Assistant modal
            if (showAiCoachDialog) {
                AiCoachDialog(
                    onDismiss = { showAiCoachDialog = false }
                )
            }
        }
    }
}

@Composable
private fun InAppAlertsDialog(
    alerts: List<UserAlert>,
    onAlertClick: (UserAlert) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp))
                        Text("IN-APP ALERTS", color = ElectricLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateSecondary)
                    }
                }

                if (alerts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No notifications yet.", color = MutedSlate, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(alerts, key = { it.id }) { alert ->
                            Surface(
                                color = if (alert.isRead) ObsidianContainer else ObsidianContainerHigh,
                                shape = RoundedCornerShape(10.dp),
                                border = if (!alert.isRead) BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAlertClick(alert) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(alert.title, color = HighClarityWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (!alert.isRead) {
                                            Text("NEW", color = ElectricLime, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Text(alert.message, color = SlateSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
