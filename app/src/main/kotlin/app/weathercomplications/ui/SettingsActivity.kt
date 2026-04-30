package app.weathercomplications.ui

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.widget.RadioGroup
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import app.weathercomplications.R
import app.weathercomplications.complications.AqiComplicationService
import app.weathercomplications.complications.ApparentTemperatureComplicationService
import app.weathercomplications.complications.DewpointComplicationService
import app.weathercomplications.complications.HumidityComplicationService
import app.weathercomplications.complications.SnowDepthComplicationService
import app.weathercomplications.complications.UvIndexComplicationService
import app.weathercomplications.complications.VisibilityComplicationService
import app.weathercomplications.data.UserPreferencesStore
import app.weathercomplications.data.UserPreferencesStore.Companion.UNIT_AUTO
import app.weathercomplications.data.UserPreferencesStore.Companion.UNIT_IMPERIAL
import app.weathercomplications.data.UserPreferencesStore.Companion.UNIT_METRIC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferences: UserPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        preferences = UserPreferencesStore(this)

        val group = findViewById<RadioGroup>(R.id.unit_system_group)

        scope.launch {
            val radioId = when (preferences.unitSystem.first()) {
                UNIT_IMPERIAL -> R.id.radio_imperial
                UNIT_METRIC -> R.id.radio_metric
                else -> R.id.radio_auto
            }
            group.check(radioId)
            // Attach listener only after initial state is set to avoid spurious save
            group.setOnCheckedChangeListener { _, checkedId ->
                val system = when (checkedId) {
                    R.id.radio_imperial -> UNIT_IMPERIAL
                    R.id.radio_metric -> UNIT_METRIC
                    else -> UNIT_AUTO
                }
                scope.launch {
                    preferences.setUnitSystem(system)
                    requestComplicationUpdates()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun requestComplicationUpdates() {
        listOf(
            HumidityComplicationService::class.java,
            DewpointComplicationService::class.java,
            ApparentTemperatureComplicationService::class.java,
            SnowDepthComplicationService::class.java,
            VisibilityComplicationService::class.java,
            UvIndexComplicationService::class.java,
            AqiComplicationService::class.java,
        ).forEach { service ->
            ComplicationDataSourceUpdateRequester
                .create(applicationContext, ComponentName(applicationContext, service))
                .requestUpdateAll()
        }
    }
}
