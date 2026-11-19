package com.example.hcmanager

object TeamForm {

    /** Последние до 5 матчей: 'В' / 'П'. Слева самый старый, справа самый новый. */
    fun lastFive(team: Team, played: List<Match>): List<Char> =
        played.asReversed()
            .filter { it.homeTeam.name == team.name || it.awayTeam.name == team.name }
            .take(5)
            .map { if (it.winner.name == team.name) 'В' else 'П' }
            .reversed()

    /** Для UI: "В П В В П" или "—", если матчей ещё нет. */
    fun format(team: Team, played: List<Match>): String {
        val chars = lastFive(team, played)
        return if (chars.isEmpty()) "—" else chars.joinToString(" ")
    }

    /** Позиция команды в таблице своей конференции или null, если команда ещё не играла. */
    fun conferencePosition(team: Team, stats: List<TeamStats>): Int? {
        val confStats = TeamTable.sort(stats).filter { it.team.conference == team.conference }
        val idx = confStats.indexOfFirst { it.team.name == team.name }
        if (idx < 0 || confStats[idx].gamesPlayed == 0) return null
        return idx + 1
    }
}