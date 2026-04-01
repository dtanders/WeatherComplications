package ac.house.studio.weathercomplications.data

interface WeatherCache {
    suspend fun getCachedData(): WeatherData?
    suspend fun saveData(data: WeatherData)
}
