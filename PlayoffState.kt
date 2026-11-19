package com.example.hcmanager

/** Команда с номером в конференции и очками. */
data class SeededTeam(
    val team: Team,
    val seed: Int,
    val conference: Conference,
    val points: Int
)

fun strongerFirst(a: SeededTeam, b: SeededTeam): Pair<SeededTeam, SeededTeam> =
    if (a.seed != b.seed) {
        if (a.seed < b.seed) a to b else b to a
    } else {
        if (a.points >= b.points) a to b else b to a
    }

data class Series(
    val round: Int,
    val group: String,          // "W" / "E" для 1/8; "A" / "B" для 1/4 и 1/2; "F" для финала
    val higherSeed: SeededTeam,
    val lowerSeed: SeededTeam,
    val higherSeedWins: Int = 0,
    val lowerSeedWins: Int = 0,
    val gamesPlayed: Int = 0,
    val matches: List<Match> = emptyList()   // 👈 новое: история матчей серии
) {
    val isFinished: Boolean get() = higherSeedWins >= 4 || lowerSeedWins >= 4
    val winner: SeededTeam? get() = when {
        higherSeedWins >= 4 -> higherSeed
        lowerSeedWins >= 4 -> lowerSeed
        else -> null
    }

    /** Дома в играх: 1, 2, 5, 7 — у сильнейшего; 3, 4, 6 — у слабейшего. */
    fun homeTeamForGame(n: Int): SeededTeam = when (n) {
        1, 2, 5, 7 -> higherSeed
        else -> lowerSeed
    }
    fun awayTeamForGame(n: Int): SeededTeam =
        if (homeTeamForGame(n) == higherSeed) lowerSeed else higherSeed

    val scoreString: String get() = "$higherSeedWins-$lowerSeedWins"
}

data class PlayoffState(
    val round: Int = 0,        // 0 = не начат, 1..4
    val currentGame: Int = 0,
    val series: List<Series> = emptyList(),
    val finished: Boolean = false,
    val champion: Team? = null
) {
    companion object {
        fun roundName(round: Int): String = when (round) {
            1 -> "1/8 финала"
            2 -> "1/4 финала"
            3 -> "1/2 финала"
            4 -> "Финал"
            else -> "Плей-офф"
        }
    }
}