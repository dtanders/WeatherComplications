package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherFormatter

class VisibilityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val formatter = WeatherFormatter()
        val text = formatter.formatVisibility(10000.0)
        val title = getString(R.string.visibility_title)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.visibility_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build()).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 10000f, min = 0f, max = 24140f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.visibility_range_description)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(title).build()).build()

            else -> null
        }
    }

    override suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val formatter = formatter()
        val text = formatter.formatVisibility(data?.current?.visibility)
        val title = getString(R.string.visibility_title)
        val tapAction = weatherAppTapAction()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.visibility_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setTapAction(tapAction).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.visibilityMin?.toFloat() ?: return null
                val max = data?.daily?.visibilityMax?.toFloat() ?: return null
                val safeMax = if (max > min) max else min + 1f
                val current = (data.current.visibility?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.visibility_range_description)).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setTapAction(tapAction).build()
            }

            else -> null
        }
    }
}
