package com.example.data

import kotlin.math.ceil
import kotlin.math.ln

data class GameFieldSpec(
    val key: String,
    val label: String,
    val isNumeric: Boolean = false,
    val isSensitiveId: Boolean = false,
    val isRankingMetric: Boolean = false,
    val isPublicStat: Boolean = false,
    val placeholder: String = "",
    val options: List<String> = emptyList()
)

data class GeneratedBracket(
    val matches: List<FirestoreMatch>,
    val seededParticipants: List<TournamentParticipant>
)

data class RoundRobinStandingRow(
    val participantId: String,
    val userId: String,
    val displayName: String,
    val statLabel: String,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalDiff: Int,
    val points: Int
)

object BracketEngine {

    val SUPPORTED_GAMES = listOf(
        "Free Fire",
        "eFootball",
        "FC Mobile",
        "DLS",
        "PUBG Mobile",
        "Custom Game"
    )

    fun getFieldSpecs(
        game: String,
        customRequiredFields: List<String> = emptyList()
    ): List<GameFieldSpec> {
        return when (game.trim()) {
            "Free Fire" -> listOf(
                GameFieldSpec(
                    key = "uid",
                    label = "Free Fire UID",
                    isNumeric = true,
                    isSensitiveId = true,
                    placeholder = "e.g. 1928374650"
                ),
                GameFieldSpec(
                    key = "ign",
                    label = "In-Game Name (IGN)",
                    isPublicStat = true,
                    placeholder = "e.g. DhakaSniper"
                ),
                GameFieldSpec(
                    key = "level",
                    label = "Account Level",
                    isNumeric = true,
                    isRankingMetric = true,
                    isPublicStat = true,
                    placeholder = "e.g. 68"
                )
            )

            "eFootball" -> listOf(
                GameFieldSpec(
                    key = "onlineId",
                    label = "eFootball Username / Online ID",
                    isSensitiveId = true,
                    placeholder = "e.g. EF-Tanvir99"
                ),
                GameFieldSpec(
                    key = "platform",
                    label = "Platform",
                    isPublicStat = true,
                    options = listOf("Android", "iOS", "Console")
                ),
                GameFieldSpec(
                    key = "teamStrength",
                    label = "Collective Strength",
                    isNumeric = true,
                    isRankingMetric = true,
                    isPublicStat = true,
                    placeholder = "e.g. 3120"
                )
            )

            "FC Mobile" -> listOf(
                GameFieldSpec(
                    key = "uid",
                    label = "FC Mobile UID",
                    isNumeric = true,
                    isSensitiveId = true,
                    placeholder = "e.g. 8844221100"
                ),
                GameFieldSpec(
                    key = "ovr",
                    label = "Team OVR",
                    isNumeric = true,
                    isRankingMetric = true,
                    isPublicStat = true,
                    placeholder = "e.g. 104"
                )
            )

            "DLS", "DLS 24" -> listOf(
                GameFieldSpec(
                    key = "teamName",
                    label = "DLS Team Name",
                    isPublicStat = true,
                    placeholder = "e.g. Bengal Tigers FC"
                ),
                GameFieldSpec(
                    key = "teamRating",
                    label = "Team Rating",
                    isNumeric = true,
                    isRankingMetric = true,
                    isPublicStat = true,
                    placeholder = "e.g. 92"
                ),
                GameFieldSpec(
                    key = "managerId",
                    label = "DLS Friend / Match Code",
                    isSensitiveId = true,
                    placeholder = "e.g. DLS-4491"
                )
            )

            "PUBG Mobile" -> listOf(
                GameFieldSpec(
                    key = "playerId",
                    label = "PUBG Character ID",
                    isNumeric = true,
                    isSensitiveId = true,
                    placeholder = "e.g. 5123456789"
                ),
                GameFieldSpec(
                    key = "nickname",
                    label = "PUBG Nickname",
                    isPublicStat = true,
                    placeholder = "e.g. MiramarKing"
                ),
                GameFieldSpec(
                    key = "level",
                    label = "Evo / Account Level",
                    isNumeric = true,
                    isRankingMetric = true,
                    isPublicStat = true,
                    placeholder = "e.g. 72"
                )
            )

            else -> {
                val customSpecs = customRequiredFields
                    .filter { it.isNotBlank() }
                    .mapIndexed { idx, fieldName ->
                        val lower = fieldName.lowercase()
                        val numeric = lower.contains("uid") ||
                            lower.contains("ovr") ||
                            lower.contains("rating") ||
                            lower.contains("level") ||
                            lower.contains("rank")
                        val sensitive = lower.contains("id") || lower.contains("uid") || lower.contains("code")
                        val metric = lower.contains("ovr") || lower.contains("rating") || lower.contains("level")
                        GameFieldSpec(
                            key = "custom_$idx",
                            label = fieldName,
                            isNumeric = numeric,
                            isSensitiveId = sensitive,
                            isRankingMetric = metric,
                            isPublicStat = !sensitive,
                            placeholder = "Enter $fieldName"
                        )
                    }
                if (customSpecs.isNotEmpty()) {
                    customSpecs
                } else {
                    listOf(
                        GameFieldSpec(
                            key = "gameHandle",
                            label = "In-Game ID / Username",
                            isSensitiveId = true,
                            placeholder = "Enter your in-game ID"
                        ),
                        GameFieldSpec(
                            key = "skillRating",
                            label = "Rank / Rating (Numeric)",
                            isNumeric = true,
                            isRankingMetric = true,
                            isPublicStat = true,
                            placeholder = "e.g. 1500"
                        )
                    )
                }
            }
        }
    }

