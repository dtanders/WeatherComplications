package ac.house.studio.weathercomplications.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoAqiApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String
    ): AqiResponse
}
