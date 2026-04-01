package ac.house.studio.weathercomplications.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object ApiServiceFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    val weatherApi: OpenMeteoWeatherApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoWeatherApi::class.java)

    val aqiApi: OpenMeteoAqiApi = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoAqiApi::class.java)
}
