package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class UvIndexComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("3.5").build(),
            contentDescription = PlainComplicationText.Builder("UV Index 3.5").build()
        ).setTitle(PlainComplicationText.Builder("UV").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 3.5f, min = 0f, max = 11f,
            contentDescription = PlainComplicationText.Builder("UV Index Range").build()
        ).setText(PlainComplicationText.Builder("3.5").build())
            .setTitle(PlainComplicationText.Builder("UV").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatUvIndex(data?.current?.uvIndex)
        val description = PlainComplicationText.Builder("UV Index $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("UV").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val dailyMax = (data?.daily?.uvIndexMax?.toFloat() ?: 11f).coerceAtMost(11f)
                val rangeMax = maxOf(dailyMax, 1f)
                val current = (data?.current?.uvIndex?.toFloat() ?: 0f).coerceIn(0f, rangeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = rangeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("UV").build()).build()
            }

            else -> null
        }
    }
}
