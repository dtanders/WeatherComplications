package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class SnowDepthComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("15cm").build(),
            contentDescription = PlainComplicationText.Builder("Snow Depth 15cm").build()
        ).setTitle(PlainComplicationText.Builder("SNOW").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 0.15f, min = 0f, max = 0.50f,
            contentDescription = PlainComplicationText.Builder("Snow Depth Range").build()
        ).setText(PlainComplicationText.Builder("15cm").build())
            .setTitle(PlainComplicationText.Builder("SNOW").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatSnowDepth(data?.current?.snowDepth)
        val description = PlainComplicationText.Builder("Snow Depth $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("SNOW").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val maxM = data?.daily?.snowDepthMax?.toFloat() ?: 1f
                val safeMax = if (maxM > 0.01f) maxM else 1f
                val current = (data?.current?.snowDepth?.toFloat() ?: 0f).coerceIn(0f, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("SNOW").build()).build()
            }

            else -> null
        }
    }
}
