package com.example.hcmanager

object SampleData {

    var teams: List<Team> = listOf(
        // ─── Запад ───
        Team(name = "СКА",                baseAttack = 70, baseDefense = 70, conference = Conference.WEST),
        Team(name = "ЦСКА",               baseAttack = 93, baseDefense = 89, conference = Conference.WEST),
        Team(name = "Динамо Москва",      baseAttack = 78, baseDefense = 83, conference = Conference.WEST),
        Team(name = "Спартак",            baseAttack = 82, baseDefense = 84, conference = Conference.WEST),
        Team(name = "Локомотив",          baseAttack = 91, baseDefense = 94, conference = Conference.WEST),
        Team(name = "Северсталь",         baseAttack = 76, baseDefense = 73, conference = Conference.WEST),
        Team(name = "Торпедо",            baseAttack = 84, baseDefense = 82, conference = Conference.WEST),
        Team(name = "Динамо Минск",       baseAttack = 82, baseDefense = 77, conference = Conference.WEST),
        Team(name = "ХК Сочи",            baseAttack = 59, baseDefense = 62, conference = Conference.WEST),
        Team(name = "Лада",               baseAttack = 56, baseDefense = 58, conference = Conference.WEST),
        Team(name = "Шанхайские драконы", baseAttack = 77, baseDefense = 72, conference = Conference.WEST),

        // ─── Восток ───
        Team(name = "Металлург",          baseAttack = 90, baseDefense = 86, conference = Conference.EAST),
        Team(name = "Авангард",           baseAttack = 83, baseDefense = 80, conference = Conference.EAST),
        Team(name = "Ак Барс",            baseAttack = 72, baseDefense = 87, conference = Conference.EAST),
        Team(name = "Салават Юлаев",      baseAttack = 80, baseDefense = 76, conference = Conference.EAST),
        Team(name = "Автомобилист",       baseAttack = 81, baseDefense = 85, conference = Conference.EAST),
        Team(name = "Трактор",            baseAttack = 74, baseDefense = 81, conference = Conference.EAST),
        Team(name = "Сибирь",             baseAttack = 64, baseDefense = 68, conference = Conference.EAST),
        Team(name = "Адмирал",            baseAttack = 67, baseDefense = 65, conference = Conference.EAST),
        Team(name = "Амур",               baseAttack = 72, baseDefense = 78, conference = Conference.EAST),
        Team(name = "Барыс",              baseAttack = 73, baseDefense = 75, conference = Conference.EAST),
        Team(name = "Нефтехимик",         baseAttack = 79, baseDefense = 77, conference = Conference.EAST)
    )

    fun updateTeams(newTeams: List<Team>) {
        teams = newTeams
    }
}


