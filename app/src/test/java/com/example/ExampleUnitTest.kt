package com.example

import com.example.data.*
import com.example.model.ParticipationType
import com.example.model.SeedingMode
import com.example.model.TournamentFormat
import com.example.ui.screens.AuthMode
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGameFieldValidation_freeFireAndFcMobile() {
        val validFf = mapOf("uid" to "1928374650", "ign" to "DhakaSniper", "level" to "68")
        assertNull(BracketEngine.validateGameFields("Free Fire", emptyList(), validFf))

        val invalidFf = mapOf("uid" to "abc", "ign" to "DhakaSniper", "level" to "68")
        assertNotNull(BracketEngine.validateGameFields("Free Fire", emptyList(), invalidFf))

        val validFc = mapOf("uid" to "8844221100", "ovr" to "104")
        assertNull(BracketEngine.validateGameFields("FC Mobile", emptyList(), validFc))
        assertEquals("OVR 104", BracketEngine.extractPublicStatLabel("FC Mobile", emptyList(), validFc))
        assertEquals(104, BracketEngine.extractRankingMetric("FC Mobile", emptyList(), validFc))
        assertEquals("8844221100", BracketEngine.extractSensitiveId("FC Mobile", emptyList(), validFc))
    }

    @Test
    fun testCustomGameFieldValidation() {
        val customFields = listOf("Valorant Riot ID", "Peak Rank Rating")
        val valid = mapOf("custom_0" to "Apex#BD1", "custom_1" to "1950")
        assertNull(BracketEngine.validateGameFields("Valorant", customFields, valid))
        assertEquals("Apex#BD1", BracketEngine.extractSensitiveId("Valorant", customFields, valid))
        assertEquals(1950, BracketEngine.extractRankingMetric("Valorant", customFields, valid))
    }

    @Test
    fun testMaskGameIdAndProfanityFilter() {
        assertEquals("••••4650", BracketEngine.maskGameId("1928374650"))
        assertEquals("••••", BracketEngine.maskGameId("1234"))
        assertEquals("Hidden", BracketEngine.maskGameId(""))

        val cleaned = BracketEngine.filterProfanity("Stop being an idiot and no scam here")
        assertFalse(cleaned.contains("idiot", ignoreCase = true))
        assertFalse(cleaned.contains("scam", ignoreCase = true))
        assertTrue(cleaned.contains("*****"))
    }

    @Test
    fun testSeededBracketGeneration_pairsHighestAgainstLowestAndAdvancement() {
        val tournament = FirestoreTournament(
            id = "trn_test_4",
            title = "FC Mobile Pro 4",
            game = "FC Mobile",
            format = "Single Elimination",
            seedingMode = "Seeded"
        )
        val participants = listOf(
            TournamentParticipant(id = "p1", userId = "u1", displayName = "Alpha", rankingMetric = 95, publicStatLabel = "OVR 95", sensitiveIdField = "1111"),
            TournamentParticipant(id = "p2", userId = "u2", displayName = "Bravo", rankingMetric = 110, publicStatLabel = "OVR 110", sensitiveIdField = "2222"),
            TournamentParticipant(id = "p3", userId = "u3", displayName = "Charlie", rankingMetric = 102, publicStatLabel = "OVR 102", sensitiveIdField = "3333"),
            TournamentParticipant(id = "p4", userId = "u4", displayName = "Delta", rankingMetric = 88, publicStatLabel = "OVR 88", sensitiveIdField = "4444")
        )

        val generated = BracketEngine.generateSingleEliminationBracket(tournament, participants, "Seeded")
        assertEquals(3, generated.matches.size) // 2 Semis + 1 Grand Final

        val semi1 = generated.matches.first { it.id == "m_r1_1" }
        val semi2 = generated.matches.first { it.id == "m_r1_2" }
        val finalMatch = generated.matches.first { it.id == "m_r2_1" }

        assertEquals("Bravo", semi1.participant1Name) // #1 seed (110)
        assertEquals("Delta", semi1.participant2Name) // #4 seed (88)
        assertEquals("m_r2_1", semi1.nextMatchId)
        assertEquals(1, semi1.nextMatchSlot)

        assertEquals("Charlie", semi2.participant1Name) // #2 seed (102)
        assertEquals("Alpha", semi2.participant2Name) // #3 seed (95)
        assertEquals("m_r2_1", semi2.nextMatchId)
        assertEquals(2, semi2.nextMatchSlot)
        assertEquals("Grand Final", finalMatch.roundName)
    }

    @Test
    fun testOddParticipants_automaticByeHandlingPromotesToRound2() {
        val tournament = FirestoreTournament(
            id = "trn_test_3",
            title = "Free Fire Trio",
            game = "Free Fire",
            format = "Single Elimination"
        )
        val participants = listOf(
            TournamentParticipant(id = "p1", userId = "u1", displayName = "Player 1", rankingMetric = 70),
            TournamentParticipant(id = "p2", userId = "u2", displayName = "Player 2", rankingMetric = 65),
            TournamentParticipant(id = "p3", userId = "u3", displayName = "Player 3", rankingMetric = 60)
        )

        val generated = BracketEngine.generateSingleEliminationBracket(tournament, participants, "Seeded")
        val r1Matches = generated.matches.filter { it.roundNumber == 1 }
        val byeMatch = r1Matches.firstOrNull { it.participant2Name == "BYE" }
        assertNotNull("3 participants in 4-slot bracket must produce 1 BYE", byeMatch)
        assertEquals("Completed", byeMatch!!.status)
        assertTrue(byeMatch.winnerName.isNotBlank())

        val finalMatch = generated.matches.first { it.roundNumber == 2 }
        assertTrue(
            "BYE winner must auto-advance to Grand Final slot",
            finalMatch.participant1Name == byeMatch.winnerName || finalMatch.participant2Name == byeMatch.winnerName
        )
    }

    @Test
    fun testRoundRobinFixturesAndStandings() {
        val tournament = FirestoreTournament(
            id = "trn_rr_4",
            title = "eFootball League",
            game = "eFootball",
            format = "Round Robin"
        )
        val participants = listOf(
            TournamentParticipant(id = "p1", userId = "u1", displayName = "Club A"),
            TournamentParticipant(id = "p2", userId = "u2", displayName = "Club B"),
            TournamentParticipant(id = "p3", userId = "u3", displayName = "Club C"),
            TournamentParticipant(id = "p4", userId = "u4", displayName = "Club D")
        )
        val generated = BracketEngine.generateRoundRobinMatches(tournament, participants, "Random")
        assertEquals(6, generated.matches.size) // 4 * 3 / 2 = 6 matches

        val completedFirst = generated.matches.first().copy(
            status = "Completed",
            score1 = 3,
            score2 = 1,
            winnerParticipantId = generated.matches.first().participant1Id
        )
        val standings = BracketEngine.computeRoundRobinStandings(
            participants,
            listOf(completedFirst) + generated.matches.drop(1)
        )
        assertEquals(4, standings.size)
        assertEquals(3, standings.first().points)
        assertEquals(2, standings.first().goalDiff)
    }

    @Test
    fun testEnumsMatchPromptRequirements() {
        assertEquals(3, TournamentFormat.values().size)
        assertEquals(2, ParticipationType.values().size)
        assertEquals(3, SeedingMode.values().size)
        assertEquals(AuthMode.LOGIN.name, "LOGIN")
        assertEquals(AuthMode.SIGN_UP.name, "SIGN_UP")
    }
}
