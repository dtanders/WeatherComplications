package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class HumidityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("65%").build(),
            contentDescription = PlainComplicationText.Builder("Relative Humidity 65%").build()
        ).setTitle(PlainComplicationText.Builder("HUMID").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatHumidity(data?.current?.relativeHumidity)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Relative Humidity $text").build()
        ).setTitle(PlainComplicationText.Builder("HUMID").build()).build()
    }
}
