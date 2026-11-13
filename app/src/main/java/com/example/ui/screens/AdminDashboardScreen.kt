package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreTournament
import com.example.data.FirestoreTournamentRepository
import com.example.data.ModerationReport
import com.example.ui.components.TanScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    tournaments: List<FirestoreTournament>,
    currentUserId: String,
    isAdminUser: Boolean,
    repository: FirestoreTournamentRepository,
    onOpenBracket: (String) -> Unit,
    onNavigate: (TanScreen) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAdminUser) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBase)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = LiveRed, modifier = Modifier.size(44.dp))
                Text("Admin Access Required", color = HighClarityWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Only authorized TAN PLAY moderators can access dispute triage and platform reports.", color = MutedSlate, fontSize = 13.sp)
                Button(
                    onClick = { onNavigate(TanScreen.HOME) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Return Home", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val reports by repository.observeReportsForAdmin().collectAsState(initial = emptyList())

    var selectedReportFilter by remember { mutableStateOf("Open") } // "All", "Open", "Resolved", "Dismissed"
    var reportToResolve by remember { mutableStateOf<Pair<ModerationReport, String>?>(null) }
    var resolutionNoteInput by remember { mutableStateOf("") }
    var tournamentToCancel by remember { mutableStateOf<FirestoreTournament?>(null) }

    val openReportsCount = remember(reports) { reports.count { it.status == "Open" } }
    val filteredReports = remember(reports, selectedReportFilter) {
        reports.filter { r -> selectedReportFilter == "All" || r.status == selectedReportFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Governance Header
        item {
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (openReportsCount > 0) LiveRed.copy(alpha = 0.5f) else ElectricLime.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_triage_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (openReportsCount > 0) ErrorContainer else ObsidianContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = if (openReportsCount > 0) OnErrorContainer else ElectricLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text("ADMIN MODERATION CONSOLE", color = ElectricLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("$openReportsCount open disputes / abuse reports", color = SlateSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigate(TanScreen.CREATE_TOURNAMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Host Event", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(TanScreen.HOME) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Player View", fontSize = 12.sp, color = HighClarityWhite)
                        }
                    }
                }
            }
        }

        // Platform Free-Play Stats Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminKpiCard("TOTAL ARENAS", tournaments.size.toString(), "100% Free Play", Modifier.weight(1f))
                AdminKpiCard("LIVE BRACKETS", tournaments.count { it.status == "In Progress" }.toString(), "${tournaments.count { it.status == "Open" }} Open Reg", Modifier.weight(1f))
                AdminKpiCard("OPEN REPORTS", openReportsCount.toString(), "${reports.size} Total", Modifier.weight(1f), highlightRed = openReportsCount > 0)
            }
        }

        // Report Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DISPUTE & ABUSE REPORTS", color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Open", "All", "Resolved", "Dismissed")) { tab ->
                        val selected = selectedReportFilter == tab
                        FilterChip(
                            selected = selected,
                            onClick = { selectedReportFilter = tab },
                            label = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricLime,
                                selectedLabelColor = OnElectricLime,
                                containerColor = ObsidianContainerLow,
                                labelColor = SlateSecondary
                            )
                        )
                    }
                }
            }
        }

        if (filteredReports.isEmpty()) {
            item {
                Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No reports in '$selectedReportFilter'.", color = EmeraldTertiaryDim, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                Surface(
                    color = ObsidianContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (report.status == "Open") LiveRed.copy(alpha = 0.5f) else BorderDivider),
                    modifier = Modifier.fillMaxWidth().testTag("admin_report_${report.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (report.status == "Open") LiveRed else ElectricLime)
                                )
                                Text(report.type.uppercase(), color = if (report.status == "Open") LiveRed else ElectricLime, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Surface(color = ObsidianContainerHighest, shape = RoundedCornerShape(4.dp)) {
                                Text(report.status, color = SlateSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Text("Reported by: ${report.reporterName.ifBlank { report.reporterId.take(8) }}", color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(report.reason, color = SlateSecondary, fontSize = 12.sp, lineHeight = 16.sp)

                        if (report.resolutionNote.isNotBlank()) {
                            Text("Resolution: ${report.resolutionNote}", color = ElectricLime, fontSize = 11.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (report.tournamentId.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        onOpenBracket(report.tournamentId)
                                        onNavigate(TanScreen.BRACKET)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Inspect Bracket", fontSize = 11.sp, color = ElectricLime)
                                }
                            }
                            if (report.status == "Open") {
                                OutlinedButton(
                                    onClick = {
                                        resolutionNoteInput = "Dismissed after review"
                                        reportToResolve = report to "Dismissed"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Dismiss", fontSize = 11.sp, color = SlateSecondary)
                                }
                                Button(
                                    onClick = {
                                        resolutionNoteInput = "Resolved & bracket updated"
                                        reportToResolve = report to "Resolved"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tournament Administration
        item {
            Text("TOURNAMENT MODERATION", color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        items(tournaments.take(15), key = { it.id }) { tournament ->
            Surface(
                color = ObsidianContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tournament.title, color = HighClarityWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${tournament.game} • ${tournament.status} • ${tournament.participantCount}/${tournament.maxParticipants} Players",
                            color = MutedSlate,
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                onOpenBracket(tournament.id)
                                onNavigate(TanScreen.BRACKET)
                            }
                        ) {
                            Text("Bracket", color = ElectricLime, fontSize = 11.sp)
                        }
                        if (tournament.status != "Cancelled" && tournament.status != "Completed") {
                            TextButton(onClick = { tournamentToCancel = tournament }) {
                                Text("Cancel", color = LiveRed, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    reportToResolve?.let { (report, targetStatus) ->
        AlertDialog(
            onDismissRequest = { reportToResolve = null },
            containerColor = ObsidianContainerLow,
            title = { Text("Mark Report as $targetStatus", color = HighClarityWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = resolutionNoteInput,
                    onValueChange = { resolutionNoteInput = it.take(200) },
                    label = { Text("Moderator Note") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighClarityWhite,
                        unfocusedTextColor = HighClarityWhite,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = BorderDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportToResolve = null
                        scope.launch {
                            val res = repository.resolveReport(report.id, targetStatus, resolutionNoteInput)
                            if (res.isSuccess) onShowMessage("Report marked $targetStatus")
                            else onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = OnElectricLime)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToResolve = null }) {
                    Text("Back", color = SlateSecondary)
                }
            }
        )
    }

    tournamentToCancel?.let { target ->
        AlertDialog(
            onDismissRequest = { tournamentToCancel = null },
            containerColor = ObsidianContainerLow,
            title = { Text("Cancel ${target.title}?", color = LiveRed, fontWeight = FontWeight.Bold) },
            text = { Text("Cancelling stops further matches in this tournament.", color = SlateSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        tournamentToCancel = null
                        scope.launch {
                            val res = repository.cancelTournament(target.id, "Cancelled by moderator ($currentUserId)")
                            if (res.isSuccess) onShowMessage("Tournament cancelled")
                            else onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to cancel")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed, contentColor = HighClarityWhite)
                ) {
                    Text("Cancel Tournament", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tournamentToCancel = null }) {
                    Text("Keep Active", color = SlateSecondary)
                }
            }
        )
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    highlightRed: Boolean = false
) {
    Surface(color = ObsidianContainerLow, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = value,
                color = if (highlightRed) LiveRed else ElectricLime,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(sub, color = SlateSecondary, fontSize = 10.sp)
        }
    }
}
