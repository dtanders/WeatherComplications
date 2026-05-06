package app.weathercomplications.data

import app.weathercomplications.util.LOG_TAG
import android.content.Context
import android.location.Location
import android.util.Log
import app.weathercomplications.data.remote.ApiServiceFactory
import app.weathercomplications.data.remote.OpenMeteoAqiApi
import app.weathercomplications.data.remote.OpenMeteoWeatherApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherRepository(
    private val weatherApi: OpenMeteoWeatherApi,
    private val aqiApi: OpenMeteoAqiApi,
    private val locationRepository: LocationRepository,
    private val cache: WeatherCache
) {
    companion object {
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L
        private const val LOCATION_INVALIDATION_METERS = 20_000f

        @Volatile private var instance: WeatherRepository? = null
        private val fetchMutex = Mutex()

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
        Log.d(LOG_TAG, "getWeatherData called")
        cache.getCachedData()?.let {
            if (System.currentTimeMillis() - it.fetchedAt < CACHE_DURATION_MS && !locationChangedSignificantly(it)) {
                Log.d(LOG_TAG, "returning cached data age=${(System.currentTimeMillis() - it.fetchedAt) / 1000}s")
                return it
            }
        }
        return fetchMutex.withLock {
            cache.getCachedData()?.let {
                if (System.currentTimeMillis() - it.fetchedAt < CACHE_DURATION_MS && !locationChangedSignificantly(it)) {
                    Log.d(LOG_TAG, "cache hit after lock (another coroutine fetched)")
                    return@withLock it
                }
            }
            Log.d(LOG_TAG, "cache miss, fetching")
            fetchAndCache()
        }
    }

    private suspend fun locationChangedSignificantly(cached: WeatherData): Boolean {
        val cachedLat = cached.latitude ?: return false
        val cachedLon = cached.longitude ?: return false
        val current = runCatching { locationRepository.getLastKnownLocation() }.getOrNull() ?: return false
        val results = FloatArray(1)
        Location.distanceBetween(cachedLat, cachedLon, current.latitude, current.longitude, results)
        val changed = results[0] > LOCATION_INVALIDATION_METERS
        if (changed) Log.d(LOG_TAG, "location changed ${results[0].toInt()}m, invalidating cache")
        return changed
    }

    private suspend fun fetchAndCache(): WeatherData {
        return try {
            Log.d(LOG_TAG, "fetchAndCache: getting location")
            val location = locationRepository.getLocation()
            Log.d(LOG_TAG, "fetchAndCache: location=${location.latitude},${location.longitude}")
            val weather = runCatching {
                weatherApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    current = "relative_humidity_2m,dew_point_2m,apparent_temperature,snow_depth,visibility,uv_index,weather_code",
                    daily = "apparent_temperature_max,apparent_temperature_min,uv_index_max,visibility_max,visibility_min",
                    models = "best_match",
                    forecastDays = 1
                )
            }.getOrElse { return cache.getCachedData() ?: throw it }
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
                    aqi = aqi?.current?.usAqi,
                    weatherCode = weather.current.weatherCode
                ),
                daily = DailyWeather(
                    apparentTemperatureMax = weather.daily.apparentTemperatureMax.firstOrNull(),
                    apparentTemperatureMin = weather.daily.apparentTemperatureMin.firstOrNull(),
                    uvIndexMax = weather.daily.uvIndexMax.firstOrNull(),
                    visibilityMax = weather.daily.visibilityMax.firstOrNull(),
                    visibilityMin = weather.daily.visibilityMin.firstOrNull()
                ),
                fetchedAt = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude
            )
            cache.saveData(data)
            Log.d(LOG_TAG, "fetchAndCache: saved uv=${data.current.uvIndex} humidity=${data.current.relativeHumidity}")
            data
        } catch (e: Exception) {
            Log.e(LOG_TAG, "fetchAndCache failed", e)
            cache.getCachedData() ?: throw e
        }
    }
}