    fun validateGameFields(
        game: String,
        customRequiredFields: List<String>,
        values: Map<String, String>
    ): String? {
        val specs = getFieldSpecs(game, customRequiredFields)
        for (spec in specs) {
            val raw = values[spec.key]?.trim().orEmpty()
            if (raw.isEmpty()) {
                return "${spec.label} is required."
            }
            if (spec.isNumeric && raw.toLongOrNull() == null) {
                return "${spec.label} must be a valid number."
            }
            if (spec.isNumeric) {
                val n = raw.toLongOrNull() ?: -1L
                if (n <= 0L) {
                    return "${spec.label} must be greater than 0."
                }
            }
        }
        return null
    }

    fun extractSensitiveId(
        game: String,
        customRequiredFields: List<String>,
        values: Map<String, String>
    ): String {
        val specs = getFieldSpecs(game, customRequiredFields)
        val sensitiveSpec = specs.firstOrNull { it.isSensitiveId } ?: specs.firstOrNull()
        return sensitiveSpec?.let { values[it.key]?.trim() }.orEmpty()
    }

    fun extractPublicStatLabel(
        game: String,
        customRequiredFields: List<String>,
        values: Map<String, String>
    ): String {
        return when (game.trim()) {
            "Free Fire" -> {
                val lvl = values["level"]?.trim().orEmpty()
                val ign = values["ign"]?.trim().orEmpty()
                if (lvl.isNotEmpty()) "Lvl $lvl • $ign".trim(' ', '•') else ign
            }
            "eFootball" -> {
                val plat = values["platform"]?.trim().orEmpty()
                val str = values["teamStrength"]?.trim().orEmpty()
                if (str.isNotEmpty()) "$plat • TS $str" else plat
            }
            "FC Mobile" -> {
                val ovr = values["ovr"]?.trim().orEmpty()
                if (ovr.isNotEmpty()) "OVR $ovr" else "Verified"
            }
            "DLS", "DLS 24" -> {
                val team = values["teamName"]?.trim().orEmpty()
                val rating = values["teamRating"]?.trim().orEmpty()
                if (rating.isNotEmpty()) "Rating $rating • $team".trim(' ', '•') else team
            }
            "PUBG Mobile" -> {
                val lvl = values["level"]?.trim().orEmpty()
                val nick = values["nickname"]?.trim().orEmpty()
                if (lvl.isNotEmpty()) "Lvl $lvl • $nick".trim(' ', '•') else nick
            }
            else -> {
                val specs = getFieldSpecs(game, customRequiredFields)
                val publicParts = specs.filter { it.isPublicStat }
                    .mapNotNull { spec ->
                        val v = values[spec.key]?.trim()
                        if (!v.isNullOrEmpty()) "${spec.label}: $v" else null
                    }
                publicParts.take(2).joinToString(" • ").ifEmpty { "Ready" }
            }
        }
    }

