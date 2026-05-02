package app.weathercomplications.complications

import android.app.PendingIntent
import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.data.ComplicationType
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

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        ComplicationDataSourceUpdateRequester
            .create(applicationContext, ComponentName(applicationContext, javaClass))
            .requestUpdate(complicationInstanceId)
    }
}
