package app.weathercomplications.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.weatherDataStore by preferencesDataStore(name = "weather_cache")

class WeatherDataStore(context: Context) : WeatherCache {

    private val dataStore = context.applicationContext.weatherDataStore

    private val weatherJsonKey = stringPreferencesKey("weather_json")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCachedData(): WeatherData? {
        val stored = dataStore.data.first()[weatherJsonKey] ?: return null
        return try { json.decodeFromString(stored) } catch (_: Exception) { null }
    }

    override suspend fun saveData(data: WeatherData) {
        dataStore.edit { it[weatherJsonKey] = json.encodeToString(data) }
    }
}
