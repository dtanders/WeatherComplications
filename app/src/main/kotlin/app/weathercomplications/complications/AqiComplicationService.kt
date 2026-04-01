package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.util.WeatherFormatter

class AqiComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("42").build(),
            contentDescription = PlainComplicationText.Builder("Air Quality Index 42 Good").build()
        ).setTitle(PlainComplicationText.Builder("Good").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 42f, min = 0f, max = 300f,
            contentDescription = PlainComplicationText.Builder("Air Quality Index 42 Good").build()
        ).setText(PlainComplicationText.Builder("42").build())
            .setTitle(PlainComplicationText.Builder("Good").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val aqi = data?.current?.aqi
        val text = WeatherFormatter.formatAqi(aqi)
        val label = WeatherFormatter.aqiLabel(aqi)
        val description = PlainComplicationText.Builder("Air Quality Index $text $label").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder(label).build()).build()

            ComplicationType.RANGED_VALUE -> {
                val value = (aqi ?: 0).toFloat().coerceIn(0f, 300f)
                RangedValueComplicationData.Builder(
                    value = value, min = 0f, max = 300f,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(label).build()).build()
            }

            else -> null
        }
    }
}
