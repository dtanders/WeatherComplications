package app.weathercomplications.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        preferences = UserPreferencesStore(this)

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        val unitGroup = findViewById<RadioGroup>(R.id.unit_system_group)
        val tapGroup = findViewById<RadioGroup>(R.id.tap_target_group)

        val remoteActivityHelper = RemoteActivityHelper(this)
        findViewById<TextView>(R.id.attribution_open_meteo).setOnClickListener {
            remoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.attribution_open_meteo_url)))
            ).addListener({}, mainExecutor)
        }

        populateTapTargets(tapGroup)

        scope.launch {
            val savedUnit = preferences.unitSystem.first()
            val savedTap = preferences.tapTarget.first()

            unitGroup.check(when (savedUnit) {
                UNIT_IMPERIAL -> R.id.radio_imperial
                UNIT_METRIC -> R.id.radio_metric
                else -> R.id.radio_auto
            })
            checkByTag(tapGroup, savedTap)

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

            tapGroup.setOnCheckedChangeListener { _, checkedId ->
                val pkg = tapGroup.findViewById<RadioButton>(checkedId)?.tag as? String ?: TAP_AUTO
                scope.launch {
                    preferences.setTapTarget(pkg)
                    requestComplicationUpdates()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun populateTapTargets(group: RadioGroup) {
        listOf(TAP_AUTO to getString(R.string.settings_tap_auto),
               TAP_NONE to getString(R.string.settings_tap_none)).forEach { (tag, label) ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                this.tag = tag
            })
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .forEach { info ->
                group.addView(RadioButton(this).apply {
                    id = View.generateViewId()
                    text = info.loadLabel(packageManager).toString()
                    tag = info.activityInfo.packageName
                })
            }
    }

    private fun checkByTag(group: RadioGroup, target: String) {
        for (i in 0 until group.childCount) {
            val button = group.getChildAt(i) as? RadioButton ?: continue
            if (button.tag == target) {
                group.check(button.id)
                return
            }
        }
        group.check((group.getChildAt(0) as? RadioButton)?.id ?: return)
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
