package com.example.hcmanager

import kotlin.random.Random

object SeasonProgression {

    fun evolveTeams(oldTeams: List<Team>, league: League, championName: String?, rng: Random = Random.Default): List<Team> {
        val sortedByRating = oldTeams.sortedByDescending { (it.baseAttack + it.baseDefense) / 2.0 }
        val expectedPositions = sortedByRating.mapIndexed { index, team -> team.name to (index + 1) }.toMap()

        val stats = TeamTable.build(oldTeams, league.regularSeasonMatches)
        val actualOverall = TeamTable.overall(stats)
        val actualPositions = actualOverall.mapIndexed { index, s -> s.team.name to (index + 1) }.toMap()

        return oldTeams.map { team ->
            val expectedPos = expectedPositions[team.name] ?: 11
            val actualPos = actualPositions[team.name] ?: 11

            val posDiff = (expectedPos - actualPos).coerceIn(-10, 10)
            val standingsScore = posDiff * 5.0

            val myStats = stats.first { it.team.name == team.name }
            val goalDiffScore = (myStats.goalDifference * 0.5).coerceIn(-30.0, 30.0)

            var playoffScore = 0.0
            if (league.isRegularSeasonOver) {
                val roundsPlayed = league.playoffState.series
                    .filter { it.higherSeed.team.name == team.name || it.lowerSeed.team.name == team.name }
                    .maxOfOrNull { it.round } ?: 0

                playoffScore = when {
                    championName == team.name -> 40.0
                    roundsPlayed == 4 -> 25.0
                    roundsPlayed == 3 -> 15.0
                    roundsPlayed == 2 -> 10.0
                    roundsPlayed == 1 -> 5.0
                    else -> -10.0
                }
            }

            val totalScore = (standingsScore * 0.5) + (goalDiffScore * 0.3) + (playoffScore * 0.2)

            var baseChange = (totalScore / 10.0).toInt() + rng.nextInt(-2, 3)
            baseChange = baseChange.coerceIn(-8, 8)

            val attackBias = rng.nextInt(-2, 3)
            var attackChange = baseChange + attackBias
            var defenseChange = baseChange - attackBias

            team.copy(
                baseAttack = (team.baseAttack + attackChange).coerceIn(40, 99),
                baseDefense = (team.baseDefense + defenseChange).coerceIn(40, 99)
            ).also { newTeam ->
                newTeam.morale = 50
                newTeam.momentum = 0f
                newTeam.winningFactor = 0f
            }
        }
    }
}