    fun extractRankingMetric(
        game: String,
        customRequiredFields: List<String>,
        values: Map<String, String>
    ): Int {
        val specs = getFieldSpecs(game, customRequiredFields)
        val metricSpec = specs.firstOrNull { it.isRankingMetric }
        return metricSpec?.let { values[it.key]?.trim()?.toIntOrNull() }?.coerceIn(0, 100000) ?: 0
    }

    /**
     * Publicly masks sensitive Game IDs/UIDs so full IDs are only disclosed
     * inside Match Lobby after both contenders click Ready (or to Organizer/Admin).
     */
    fun maskGameId(rawId: String): String {
        val trimmed = rawId.trim()
        if (trimmed.isEmpty()) return "Hidden"
        if (trimmed.length <= 4) return "••••"
        return "••••" + trimmed.takeLast(4)
    }

    fun roundNameFor(roundNumber: Int, totalRounds: Int): String {
        val remaining = totalRounds - roundNumber
        return when (remaining) {
            0 -> "Grand Final"
            1 -> "Semi-Finals"
            2 -> "Quarter-Finals"
            3 -> "Round of 16"
            else -> "Round $roundNumber"
        }
    }

    /**
     * Generates a balanced Single Elimination bracket with automatic Bye handling.
     * - "Random": shuffles participants randomly.
     * - "Seeded": sorts by rankingMetric descending and pairs Seed 1 vs Seed N, 2 vs N-1.
     * - "Manual": uses organizer-chosen Round 1 pairings.
     */
    fun generateSingleEliminationBracket(
        tournament: FirestoreTournament,
        participants: List<TournamentParticipant>,
        seedingMode: String = tournament.seedingMode,
        manualPairs: List<Pair<TournamentParticipant, TournamentParticipant?>> = emptyList()
    ): GeneratedBracket {
        if (participants.size < 2) {
            return GeneratedBracket(emptyList(), participants)
        }

        val orderedSlots: List<TournamentParticipant?> = when (seedingMode) {
            "Seeded" -> {
                val sorted = participants.sortedByDescending { it.rankingMetric }
                val bracketSize = nextPowerOfTwo(sorted.size)
                buildBalancedSeedOrder(bracketSize).map { seedIndex ->
                    sorted.getOrNull(seedIndex - 1)
                }
            }
            "Manual" -> {
                if (manualPairs.isNotEmpty()) {
                    val usedIds = mutableSetOf<String>()
                    val slots = mutableListOf<TournamentParticipant?>()
                    manualPairs.forEach { (p1, p2) ->
                        slots.add(p1)
                        usedIds.add(p1.id)
                        slots.add(p2)
                        if (p2 != null) usedIds.add(p2.id)
                    }
                    val remaining = participants.filterNot { it.id in usedIds }
                    remaining.forEach { slots.add(it) }
                    val targetSize = nextPowerOfTwo(maxOf(2, slots.size / 2 + slots.size % 2 * 2, participants.size))
                    while (slots.size < targetSize) slots.add(null)
                    slots
                } else {
                    padWithByes(participants.shuffled())
                }
            }
            else -> {
                // Default: Random Draw
                padWithByes(participants.shuffled())
            }
        }

        val seededParticipants = orderedSlots.filterNotNull().mapIndexed { index, p ->
            p.copy(seedNumber = index + 1)
        }

        val bracketSize = orderedSlots.size
        val totalRounds = maxOf(1, ceil(ln(bracketSize.toDouble()) / ln(2.0)).toInt())

        // Create blank skeleton nodes for all rounds from Round 1 .. Grand Final
        val matchMap = linkedMapOf<String, FirestoreMatch>()
        for (r in 1..totalRounds) {
            val matchesInRound = bracketSize / (1 shl r)
            val rName = roundNameFor(r, totalRounds)
            for (m in 1..matchesInRound) {
                val matchId = "m_r${r}_$m"
                val nextId = if (r < totalRounds) "m_r${r + 1}_${(m + 1) / 2}" else ""
                val nextSlot = if (r < totalRounds) (if (m % 2 == 1) 1 else 2) else 0
                matchMap[matchId] = FirestoreMatch(
                    id = matchId,
                    tournamentId = tournament.id,
                    organizerId = tournament.organizerId,
                    roundNumber = r,
                    roundName = rName,
                    matchNumber = m,
                    status = "Pending",
                    nextMatchId = nextId,
                    nextMatchSlot = nextSlot,
                    roomId = tournament.roomId,
                    roomPassword = tournament.roomPassword,
                    scheduledTimeText = tournament.startTimeText.ifBlank { "Round $r" }
                )
            }
        }

        // Populate Round 1 and resolve Byes automatically
        val round1Count = bracketSize / 2
        for (m in 1..round1Count) {
            val p1 = orderedSlots.getOrNull((m - 1) * 2)
            val p2 = orderedSlots.getOrNull((m - 1) * 2 + 1)
            val matchId = "m_r1_$m"
            val current = matchMap[matchId]!!

            if (p1 != null && p2 != null) {
                matchMap[matchId] = current.copy(
                    participant1Id = p1.id,
                    participant1UserId = p1.userId,
                    participant1Name = p1.displayName,
                    participant1Stat = p1.publicStatLabel,
                    participant1GameId = p1.sensitiveIdField,
                    participant2Id = p2.id,
                    participant2UserId = p2.userId,
                    participant2Name = p2.displayName,
                    participant2Stat = p2.publicStatLabel,
                    participant2GameId = p2.sensitiveIdField,
                    status = "Scheduled",
                    auditLog = listOf("Draw generated ($seedingMode)")
                )
            } else if (p1 != null || p2 != null) {
                val solo = p1 ?: p2!!
                val completedBye = current.copy(
                    participant1Id = solo.id,
                    participant1UserId = solo.userId,
                    participant1Name = solo.displayName,
                    participant1Stat = solo.publicStatLabel,
                    participant1GameId = solo.sensitiveIdField,
                    participant2Id = "",
                    participant2UserId = "",
                    participant2Name = "BYE",
                    participant2Stat = "Auto-Advance",
                    participant2GameId = "",
                    score1 = 1,
                    score2 = 0,
                    status = "Completed",
                    winnerParticipantId = solo.id,
                    winnerUserId = solo.userId,
                    winnerName = solo.displayName,
                    auditLog = listOf("Automatic Bye advancement for ${solo.displayName}")
                )
                matchMap[matchId] = completedBye

                // Promote Bye winner directly into Round 2 slot
                if (completedBye.nextMatchId.isNotEmpty()) {
                    val next = matchMap[completedBye.nextMatchId]
                    if (next != null) {
                        val updatedNext = if (completedBye.nextMatchSlot == 1) {
                            next.copy(
                                participant1Id = solo.id,
                                participant1UserId = solo.userId,
                                participant1Name = solo.displayName,
                                participant1Stat = solo.publicStatLabel,
                                participant1GameId = solo.sensitiveIdField
                            )
                        } else {
                            next.copy(
                                participant2Id = solo.id,
                                participant2UserId = solo.userId,
                                participant2Name = solo.displayName,
                                participant2Stat = solo.publicStatLabel,
                                participant2GameId = solo.sensitiveIdField
                            )
                        }
                        val nextStatus = if (updatedNext.participant1Id.isNotEmpty() && updatedNext.participant2Id.isNotEmpty()) {
                            "Scheduled"
                        } else {
                            "Pending"
                        }
                        matchMap[completedBye.nextMatchId] = updatedNext.copy(status = nextStatus)
                    }
                }
            }
        }

        return GeneratedBracket(
            matches = matchMap.values.toList(),
            seededParticipants = seededParticipants
        )
    }

