package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TournamentFirestoreRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun observeTournaments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreTournamentRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTournaments().first()
            }
        } catch (e: Exception) {
            caught = e
        }

        assertNotNull("Expected unauthenticated observeTournaments to throw", caught)
        val fsEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }

    @Test
    fun usersCollection_crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
        val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@test.com"

        val aliceUid = signInTestUser(aliceEmail)
        val repo = FirestoreTournamentRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncGamerProfile(
                userId = aliceUid,
                email = aliceEmail,
                displayName = "Alice",
                gamerTag = "AliceBD",
                primaryGame = "FC Mobile"
            ).getOrThrow()
        }

        signInTestUser(bobEmail)
        var crossReadEx: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            crossReadEx = e
        }
        assertNotNull("Cross-user read on users/{aliceUid} must fail", crossReadEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossReadEx?.code)
    }

    @Test
    fun tournamentLifecycle_createJoinDrawReadySubmitConfirmAndAdvance() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val organizerEmail = "org_$suffix@test.com"
        val p1Email = "p1_$suffix@test.com"
        val p2Email = "p2_$suffix@test.com"

        // 1. Organizer creates free tournament
        val orgUid = signInTestUser(organizerEmail)
        val orgRepo = FirestoreTournamentRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.syncGamerProfile(orgUid, organizerEmail, "Org", "OrgMaster", "FC Mobile").getOrThrow()
        }
        val tournament = withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.createTournament(
                title = "Rule Test Cup $suffix",
                game = "FC Mobile",
                description = "Free competitive cup",
                rules = "Screenshot proof required",
                format = "Single Elimination",
                participationType = "Individual",
                maxTeamSize = 1,
                maxParticipants = 8,
                registrationDeadlineText = "2026-12-01",
                startTimeText = "2026-12-02",
                matchDurationMinutes = 15,
                seedingMode = "Seeded",
                prizePoolText = "৳5,000 Sponsored Pool",
                prizePoolAmount = 5000,
                prizeSplitNote = "1st 60% • 2nd 25% • 3rd 15%",
                organizerName = "OrgMaster"
            ).getOrThrow()
        }
        assertEquals("Open", tournament.status)
        val persisted = withTimeout(FLOW_TIMEOUT_MS) {
            orgRepo.observeTournaments().first { list -> list.any { it.id == tournament.id } }
        }.first { it.id == tournament.id }
        assertEquals("৳5,000 Sponsored Pool", persisted.prizePoolText)
        assertEquals(5000, persisted.prizePoolAmount)
        assertEquals("2026-12-01", persisted.registrationDeadlineText)
        assertEquals("Screenshot proof required", persisted.rules)

        // 2. Player 1 joins
        val p1Uid = signInTestUser(p1Email)
        val p1Repo = FirestoreTournamentRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p1Repo.joinOrUpdateParticipant(
                tournament = tournament,
                displayName = "StrikerOne",
                gameFields = mapOf("uid" to "8844221100", "ovr" to "108")
            ).getOrThrow()
        }

        // 3. Player 2 joins (lower OVR so Player 1 & Player 2 meet or test dynamically)
        val p2Uid = signInTestUser(p2Email)
        val p2Repo = FirestoreTournamentRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p2Repo.joinOrUpdateParticipant(
                tournament = tournament,
                displayName = "StrikerTwo",
                gameFields = mapOf("uid" to "7733110099", "ovr" to "99")
            ).getOrThrow()
        }

        // 3b. Player 3 joins (3 players -> 1 BYE auto-advances highest seed to m_r2_1, other 2 play m_r1_1)
        val p3Email = "p3_$suffix@test.com"
        signInTestUser(p3Email)
        val p3Repo = FirestoreTournamentRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p3Repo.joinOrUpdateParticipant(
                tournament = tournament,
                displayName = "StrikerThree",
                gameFields = mapOf("uid" to "6622009988", "ovr" to "115")
            ).getOrThrow()
        }

        // 4. Organizer draws bracket
        signInTestUser(organizerEmail)
        val participants = withTimeout(FLOW_TIMEOUT_MS) {
            orgRepo.observeParticipants(tournament.id).first { it.size == 3 }
        }
        val matches = withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.startTournamentAndDrawBracket(tournament, participants, "Seeded").getOrThrow()
        }
        assertEquals(3, matches.size) // m_r1_1 (BYE), m_r1_2 (Scheduled), m_r2_1 (Grand Final)
        val playMatch = matches.first { it.roundNumber == 1 && it.status == "Scheduled" }

        // 5. Both players in playMatch click Ready
        val firstEmail = if (playMatch.participant1UserId == p1Uid) p1Email else p2Email
        val secondEmail = if (playMatch.participant1UserId == p1Uid) p2Email else p1Email

        signInTestUser(firstEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p1Repo.markPlayerReady(tournament.id, playMatch).getOrThrow()
        }
        val afterFirstReady = withTimeout(FLOW_TIMEOUT_MS) {
            p1Repo.observeMatches(tournament.id)
                .first { list -> list.first { it.id == playMatch.id }.let { it.participant1Ready || it.participant2Ready } }
                .first { it.id == playMatch.id }
        }

        signInTestUser(secondEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p2Repo.markPlayerReady(tournament.id, afterFirstReady).getOrThrow()
        }
        val readyMatch = withTimeout(FLOW_TIMEOUT_MS) {
            p2Repo.observeMatches(tournament.id)
                .first { list -> list.first { it.id == playMatch.id }.status == "Ready" }
                .first { it.id == playMatch.id }
        }
        assertTrue(readyMatch.participant1Ready && readyMatch.participant2Ready)

        // 6. First player submits score, second player confirms -> winner advances to m_r2_1
        signInTestUser(firstEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p1Repo.submitMatchResult(
                tournamentId = tournament.id,
                match = readyMatch,
                score1 = 3,
                score2 = 1,
                proofImageUri = "content://media/picker/101",
                proofNote = "GG clean match"
            ).getOrThrow()
        }
        val awaitingMatch = withTimeout(FLOW_TIMEOUT_MS) {
            p1Repo.observeMatches(tournament.id)
                .first { list -> list.first { it.id == playMatch.id }.status == "Awaiting Results" }
                .first { it.id == playMatch.id }
        }

        signInTestUser(secondEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            p2Repo.confirmMatchResultAndAdvance(tournament, awaitingMatch).getOrThrow()
        }

        val updatedMatches = withTimeout(FLOW_TIMEOUT_MS) {
            p2Repo.observeMatches(tournament.id).first { list ->
                val cur = list.first { it.id == playMatch.id }
                val fin = list.first { it.id == "m_r2_1" }
                cur.status == "Completed" && fin.status == "Scheduled"
            }
        }
        val completedMatch = updatedMatches.first { it.id == playMatch.id }
        val grandFinal = updatedMatches.first { it.id == "m_r2_1" }
        assertEquals(awaitingMatch.participant1Name, completedMatch.winnerName)
        assertTrue(
            "Confirmed winner must populate Grand Final slot",
            grandFinal.participant1Name == completedMatch.winnerName || grandFinal.participant2Name == completedMatch.winnerName
        )
    }

    @Test
    fun tournamentLobbyChat_captainsAndOrganizerRealtimeListenerWorks() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val organizerEmail = "chat_org_$suffix@test.com"
        val captainEmail = "chat_cap_$suffix@test.com"

        // 1. Organizer creates tournament
        signInTestUser(organizerEmail)
        val orgRepo = FirestoreTournamentRepository(firestore)
        val tournament = withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.createTournament(
                title = "Lobby Chat Cup $suffix",
                game = "Free Fire",
                description = "Squad tournament with live captain chat",
                rules = "Check in 10 mins early",
                format = "Single Elimination",
                participationType = "Team",
                maxTeamSize = 4,
                maxParticipants = 16,
                registrationDeadlineText = "Tonight 7 PM",
                startTimeText = "Tonight 8 PM",
                matchDurationMinutes = 20,
                seedingMode = "Seeded",
                organizerName = "OrgHost"
            ).getOrThrow()
        }

        // 2. Team captain creates a squad in the tournament
        signInTestUser(captainEmail)
        val capRepo = FirestoreTournamentRepository(firestore)
        val team = withTimeout(DEFAULT_TIMEOUT_MS) {
            capRepo.createTeam(
                tournamentId = tournament.id,
                teamName = "Dhaka Falcons",
                captainDisplayName = "FalconCap",
                strength = 78
            ).getOrThrow()
        }

        // 3. Captain posts a real-time coordination message in Captains & Host channel
        withTimeout(DEFAULT_TIMEOUT_MS) {
            capRepo.sendChatMessage(
                tournamentId = tournament.id,
                senderName = "FalconCap",
                rawText = "Dhaka Falcons roster locked and ready for Round 1",
                senderRole = "captain",
                teamName = team.name,
                channel = "captains",
                isPinned = false
            ).getOrThrow()
        }

        val afterCaptainMsg = withTimeout(FLOW_TIMEOUT_MS) {
            capRepo.observeMessages(tournament.id).first { list ->
                list.any { it.senderRole == "captain" && it.channel == "captains" && it.teamName == "Dhaka Falcons" }
            }
        }
        val capMsg = afterCaptainMsg.first { it.senderRole == "captain" }
        assertEquals("captains", capMsg.channel)
        assertEquals("Dhaka Falcons", capMsg.teamName)
        assertFalse(capMsg.isPinned)

        // 4. Non-organizer captain cannot pin messages
        val pinAttempt = capRepo.togglePinChatMessage(tournament.id, capMsg)
        assertTrue("Captain must not be allowed to pin messages", pinAttempt.isFailure)
        val pinEx = pinAttempt.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, pinEx?.code)

        // 5. Organizer receives live messages, posts pinned announcement, and pins captain check-in
        signInTestUser(organizerEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.sendChatMessage(
                tournamentId = tournament.id,
                senderName = "OrgHost",
                rawText = "Room ID 99210 drops at 7:50 PM BST",
                senderRole = "organizer",
                teamName = "Tournament Host",
                channel = "captains",
                isPinned = true
            ).getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            orgRepo.togglePinChatMessage(tournament.id, capMsg).getOrThrow()
        }

        val finalMessages = withTimeout(FLOW_TIMEOUT_MS) {
            orgRepo.observeMessages(tournament.id).first { list ->
                list.size >= 2 && list.count { it.isPinned } == 2
            }
        }
        assertTrue(finalMessages.any { it.senderRole == "organizer" && it.isPinned && it.channel == "captains" })
        assertTrue(finalMessages.any { it.senderRole == "captain" && it.teamName == "Dhaka Falcons" && it.isPinned })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
