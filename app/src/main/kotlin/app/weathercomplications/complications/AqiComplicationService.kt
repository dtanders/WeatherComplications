package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherFormatter

class AqiComplicationService : BaseWeatherComplicationService() {

    private fun aqiLabel(value: Int?): String = when {
        value == null -> getString(R.string.aqi_label_unknown)
        value <= 50   -> getString(R.string.aqi_label_good)
        value <= 100  -> getString(R.string.aqi_label_moderate)
        value <= 150  -> getString(R.string.aqi_label_usg)
        value <= 200  -> getString(R.string.aqi_label_unhealthy)
        value <= 300  -> getString(R.string.aqi_label_very_unhealthy)
        else          -> getString(R.string.aqi_label_hazardous)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val text = WeatherFormatter().formatAqi(42)
        val label = aqiLabel(42)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.aqi_description, text, label)).build()
            ).setTitle(PlainComplicationText.Builder(label).build()).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 42f, min = 0f, max = 300f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.aqi_description, text, label)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(label).build()).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val aqi = data?.current?.aqi
        val text = formatter().formatAqi(aqi)
        val label = aqiLabel(aqi)
        val description = PlainComplicationText.Builder(getString(R.string.aqi_description, text, label)).build()
        val tapAction = weatherAppTapAction()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder(label).build())
                .setTapAction(tapAction).build()

            ComplicationType.RANGED_VALUE -> {
                val value = (aqi ?: 0).toFloat().coerceIn(0f, 300f)
                RangedValueComplicationData.Builder(
                    value = value, min = 0f, max = 300f,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(label).build())
                    .setTapAction(tapAction).build()
            }

            else -> null
        }
    }
}
