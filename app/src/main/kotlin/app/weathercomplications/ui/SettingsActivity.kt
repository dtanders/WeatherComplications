package app.weathercomplications.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.wear.remote.interactions.RemoteActivityHelper
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
import app.weathercomplications.data.UserPreferencesStore.Companion.AQI_EU
import app.weathercomplications.data.UserPreferencesStore.Companion.AQI_US
import app.weathercomplications.data.UserPreferencesStore.Companion.TAP_AUTO
import app.weathercomplications.data.UserPreferencesStore.Companion.TAP_NONE
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

    private data class TapOption(val tag: String, val label: String)

    private val tapOptions = mutableListOf<TapOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        preferences = UserPreferencesStore(this)

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        val unitGroup = findViewById<RadioGroup>(R.id.unit_system_group)
        val aqiTypeGroup = findViewById<RadioGroup>(R.id.aqi_type_group)
        val tapButton = findViewById<Button>(R.id.tap_target_button)

        val remoteActivityHelper = RemoteActivityHelper(this)
        findViewById<TextView>(R.id.attribution_open_meteo).setOnClickListener {
            remoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.attribution_open_meteo_url)))
            ).addListener({}, mainExecutor)
        }

        buildTapOptions()

        scope.launch {
            val savedUnit = preferences.unitSystem.first()
            val savedTap = preferences.tapTarget.first()
            val savedAqiType = preferences.aqiType.first()

            unitGroup.check(when (savedUnit) {
                UNIT_IMPERIAL -> R.id.radio_imperial
                UNIT_METRIC -> R.id.radio_metric
                else -> R.id.radio_auto
            })
            aqiTypeGroup.check(if (savedAqiType == AQI_EU) R.id.radio_aqi_eu else R.id.radio_aqi_us)
            tapButton.text = labelForTag(savedTap)
            tapButton.tag = savedTap

            unitGroup.setOnCheckedChangeListener { _, checkedId ->
                val system = when (checkedId) {
                    R.id.radio_imperial -> UNIT_IMPERIAL
                    R.id.radio_metric -> UNIT_METRIC
                    else -> UNIT_AUTO
                }
                scope.launch {
                    preferences.setUnitSystem(system)
                    requestComplicationUpdates()
                }
            }

            aqiTypeGroup.setOnCheckedChangeListener { _, checkedId ->
                val type = if (checkedId == R.id.radio_aqi_eu) AQI_EU else AQI_US
                scope.launch {
                    preferences.setAqiType(type)
                    requestComplicationUpdates()
                }
            }

            tapButton.setOnClickListener {
                val currentIndex = tapOptions.indexOfFirst { it.tag == tapButton.tag }
                    .coerceAtLeast(0)
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle(R.string.settings_tap_target_title)
                    .setSingleChoiceItems(
                        tapOptions.map { it.label }.toTypedArray(),
                        currentIndex
                    ) { dialog, which ->
                        val selected = tapOptions[which].tag
                        tapButton.text = tapOptions[which].label
                        tapButton.tag = tapOptions[which].tag
                        scope.launch {
                            preferences.setTapTarget(selected)
                            requestComplicationUpdates()
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun buildTapOptions() {
        tapOptions.clear()
        tapOptions.add(TapOption(TAP_AUTO, getString(R.string.settings_tap_auto)))
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .forEach { info ->
                tapOptions.add(TapOption(info.activityInfo.packageName, info.loadLabel(packageManager).toString()))
            }
        tapOptions.add(TapOption(TAP_NONE, getString(R.string.settings_tap_none)))
    }

    private fun labelForTag(tag: String): String =
        tapOptions.firstOrNull { it.tag == tag }?.label ?: getString(R.string.settings_tap_auto)

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
