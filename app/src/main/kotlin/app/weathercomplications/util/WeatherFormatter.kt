package app.weathercomplications.util

import java.util.Locale

class WeatherFormatter(private val isImperial: Boolean = Locale.getDefault().country == "US") {

    fun formatHumidity(value: Int?): String =
        value?.let { "$it%" } ?: "--"

    fun formatDewpoint(valueC: Double?): String =
        valueC?.let { "%.1f°".format(if (isImperial) it * 9.0 / 5.0 + 32.0 else it) } ?: "--"

    fun formatApparentTemperature(valueC: Double?): String =
        valueC?.let { "%.1f°".format(if (isImperial) it * 9.0 / 5.0 + 32.0 else it) } ?: "--"

    fun formatSnowDepth(valueM: Double?): String = valueM?.let {
        if (isImperial) {
            val inches = it * 39.3701
            if (inches < 0.1) "0in" else "%.1fin".format(inches)
        } else {
            if (it < 0.01) "0cm" else "%.0fcm".format(it * 100)
        }
    } ?: "--"

    fun formatVisibility(valueM: Double?): String = valueM?.let {
        if (isImperial) {
            val miles = it / 1609.344
            if (miles >= 1) "%.0fmi".format(miles) else "%.1fmi".format(miles)
        } else {
            if (it >= 1000) "%.0fkm".format(it / 1000) else "%.0fm".format(it)
        }
    } ?: "--"

    fun formatUvIndex(value: Double?): String =
        value?.let { "%.1f".format(it) } ?: "--"

    fun formatAqi(value: Int?): String =
        value?.let { "$it" } ?: "--"
}
