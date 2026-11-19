package com.example.hcmanager

import kotlin.math.abs
import kotlin.random.Random

class League(
    val teams: List<Team>,
    val seed: Long = System.currentTimeMillis()
) {

    companion object {
        const val TOTAL_ROUNDS = 70
        const val CONFERENCE_PHASE_MATCHES = 682
        const val REGULAR_SEASON_MATCHES = 748
        const val REGULAR_SEASON_ROUNDS = 70
    }

    private val _schedule: MutableList<List<Pair<Team, Team>>> = run {
        val base = ScheduleGenerator.generateBaseRounds(teams, seed)
        val conf = ScheduleGenerator.generateConferenceRounds(teams, seed)
        (base + conf).toMutableList()
    }

    val schedule: List<List<Pair<Team, Team>>> get() = _schedule
    val playedMatches: MutableList<Match> = mutableListOf()

    private var _playoffState: PlayoffState = PlayoffState()
    val playoffState: PlayoffState get() = _playoffState

    val regularSeasonMatches: List<Match>
        get() = playedMatches.filter { !it.isPlayoff }

    val isRegularSeasonOver: Boolean
        get() = regularSeasonMatches.size >= REGULAR_SEASON_MATCHES

    val totalRounds: Int
        get() = REGULAR_SEASON_ROUNDS

    val isSeasonOver: Boolean
        get() = _playoffState.finished

    val currentRound: Int
        get() {
            if (isRegularSeasonOver) return _schedule.size
            var total = 0
            for ((i, round) in _schedule.withIndex()) {
                total += round.size
                if (regularSeasonMatches.size < total) return i
            }
            return _schedule.size
        }

    // 👇 НОВОЕ: накопление импульса
    private val pendingMomentum = mutableMapOf<String, Float>()

    fun ensureFinalPhase() {
        if (_schedule.size >= TOTAL_ROUNDS) return
        if (regularSeasonMatches.size < CONFERENCE_PHASE_MATCHES) return

        val conferencePhaseMatches = regularSeasonMatches.take(CONFERENCE_PHASE_MATCHES)
        val stats = TeamTable.build(teams, conferencePhaseMatches)
        val standings = TeamTable.sort(stats).map { it.team }
        val finalRounds = ScheduleGenerator.generateFinalPhase(teams, standings, seed)
        _schedule.addAll(finalRounds)
    }

    fun startPlayoff() {
        if (_playoffState.round == 0) {
            _playoffState = buildInitialPlayoffState()
        }
    }

    private fun buildInitialPlayoffState(): PlayoffState {
        val stats = TeamTable.build(teams, regularSeasonMatches)
        val byConf = TeamTable.byConference(stats)

        fun seeded(conf: Conference): List<SeededTeam> =
            byConf[conf].orEmpty()
                .take(8)
                .mapIndexed { idx, s ->
                    SeededTeam(s.team, seed = idx + 1, conference = conf, points = s.points)
                }

        val west = seeded(Conference.WEST)
        val east = seeded(Conference.EAST)
        require(west.size == 8 && east.size == 8) { "Для 1/8 нужно по 8 команд" }

        val series = mutableListOf<Series>()
        for ((group, list) in listOf("W" to west, "E" to east)) {
            for (i in 0 until 4) {
                val hi = list[i]
                val lo = list[7 - i]
                val (h, l) = strongerFirst(hi, lo)
                series.add(Series(round = 1, group = group, higherSeed = h, lowerSeed = l))
            }
        }
        return PlayoffState(round = 1, currentGame = 0, series = series)
    }

    private fun simulatePlayoffRound(rng: Random): List<Match> {
        if (_playoffState.round == 0) return emptyList()
        val roundSeries = _playoffState.series.filter { it.round == _playoffState.round }
        val live = roundSeries.filter { !it.isFinished }
        if (live.isEmpty()) {
            advancePlayoffRound()
            return emptyList()
        }

        val newMatches = mutableListOf<Match>()
        val allSeries = _playoffState.series.toMutableList()

        for (s in live) {
            val gameNumber = s.gamesPlayed + 1
            val homeST = s.homeTeamForGame(gameNumber)
            val awayST = s.awayTeamForGame(gameNumber)

            val match = MatchSimulator.simulatePlayoff(homeST.team, awayST.team, rng)
            newMatches.add(match)
            playedMatches.add(match)

            val idx = allSeries.indexOf(s)
            val higherWon = match.winner.name == s.higherSeed.team.name
            allSeries[idx] = s.copy(
                higherSeedWins = s.higherSeedWins + if (higherWon) 1 else 0,
                lowerSeedWins = s.lowerSeedWins + if (!higherWon) 1 else 0,
                gamesPlayed = s.gamesPlayed + 1,
                matches = s.matches + match
            )
        }

        _playoffState = _playoffState.copy(
            series = allSeries,
            currentGame = _playoffState.currentGame + 1
        )

        val after = _playoffState.series.filter { it.round == _playoffState.round }
        if (after.all { it.isFinished }) {
            advancePlayoffRound()
        }

        return newMatches
    }

    private fun advancePlayoffRound() {
        val current = _playoffState.series.filter { it.round == _playoffState.round }
        val nextSeries: List<Series> = when (_playoffState.round) {
            1 -> buildQuarterfinals(current)
            2 -> buildSemifinals(current)
            3 -> buildFinal(current)
            4 -> {
                _playoffState = _playoffState.copy(
                    finished = true,
                    champion = current.single().winner?.team
                )
                return
            }
            else -> return
        }
        _playoffState = _playoffState.copy(
            round = _playoffState.round + 1,
            currentGame = 0,
            series = _playoffState.series + nextSeries
        )
    }

    private fun buildQuarterfinals(r16: List<Series>): List<Series> {
        val west = r16.filter { it.group == "W" }.map { it.winner!! }.sortedBy { it.seed }
        val east = r16.filter { it.group == "E" }.map { it.winner!! }.sortedBy { it.seed }
        return listOf(
            makeSeries(2, "A", west[0], east[3]),
            makeSeries(2, "A", east[1], west[2]),
            makeSeries(2, "B", east[0], west[3]),
            makeSeries(2, "B", west[1], east[2])
        )
    }

    private fun buildSemifinals(qf: List<Series>): List<Series> {
        val winnersA = qf.filter { it.group == "A" }.map { it.winner!! }
        val winnersB = qf.filter { it.group == "B" }.map { it.winner!! }
        return listOf(
            makeSeries(3, "A", winnersA[0], winnersA[1]),
            makeSeries(3, "B", winnersB[0], winnersB[1])
        )
    }

    private fun buildFinal(sf: List<Series>): List<Series> {
        val winnerA = sf.first { it.group == "A" }.winner!!
        val winnerB = sf.first { it.group == "B" }.winner!!
        return listOf(makeSeries(4, "F", winnerA, winnerB))
    }

    private fun makeSeries(round: Int, group: String, a: SeededTeam, b: SeededTeam): Series {
        val (h, l) = strongerFirst(a, b)
        return Series(round = round, group = group, higherSeed = h, lowerSeed = l)
    }

    // ─────────────────────── Динамика команд ───────────────────────

    // ─────────────────────── Динамика команд ───────────────────────

    private fun updateTeamStates() {
        teams.forEach { team ->
            val lastMatches = playedMatches
                .filter { it.homeTeam.name == team.name || it.awayTeam.name == team.name }
                .takeLast(5)

            if (lastMatches.isNotEmpty()) {
                // --- 1. Winning Factor ---
                var points = 0.0
                lastMatches.forEach { m ->
                    val isWin = m.winner.name == team.name
                    val isOtLoss = !isWin && m.outcome != MatchOutcome.REGULATION
                    if (isWin) points += 1.0
                    else if (isOtLoss) points += 0.5
                }
                team.winningFactor = ((points - 2.5) * 1.2).toFloat().coerceIn(-3.0f, 3.0f)

                // --- 2. Momentum ---
                val recent = lastMatches.last()
                val isHome = recent.homeTeam.name == team.name
                val opponent = if (isHome) recent.awayTeam else recent.homeTeam
                val isWin = recent.winner.name == team.name
                val diff = team.baseStrength - opponent.baseStrength

                var delta = if (isWin) 1.0f else -1.0f
                if (isWin && diff < -10) delta += 1.0f // Апсет
                if (!isWin && diff > 10) delta -= 1.0f // Фейл фаворита
                team.momentum = (team.momentum + delta).coerceIn(-5.0f, 5.0f)

                // --- 3. Morale ---
                val isOtLoss = !isWin && recent.outcome != MatchOutcome.REGULATION
                if (isWin) team.morale += 2
                else if (isOtLoss) team.morale -= 1
                else team.morale -= 2

                team.morale = team.morale.coerceIn(0, 100)
            }
        }
    }

    fun simulateNextRound(rng: Random = Random.Default): List<Match> {
        if (isSeasonOver) return emptyList()

        if (!isRegularSeasonOver) {
            ensureFinalPhase()
            if (currentRound >= _schedule.size) return emptyList()

            val round = _schedule[currentRound]
            val results = round.map { (home, away) -> MatchSimulator.simulate(home, away, rng) }
            playedMatches.addAll(results)

            // 👇 ВЫЗЫВАЕМ ОБНОВЛЕНИЕ ПОСЛЕ ТУРА
            updateTeamStates()

            ensureFinalPhase()
            return results
        }
        return simulatePlayoffRound(rng)
    }

    fun matchOfRound(roundIndex: Int, home: Team, away: Team): Match? {
        if (roundIndex >= _schedule.size) return null
        var start = 0
        for (i in 0 until roundIndex) start += _schedule[i].size
        val size = _schedule[roundIndex].size
        if (start + size > playedMatches.size) return null
        return playedMatches.subList(start, start + size).firstOrNull {
            it.homeTeam.name == home.name && it.awayTeam.name == away.name
        }
    }

    fun exportPlayoffState(): SavedPlayoff? {
        if (_playoffState.round == 0) return null
        return SavedPlayoff(
            round = _playoffState.round,
            currentGame = _playoffState.currentGame,
            finished = _playoffState.finished,
            championName = _playoffState.champion?.name,
            series = _playoffState.series.map { s ->
                SavedSeries(
                    round = s.round,
                    group = s.group,
                    higherSeed = SavedSeededTeam(
                        s.higherSeed.team.name, s.higherSeed.seed,
                        s.higherSeed.conference.name, s.higherSeed.points
                    ),
                    lowerSeed = SavedSeededTeam(
                        s.lowerSeed.team.name, s.lowerSeed.seed,
                        s.lowerSeed.conference.name, s.lowerSeed.points
                    ),
                    higherSeedWins = s.higherSeedWins,
                    lowerSeedWins = s.lowerSeedWins,
                    gamesPlayed = s.gamesPlayed,
                    matches = s.matches.map { m ->
                        SavedMatch(
                            m.homeTeam.name, m.awayTeam.name,
                            m.homeGoals, m.awayGoals, m.outcome, m.isPlayoff
                        )
                    }
                )
            }
        )
    }

    fun restorePlayoffState(saved: SavedPlayoff) {
        val byName = teams.associateBy { it.name }
        fun toST(s: SavedSeededTeam): SeededTeam? {
            val team = byName[s.teamName] ?: return null
            return SeededTeam(team, s.seed, Conference.valueOf(s.conference), s.points)
        }
        val series = saved.series.mapNotNull { s ->
            val hi = toST(s.higherSeed) ?: return@mapNotNull null
            val lo = toST(s.lowerSeed) ?: return@mapNotNull null
            val seriesMatches = s.matches.mapNotNull { sm ->
                val home = byName[sm.homeName] ?: return@mapNotNull null
                val away = byName[sm.awayName] ?: return@mapNotNull null
                Match(home, away, sm.homeGoals, sm.awayGoals, sm.outcome, sm.isPlayoff)
            }
            Series(
                round = s.round, group = s.group,
                higherSeed = hi, lowerSeed = lo,
                higherSeedWins = s.higherSeedWins,
                lowerSeedWins = s.lowerSeedWins,
                gamesPlayed = s.gamesPlayed,
                matches = seriesMatches
            )
        }
        _playoffState = PlayoffState(
            round = saved.round,
            currentGame = saved.currentGame,
            series = series,
            finished = saved.finished,
            champion = saved.championName?.let { byName[it] }
        )
    }
}