    /**
     * Generates Round Robin all-play-all fixtures using circle scheduling algorithm.
     */
    fun generateRoundRobinMatches(
        tournament: FirestoreTournament,
        participants: List<TournamentParticipant>,
        seedingMode: String = tournament.seedingMode
    ): GeneratedBracket {
        if (participants.size < 2) return GeneratedBracket(emptyList(), participants)
        val ordered = if (seedingMode == "Seeded") {
            participants.sortedByDescending { it.rankingMetric }
        } else {
            participants.shuffled()
        }
        val seeded = ordered.mapIndexed { idx, p -> p.copy(seedNumber = idx + 1) }
        val pool: MutableList<TournamentParticipant?> = seeded.toMutableList()
        if (pool.size % 2 != 0) pool.add(null)

        val n = pool.size
        val rounds = n - 1
        val matches = mutableListOf<FirestoreMatch>()
        val rotating = pool.drop(1).toMutableList()

        for (r in 1..rounds) {
            val roundPool = listOf(pool.first()) + rotating
            for (i in 0 until n / 2) {
                val p1 = roundPool[i]
                val p2 = roundPool[n - 1 - i]
                if (p1 != null && p2 != null) {
                    val mNum = i + 1
                    matches.add(
                        FirestoreMatch(
                            id = "rr_r${r}_$mNum",
                            tournamentId = tournament.id,
                            organizerId = tournament.organizerId,
                            roundNumber = r,
                            roundName = "Matchday $r",
                            matchNumber = mNum,
                            participant1Id = p1.id,
                            participant1UserId = p1.userId,
                            participant1Name = p1.displayName,
                            participant1Stat = p1.publicStatLabel,
                            participant1GameId = p1.sensitiveIdField,
                            participant2Id = p2.id,
                            participant2UserId = p2.userId,
                            participant2Name = p2.displayName,
                            participant2Stat = p2.publicStatLabel,
                            participant2GameId = p2.sensitiveIdField,
                            status = "Scheduled",
                            roomId = tournament.roomId,
                            roomPassword = tournament.roomPassword,
                            scheduledTimeText = "Matchday $r • ${tournament.startTimeText}".trim(' ', '•'),
                            auditLog = listOf("Round Robin fixture generated")
                        )
                    )
                }
            }
            val last = rotating.removeAt(rotating.lastIndex)
            rotating.add(0, last)
        }

        return GeneratedBracket(matches = matches, seededParticipants = seeded)
    }

