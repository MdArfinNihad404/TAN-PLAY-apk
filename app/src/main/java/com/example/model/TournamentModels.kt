package com.example.model

enum class GameType(val displayName: String, val category: String) {
    ALL("All Games", "All"),
    FREE_FIRE("Free Fire", "Battle Royale"),
    EFOOTBALL("eFootball", "Football"),
    FC_MOBILE("FC Mobile", "Football"),
    DLS("DLS", "Football"),
    PUBG_MOBILE("PUBG Mobile", "Battle Royale"),
    CUSTOM("Custom Game", "Custom")
}

enum class TournamentFormat(val displayName: String, val apiValue: String) {
    KNOCKOUT("Single Elimination", "Single Elimination"),
    DOUBLE_ELIM("Double Elimination", "Double Elimination"),
    ROUND_ROBIN("Round Robin", "Round Robin")
}

enum class ParticipationType(val displayName: String, val apiValue: String) {
    INDIVIDUAL("Individual (1v1)", "Individual"),
    TEAM("Team / Squad", "Team")
}

enum class SeedingMode(val displayName: String, val apiValue: String, val description: String) {
    RANDOM(
        "Random Draw",
        "Random",
        "Shuffles all registered players randomly for unpredictable first-round matchups."
    ),
    SEEDED(
        "Seeded (Balanced)",
        "Seeded",
        "Ranks by OVR/Level/Rating and pairs Seed #1 vs lowest seed (#N) so top contenders meet in later rounds."
    ),
    MANUAL(
        "Manual Pairing",
        "Manual",
        "Organizer selects Round 1 matchups manually before locking the bracket."
    )
}
