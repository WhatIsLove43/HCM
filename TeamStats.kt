package com.example.hcmanager

data class TeamStats(
    val team: Team,
    var gamesPlayed: Int = 0,
    var winsRegulation: Int = 0,
    var winsOvertime: Int = 0,
    var winsShootout: Int = 0,
    var lossesRegulation: Int = 0,
    var lossesOvertime: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0
) {
    val points: Int
        get() = (winsRegulation + winsOvertime + winsShootout) * 2 + lossesOvertime

    val totalWins: Int
        get() = winsRegulation + winsOvertime + winsShootout

    val goalDifference: Int
        get() = goalsFor - goalsAgainst
}