package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.FirebaseAuthService
import com.example.data.FirestoreTournamentRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    SIGN_UP
}

@Composable
fun SignInScreen(
    authService: FirebaseAuthService,
    onSignInSuccess: (FirebaseUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity
    val tournamentRepo = remember { FirestoreTournamentRepository(context) }

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var gamerTag by remember { mutableStateOf("") }
    var selectedPrimaryGame by remember { mutableStateOf("FC Mobile") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val gamesList = listOf("FC Mobile", "eFootball", "Free Fire", "PUBG Mobile", "DLS")

    // Attempt silent auto sign-in once when the screen launches (for returning users)
    LaunchedEffect(Unit) {
        authService.attemptAutoSignIn(
            scope = coroutineScope,
            onAuthSuccess = { user ->
                onSignInSuccess(user)
            },
            onUnauthenticated = {
                // Stay on SignInScreen for user to interact
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding & Logo
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricLime, ElectricLimeDim)
                        )
                    )
                    .border(2.dp, HighClarityWhite.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TP",
                    color = OnElectricLime,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TAN PLAY",
                        color = HighClarityWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricLime)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ESPORTS",
                            color = OnElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = if (authMode == AuthMode.LOGIN)
                        "Sign in to access Free Tournaments, Live Brackets & Match Lobbies"
                    else
                        "Create your Gamer Profile & Compete in Free eSports Brackets",
                    color = SlateSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Dual Flow Segmented Tabs (Log In vs Sign Up)
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Log In Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (authMode == AuthMode.LOGIN) ElectricLime else Color.Transparent)
                            .clickable {
                                authMode = AuthMode.LOGIN
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("auth_tab_login"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = if (authMode == AuthMode.LOGIN) OnElectricLime else SlateSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LOG IN",
                                color = if (authMode == AuthMode.LOGIN) OnElectricLime else SlateSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Sign Up Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (authMode == AuthMode.SIGN_UP) ElectricLime else Color.Transparent)
                            .clickable {
                                authMode = AuthMode.SIGN_UP
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("auth_tab_signup"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (authMode == AuthMode.SIGN_UP) OnElectricLime else SlateSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SIGN UP",
                                color = if (authMode == AuthMode.SIGN_UP) OnElectricLime else SlateSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Flow-Specific Form Content
            AnimatedVisibility(
                visible = authMode == AuthMode.SIGN_UP,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PLAYER IDENTITY SETUP",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        // Gamer Tag / IGN Input
                        OutlinedTextField(
                            value = gamerTag,
                            onValueChange = { gamerTag = it.take(20) },
                            label = { Text("Competitive Gamer Tag / IGN", fontSize = 11.sp) },
                            placeholder = { Text("e.g. TanvirX, DhakaSniper", color = MutedSlate, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = ElectricLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HighClarityWhite,
                                unfocusedTextColor = HighClarityWhite,
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = BorderDivider,
                                focusedContainerColor = ObsidianContainerLowest,
                                unfocusedContainerColor = ObsidianContainerLowest
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gamer_tag_input")
                        )

                        // Primary Game Selector
                        Text(
                            text = "PRIMARY ESPORTS TITLE",
                            color = MutedSlate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(gamesList) { game ->
                                val isSelected = selectedPrimaryGame == game
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricLime else ObsidianContainerLowest)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricLime else BorderDivider,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedPrimaryGame = game }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = game,
                                        color = if (isSelected) OnElectricLime else SlateSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Terms Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreeToTerms = !agreeToTerms }
                        ) {
                            Checkbox(
                                checked = agreeToTerms,
                                onCheckedChange = { agreeToTerms = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ElectricLime,
                                    checkmarkColor = OnElectricLime
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I agree to the Fair Play protocol, Privacy Policy, and Terms of Service",
                                color = SlateSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = authMode == AuthMode.LOGIN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // Platform Highlights Card for Login
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HighlightItem(
                            icon = Icons.Default.EmojiEvents,
                            iconTint = ElectricLime,
                            title = "100% Free Tournaments",
                            subtitle = "Zero entry fees — create custom cups & invite friends"
                        )
                        HighlightItem(
                            icon = Icons.Default.AccountTree,
                            iconTint = EmeraldTertiary,
                            title = "Real-Time Visual Brackets",
                            subtitle = "Random, Seeded & Manual draws mapped live to Firestore"
                        )
                        HighlightItem(
                            icon = Icons.Default.VerifiedUser,
                            iconTint = ElectricLime,
                            title = "ID Privacy & Fair Play",
                            subtitle = "Game IDs stay masked until both players click Ready"
                        )
                    }
                }
            }

            // Error Message Display
            errorMessage?.let { msg ->
                Surface(
                    color = ErrorContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = OnErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            color = OnErrorContainer,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Primary Google Action Button (Handles both Login & Sign-Up)
            Button(
                onClick = {
                    if (activity != null && !isLoading) {
                        if (authMode == AuthMode.SIGN_UP && !agreeToTerms) {
                            errorMessage = "Please accept the Fair Play terms to create an account"
                            return@Button
                        }
                        if (authMode == AuthMode.SIGN_UP && gamerTag.isBlank()) {
                            errorMessage = "Please enter a valid competitive Gamer Tag"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        authService.signInWithGoogle(
                            activity = activity,
                            scope = coroutineScope,
                            onAuthSuccess = { user ->
                                coroutineScope.launch {
                                    val resolvedTag = if (authMode == AuthMode.SIGN_UP && gamerTag.isNotBlank()) {
                                        gamerTag.trim()
                                    } else {
                                        user.displayName?.take(20) ?: "Player_${user.uid.take(5)}"
                                    }
                                    tournamentRepo.syncGamerProfile(
                                        userId = user.uid,
                                        email = user.email,
                                        displayName = user.displayName,
                                        gamerTag = resolvedTag,
                                        primaryGame = selectedPrimaryGame,
                                        termsAccepted = agreeToTerms
                                    )
                                    isLoading = false
                                    onSignInSuccess(user)
                                }
                            },
                            onAuthError = { errorMsg ->
                                isLoading = false
                                errorMessage = errorMsg
                            },
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricLime,
                    disabledContainerColor = ElectricLime.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_auth_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnElectricLime,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (authMode == AuthMode.SIGN_UP) "Creating Gamer Profile..." else "Authenticating...",
                        color = OnElectricLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = if (authMode == AuthMode.SIGN_UP) Icons.Default.PersonAdd else Icons.Default.Security,
                        contentDescription = null,
                        tint = OnElectricLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (authMode == AuthMode.SIGN_UP) "Sign Up with Google" else "Log In with Google",
                        color = OnElectricLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Legal & Security Footer
            Text(
                text = "Protected by Google Identity & TAN PLAY Fair-Play Protocol. Multi-accounting or emulator spoofing in competitive mobile brackets is prohibited.",
                color = MutedSlate,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun HighlightItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                color = HighClarityWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = MutedSlate,
                fontSize = 11.sp
            )
        }
    }
}
