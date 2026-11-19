package com.example.hcmanager

object Derby {

    /**
     * 6 дерби-пар для финальной фазы.
     * У каждой команды — не более одного «самого принципиального» соперника.
     */
    val pairs: List<Pair<String, String>> = listOf(
        "ЦСКА" to "СКА",
        "Спартак" to "Динамо Москва",
        "Авангард" to "Металлург",
        "Ак Барс" to "Салават Юлаев",
        "Трактор" to "Автомобилист",
        "Амур" to "Адмирал"
    )

    fun opponentsOf(teamName: String): Set<String> {
        val result = mutableSetOf<String>()
        for ((a, b) in pairs) {
            if (a == teamName) result.add(b)
            if (b == teamName) result.add(a)
        }
        return result
    }

    fun isDerby(nameA: String, nameB: String): Boolean =
        pairs.any { (a, b) -> (a == nameA && b == nameB) || (a == nameB && b == nameA) }
}