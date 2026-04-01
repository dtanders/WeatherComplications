package app.weathercomplications.util

object WeatherFormatter {

    fun formatHumidity(value: Int?): String =
        value?.let { "$it%" } ?: "--"

    fun formatDewpoint(valueC: Double?): String =
        valueC?.let { "%.1f°".format(it) } ?: "--"

    fun formatApparentTemperature(valueC: Double?): String =
        valueC?.let { "%.1f°".format(it) } ?: "--"

    fun formatSnowDepth(valueM: Double?): String =
        valueM?.let { if (it < 0.01) "0cm" else "%.0fcm".format(it * 100) } ?: "--"

    fun formatVisibility(valueM: Double?): String =
        valueM?.let { if (it >= 1000) "%.0fkm".format(it / 1000) else "%.0fm".format(it) } ?: "--"

    fun formatUvIndex(value: Double?): String =
        value?.let { "%.1f".format(it) } ?: "--"

    fun formatAqi(value: Int?): String =
        value?.let { "$it" } ?: "--"

    fun aqiLabel(value: Int?): String = when {
        value == null -> "AQI"
        value <= 50   -> "Good"
        value <= 100  -> "Mod"
        value <= 150  -> "USG"
        value <= 200  -> "Unhl"
        value <= 300  -> "VUnhl"
        else          -> "Haz"
    }
}
