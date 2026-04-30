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

    val unitSystem: Flow<String> = dataStore.data.map { it[unitSystemKey] ?: UNIT_AUTO }

    suspend fun setUnitSystem(system: String) {
        dataStore.edit { it[unitSystemKey] = system }
    }

    suspend fun isImperial(): Boolean = when (dataStore.data.first()[unitSystemKey] ?: UNIT_AUTO) {
        UNIT_IMPERIAL -> true
        UNIT_METRIC -> false
        else -> Locale.getDefault().country == "US"
    }

    companion object {
        const val UNIT_AUTO = "auto"
        const val UNIT_METRIC = "metric"
        const val UNIT_IMPERIAL = "imperial"
    }
}
