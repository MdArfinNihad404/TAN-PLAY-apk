package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "TournamentRepo"

class FirestoreTournamentRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth = FirebaseAuth.getInstance(db.app)

    // --- USER PROFILE & PRIVACY ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun syncGamerProfile(
        userId: String,
        email: String?,
        displayName: String?,
        gamerTag: String,
        primaryGame: String,
        termsAccepted: Boolean = true
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val cleanUsername = gamerTag.trim().ifEmpty {
                displayName?.trim()?.take(20) ?: "Player_${userId.take(5)}"
            }
            val existing = db.collection("users").document(userId).get().await()
            if (existing.exists()) {
                db.collection("users").document(userId).update(
                    mapOf(
                        "username" to cleanUsername,
                        "email" to (email ?: ""),
                        "displayName" to (displayName ?: cleanUsername),
                        "avatarInitial" to cleanUsername.first().uppercaseChar().toString(),
                        "primaryGame" to primaryGame,
                        "termsAccepted" to termsAccepted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val role = if (email.equals("nmd634384@gmail.com", ignoreCase = true)) "admin" else "player"
                val userPayload = mapOf(
                    "userId" to userId,
                    "username" to cleanUsername,
                    "email" to (email ?: ""),
                    "displayName" to (displayName ?: cleanUsername),
                    "avatarInitial" to cleanUsername.first().uppercaseChar().toString(),
                    "contactInfo" to "",
                    "primaryGame" to primaryGame,
                    "role" to role,
                    "freeFireUid" to "",
                    "freeFireIgn" to "",
                    "freeFireLevel" to "",
                    "fcMobileUid" to "",
                    "fcMobileOvr" to "",
                    "efootballId" to "",
                    "efootballPlatform" to "Android",
                    "dlsTeamName" to "",
                    "dlsTeamRating" to "",
                    "pubgPlayerId" to "",
                    "pubgNickname" to "",
                    "termsAccepted" to termsAccepted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(userId).set(userPayload).await()
            }
            ensureOwnPlayerStat(userId, cleanUsername, primaryGame)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileDetails(profile: UserProfile): Result<Unit> {
        val uid = profile.userId.ifEmpty { auth.currentUser?.uid.orEmpty() }
        if (uid.isEmpty()) return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            val cleanName = profile.username.trim().ifEmpty { "Player_${uid.take(5)}" }
            val existing = db.collection("users").document(uid).get().await()
            val payload = mapOf(
                "userId" to uid,
                "username" to cleanName,
                "email" to profile.email,
                "displayName" to profile.displayName.ifBlank { cleanName },
                "avatarInitial" to cleanName.first().uppercaseChar().toString(),
                "contactInfo" to profile.contactInfo.trim(),
                "primaryGame" to profile.primaryGame,
                "role" to (existing.getString("role") ?: "player"),
                "freeFireUid" to profile.freeFireUid.trim(),
                "freeFireIgn" to profile.freeFireIgn.trim(),
                "freeFireLevel" to profile.freeFireLevel.trim(),
                "fcMobileUid" to profile.fcMobileUid.trim(),
                "fcMobileOvr" to profile.fcMobileOvr.trim(),
                "efootballId" to profile.efootballId.trim(),
                "efootballPlatform" to profile.efootballPlatform.trim().ifEmpty { "Android" },
                "dlsTeamName" to profile.dlsTeamName.trim(),
                "dlsTeamRating" to profile.dlsTeamRating.trim(),
                "pubgPlayerId" to profile.pubgPlayerId.trim(),
                "pubgNickname" to profile.pubgNickname.trim(),
                "termsAccepted" to profile.termsAccepted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing.exists()) {
                db.collection("users").document(uid).update(payload).await()
            } else {
                db.collection("users").document(uid).set(
                    payload + mapOf("createdAt" to FieldValue.serverTimestamp())
                ).await()
            }
            ensureOwnPlayerStat(uid, cleanName, profile.primaryGame)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun exportUserDataJson(userId: String): Result<String> {
        return try {
            val userSnap = db.collection("users").document(userId).get().await()
            val statSnap = db.collection("playerStats").document(userId).get().await()
            val alertSnaps = db.collection("users").document(userId).collection("alerts").get().await()

            val json = JSONObject().apply {
                put("userId", userId)
                put("profile", JSONObject(userSnap.data ?: emptyMap<String, Any>()))
                put("competitiveStats", JSONObject(statSnap.data ?: emptyMap<String, Any>()))
                put(
                    "alerts",
                    JSONArray(alertSnaps.documents.map { JSONObject(it.data ?: emptyMap<String, Any>()) })
                )
            }
            Result.success(json.toString(2))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(userId: String): Result<Unit> {
        return try {
            val alerts = db.collection("users").document(userId).collection("alerts").get().await()
            alerts.documents.forEach { it.reference.delete().await() }
            db.collection("playerStats").document(userId).delete().await()
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }

    // --- ALERTS ---

    fun observeAlerts(userId: String): Flow<List<UserAlert>> {
        val path = "users/$userId/alerts"
        return db.collection("users").document(userId).collection("alerts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { it.toObjects(UserAlert::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendAlert(
        targetUserId: String,
        title: String,
        message: String,
        tournamentId: String = "",
        matchId: String = ""
    ) {
        if (targetUserId.isBlank()) return
        val alertId = "al_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "users/$targetUserId/alerts/$alertId"
        try {
            db.collection("users").document(targetUserId).collection("alerts").document(alertId)
                .set(
                    mapOf(
                        "userId" to targetUserId,
                        "title" to title.take(100),
                        "message" to message.take(300),
                        "tournamentId" to tournamentId,
                        "matchId" to matchId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal alert send issue at $path: ${e.message}")
        }
    }

    suspend fun markAlertRead(userId: String, alertId: String): Result<Unit> {
        val path = "users/$userId/alerts/$alertId"
        return try {
            db.collection("users").document(userId).collection("alerts").document(alertId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PUBLIC LEADERBOARD STATS ---

    fun observePlayerStats(): Flow<List<PlayerStat>> {
        val path = "playerStats"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlayerStat::class.java)
                    .sortedWith(compareByDescending<PlayerStat> { it.rating }.thenByDescending { it.wins })
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private suspend fun ensureOwnPlayerStat(userId: String, gamerTag: String, primaryGame: String) {
        try {
            val docRef = db.collection("playerStats").document(userId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "gamerTag" to gamerTag.take(30),
                        "primaryGame" to primaryGame.take(40),
                        "matchesPlayed" to 0,
                        "wins" to 0,
                        "rating" to 1000,
                        "isPublic" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "gamerTag" to gamerTag.take(30),
                        "primaryGame" to primaryGame.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "PlayerStat sync warning: ${e.message}")
        }
    }

    suspend fun recordOwnMatchOutcomeStat(userId: String, won: Boolean) {
        if (auth.currentUser?.uid != userId) return
        try {
            val docRef = db.collection("playerStats").document(userId)
            val snap = docRef.get().await()
            val currentPlayed = snap.getLong("matchesPlayed")?.toInt() ?: 0
            val currentWins = snap.getLong("wins")?.toInt() ?: 0
            val currentRating = snap.getLong("rating")?.toInt() ?: 1000
            val gamerTag = snap.getString("gamerTag")
                ?: auth.currentUser?.displayName?.take(30)
                ?: "Player_${userId.take(5)}"

            val payload = mapOf(
                "userId" to userId,
                "gamerTag" to gamerTag,
                "primaryGame" to (snap.getString("primaryGame") ?: "FC Mobile"),
                "matchesPlayed" to (currentPlayed + 1),
                "wins" to (currentWins + if (won) 1 else 0),
                "rating" to maxOf(100, currentRating + if (won) 25 else -10),
                "isPublic" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (snap.exists()) docRef.update(payload).await() else docRef.set(payload).await()
        } catch (e: Exception) {
            Log.w(TAG, "Own stat update skipped: ${e.message}")
        }
    }

    // --- TOURNAMENTS (ALL FREE) ---

    fun observeTournaments(): Flow<List<FirestoreTournament>> {
        val path = "tournaments"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreTournament::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createTournament(
        title: String,
        game: String,
        customGameName: String = "",
        customRequiredFields: List<String> = emptyList(),
        description: String,
        rules: String,
        format: String,
        participationType: String,
        maxTeamSize: Int,
        maxParticipants: Int,
        registrationDeadlineText: String,
        startTimeText: String,
        matchDurationMinutes: Int,
        seedingMode: String = "Random",
        roomId: String = "",
        roomPassword: String = "",
        prizePoolText: String = "Glory & Champion Badge",
        prizePoolAmount: Int = 0,
        prizeSplitNote: String = "1st 60% • 2nd 25% • 3rd 15%",
        organizerName: String
    ): Result<FirestoreTournament> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val tournamentId = "trn_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val inviteCode = "TAN-${tournamentId.takeLast(6).uppercase()}"
        val path = "tournaments/$tournamentId"
        val cleanPrizeText = prizePoolText.trim().ifEmpty { "Glory & Champion Badge" }.take(80)
        val safePrizeAmount = prizePoolAmount.coerceIn(0, 10_000_000)
        val cleanPrizeSplit = prizeSplitNote.trim().ifEmpty { "1st 60% • 2nd 25% • 3rd 15%" }.take(120)

        return try {
            val payload = mapOf(
                "title" to title.trim().take(120),
                "game" to game.trim().take(60),
                "customGameName" to customGameName.trim().take(60),
                "customRequiredFields" to customRequiredFields.filter { it.isNotBlank() }.take(5),
                "description" to description.trim().take(600),
                "rules" to rules.trim().take(1200),
                "format" to format,
                "participationType" to participationType,
                "maxTeamSize" to maxTeamSize.coerceIn(1, 10),
                "maxParticipants" to maxParticipants.coerceIn(2, 128),
                "participantCount" to 0,
                "registrationDeadlineText" to registrationDeadlineText.trim().take(60),
                "startTimeText" to startTimeText.trim().take(60),
                "matchDurationMinutes" to matchDurationMinutes.coerceIn(5, 180),
                "organizerId" to user.uid,
                "organizerName" to organizerName.trim().ifEmpty { "Organizer" }.take(60),
                "inviteCode" to inviteCode,
                "status" to "Open",
                "seedingMode" to seedingMode,
                "roomId" to roomId.trim().take(40),
                "roomPassword" to roomPassword.trim().take(40),
                "prizePoolText" to cleanPrizeText,
                "prizePoolAmount" to safePrizeAmount,
                "prizeSplitNote" to cleanPrizeSplit,
                "isPublic" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("tournaments").document(tournamentId).set(payload).await()
            val created = FirestoreTournament(
                id = tournamentId,
                title = title.trim(),
                game = game.trim(),
                customGameName = customGameName.trim(),
                customRequiredFields = customRequiredFields,
                description = description.trim(),
                rules = rules.trim(),
                format = format,
                participationType = participationType,
                maxTeamSize = maxTeamSize.coerceIn(1, 10),
                maxParticipants = maxParticipants.coerceIn(2, 128),
                participantCount = 0,
                registrationDeadlineText = registrationDeadlineText.trim(),
                startTimeText = startTimeText.trim(),
                matchDurationMinutes = matchDurationMinutes.coerceIn(5, 180),
                organizerId = user.uid,
                organizerName = organizerName.trim().ifEmpty { "Organizer" },
                inviteCode = inviteCode,
                status = "Open",
                seedingMode = seedingMode,
                roomId = roomId.trim(),
                roomPassword = roomPassword.trim(),
                prizePoolText = cleanPrizeText,
                prizePoolAmount = safePrizeAmount,
                prizeSplitNote = cleanPrizeSplit,
                isPublic = true
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun findTournamentByInviteCode(codeOrId: String): Result<FirestoreTournament?> {
        val clean = codeOrId.trim()
            .removePrefix("tanplay://join/")
            .trim()
        if (clean.isEmpty()) return Result.success(null)
        return try {
            val byCode = db.collection("tournaments")
                .whereEqualTo("isPublic", true)
                .whereEqualTo("inviteCode", clean.uppercase())
                .limit(1)
                .get()
                .await()
            if (!byCode.isEmpty) {
                return Result.success(byCode.documents.first().toObject(FirestoreTournament::class.java))
            }
            val byId = db.collection("tournaments").document(clean).get().await()
            Result.success(byId.toObject(FirestoreTournament::class.java))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "tournaments/$clean")
            Result.failure(e)
        }
    }

    suspend fun cancelTournament(tournamentId: String, reason: String = "Cancelled by organizer/admin"): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "status" to "Cancelled",
                    "description" to reason.take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PARTICIPANTS & TEAMS ---

    fun observeParticipants(tournamentId: String): Flow<List<TournamentParticipant>> {
        val path = "tournaments/$tournamentId/participants"
        return db.collection("tournaments").document(tournamentId).collection("participants")
            .whereEqualTo("tournamentId", tournamentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TournamentParticipant::class.java)
                    .sortedWith(compareBy<TournamentParticipant> { if (it.seedNumber == 0) Int.MAX_VALUE else it.seedNumber }.thenByDescending { it.rankingMetric })
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun joinOrUpdateParticipant(
        tournament: FirestoreTournament,
        displayName: String,
        gameFields: Map<String, String>,
        teamId: String = "",
        teamName: String = ""
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val participantId = user.uid
        val path = "tournaments/${tournament.id}/participants/$participantId"
        val effectiveGame = if (tournament.game == "Custom Game" && tournament.customGameName.isNotBlank()) {
            tournament.customGameName
        } else {
            tournament.game
        }

        val validationError = BracketEngine.validateGameFields(
            game = effectiveGame,
            customRequiredFields = tournament.customRequiredFields,
            values = gameFields
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val publicStat = BracketEngine.extractPublicStatLabel(
            game = effectiveGame,
            customRequiredFields = tournament.customRequiredFields,
            values = gameFields
        )
        val sensitiveId = BracketEngine.extractSensitiveId(
            game = effectiveGame,
            customRequiredFields = tournament.customRequiredFields,
            values = gameFields
        )
        val metric = BracketEngine.extractRankingMetric(
            game = effectiveGame,
            customRequiredFields = tournament.customRequiredFields,
            values = gameFields
        )

        return try {
            val partRef = db.collection("tournaments").document(tournament.id)
                .collection("participants").document(participantId)
            val existing = partRef.get().await()

            if (existing.exists()) {
                val oldFields = (existing.get("gameFields") as? Map<*, *>)?.entries
                    ?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
                val oldAudit = (existing.get("auditLog") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val newEntry = "Updated fields (prev: $oldFields)"
                partRef.update(
                    mapOf(
                        "displayName" to displayName.trim().take(40),
                        "teamId" to teamId,
                        "teamName" to teamName.trim().take(50),
                        "gameFields" to gameFields,
                        "publicStatLabel" to publicStat.take(60),
                        "sensitiveIdField" to sensitiveId.take(60),
                        "rankingMetric" to metric,
                        "auditLog" to (oldAudit + newEntry).takeLast(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val tournRef = db.collection("tournaments").document(tournament.id)
                val tournSnap = tournRef.get().await()
                val currentCount = tournSnap.getLong("participantCount")?.toInt() ?: 0
                val maxPart = tournSnap.getLong("maxParticipants")?.toInt() ?: tournament.maxParticipants
                if (currentCount >= maxPart) {
                    return Result.failure(IllegalStateException("Tournament is already full ($currentCount/$maxPart)."))
                }
                val payload = mapOf(
                    "tournamentId" to tournament.id,
                    "userId" to user.uid,
                    "displayName" to displayName.trim().take(40),
                    "teamId" to teamId,
                    "teamName" to teamName.trim().take(50),
                    "gameFields" to gameFields,
                    "publicStatLabel" to publicStat.take(60),
                    "sensitiveIdField" to sensitiveId.take(60),
                    "rankingMetric" to metric,
                    "seedNumber" to 0,
                    "auditLog" to listOf("Registered as ${displayName.trim()}"),
                    "registeredAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                partRef.set(payload).await()
                tournRef.update(
                    mapOf(
                        "participantCount" to (currentCount + 1).coerceAtMost(maxPart),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendAlert(
                    targetUserId = tournament.organizerId,
                    title = "New Participant Joined",
                    message = "${displayName.trim()} registered for ${tournament.title}.",
                    tournamentId = tournament.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeTeams(tournamentId: String): Flow<List<TournamentTeam>> {
        val path = "tournaments/$tournamentId/teams"
        return db.collection("tournaments").document(tournamentId).collection("teams")
            .whereEqualTo("tournamentId", tournamentId)
            .snapshots()
            .map { it.toObjects(TournamentTeam::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createTeam(
        tournamentId: String,
        teamName: String,
        captainDisplayName: String,
        strength: Int
    ): Result<TournamentTeam> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val teamId = "tm_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val teamCode = teamId.takeLast(6).uppercase()
        val path = "tournaments/$tournamentId/teams/$teamId"
        return try {
            db.collection("tournaments").document(tournamentId).collection("teams").document(teamId)
                .set(
                    mapOf(
                        "tournamentId" to tournamentId,
                        "name" to teamName.trim().take(40),
                        "teamCode" to teamCode,
                        "captainUserId" to user.uid,
                        "memberIds" to listOf(user.uid),
                        "memberNames" to listOf(captainDisplayName.trim().take(40)),
                        "aggregatedStrength" to strength.coerceIn(0, 100000),
                        "status" to "Open",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(
                TournamentTeam(
                    id = teamId,
                    tournamentId = tournamentId,
                    name = teamName.trim(),
                    teamCode = teamCode,
                    captainUserId = user.uid,
                    memberIds = listOf(user.uid),
                    memberNames = listOf(captainDisplayName.trim()),
                    aggregatedStrength = strength.coerceIn(0, 100000),
                    status = "Open"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinTeamByCode(
        tournamentId: String,
        teamCode: String,
        memberDisplayName: String,
        memberStrength: Int,
        maxTeamSize: Int
    ): Result<TournamentTeam> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            val snap = db.collection("tournaments").document(tournamentId).collection("teams")
                .whereEqualTo("tournamentId", tournamentId)
                .whereEqualTo("teamCode", teamCode.trim().uppercase())
                .limit(1)
                .get()
                .await()
            if (snap.isEmpty) {
                return Result.failure(IllegalArgumentException("No team found with code $teamCode"))
            }
            val doc = snap.documents.first()
            val team = doc.toObject(TournamentTeam::class.java)
                ?: return Result.failure(IllegalStateException("Invalid team record"))
            if (team.status != "Open") {
                return Result.failure(IllegalStateException("Team '${team.name}' is locked."))
            }
            if (user.uid in team.memberIds) {
                return Result.success(team)
            }
            if (team.memberIds.size >= maxTeamSize) {
                return Result.failure(IllegalStateException("Team '${team.name}' is full (${team.memberIds.size}/$maxTeamSize)."))
            }
            val newIds = team.memberIds + user.uid
            val newNames = team.memberNames + memberDisplayName.trim().take(40)
            val newStrength = (team.aggregatedStrength + memberStrength).coerceIn(0, 100000)
            doc.reference.update(
                mapOf(
                    "memberIds" to newIds,
                    "memberNames" to newNames,
                    "aggregatedStrength" to newStrength
                )
            ).await()
            Result.success(team.copy(memberIds = newIds, memberNames = newNames, aggregatedStrength = newStrength))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId/teams")
            Result.failure(e)
        }
    }

    suspend fun toggleTeamLock(tournamentId: String, team: TournamentTeam): Result<Unit> {
        val newStatus = if (team.status == "Open") "Locked" else "Open"
        return try {
            db.collection("tournaments").document(tournamentId).collection("teams").document(team.id)
                .update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/$tournamentId/teams/${team.id}")
            Result.failure(e)
        }
    }

    // --- BRACKET DRAW & REAL-TIME MATCHES ---

    fun observeMatches(tournamentId: String): Flow<List<FirestoreMatch>> {
        val path = "tournaments/$tournamentId/matches"
        return db.collection("tournaments").document(tournamentId).collection("matches")
            .whereEqualTo("tournamentId", tournamentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreMatch::class.java)
                    .sortedWith(compareBy<FirestoreMatch> { it.roundNumber }.thenBy { it.matchNumber })
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun startTournamentAndDrawBracket(
        tournament: FirestoreTournament,
        participants: List<TournamentParticipant>,
        seedingMode: String,
        manualPairs: List<Pair<TournamentParticipant, TournamentParticipant?>> = emptyList()
    ): Result<List<FirestoreMatch>> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (participants.size < 2) {
            return Result.failure(IllegalStateException("At least 2 participants are required to draw a bracket."))
        }
        val path = "tournaments/${tournament.id}/matches"
        return try {
            val generated = if (tournament.format == "Round Robin") {
                BracketEngine.generateRoundRobinMatches(tournament, participants, seedingMode)
            } else {
                BracketEngine.generateSingleEliminationBracket(tournament, participants, seedingMode, manualPairs)
            }

            // Persist seed numbers on participants
            generated.seededParticipants.forEach { p ->
                if (p.id.isNotEmpty()) {
                    db.collection("tournaments").document(tournament.id)
                        .collection("participants").document(p.id)
                        .update(
                            mapOf(
                                "seedNumber" to p.seedNumber,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
            }

            // Write all generated match nodes cleanly (without "id" field)
            generated.matches.forEach { m ->
                val matchMap = mapOf(
                    "tournamentId" to tournament.id,
                    "organizerId" to tournament.organizerId.ifEmpty { user.uid },
                    "roundNumber" to m.roundNumber,
                    "roundName" to m.roundName,
                    "matchNumber" to m.matchNumber,
                    "participant1Id" to m.participant1Id,
                    "participant1UserId" to m.participant1UserId,
                    "participant1Name" to m.participant1Name,
                    "participant1Stat" to m.participant1Stat,
                    "participant1GameId" to m.participant1GameId,
                    "participant2Id" to m.participant2Id,
                    "participant2UserId" to m.participant2UserId,
                    "participant2Name" to m.participant2Name,
                    "participant2Stat" to m.participant2Stat,
                    "participant2GameId" to m.participant2GameId,
                    "participant1Ready" to m.participant1Ready,
                    "participant2Ready" to m.participant2Ready,
                    "score1" to m.score1,
                    "score2" to m.score2,
                    "submittedByUserId" to m.submittedByUserId,
                    "proofImageUri" to m.proofImageUri,
                    "proofNote" to m.proofNote,
                    "status" to m.status,
                    "winnerParticipantId" to m.winnerParticipantId,
                    "winnerUserId" to m.winnerUserId,
                    "winnerName" to m.winnerName,
                    "nextMatchId" to m.nextMatchId,
                    "nextMatchSlot" to m.nextMatchSlot,
                    "roomId" to m.roomId,
                    "roomPassword" to m.roomPassword,
                    "scheduledTimeText" to m.scheduledTimeText,
                    "auditLog" to m.auditLog,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("tournaments").document(tournament.id)
                    .collection("matches").document(m.id)
                    .set(matchMap).await()
            }

            // Transition tournament to In Progress
            db.collection("tournaments").document(tournament.id).update(
                mapOf(
                    "status" to "In Progress",
                    "seedingMode" to seedingMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Notify registered players
            participants.forEach { p ->
                sendAlert(
                    targetUserId = p.userId,
                    title = "Bracket Draw Live!",
                    message = "${tournament.title} bracket ($seedingMode) is live. Check your Round 1 opponent!",
                    tournamentId = tournament.id
                )
            }

            Result.success(generated.matches)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markPlayerReady(tournamentId: String, match: FirestoreMatch): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "tournaments/$tournamentId/matches/${match.id}"
        return try {
            val isP1 = uid == match.participant1UserId
            val isP2 = uid == match.participant2UserId
            if (!isP1 && !isP2) {
                return Result.failure(IllegalStateException("You are not a participant in this match."))
            }
            val newP1Ready = if (isP1) true else match.participant1Ready
            val newP2Ready = if (isP2) true else match.participant2Ready
            val newStatus = if (newP1Ready && newP2Ready) "Ready" else match.status

            db.collection("tournaments").document(tournamentId)
                .collection("matches").document(match.id)
                .update(
                    mapOf(
                        "participant1Ready" to newP1Ready,
                        "participant2Ready" to newP2Ready,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val opponentUid = if (isP1) match.participant2UserId else match.participant1UserId
            val myName = if (isP1) match.participant1Name else match.participant2Name
            if (opponentUid.isNotEmpty()) {
                sendAlert(
                    targetUserId = opponentUid,
                    title = if (newStatus == "Ready") "Match Ready to Start!" else "Opponent Ready",
                    message = "$myName clicked Ready for ${match.roundName}. Open Match Lobby to view Game ID.",
                    tournamentId = tournamentId,
                    matchId = match.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitMatchResult(
        tournamentId: String,
        match: FirestoreMatch,
        score1: Int,
        score2: Int,
        proofImageUri: String,
        proofNote: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "tournaments/$tournamentId/matches/${match.id}"
        return try {
            val submitterName = when (uid) {
                match.participant1UserId -> match.participant1Name
                match.participant2UserId -> match.participant2Name
                else -> "Organizer"
            }
            val updatedAudit = (match.auditLog + "$submitterName submitted score $score1-$score2").takeLast(30)
            db.collection("tournaments").document(tournamentId)
                .collection("matches").document(match.id)
                .update(
                    mapOf(
                        "score1" to score1.coerceIn(0, 999),
                        "score2" to score2.coerceIn(0, 999),
                        "submittedByUserId" to uid,
                        "proofImageUri" to proofImageUri.take(500),
                        "proofNote" to proofNote.trim().take(300),
                        "status" to "Awaiting Results",
                        "auditLog" to updatedAudit,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val opponentUid = if (uid == match.participant1UserId) match.participant2UserId else match.participant1UserId
            if (opponentUid.isNotEmpty()) {
                sendAlert(
                    targetUserId = opponentUid,
                    title = "Confirm Match Result",
                    message = "$submitterName reported $score1-$score2 in ${match.roundName}. Please Confirm or Dispute.",
                    tournamentId = tournamentId,
                    matchId = match.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun confirmMatchResultAndAdvance(
        tournament: FirestoreTournament,
        match: FirestoreMatch
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "tournaments/${tournament.id}/matches/${match.id}"
        return try {
            val s1 = match.score1 ?: 0
            val s2 = match.score2 ?: 0
            val p1Won = s1 >= s2
            val winnerPartId = if (p1Won) match.participant1Id else match.participant2Id
            val winnerUid = if (p1Won) match.participant1UserId else match.participant2UserId
            val winnerName = if (p1Won) match.participant1Name else match.participant2Name
            val winnerStat = if (p1Won) match.participant1Stat else match.participant2Stat
            val winnerGameId = if (p1Won) match.participant1GameId else match.participant2GameId

            val confirmerLabel = when (uid) {
                match.participant1UserId -> match.participant1Name
                match.participant2UserId -> match.participant2Name
                else -> "Organizer"
            }
            val updatedAudit = (match.auditLog + "$confirmerLabel confirmed $s1-$s2 (Winner: $winnerName)").takeLast(30)

            db.collection("tournaments").document(tournament.id)
                .collection("matches").document(match.id)
                .update(
                    mapOf(
                        "status" to "Completed",
                        "winnerParticipantId" to winnerPartId,
                        "winnerUserId" to winnerUid,
                        "winnerName" to winnerName,
                        "auditLog" to updatedAudit,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            advanceWinnerToNextMatch(
                tournament = tournament,
                currentMatch = match,
                winnerPartId = winnerPartId,
                winnerUid = winnerUid,
                winnerName = winnerName,
                winnerStat = winnerStat,
                winnerGameId = winnerGameId
            )

            // Update own competitive stats safely without cross-user write
            if (uid == match.participant1UserId || uid == match.participant2UserId) {
                recordOwnMatchOutcomeStat(uid, won = (uid == winnerUid))
            }

            if (winnerUid.isNotEmpty()) {
                sendAlert(
                    targetUserId = winnerUid,
                    title = "Victory Confirmed!",
                    message = "You won ${match.roundName} ($s1-$s2) in ${tournament.title}!",
                    tournamentId = tournament.id,
                    matchId = match.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun advanceWinnerToNextMatch(
        tournament: FirestoreTournament,
        currentMatch: FirestoreMatch,
        winnerPartId: String,
        winnerUid: String,
        winnerName: String,
        winnerStat: String,
        winnerGameId: String
    ) {
        if (currentMatch.nextMatchId.isNotEmpty() && currentMatch.nextMatchSlot in 1..2) {
            val nextRef = db.collection("tournaments").document(tournament.id)
                .collection("matches").document(currentMatch.nextMatchId)
            val nextSnap = nextRef.get().await()
            if (nextSnap.exists()) {
                val otherSlotFilled = if (currentMatch.nextMatchSlot == 1) {
                    !nextSnap.getString("participant2Id").isNullOrEmpty()
                } else {
                    !nextSnap.getString("participant1Id").isNullOrEmpty()
                }
                val newNextStatus = if (otherSlotFilled) "Scheduled" else "Pending"
                val slotUpdates = if (currentMatch.nextMatchSlot == 1) {
                    mapOf(
                        "participant1Id" to winnerPartId,
                        "participant1UserId" to winnerUid,
                        "participant1Name" to winnerName,
                        "participant1Stat" to winnerStat,
                        "participant1GameId" to winnerGameId,
                        "status" to newNextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    mapOf(
                        "participant2Id" to winnerPartId,
                        "participant2UserId" to winnerUid,
                        "participant2Name" to winnerName,
                        "participant2Stat" to winnerStat,
                        "participant2GameId" to winnerGameId,
                        "status" to newNextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                }
                nextRef.update(slotUpdates).await()
            }
        } else {
            // Final match completed — if caller is organizer, transition tournament to Completed
            if (auth.currentUser?.uid == tournament.organizerId) {
                db.collection("tournaments").document(tournament.id).update(
                    mapOf(
                        "status" to "Completed",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun disputeMatchResult(
        tournament: FirestoreTournament,
        match: FirestoreMatch,
        reason: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "tournaments/${tournament.id}/matches/${match.id}"
        return try {
            val reporterName = when (user.uid) {
                match.participant1UserId -> match.participant1Name
                match.participant2UserId -> match.participant2Name
                else -> user.displayName ?: "Player"
            }
            val updatedAudit = (match.auditLog + "$reporterName disputed: ${reason.take(80)}").takeLast(30)
            db.collection("tournaments").document(tournament.id)
                .collection("matches").document(match.id)
                .update(
                    mapOf(
                        "status" to "Disputed",
                        "auditLog" to updatedAudit,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("reports").document(reportId).set(
                mapOf(
                    "type" to "Match Dispute",
                    "tournamentId" to tournament.id,
                    "matchId" to match.id,
                    "reporterId" to user.uid,
                    "reporterName" to reporterName.take(50),
                    "reason" to reason.trim().take(300),
                    "status" to "Open",
                    "resolutionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            sendAlert(
                targetUserId = tournament.organizerId,
                title = "Match Disputed",
                message = "$reporterName disputed ${match.roundName} in ${tournament.title}.",
                tournamentId = tournament.id,
                matchId = match.id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminOrOrganizerOverrideMatch(
        tournament: FirestoreTournament,
        match: FirestoreMatch,
        winnerSlot: Int, // 1 or 2
        score1: Int,
        score2: Int,
        resolutionNote: String
    ): Result<Unit> {
        val path = "tournaments/${tournament.id}/matches/${match.id}"
        return try {
            val p1Won = winnerSlot == 1
            val winnerPartId = if (p1Won) match.participant1Id else match.participant2Id
            val winnerUid = if (p1Won) match.participant1UserId else match.participant2UserId
            val winnerName = if (p1Won) match.participant1Name else match.participant2Name
            val winnerStat = if (p1Won) match.participant1Stat else match.participant2Stat
            val winnerGameId = if (p1Won) match.participant1GameId else match.participant2GameId

            val updatedAudit = (match.auditLog + "Official ruling: $winnerName ($score1-$score2) - $resolutionNote").takeLast(30)
            db.collection("tournaments").document(tournament.id)
                .collection("matches").document(match.id)
                .update(
                    mapOf(
                        "score1" to score1.coerceIn(0, 999),
                        "score2" to score2.coerceIn(0, 999),
                        "status" to "Completed",
                        "winnerParticipantId" to winnerPartId,
                        "winnerUserId" to winnerUid,
                        "winnerName" to winnerName,
                        "auditLog" to updatedAudit,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            advanceWinnerToNextMatch(
                tournament = tournament,
                currentMatch = match,
                winnerPartId = winnerPartId,
                winnerUid = winnerUid,
                winnerName = winnerName,
                winnerStat = winnerStat,
                winnerGameId = winnerGameId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- TOURNAMENT CHAT ---

    fun observeMessages(tournamentId: String): Flow<List<TournamentChatMessage>> {
        val path = "tournaments/$tournamentId/messages"
        return db.collection("tournaments").document(tournamentId).collection("messages")
            .whereEqualTo("tournamentId", tournamentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TournamentChatMessage::class.java)
                    .sortedWith(
                        compareBy<TournamentChatMessage> { it.createdAt?.seconds ?: Long.MAX_VALUE }
                            .thenBy { it.createdAt?.nanoseconds ?: Int.MAX_VALUE }
                    )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendChatMessage(
        tournamentId: String,
        senderName: String,
        rawText: String,
        senderRole: String = "player",
        teamName: String = "",
        channel: String = "lobby",
        isPinned: Boolean = false
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val filtered = BracketEngine.filterProfanity(rawText.trim()).take(300)
        if (filtered.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))

        val safeRole = if (senderRole in listOf("organizer", "captain", "admin", "player")) senderRole else "player"
        val safeChannel = if (channel in listOf("lobby", "captains")) channel else "lobby"
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "tournaments/$tournamentId/messages/$msgId"
        return try {
            db.collection("tournaments").document(tournamentId).collection("messages").document(msgId)
                .set(
                    mapOf(
                        "tournamentId" to tournamentId,
                        "senderId" to user.uid,
                        "senderName" to senderName.trim().ifEmpty { "Player" }.take(40),
                        "senderRole" to safeRole,
                        "teamName" to teamName.trim().take(50),
                        "channel" to safeChannel,
                        "isPinned" to (isPinned && (safeRole == "organizer" || safeRole == "admin")),
                        "text" to filtered,
                        "isReported" to false,
                        "reportReason" to "",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePinChatMessage(
        tournamentId: String,
        message: TournamentChatMessage
    ): Result<Unit> {
        val path = "tournaments/$tournamentId/messages/${message.id}"
        return try {
            db.collection("tournaments").document(tournamentId)
                .collection("messages").document(message.id)
                .update("isPinned", !message.isPinned)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reportChatMessage(
        tournamentId: String,
        message: TournamentChatMessage,
        reason: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "tournaments/$tournamentId/messages/${message.id}"
        return try {
            db.collection("tournaments").document(tournamentId).collection("messages").document(message.id)
                .update(
                    mapOf(
                        "isReported" to true,
                        "reportReason" to reason.trim().take(200)
                    )
                ).await()

            val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("reports").document(reportId).set(
                mapOf(
                    "type" to "Chat Abuse",
                    "tournamentId" to tournamentId,
                    "matchId" to message.id,
                    "reporterId" to user.uid,
                    "reporterName" to (user.displayName ?: "Player").take(50),
                    "reason" to "Message by ${message.senderName}: '${message.text.take(80)}' - $reason".take(300),
                    "status" to "Open",
                    "resolutionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- MODERATION REPORTS (ADMIN) ---

    fun observeReportsForAdmin(): Flow<List<ModerationReport>> {
        val path = "reports"
        return db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ModerationReport::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeMyReports(userId: String): Flow<List<ModerationReport>> {
        val path = "reports"
        return db.collection("reports")
            .whereEqualTo("reporterId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ModerationReport::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun resolveReport(reportId: String, status: String, note: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
                    "resolutionNote" to note.trim().take(300)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
