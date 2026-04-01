package app.weathercomplications.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherResponse,
    val daily: DailyWeatherResponse
)

@Serializable
data class CurrentWeatherResponse(
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("dew_point_2m") val dewPoint2m: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("snow_depth") val snowDepth: Double? = null,
    val visibility: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null
)

@Serializable
data class DailyWeatherResponse(
    val time: List<String> = emptyList(),
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("visibility_max") val visibilityMax: List<Double?> = emptyList(),
    @SerialName("visibility_min") val visibilityMin: List<Double?> = emptyList()
)
