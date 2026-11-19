package com.example.hcmanager

enum class MatchOutcome { REGULATION, OVERTIME, SHOOTOUT }

data class Match(
    val homeTeam: Team,
    val awayTeam: Team,
    val homeGoals: Int,
    val awayGoals: Int,
    val outcome: MatchOutcome,
    val isPlayoff: Boolean = false
) {
    val winner: Team get() = if (homeGoals > awayGoals) homeTeam else awayTeam
    val loser: Team  get() = if (homeGoals > awayGoals) awayTeam else homeTeam
}