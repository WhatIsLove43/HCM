package com.example.hcmanager

import kotlin.random.Random

object ScheduleGenerator {

    /** 42 тура: двойной круг по всем 22 командам (2 матча с каждым). */
    fun generateBaseRounds(teams: List<Team>, seed: Long): List<List<Pair<Team, Team>>> {
        require(teams.size % 2 == 0) { "Нужно чётное число команд" }
        val rng = Random(seed)
        val shuffled = teams.shuffled(rng)
        val single = circleMethod(shuffled)
        val secondHalf = single.map { round -> round.map { (a, b) -> b to a } }
        return single + secondHalf
    }

    /** 22 тура по 10 матчей: 5 пар Запад + 5 пар Восток. */
    fun generateConferenceRounds(teams: List<Team>, seed: Long): List<List<Pair<Team, Team>>> {
        val rng = Random(seed + 1000)
        val west = teams.filter { it.conference == Conference.WEST }.shuffled(rng)
        val east = teams.filter { it.conference == Conference.EAST }.shuffled(rng)
        require(west.size == east.size) { "Конференции должны быть равны по размеру" }

        val westRounds = miniRounds(west, rounds = 22)
        val eastRounds = miniRounds(east, rounds = 22)

        return (0 until 22).map { i ->
            val (westPairs, _) = westRounds[i]
            val (eastPairs, _) = eastRounds[i]
            westPairs + eastPairs
        }
    }

    /**
     * Финальная фаза: 6 туров по 11 матчей = 66 матчей.
     *
     *  1. «Соседи по таблице» — 11 пар, образующих цикл через обе конференции:
     *       1W — 1E — 2E — 3E — … — 11E — 11W — 10W — … — 2W — 1W.
     *     У каждой команды ровно 2 соседа; с каждым — по матчу дома и на выезде
     *     → 4 тура.
     *  2. Третий соперник — 11 пар:
     *     — 6 фиксированных дерби-пар (Derby.pairs);
     *     — 10 оставшихся команд паруются по общему рейтингу КХЛ: 1–2, 3–4, …, 9–10.
     *     С третьим соперником — дома + на выезде → 2 тура.
     *
     *  Каждый тур содержит 11 матчей и задействует все 22 команды ровно по разу.
     *
     *  @param standings команды, отсортированные по правилам КХЛ после 64 туров.
     */
    fun generateFinalPhase(
        teams: List<Team>,
        standings: List<Team>,
        seed: Long
    ): List<List<Pair<Team, Team>>> {
        require(teams.size == 22) { "Ожидалось 22 команды, получили ${teams.size}" }
        require(standings.size == 22) { "Ожидалось 22 команды в таблице" }

        val rng = Random(seed + 2000)
        val byName = teams.associateBy { it.name }

        // ── 1. Соседи по таблице (цикл) ──
        val westStandings = standings.filter { it.conference == Conference.WEST }
        val eastStandings = standings.filter { it.conference == Conference.EAST }
        require(westStandings.size == 11 && eastStandings.size == 11) {
            "Ожидалось по 11 команд в каждой конференции, получили " +
                    "Запад=${westStandings.size}, Восток=${eastStandings.size}"
        }

        // Порядок в цикле: W1, E1, E2, …, E11, W11, W10, …, W2.
        val cycle = mutableListOf<Team>()
        cycle.add(westStandings[0])                        // W1
        cycle.add(eastStandings[0])                        // E1
        for (i in 1 until 11) cycle.add(eastStandings[i])  // E2..E11
        cycle.add(westStandings[10])                       // W11
        for (i in 9 downTo 1) cycle.add(westStandings[i])  // W10..W2
        require(cycle.size == 22)

        val cycleEdges = (0 until 22).map { i -> cycle[i] to cycle[(i + 1) % 22] }
        val matchOdd  = (0 until 22 step 2).map { cycleEdges[it] }   // (v0,v1), (v2,v3), …
        val matchEven = (1 until 22 step 2).map { cycleEdges[it] }   // (v1,v2), (v3,v4), …

        // ── 2. Третий соперник ──
        // 2a. Дерби (6 фиксированных пар).
        val derbyPairs: List<Pair<Team, Team>> = Derby.pairs.map { (a, b) ->
            val ta = byName[a] ?: error("Неизвестная команда в Derby.pairs: $a")
            val tb = byName[b] ?: error("Неизвестная команда в Derby.pairs: $b")
            ta to tb
        }
        require(derbyPairs.size == 6) { "Ожидалось 6 дерби-пар, получили ${derbyPairs.size}" }

        val derbyTeams = derbyPairs.flatMap { listOf(it.first.name, it.second.name) }.toSet()
        require(derbyTeams.size == 12) { "Дерби должны покрывать 12 разных команд" }

        // 2b. Оставшиеся 10 — по общему рейтингу КХЛ (порядок standings).
        val remaining = standings.filter { it.name !in derbyTeams }
        require(remaining.size == 10) {
            "После дерби должно остаться 10 команд, а не ${remaining.size}"
        }

        val ratingPairs = (0 until 10 step 2).map { i -> remaining[i] to remaining[i + 1] }
        val thirdPairs = derbyPairs + ratingPairs
        require(thirdPairs.size == 11)

        // ── 3. Сборка 6 туров ──
        fun orient(pairs: List<Pair<Team, Team>>): List<Pair<Team, Team>> =
            pairs.map { (a, b) -> if (rng.nextBoolean()) a to b else b to a }

        fun reverse(pairs: List<Pair<Team, Team>>): List<Pair<Team, Team>> =
            pairs.map { (a, b) -> b to a }

        val r1 = orient(matchOdd)   // соседи-нечётные, 1-е касание
        val r2 = orient(matchEven)  // соседи-чётные,   1-е касание
        val r3 = orient(thirdPairs) // третьи пары,     1-е касание
        val r4 = reverse(r1)        // соседи-нечётные, ответка
        val r5 = reverse(r2)        // соседи-чётные,   ответка
        val r6 = reverse(r3)        // третьи пары,     ответка

        return listOf(r1, r2, r3, r4, r5, r6)
    }

