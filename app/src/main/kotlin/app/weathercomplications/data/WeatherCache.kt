package app.weathercomplications.data

interface WeatherCache {
    suspend fun getCachedData(): WeatherData?
    suspend fun saveData(data: WeatherData)
}
