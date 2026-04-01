package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.util.WeatherFormatter

class VisibilityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("10km").build(),
            contentDescription = PlainComplicationText.Builder("Visibility 10km").build()
        ).setTitle(PlainComplicationText.Builder("VIS").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 10000f, min = 0f, max = 24140f,
            contentDescription = PlainComplicationText.Builder("Visibility Range").build()
        ).setText(PlainComplicationText.Builder("10km").build())
            .setTitle(PlainComplicationText.Builder("VIS").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatVisibility(data?.current?.visibility)
        val description = PlainComplicationText.Builder("Visibility $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("VIS").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.visibilityMin?.toFloat() ?: return null
                val max = data?.daily?.visibilityMax?.toFloat() ?: return null
                val safeMax = if (max > min) max else min + 1f
                val current = (data?.current?.visibility?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("VIS").build()).build()
            }

            else -> null
        }
    }
}
