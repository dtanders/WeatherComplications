package app.weathercomplications.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AqiResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentAqiResponse
)

@Serializable
data class CurrentAqiResponse(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null
)
