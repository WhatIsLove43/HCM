package com.example.hcmanager

import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

object MatchSimulator {

    private const val HOME_BASE_GOALS = 1.5
    private const val AWAY_BASE_GOALS = 1.2
    private const val OT_SOMEONE_SCORES_P = 0.4
    private const val OT_DECAY = 0.97
    private const val OT_SAFETY_LIMIT = 50

    fun simulate(home: Team, away: Team, rng: Random = Random.Default): Match {
        val homeExpected = HOME_BASE_GOALS * (home.currentAttack / max(1.0, away.currentDefense))
        val awayExpected = AWAY_BASE_GOALS * (away.currentAttack / max(1.0, home.currentDefense))

        val homeGoals = rollGoals(homeExpected, rng)
        val awayGoals = rollGoals(awayExpected, rng)

        if (homeGoals != awayGoals) {
            return Match(home, away, homeGoals, awayGoals, MatchOutcome.REGULATION)
        }
        if (rng.nextDouble() < OT_SOMEONE_SCORES_P) {
            val pHome = home.currentAttack / (home.currentAttack + away.currentAttack)
            return if (rng.nextDouble() < pHome)
                Match(home, away, homeGoals + 1, awayGoals, MatchOutcome.OVERTIME)
            else
                Match(home, away, homeGoals, awayGoals + 1, MatchOutcome.OVERTIME)
        }
        val pHomeWins = 0.5 + (home.currentAttack - away.currentAttack) / 1000.0
        return if (rng.nextDouble() < pHomeWins)
            Match(home, away, homeGoals + 1, awayGoals, MatchOutcome.SHOOTOUT)
        else
            Match(home, away, homeGoals, awayGoals + 1, MatchOutcome.SHOOTOUT)
    }

    fun simulatePlayoff(home: Team, away: Team, rng: Random = Random.Default): Match {
        val homeExpectedBase = HOME_BASE_GOALS * (home.currentAttack / max(1.0, away.currentDefense))
        val awayExpectedBase = AWAY_BASE_GOALS * (away.currentAttack / max(1.0, home.currentDefense))

        var h = rollGoals(homeExpectedBase, rng)
        var a = rollGoals(awayExpectedBase, rng)
        if (h != a) return Match(home, away, h, a, MatchOutcome.REGULATION, isPlayoff = true)

        var ot = 0
        while (h == a) {
            ot++
            if (ot > OT_SAFETY_LIMIT) {
                if (rng.nextBoolean()) h++ else a++
                break
            }
            val decay = OT_DECAY.pow(ot - 1)
            val expectedTotal = (homeExpectedBase + awayExpectedBase) * decay
            val pGoal = (expectedTotal / 10.0).coerceIn(0.0, 0.95)

            if (rng.nextDouble() < pGoal) {
                val pHomeScores = home.currentAttack / (home.currentAttack + away.currentAttack)
                if (rng.nextDouble() < pHomeScores) h++ else a++
            }
        }
        return Match(home, away, h, a, MatchOutcome.OVERTIME, isPlayoff = true)
    }

    private fun rollGoals(lambda: Double, rng: Random): Int {
        var goals = 0
        val chance = (lambda / 10.0).coerceIn(0.0, 1.0)
        repeat(10) { if (rng.nextDouble() < chance) goals++ }
        return goals
    }
}