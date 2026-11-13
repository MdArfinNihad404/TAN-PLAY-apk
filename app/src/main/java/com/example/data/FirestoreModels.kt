package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarInitial: String = "T",
    val contactInfo: String = "",
    val primaryGame: String = "FC Mobile",
    val role: String = "player", // "player", "organizer", "admin"
    val freeFireUid: String = "",
    val freeFireIgn: String = "",
    val freeFireLevel: String = "",
    val fcMobileUid: String = "",
    val fcMobileOvr: String = "",
    val efootballId: String = "",
    val efootballPlatform: String = "Android",
    val dlsTeamName: String = "",
    val dlsTeamRating: String = "",
    val pubgPlayerId: String = "",
    val pubgNickname: String = "",
    val termsAccepted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val tournamentId: String = "",
    val matchId: String = "",
    @get:PropertyName("isRead") @field:PropertyName("isRead")
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlayerStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gamerTag: String = "",
    val primaryGame: String = "FC Mobile",
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val rating: Int = 1000,
    @get:PropertyName("isPublic") @field:PropertyName("isPublic")
    val isPublic: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val game: String = "FC Mobile",
    val customGameName: String = "",
    val customRequiredFields: List<String> = emptyList(),
    val description: String = "",
    val rules: String = "",
    val format: String = "Single Elimination", // "Single Elimination", "Double Elimination", "Round Robin"
    val participationType: String = "Individual", // "Individual", "Team"
    val maxTeamSize: Int = 4,
    val maxParticipants: Int = 16,
    val participantCount: Int = 0,
    val registrationDeadlineText: String = "",
    val startTimeText: String = "",
    val matchDurationMinutes: Int = 20,
    val organizerId: String = "",
    val organizerName: String = "",
    val inviteCode: String = "",
    val status: String = "Open", // "Open", "In Progress", "Completed", "Cancelled"
    val seedingMode: String = "Random", // "Random", "Seeded", "Manual"
    val roomId: String = "",
    val roomPassword: String = "",
    val prizePoolText: String = "Glory & Champion Badge",
    val prizePoolAmount: Int = 0,
    val prizeSplitNote: String = "1st 60% • 2nd 25% • 3rd 15%",
    @get:PropertyName("isPublic") @field:PropertyName("isPublic")
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val gameFields: Map<String, String> = emptyMap(),
    val publicStatLabel: String = "",
    val sensitiveIdField: String = "",
    val rankingMetric: Int = 0,
    val seedNumber: Int = 0,
    val auditLog: List<String> = emptyList(),
    @ServerTimestamp val registeredAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TournamentTeam(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val name: String = "",
    val teamCode: String = "",
    val captainUserId: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val aggregatedStrength: Int = 0,
    val status: String = "Open", // "Open", "Locked"
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMatch(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val organizerId: String = "",
    val roundNumber: Int = 1,
    val roundName: String = "Round 1",
    val matchNumber: Int = 1,
    val participant1Id: String = "",
    val participant1UserId: String = "",
    val participant1Name: String = "TBD",
    val participant1Stat: String = "",
    val participant1GameId: String = "",
    val participant2Id: String = "",
    val participant2UserId: String = "",
    val participant2Name: String = "TBD",
    val participant2Stat: String = "",
    val participant2GameId: String = "",
    val participant1Ready: Boolean = false,
    val participant2Ready: Boolean = false,
    val score1: Int? = null,
    val score2: Int? = null,
    val submittedByUserId: String = "",
    val proofImageUri: String = "",
    val proofNote: String = "",
    val status: String = "Pending", // "Pending", "Scheduled", "Ready", "Awaiting Results", "Disputed", "Completed", "Cancelled"
    val winnerParticipantId: String = "",
    val winnerUserId: String = "",
    val winnerName: String = "",
    val nextMatchId: String = "",
    val nextMatchSlot: Int = 0, // 1 or 2, 0 if final
    val roomId: String = "",
    val roomPassword: String = "",
    val scheduledTimeText: String = "",
    val auditLog: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TournamentChatMessage(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "player", // "organizer", "captain", "admin", "player"
    val teamName: String = "",
    val channel: String = "lobby", // "lobby" | "captains"
    @get:PropertyName("isPinned") @field:PropertyName("isPinned")
    val isPinned: Boolean = false,
    val text: String = "",
    @get:PropertyName("isReported") @field:PropertyName("isReported")
    val isReported: Boolean = false,
    val reportReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val type: String = "Match Dispute", // "Match Dispute", "Chat Abuse", "Tournament Issue"
    val tournamentId: String = "",
    val matchId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val status: String = "Open", // "Open", "Resolved", "Dismissed"
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
