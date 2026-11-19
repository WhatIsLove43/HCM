package com.example.hcmanager

import org.json.JSONArray
import org.json.JSONObject

data class SavedMatch(
    val homeName: String,
    val awayName: String,
    val homeGoals: Int,
    val awayGoals: Int,
    val outcome: MatchOutcome,
    val isPlayoff: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("homeName", homeName)
        put("awayName", awayName)
        put("homeGoals", homeGoals)
        put("awayGoals", awayGoals)
        put("outcome", outcome.name)
        put("isPlayoff", isPlayoff)
    }
    companion object {
        fun fromJson(obj: JSONObject): SavedMatch = SavedMatch(
            homeName = obj.getString("homeName"),
            awayName = obj.getString("awayName"),
            homeGoals = obj.getInt("homeGoals"),
            awayGoals = obj.getInt("awayGoals"),
            outcome = MatchOutcome.valueOf(obj.getString("outcome")),
            isPlayoff = obj.optBoolean("isPlayoff", false)
        )
    }
}

data class SavedSeededTeam(
    val teamName: String,
    val seed: Int,
    val conference: String,
    val points: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("teamName", teamName); put("seed", seed)
        put("conference", conference); put("points", points)
    }
    companion object {
        fun fromJson(o: JSONObject) = SavedSeededTeam(
            o.getString("teamName"), o.getInt("seed"),
            o.getString("conference"), o.getInt("points")
        )
    }
}

data class SavedSeries(
    val round: Int,
    val group: String,
    val higherSeed: SavedSeededTeam,
    val lowerSeed: SavedSeededTeam,
    val higherSeedWins: Int,
    val lowerSeedWins: Int,
    val gamesPlayed: Int,
    val matches: List<SavedMatch> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("round", round); put("group", group)
        put("higherSeed", higherSeed.toJson())
        put("lowerSeed", lowerSeed.toJson())
        put("higherSeedWins", higherSeedWins)
        put("lowerSeedWins", lowerSeedWins)
        put("gamesPlayed", gamesPlayed)
        val arr = JSONArray()
        matches.forEach { arr.put(it.toJson()) }
        put("matches", arr)
    }

    companion object {
        fun fromJson(o: JSONObject) = SavedSeries(
            o.getInt("round"), o.getString("group"),
            SavedSeededTeam.fromJson(o.getJSONObject("higherSeed")),
            SavedSeededTeam.fromJson(o.getJSONObject("lowerSeed")),
            o.getInt("higherSeedWins"), o.getInt("lowerSeedWins"),
            o.getInt("gamesPlayed"),
            if (o.has("matches")) {
                val arr = o.getJSONArray("matches")
                (0 until arr.length()).map { SavedMatch.fromJson(arr.getJSONObject(it)) }
            } else emptyList()
        )
    }
}

data class SavedPlayoff(
    val round: Int,
    val currentGame: Int,
    val finished: Boolean,
    val championName: String?,
    val series: List<SavedSeries>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("round", round); put("currentGame", currentGame)
        put("finished", finished)
        put("championName", championName ?: JSONObject.NULL)
        val arr = JSONArray(); series.forEach { arr.put(it.toJson()) }
        put("series", arr)
    }
    companion object {
        fun fromJson(o: JSONObject): SavedPlayoff {
            val arr = o.getJSONArray("series")
            val series = (0 until arr.length()).map { SavedSeries.fromJson(arr.getJSONObject(it)) }
            return SavedPlayoff(
                round = o.getInt("round"),
                currentGame = o.getInt("currentGame"),
                finished = o.getBoolean("finished"),
                championName = if (o.isNull("championName")) null else o.getString("championName"),
                series = series
            )
        }
    }
}

data class SavedGame(
    val selectedTeamName: String,
    val seed: Long,
    val matches: List<SavedMatch>,
    val playoff: SavedPlayoff? = null,
    val teamMomentums: Map<String, Float> = emptyMap(),
    val teamMorales: Map<String, Int> = emptyMap(),       // 👈 НОВОЕ
    val teamWinningFactors: Map<String, Float> = emptyMap() // 👈 НОВОЕ
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("selectedTeamName", selectedTeamName)
        obj.put("seed", seed)
        val arr = JSONArray(); matches.forEach { arr.put(it.toJson()) }
        obj.put("matches", arr)
        obj.put("playoff", playoff?.toJson() ?: JSONObject.NULL)

        // Сохраняем карты
        val momentumObj = JSONObject()
        teamMomentums.forEach { (name, m) -> momentumObj.put(name, m) }
        obj.put("teamMomentums", momentumObj)

        val moraleObj = JSONObject()
        teamMorales.forEach { (name, m) -> moraleObj.put(name, m) }
        obj.put("teamMorales", moraleObj)

        val winningObj = JSONObject()
        teamWinningFactors.forEach { (name, w) -> winningObj.put(name, w) }
        obj.put("teamWinningFactors", winningObj)

        return obj.toString()
    }
    companion object {
        fun fromJson(json: String): SavedGame {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("matches")
            val matches = (0 until arr.length()).map { SavedMatch.fromJson(arr.getJSONObject(it)) }
            val playoff = if (obj.isNull("playoff")) null else SavedPlayoff.fromJson(obj.getJSONObject("playoff"))

            // Восстанавливаем карту импульсов
            val teamMomentums = mutableMapOf<String, Float>()
            val momentumObj = obj.optJSONObject("teamMomentums")
            if (momentumObj != null) {
                val keys = momentumObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    teamMomentums[key] = momentumObj.getDouble(key).toFloat()
                }
            }

            // Восстанавливаем мораль
            val teamMorales = mutableMapOf<String, Int>()
            val moraleObj = obj.optJSONObject("teamMorales")
            if (moraleObj != null) {
                val keys = moraleObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    teamMorales[key] = moraleObj.getInt(key)
                }
            }

            // Восстанавливаем фактор побед
            val teamWinningFactors = mutableMapOf<String, Float>()
            val winningObj = obj.optJSONObject("teamWinningFactors")
            if (winningObj != null) {
                val keys = winningObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    teamWinningFactors[key] = winningObj.getDouble(key).toFloat()
                }
            }

            return SavedGame(
                selectedTeamName = obj.getString("selectedTeamName"),
                seed = obj.getLong("seed"),
                matches = matches,
                playoff = playoff,
                teamMomentums = teamMomentums,
                teamMorales = teamMorales,
                teamWinningFactors = teamWinningFactors
            )
        }
    }
}