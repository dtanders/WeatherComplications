package app.weathercomplications.complications

import app.weathercomplications.util.LOG_TAG
import android.app.PendingIntent
import app.weathercomplications.util.LOG_TAG
import android.content.ComponentName
import app.weathercomplications.util.LOG_TAG
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import app.weathercomplications.data.UserPreferencesStore
import app.weathercomplications.data.WeatherRepository
import app.weathercomplications.util.WeatherFormatter

abstract class BaseWeatherComplicationService : SuspendingComplicationDataSourceService() {

    protected val repository: WeatherRepository
        get() = WeatherRepository.getInstance(applicationContext)

    protected suspend fun formatter(): WeatherFormatter =
        WeatherFormatter(UserPreferencesStore(applicationContext).isImperial())

    protected suspend fun weatherAppTapAction(): PendingIntent? {
        val target = UserPreferencesStore(applicationContext).getTapTarget()
        return when (target) {
            UserPreferencesStore.TAP_NONE -> null
            UserPreferencesStore.TAP_AUTO -> launchIntentForPackage("com.google.android.wearable.app")
            else -> launchIntentForPackage(target)
        }
    }

    private fun launchIntentForPackage(pkg: String): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    final override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(LOG_TAG, "${javaClass.simpleName} onComplicationRequest type=${request.complicationType}")
        return runCatching { buildComplicationData(request) }
            .onFailure { Log.e(LOG_TAG, "${javaClass.simpleName} buildComplicationData failed", it) }
            .getOrNull()
    }

    abstract suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData?

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        Log.d(LOG_TAG, "${javaClass.simpleName} activated id=$complicationInstanceId type=$type")
        ComplicationDataSourceUpdateRequester
            .create(applicationContext, ComponentName(applicationContext, javaClass))
            .requestUpdate(complicationInstanceId)
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(LOG_TAG, "${javaClass.simpleName} deactivated id=$complicationInstanceId")
    }
}

