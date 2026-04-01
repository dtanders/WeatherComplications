package app.weathercomplications.data

import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val current: CurrentWeather,
    val daily: DailyWeather,
    val fetchedAt: Long
)

@Serializable
data class CurrentWeather(
    val relativeHumidity: Int?,
    val dewpoint: Double?,
    val apparentTemperature: Double?,
    val snowDepth: Double?,
    val visibility: Double?,
    val uvIndex: Double?,
    val aqi: Int?
)

@Serializable
data class DailyWeather(
    val apparentTemperatureMax: Double?,
    val apparentTemperatureMin: Double?,
    val uvIndexMax: Double?,
    val visibilityMax: Double?,
    val visibilityMin: Double?
)