    fun computeRoundRobinStandings(
        participants: List<TournamentParticipant>,
        matches: List<FirestoreMatch>
    ): List<RoundRobinStandingRow> {
        val map = participants.associateBy { it.id }
        val rows = participants.associate { p ->
            p.id to RoundRobinStandingRow(
                participantId = p.id,
                userId = p.userId,
                displayName = p.displayName,
                statLabel = p.publicStatLabel,
                played = 0,
                wins = 0,
                draws = 0,
                losses = 0,
                goalDiff = 0,
                points = 0
            )
        }.toMutableMap()

        matches.filter { it.status == "Completed" && it.participant1Id.isNotEmpty() && it.participant2Id.isNotEmpty() }
            .forEach { m ->
                val s1 = m.score1 ?: 0
                val s2 = m.score2 ?: 0
                val r1 = rows[m.participant1Id] ?: map[m.participant1Id]?.let {
                    RoundRobinStandingRow(it.id, it.userId, it.displayName, it.publicStatLabel, 0, 0, 0, 0, 0, 0)
                }
                val r2 = rows[m.participant2Id] ?: map[m.participant2Id]?.let {
                    RoundRobinStandingRow(it.id, it.userId, it.displayName, it.publicStatLabel, 0, 0, 0, 0, 0, 0)
                }
                if (r1 != null && r2 != null) {
                    val p1Won = m.winnerParticipantId == m.participant1Id || s1 > s2
                    val p2Won = m.winnerParticipantId == m.participant2Id || s2 > s1
                    val isDraw = !p1Won && !p2Won

                    rows[m.participant1Id] = r1.copy(
                        played = r1.played + 1,
                        wins = r1.wins + if (p1Won) 1 else 0,
                        draws = r1.draws + if (isDraw) 1 else 0,
                        losses = r1.losses + if (p2Won) 1 else 0,
                        goalDiff = r1.goalDiff + (s1 - s2),
                        points = r1.points + when {
                            p1Won -> 3
                            isDraw -> 1
                            else -> 0
                        }
                    )
                    rows[m.participant2Id] = r2.copy(
                        played = r2.played + 1,
                        wins = r2.wins + if (p2Won) 1 else 0,
                        draws = r2.draws + if (isDraw) 1 else 0,
                        losses = r2.losses + if (p1Won) 1 else 0,
                        goalDiff = r2.goalDiff + (s2 - s1),
                        points = r2.points + when {
                            p2Won -> 3
                            isDraw -> 1
                            else -> 0
                        }
                    )
                }
            }

        return rows.values.sortedWith(
            compareByDescending<RoundRobinStandingRow> { it.points }
                .thenByDescending { it.wins }
                .thenByDescending { it.goalDiff }
        )
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var p = 2
        while (p < n) p *= 2
        return p.coerceAtMost(128)
    }

