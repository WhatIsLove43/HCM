package com.example.hcmanager

import kotlin.math.roundToInt

data class Team(
    val name: String,
    val baseAttack: Int,
    val baseDefense: Int,
    val conference: Conference
) {
    // Психологическое состояние (0..100)
    var morale: Int = 65

    // Краткосрочная форма (-5.0 .. +5.0)
    var momentum: Float = 0.0f

    // Успешная серия (-3.0 .. +3.0)
    var winningFactor: Float = 0.0f

    // Итоговый модификатор в процентах (-5% .. +5%)
    val dynamicModifier: Double
        get() {
            val moraleMod = ((morale - 50) / 50.0) * 0.03       // ±3%
            val momentumMod = (momentum / 5.0) * 0.02           // ±2%
            val winningMod = (winningFactor / 3.0) * 0.02       // ±2%

            // Суммируем и жестко ограничиваем от -0.05 до +0.05 (±5%)
            return (moraleMod + momentumMod + winningMod).coerceIn(-0.05, 0.05)
        }

    // Эффективные рейтинги для симулятора
    val currentAttack: Double
        get() = baseAttack * (1.0 + dynamicModifier)

    val currentDefense: Double
        get() = baseDefense * (1.0 + dynamicModifier)

    // Базовая сила для определения фаворита/аутсайдера
    val baseStrength: Int
        get() = baseAttack + baseDefense
}