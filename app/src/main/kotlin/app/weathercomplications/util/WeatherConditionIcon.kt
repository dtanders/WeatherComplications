package app.weathercomplications.util

import androidx.annotation.DrawableRes
import app.weathercomplications.R

object WeatherConditionIcon {
    @DrawableRes
    fun forWmoCode(code: Int?): Int = when (code) {
        0, 1 -> R.drawable.ic_condition_clear
        2 -> R.drawable.ic_condition_partly_cloudy
        3 -> R.drawable.ic_condition_cloudy
        45, 48 -> R.drawable.ic_condition_fog
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        80, 81, 82 -> R.drawable.ic_condition_rain
        71, 73, 75, 77,
        85, 86 -> R.drawable.ic_condition_snow
        95, 96, 99 -> R.drawable.ic_condition_thunderstorm
        else -> R.drawable.ic_condition_cloudy
    }
}
