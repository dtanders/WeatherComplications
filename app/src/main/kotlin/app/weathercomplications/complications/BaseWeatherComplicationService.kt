package app.weathercomplications.complications

import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import app.weathercomplications.data.WeatherRepository

abstract class BaseWeatherComplicationService : SuspendingComplicationDataSourceService() {

    protected val repository: WeatherRepository
        get() = WeatherRepository.getInstance(applicationContext)

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        ComplicationDataSourceUpdateRequester
            .create(applicationContext, ComponentName(applicationContext, javaClass))
            .requestUpdate(complicationInstanceId)
    }
}
