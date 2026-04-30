package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherFormatter

class HumidityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val text = WeatherFormatter().formatHumidity(65)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(getString(R.string.humidity_description, text)).build()
        ).setTitle(PlainComplicationText.Builder(getString(R.string.humidity_title)).build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = formatter().formatHumidity(data?.current?.relativeHumidity)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(getString(R.string.humidity_description, text)).build()
        ).setTitle(PlainComplicationText.Builder(getString(R.string.humidity_title)).build()).build()
    }
}
