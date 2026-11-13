package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BracketEngine
import com.example.data.FirestoreMatch
import com.example.data.FirestoreTournament
import com.example.data.TournamentParticipant
import com.example.ui.theme.*

/**
 * Visual Tournament Bracket Component mapped directly to Firestore match nodes.
 * Uses a synchronized multi-round tree layout with Compose Canvas orthogonal connectors
 * showing real-time match progression and winner advancement paths.
 */
@Composable
fun VisualBracketTree(
    tournament: FirestoreTournament,
    matches: List<FirestoreMatch>,
    participants: List<TournamentParticipant>,
    currentUserId: String,
    isOrganizerOrAdmin: Boolean,
    onMatchClick: (FirestoreMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) {
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
            modifier = modifier
                .fillMaxWidth()
                .testTag("bracket_empty_state")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Bracket Not Drawn Yet",
                    color = HighClarityWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Waiting for organizer to close registration and generate the ${tournament.format} draw (${participants.size}/${tournament.maxParticipants} registered).",
                    color = MutedSlate,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    if (tournament.format == "Round Robin") {
        RoundRobinBracketView(
            matches = matches,
            participants = participants,
            currentUserId = currentUserId,
            onMatchClick = onMatchClick,
            modifier = modifier
        )
        return
    }

    val roundsMap = remember(matches) {
        matches.groupBy { it.roundNumber }
            .toSortedMap()
            .mapValues { (_, list) -> list.sortedBy { it.matchNumber } }
    }
    val roundNumbers = roundsMap.keys.toList()
    val round1Count = roundsMap[roundNumbers.firstOrNull()]?.size ?: 1

    val cardWidth = 232.dp
    val cardHeight = 134.dp
    val basePitch = 154.dp
    val connectorWidth = 36.dp
    val headerHeight = 36.dp

    val totalTreeHeight = basePitch * round1Count
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_bracket_tree"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Champion Banner if Final Match is Completed
        val finalMatch = roundsMap[roundNumbers.lastOrNull()]?.firstOrNull()
        if (finalMatch != null && finalMatch.status == "Completed" && finalMatch.winnerName.isNotEmpty()) {
            Surface(
                color = ElectricLime.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricLime),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bracket_champion_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Champion",
                        tint = ElectricLime,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "TOURNAMENT CHAMPION",
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = finalMatch.winnerName,
                            color = HighClarityWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Scrollable Bracket Tree Canvas + Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            roundNumbers.forEachIndexed { rIdx, roundNum ->
                val roundMatches = roundsMap[roundNum].orEmpty()
                val roundTitle = roundMatches.firstOrNull()?.roundName ?: "Round $roundNum"
                val pitch = basePitch * (1 shl rIdx)

                // Column for this Round
                Column(
                    modifier = Modifier.width(cardWidth)
                ) {
                    // Round Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roundTitle.uppercase(),
                                color = if (rIdx == roundNumbers.lastIndex) ElectricLime else HighClarityWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${roundMatches.count { it.status == "Completed" }}/${roundMatches.size}",
                                color = MutedSlate,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Match Cards positioned at exact tree coordinates
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(totalTreeHeight)
                    ) {
                        roundMatches.forEachIndexed { mIdx, match ->
                            val centerY = pitch * (mIdx + 0.5f)
                            val topOffset = centerY - (cardHeight / 2f)
                            BracketFirestoreMatchCard(
                                match = match,
                                currentUserId = currentUserId,
                                isOrganizerOrAdmin = isOrganizerOrAdmin,
                                onClick = { onMatchClick(match) },
                                modifier = Modifier
                                    .offset(y = topOffset)
                                    .width(cardWidth)
                                    .height(cardHeight)
                            )
                        }
                    }
                }

                // Orthogonal Progression Connectors to Next Round
                if (rIdx < roundNumbers.lastIndex) {
                    val nextMatches = roundsMap[roundNumbers[rIdx + 1]].orEmpty()
                    BracketConnectorColumn(
                        sourceMatches = roundMatches,
                        targetMatchesCount = nextMatches.size,
                        sourcePitch = pitch,
                        headerHeight = headerHeight,
                        totalTreeHeight = totalTreeHeight,
                        width = connectorWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketConnectorColumn(
    sourceMatches: List<FirestoreMatch>,
    targetMatchesCount: Int,
    sourcePitch: Dp,
    headerHeight: Dp,
    totalTreeHeight: Dp,
    width: Dp
) {
    Column(modifier = Modifier.width(width)) {
        Spacer(modifier = Modifier.height(headerHeight))
        Canvas(
            modifier = Modifier
                .width(width)
                .height(totalTreeHeight)
        ) {
            val pitchPx = sourcePitch.toPx()
            val midX = size.width * 0.5f
            val strokePx = 2.dp.toPx()

            for (tIdx in 0 until targetMatchesCount) {
                val topMatchIdx = tIdx * 2
                val bottomMatchIdx = tIdx * 2 + 1
                val targetCenterY = (sourcePitch * 2f).toPx() * (tIdx + 0.5f)

                val topMatch = sourceMatches.getOrNull(topMatchIdx)
                val bottomMatch = sourceMatches.getOrNull(bottomMatchIdx)

                if (topMatch != null) {
                    val startY = pitchPx * (topMatchIdx + 0.5f)
                    val advanced = topMatch.winnerParticipantId.isNotEmpty()
                    val path = Path().apply {
                        moveTo(0f, startY)
                        lineTo(midX, startY)
                        lineTo(midX, targetCenterY)
                        lineTo(size.width, targetCenterY)
                    }
                    drawPath(
                        path = path,
                        color = if (advanced) ElectricLime else BorderDivider,
                        style = Stroke(width = if (advanced) strokePx * 1.4f else strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                if (bottomMatch != null) {
                    val startY = pitchPx * (bottomMatchIdx + 0.5f)
                    val advanced = bottomMatch.winnerParticipantId.isNotEmpty()
                    val path = Path().apply {
                        moveTo(0f, startY)
                        lineTo(midX, startY)
                        lineTo(midX, targetCenterY)
                        lineTo(size.width, targetCenterY)
                    }
                    drawPath(
                        path = path,
                        color = if (advanced) ElectricLime else BorderDivider,
                        style = Stroke(width = if (advanced) strokePx * 1.4f else strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun BracketFirestoreMatchCard(
    match: FirestoreMatch,
    currentUserId: String,
    isOrganizerOrAdmin: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMyMatch = currentUserId.isNotEmpty() &&
        (match.participant1UserId == currentUserId || match.participant2UserId == currentUserId)
    val isDisputed = match.status == "Disputed"
    val isReady = match.status == "Ready" || match.status == "Awaiting Results"
    val isCompleted = match.status == "Completed"
    val bothReady = match.participant1Ready && match.participant2Ready

    val borderColor = when {
        isDisputed -> LiveRed
        isMyMatch -> ElectricLime
        isReady -> EmeraldTertiaryDim
        else -> BorderDivider
    }

    val statusColor = when (match.status) {
        "Completed" -> EmeraldTertiaryDim
        "Ready" -> ElectricLime
        "Awaiting Results" -> Color(0xFFFFD56B)
        "Disputed" -> LiveRed
        "Scheduled" -> SlateSecondary
        else -> MutedSlate
    }

    Surface(
        color = if (isMyMatch) ObsidianContainerHigh else ObsidianContainer,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(if (isMyMatch || isDisputed) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable { onClick() }
            .testTag("bracket_match_${match.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Match number + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "M#${match.matchNumber}",
                    color = MutedSlate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = match.status.uppercase(),
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Competitor 1
            BracketCompetitorRow(
                name = match.participant1Name.ifBlank { "TBD" },
                statLabel = match.participant1Stat,
                gameId = match.participant1GameId,
                score = match.score1,
                isYou = currentUserId.isNotEmpty() && match.participant1UserId == currentUserId,
                isWinner = isCompleted && match.winnerParticipantId.isNotEmpty() && match.winnerParticipantId == match.participant1Id,
                isReady = match.participant1Ready,
                revealGameId = isOrganizerOrAdmin || (isMyMatch && bothReady)
            )

            HorizontalDivider(color = BorderDivider, thickness = 0.5.dp)

            // Competitor 2
            BracketCompetitorRow(
                name = match.participant2Name.ifBlank { "TBD" },
                statLabel = match.participant2Stat,
                gameId = match.participant2GameId,
                score = match.score2,
                isYou = currentUserId.isNotEmpty() && match.participant2UserId == currentUserId,
                isWinner = isCompleted && match.winnerParticipantId.isNotEmpty() && match.winnerParticipantId == match.participant2Id,
                isReady = match.participant2Ready,
                revealGameId = isOrganizerOrAdmin || (isMyMatch && bothReady)
            )
        }
    }
}

@Composable
private fun BracketCompetitorRow(
    name: String,
    statLabel: String,
    gameId: String,
    score: Int?,
    isYou: Boolean,
    isWinner: Boolean,
    isReady: Boolean,
    revealGameId: Boolean
) {
    val displayedId = when {
        gameId.isEmpty() -> ""
        revealGameId -> gameId
        else -> BracketEngine.maskGameId(gameId)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isWinner) ElectricLime.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReady) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ElectricLime)
                    )
                }
                Text(
                    text = name,
                    color = if (isWinner) ElectricLime else HighClarityWhite,
                    fontSize = 11.sp,
                    fontWeight = if (isWinner || isYou) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isYou) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(ElectricLime)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("YOU", color = OnElectricLime, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            val meta = listOfNotNull(
                statLabel.takeIf { it.isNotEmpty() },
                displayedId.takeIf { it.isNotEmpty() }
            ).joinToString(" • ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = MutedSlate,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = score?.toString() ?: "-",
            color = if (isWinner) ElectricLime else SlateSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RoundRobinBracketView(
    matches: List<FirestoreMatch>,
    participants: List<TournamentParticipant>,
    currentUserId: String,
    onMatchClick: (FirestoreMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val standings = remember(participants, matches) {
        BracketEngine.computeRoundRobinStandings(participants, matches)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("round_robin_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Standings Table
        Surface(
            color = ObsidianContainerLow,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ROUND ROBIN STANDINGS",
                    color = ElectricLime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("#  PLAYER", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
                    Text("P", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.4f))
                    Text("W", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.4f))
                    Text("GD", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                    Text("PTS", color = ElectricLime, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                }
                HorizontalDivider(color = BorderDivider)
                standings.forEachIndexed { idx, row ->
                    val isMe = row.userId == currentUserId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMe) ElectricLime.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(
                                text = "${idx + 1}. ${row.displayName}" + if (isMe) " (YOU)" else "",
                                color = if (isMe) ElectricLime else HighClarityWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (row.statLabel.isNotEmpty()) {
                                Text(row.statLabel, color = MutedSlate, fontSize = 9.sp)
                            }
                        }
                        Text("${row.played}", color = SlateSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.4f))
                        Text("${row.wins}", color = SlateSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.4f))
                        Text("${row.goalDiff}", color = SlateSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                        Text("${row.points}", color = ElectricLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }

        // Matchdays List
        val byRound = matches.groupBy { it.roundNumber }.toSortedMap()
        byRound.forEach { (round, roundMatches) ->
            Text(
                text = "MATCHDAY $round",
                color = SlateSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roundMatches.forEach { match ->
                    BracketFirestoreMatchCard(
                        match = match,
                        currentUserId = currentUserId,
                        isOrganizerOrAdmin = false,
                        onClick = { onMatchClick(match) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                    )
                }
            }
        }
    }
}
