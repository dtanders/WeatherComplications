package app.weathercomplications.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesStore(context: Context) {

    private val dataStore = context.applicationContext.userPreferencesDataStore
    private val unitSystemKey = stringPreferencesKey("unit_system")
    private val tapTargetKey = stringPreferencesKey("tap_target")
    private val aqiTypeKey = stringPreferencesKey("aqi_type")

    val unitSystem: Flow<String> = dataStore.data.map { it[unitSystemKey] ?: UNIT_AUTO }
    val tapTarget: Flow<String> = dataStore.data.map { it[tapTargetKey] ?: TAP_AUTO }
    val aqiType: Flow<String> = dataStore.data.map { it[aqiTypeKey] ?: AQI_US }

    suspend fun setUnitSystem(system: String) {
        dataStore.edit { it[unitSystemKey] = system }
    }

    suspend fun setTapTarget(packageName: String) {
        dataStore.edit { it[tapTargetKey] = packageName }
    }

    suspend fun setAqiType(type: String) {
        dataStore.edit { it[aqiTypeKey] = type }
    }

    suspend fun getAqiType(): String = dataStore.data.first()[aqiTypeKey] ?: AQI_US

    suspend fun isImperial(): Boolean = when (dataStore.data.first()[unitSystemKey] ?: UNIT_AUTO) {
        UNIT_IMPERIAL -> true
        UNIT_METRIC -> false
        else -> Locale.getDefault().country == "US"
    }

    suspend fun getTapTarget(): String = dataStore.data.first()[tapTargetKey] ?: TAP_AUTO

    companion object {
        const val UNIT_AUTO = "auto"
        const val UNIT_METRIC = "metric"
        const val UNIT_IMPERIAL = "imperial"

        const val TAP_AUTO = "auto"
        const val TAP_NONE = "none"

        const val AQI_US = "us"
        const val AQI_EU = "eu"
    }
}
