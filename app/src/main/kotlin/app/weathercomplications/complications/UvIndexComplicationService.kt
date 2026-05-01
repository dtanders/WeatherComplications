package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherFormatter

class UvIndexComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val text = WeatherFormatter().formatUvIndex(3.5)
        val title = getString(R.string.uv_index_title)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.uv_index_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build()).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 3.5f, min = 0f, max = 11f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.uv_index_range_description)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(title).build()).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = formatter().formatUvIndex(data?.current?.uvIndex)
        val title = getString(R.string.uv_index_title)
        val tapAction = weatherAppTapAction()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.uv_index_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setTapAction(tapAction).build()

            ComplicationType.RANGED_VALUE -> {
                val dailyMax = (data?.daily?.uvIndexMax?.toFloat() ?: 0f).coerceAtMost(11f)
                val rangeMax = maxOf(dailyMax, 1f)
                val current = (data?.current?.uvIndex?.toFloat() ?: 0f).coerceIn(0f, rangeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = rangeMax,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.uv_index_range_description)).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setTapAction(tapAction).build()
            }

            else -> null
        }
    }
}