    /**
     * Standard tournament seeding bracket tree ordering (e.g., for 8 -> [1,8, 4,5, 2,7, 3,6]).
     */
    private fun buildBalancedSeedOrder(size: Int): List<Int> {
        var seeds = listOf(1, 2)
        var currentSize = 2
        while (currentSize < size) {
            currentSize *= 2
            val next = mutableListOf<Int>()
            for (s in seeds) {
                next.add(s)
                next.add(currentSize + 1 - s)
            }
            seeds = next
        }
        return seeds
    }

    /**
     * Places BYEs evenly across Round 1 slots so top slots get BYEs rather than pairing two BYEs together.
     */
    private fun padWithByes(participants: List<TournamentParticipant>): List<TournamentParticipant?> {
        val bracketSize = nextPowerOfTwo(participants.size)
        val totalMatches = bracketSize / 2
        val byesCount = bracketSize - participants.size
        val result = MutableList<TournamentParticipant?>(bracketSize) { null }
        var pIdx = 0
        // First assign 1 player to every Round 1 match
        for (m in 0 until totalMatches) {
            if (pIdx < participants.size) {
                result[m * 2] = participants[pIdx++]
            }
        }
        // Then fill second slot for matches without BYE (leave last `byesCount` matches as BYEs)
        val fullMatches = totalMatches - byesCount
        for (m in 0 until fullMatches) {
            if (pIdx < participants.size) {
                result[m * 2 + 1] = participants[pIdx++]
            }
        }
        return result
    }

    private val BANNED_WORDS = listOf(
        "fuck", "shit", "bitch", "asshole", "bastard", "dick", "idiot", "scam", "cheater_slur"
    )

    fun filterProfanity(message: String): String {
        var cleaned = message
        BANNED_WORDS.forEach { word ->
            val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            cleaned = regex.replace(cleaned) { "*".repeat(it.value.length) }
        }
        return cleaned
    }
}
