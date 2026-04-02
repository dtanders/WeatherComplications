package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherFormatter

class SnowDepthComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val formatter = WeatherFormatter()
        val text = formatter.formatSnowDepth(0.15)
        val title = getString(R.string.snow_depth_title)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.snow_depth_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build()).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 0.15f, min = 0f, max = 2.0f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.snow_depth_range_description)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(title).build()).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val formatter = WeatherFormatter()
        val text = formatter.formatSnowDepth(data?.current?.snowDepth)
        val title = getString(R.string.snow_depth_title)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.snow_depth_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build()).build()

            ComplicationType.RANGED_VALUE -> {
                val current = (data?.current?.snowDepth?.toFloat() ?: 0f).coerceIn(0f, 2.0f)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = 2.0f,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.snow_depth_range_description)).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build()).build()
            }

            else -> null
        }
    }
}
