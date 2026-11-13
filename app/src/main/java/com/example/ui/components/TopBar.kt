package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TanPlayTopBar(
    screenTitle: String,
    unreadAlertsCount: Int,
    isAdminUser: Boolean,
    isAdminMode: Boolean,
    onAlertsClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onAdminToggle: () -> Unit,
    onAiCoachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianContainerLow.copy(alpha = 0.95f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Logo & App Title + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricLime)
                        .clickable { if (isAdminUser) onAdminToggle() }
                        .testTag("app_logo_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TP",
                        color = OnElectricLime,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TAN PLAY",
                            color = HighClarityWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isAdminMode) ErrorContainer else ObsidianContainerHighest)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isAdminMode) "ADMIN" else "FREE PLAY",
                                color = if (isAdminMode) OnErrorContainer else ElectricLime,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = screenTitle,
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Actions (Alerts, Rankings, AI Coach, Admin Console if authorized)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // In-App Alerts Button
                Box {
                    IconButton(
                        onClick = onAlertsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ObsidianContainerHigh)
                            .testTag("alerts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadAlertsCount > 0) ElectricLime else SlateSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (unreadAlertsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(LiveRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadAlertsCount.coerceAtMost(9).toString(),
                                color = HighClarityWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Rankings Shortcut
                IconButton(
                    onClick = onLeaderboardClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ObsidianContainerHigh)
                        .testTag("leaderboard_shortcut_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = SlateSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // AI Coach Button
                IconButton(
                    onClick = onAiCoachClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ObsidianContainerHigh)
                        .testTag("ai_coach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "TAN Strategy Coach",
                        tint = ElectricLime,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Admin Console Button (strictly for Admin role)
                if (isAdminUser) {
                    IconButton(
                        onClick = onAdminToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAdminMode) ElectricLime.copy(alpha = 0.2f) else ObsidianContainerHigh)
                            .testTag("admin_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = if (isAdminMode) "Exit Admin Panel" else "Open Admin Console",
                            tint = if (isAdminMode) ElectricLime else SlateSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
