package com.example.hcmanager

import androidx.annotation.DrawableRes

object TeamLogo {
    @DrawableRes
    fun getLogoRes(teamName: String): Int? {
        return when (teamName) {
            "СКА" -> R.drawable.ska
            "ЦСКА" -> R.drawable.cska
            "Динамо Москва" -> R.drawable.dinamo_moscow
            "Спартак" -> R.drawable.spartak
            "Локомотив" -> R.drawable.lokomotiv
            "Северсталь" -> R.drawable.severstal
            "Торпедо" -> R.drawable.torpedo
            "Динамо Минск" -> R.drawable.dinamo_minsk
            "ХК Сочи" -> R.drawable.sochi
            "Лада" -> R.drawable.lada
            "Шанхайские драконы" -> R.drawable.dragons

            "Металлург" -> R.drawable.metallurg
            "Авангард" -> R.drawable.avangard
            "Ак Барс" -> R.drawable.akbars
            "Салават Юлаев" -> R.drawable.salavat
            "Автомобилист" -> R.drawable.avtomobilist
            "Трактор" -> R.drawable.traktor
            "Сибирь" -> R.drawable.sibir
            "Адмирал" -> R.drawable.admiral
            "Амур" -> R.drawable.amur
            "Барыс" -> R.drawable.baris
            "Нефтехимик" -> R.drawable.neftechimik
            else -> null
        }
    }
}