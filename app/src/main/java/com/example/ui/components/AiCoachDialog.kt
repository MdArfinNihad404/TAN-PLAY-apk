package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun AiCoachDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("FC Mobile Tactics") }

    val presetResponses = remember {
        mapOf(
            "FC Mobile Tactics" to "⚽ FC Mobile Meta Tip: Use 4-3-3 Holding to counter 4-2-4 aggressive spam. For Bangladesh server ping (~45ms), trigger 'Heel-to-Heel' flick right at the edge of the penalty box. Never rush your CBs out—hold Second Defender button sparingly.",
            "eFootball Formations" to "🕹️ eFootball 2024 Pro Insight: Against high-pressing opponents, switch playstyle to 'Quick Counter' with 4-2-1-3. Master the 'Super Cancel' (Double tap Dash + Pass) to intercept scripted passes in competitive division matches.",
            "Free Fire Rush Guide" to "🔥 Free Fire Squad Strategy: Pochinok/Clock Tower drops require 1 Designated Sniper and 2 Fast Rusher loadouts (MP40 + Woodpecker). Always carry minimum 4 Gloo walls into Final Zone 5; create 360° defensive pocket during rotation.",
            "PUBG Erangel Zones" to "🪖 PUBG Mobile Tier Advice: When bluezone pulls towards Military Base island, prioritize bridge scout at 02:30 or take Eastern ferry boats. In Bo3 tournaments, placement points (Top 3 = +15 pts) outweigh early hot-drop elimination risk.",
            "Dispute & Match Proofs" to "⚖️ TAN PLAY Rulebook: Capture a full-screen match end screenshot showing final score & player names. Attach it via Photo Picker in Match Lobby; opponent confirms or Admin reviews.",
            "Brackets & Seeding" to "🏆 Bracket Guide: Random Draw shuffles Round 1 fairly. Seeded (Balanced) pairs #1 vs lowest seed by OVR/Level/Rating so top seeds meet in Finals. Non-power-of-2 counts get automatic BYEs."
        )
    }

    var displayedAnswer by remember { mutableStateOf(presetResponses["FC Mobile Tactics"]!!) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricLime.copy(alpha = 0.15f))
                                .border(1.dp, ElectricLime, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "TAN AI COACH",
                                    color = HighClarityWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricLime)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = OnElectricLime,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Tactics, Meta Picks & Rules Assistant",
                                color = SlateSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Topic Chips
                Text(
                    text = "SELECT TOPIC / PRESET TACTIC",
                    color = SlateSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetResponses.keys.toList()) { topic ->
                        val isSelected = topic == selectedTopic
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricLime else ObsidianContainerHighest)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricLime else ObsidianStroke,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedTopic = topic
                                    displayedAnswer = presetResponses[topic] ?: ""
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = topic,
                                color = if (isSelected) OnElectricLime else SlateSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Advice Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ANALYTICAL REPORT & STRATEGY INSIGHT",
                                color = ElectricLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = displayedAnswer,
                            color = HighClarityWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Query Input
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            "Ask strategy, e.g., 'Free Fire zone guide' or 'DLS'",
                            color = SlateDisabled,
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank()) {
                                    val q = queryText.lowercase()
                                    displayedAnswer = when {
                                        q.contains("free fire") || q.contains("ff") ->
                                            "🔥 FF Custom Room Strategy: In Clash Squad 4v4, always buy Desert Eagle round 1. Hold high ground on Factory roof or Mill stairs. Avoid peaking AWM snipers without gloo wall cover."
                                        q.contains("dls") || q.contains("dream league") ->
                                            "⚽ DLS Pro Setup: Run 3-5-2 with wingbacks set to high stamina. Use 'B' through-balls for fast breakaways when opponent plays high defensive line."
                                        q.contains("pubg") || q.contains("bgmi") ->
                                            "🪖 PUBG Mobile Competitive: Use M416 with Compensator + Half Grip + Stock for 3x spray laser accuracy. Save energy drinks for Zone 4 boost."
                                        q.contains("bracket") || q.contains("seed") || q.contains("bye") ->
                                            "🏆 Bracket Protocol: All tournaments are 100% free to join. Once both players click Ready in Match Lobby, opponent Game ID unlocks for 1v1 challenge."
                                        else ->
                                            "💡 Strategy Recommendation for '$queryText': Focus on squad chemistry, call out enemy positions immediately, and confirm final match screenshots cleanly in Match Lobby."
                                    }
                                    selectedTopic = "Custom Query"
                                    queryText = ""
                                }
                            },
                            enabled = queryText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (queryText.isNotBlank()) ElectricLime else SlateDisabled
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighClarityWhite,
                        unfocusedTextColor = HighClarityWhite,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = ObsidianStroke,
                        focusedContainerColor = ObsidianBackground,
                        unfocusedContainerColor = ObsidianBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_coach_query_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianContainerHighest),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO ARENA",
                        color = HighClarityWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
