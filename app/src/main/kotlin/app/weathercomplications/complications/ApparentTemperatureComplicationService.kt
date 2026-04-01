package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.util.WeatherFormatter

class ApparentTemperatureComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("12.3°").build(),
            contentDescription = PlainComplicationText.Builder("Apparent Temperature 12.3°").build()
        ).setTitle(PlainComplicationText.Builder("FEELS").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 12.3f, min = 5f, max = 20f,
            contentDescription = PlainComplicationText.Builder("Apparent Temperature Range").build()
        ).setText(PlainComplicationText.Builder("12.3°").build())
            .setTitle(PlainComplicationText.Builder("FEELS").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatApparentTemperature(data?.current?.apparentTemperature)
        val description = PlainComplicationText.Builder("Apparent Temperature $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("FEELS").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
                val max = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
                val safeMax = if (max > min) max else min + 1f
                val current = (data?.current?.apparentTemperature?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("FEELS").build()).build()
            }

            else -> null
        }
    }
}
