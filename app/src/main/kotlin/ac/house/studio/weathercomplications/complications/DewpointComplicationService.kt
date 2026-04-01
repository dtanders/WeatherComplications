package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class DewpointComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("10.5°").build(),
            contentDescription = PlainComplicationText.Builder("Dewpoint 10.5°").build()
        ).setTitle(PlainComplicationText.Builder("DEW").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatDewpoint(data?.current?.dewpoint)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Dewpoint $text").build()
        ).setTitle(PlainComplicationText.Builder("DEW").build()).build()
    }
}
