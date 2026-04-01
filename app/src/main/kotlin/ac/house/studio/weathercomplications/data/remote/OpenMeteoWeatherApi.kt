package ac.house.studio.weathercomplications.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("daily") daily: String,
        @Query("models") models: String,
        @Query("forecast_days") forecastDays: Int
    ): WeatherResponse
}
