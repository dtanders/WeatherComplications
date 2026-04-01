package ac.house.studio.weathercomplications.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WeatherDataStore(context: Context) : WeatherCache {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("weather_cache") }
    )

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
