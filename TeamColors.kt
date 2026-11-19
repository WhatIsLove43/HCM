package com.example.hcmanager

import androidx.compose.ui.graphics.Color

object TeamColors {
    // Яркий зеленый по умолчанию (если цвета нет в списке)
    private val DefaultColor = Color(0xFF22C55E)

    fun getColor(teamName: String): Color = when (teamName) {
        // Синие команды делаем ярко-голубыми, чтобы выделялись на темном фоне
        "СКА", "Динамо Москва", "Динамо Минск", "Торпедо", "Лада", "Сибирь", "Барыс", "Нефтехимик"
            -> Color(0xFF4DB8FF) // Светло-голубой (Sky Blue)

        // Бирюзовые
        "ХК Сочи"
            -> Color(0xFF11887F)

        // Красные команды
        "ЦСКА", "Спартак", "Локомотив", "Авангард", "Автомобилист"
            -> Color(0xFFFF453A) // Ярко-красный

        "Трактор"
            -> Color(0xFFFFFFFF)

        // Зеленые команды
        "Ак Барс", "Салават Юлаев"
            -> Color(0xFF14802E) // Ярко-зеленый

        // Желтые/Оранжевые
        "Северсталь", "Металлург", "Адмирал"
            -> Color(0xFFFFCC00) // Золотой/Желтый

        // Желтые/Оранжевые
        "Шанхайские драконы", "Амур"
            -> Color(0xFFCB8214)

        else -> DefaultColor
    }
}