    // ─────────────────────────── helpers ───────────────────────────

    private fun circleMethod(teams: List<Team>): List<List<Pair<Team, Team>>> {
        val n = teams.size
        val indices = teams.indices.toMutableList()
        val rounds = mutableListOf<List<Pair<Team, Team>>>()
        repeat(n - 1) { round ->
            val pairs = mutableListOf<Pair<Team, Team>>()
            for (i in 0 until n / 2) {
                val a = indices[i]
                val b = indices[n - 1 - i]
                if ((round + i) % 2 == 0) pairs.add(teams[a] to teams[b])
                else pairs.add(teams[b] to teams[a])
            }
            rounds.add(pairs)
            val last = indices.removeAt(n - 1)
            indices.add(1, last)
        }
        return rounds
    }

    private fun miniRounds(
        conf: List<Team>,
        rounds: Int
    ): List<Pair<List<Pair<Team, Team>>, Team>> {
        val withBye = conf + Team(
            name = "__BYE__",
            baseAttack = 0, // 👈 ИСПРАВЛЕНО
            baseDefense = 0, // 👈 ИСПРАВЛЕНО
            conference = conf[0].conference
        )
        val firstCircle = circleMethod(withBye)
        val secondCircle = firstCircle.map { r -> r.map { (a, b) -> b to a } }
        val doubleRounds = firstCircle + secondCircle

        return doubleRounds.take(rounds).map { round ->
            val pairs = mutableListOf<Pair<Team, Team>>()
            var freeTeam: Team? = null
            for ((a, b) in round) {
                if (a.name == "__BYE__") freeTeam = b
                else if (b.name == "__BYE__") freeTeam = a
                else pairs.add(a to b)
            }
            requireNotNull(freeTeam) { "В туре должен быть ровно один bye" }
            pairs to freeTeam
        }
    }
}