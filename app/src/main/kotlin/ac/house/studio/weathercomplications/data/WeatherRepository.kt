package ac.house.studio.weathercomplications.data

import android.content.Context
import ac.house.studio.weathercomplications.data.remote.ApiServiceFactory
import ac.house.studio.weathercomplications.data.remote.OpenMeteoAqiApi
import ac.house.studio.weathercomplications.data.remote.OpenMeteoWeatherApi

class WeatherRepository(
    private val weatherApi: OpenMeteoWeatherApi,
    private val aqiApi: OpenMeteoAqiApi,
    private val locationRepository: LocationRepository,
    private val cache: WeatherCache
) {
    companion object {
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L

        @Volatile private var instance: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository = instance ?: synchronized(this) {
            instance ?: WeatherRepository(
                weatherApi = ApiServiceFactory.weatherApi,
                aqiApi = ApiServiceFactory.aqiApi,
                locationRepository = LocationRepository(context.applicationContext),
                cache = WeatherDataStore(context.applicationContext)
            ).also { instance = it }
        }
    }

    suspend fun getWeatherData(): WeatherData {
        val cached = cache.getCachedData()
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_DURATION_MS) {
            return cached
        }
        return fetchAndCache()
    }

    private suspend fun fetchAndCache(): WeatherData {
        val location = locationRepository.getLocation()
        val weather = weatherApi.getForecast(
            latitude = location.latitude,
            longitude = location.longitude,
            current = "relative_humidity_2m,dew_point_2m,apparent_temperature,snow_depth,visibility,uv_index",
            daily = "apparent_temperature_max,apparent_temperature_min,uv_index_max,snow_depth_max,visibility_max,visibility_min",
            models = "best_match",
            forecastDays = 1
        )
        val aqi = runCatching {
            aqiApi.getAirQuality(
                latitude = location.latitude,
                longitude = location.longitude,
                current = "us_aqi,european_aqi"
            )
        }.getOrNull()

        val data = WeatherData(
            current = CurrentWeather(
                relativeHumidity = weather.current.relativeHumidity2m,
                dewpoint = weather.current.dewPoint2m,
                apparentTemperature = weather.current.apparentTemperature,
                snowDepth = weather.current.snowDepth,
                visibility = weather.current.visibility,
                uvIndex = weather.current.uvIndex,
                aqi = aqi?.current?.usAqi
            ),
            daily = DailyWeather(
                apparentTemperatureMax = weather.daily.apparentTemperatureMax.firstOrNull(),
                apparentTemperatureMin = weather.daily.apparentTemperatureMin.firstOrNull(),
                uvIndexMax = weather.daily.uvIndexMax.firstOrNull(),
                snowDepthMax = weather.daily.snowDepthMax.firstOrNull(),
                visibilityMax = weather.daily.visibilityMax.firstOrNull(),
                visibilityMin = weather.daily.visibilityMin.firstOrNull()
            ),
            fetchedAt = System.currentTimeMillis()
        )
        cache.saveData(data)
        return data
    }
}
