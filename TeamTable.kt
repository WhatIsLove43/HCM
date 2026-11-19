package com.example.hcmanager

object TeamTable {

    fun build(teams: List<Team>, matches: List<Match>): List<TeamStats> {
        val statsByName = teams.associate { it.name to TeamStats(it) }.toMutableMap()

        matches.forEach { match ->
            val home = statsByName[match.homeTeam.name] ?: return@forEach
            val away = statsByName[match.awayTeam.name] ?: return@forEach

            home.gamesPlayed++
            away.gamesPlayed++
            home.goalsFor += match.homeGoals
            home.goalsAgainst += match.awayGoals
            away.goalsFor += match.awayGoals
            away.goalsAgainst += match.homeGoals

            val homeWon = match.homeGoals > match.awayGoals
            when (match.outcome) {
                MatchOutcome.REGULATION -> if (homeWon) {
                    home.winsRegulation++
                    away.lossesRegulation++
                } else {
                    away.winsRegulation++
                    home.lossesRegulation++
                }
                MatchOutcome.OVERTIME   -> if (homeWon) { home.winsOvertime++; away.lossesOvertime++ }
                else          { away.winsOvertime++; home.lossesOvertime++ }
                MatchOutcome.SHOOTOUT   -> if (homeWon) { home.winsShootout++; away.lossesOvertime++ }
                else          { away.winsShootout++; home.lossesOvertime++ }
            }
        }

        return statsByName.values.toList()
    }

    fun sort(stats: List<TeamStats>): List<TeamStats> =
        stats.sortedWith(
            compareByDescending<TeamStats> { it.points }
                .thenByDescending { it.winsRegulation }
                .thenByDescending { it.winsOvertime }
                .thenByDescending { it.winsShootout }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
        )

    fun byConference(stats: List<TeamStats>): Map<Conference, List<TeamStats>> {
        val sorted = sort(stats)
        return mapOf(
            Conference.WEST to sorted.filter { it.team.conference == Conference.WEST },
            Conference.EAST to sorted.filter { it.team.conference == Conference.EAST }
        )
    }

    fun overall(stats: List<TeamStats>): List<TeamStats> = sort(stats)
}