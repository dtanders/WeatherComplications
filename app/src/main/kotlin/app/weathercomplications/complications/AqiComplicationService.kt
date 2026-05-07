package app.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.data.UserPreferencesStore
import app.weathercomplications.util.WeatherFormatter

class AqiComplicationService : BaseWeatherComplicationService() {

    private fun usAqiLabel(value: Int?): String = when {
        value == null -> getString(R.string.aqi_label_unknown)
        value <= 50   -> getString(R.string.aqi_label_good)
        value <= 100  -> getString(R.string.aqi_label_moderate)
        value <= 150  -> getString(R.string.aqi_label_usg)
        value <= 200  -> getString(R.string.aqi_label_unhealthy)
        value <= 300  -> getString(R.string.aqi_label_very_unhealthy)
        else          -> getString(R.string.aqi_label_hazardous)
    }

    private fun euAqiLabel(value: Int?): String = when {
        value == null -> getString(R.string.aqi_label_unknown)
        value <= 20   -> getString(R.string.aqi_label_good)
        value <= 40   -> getString(R.string.aqi_label_fair)
        value <= 60   -> getString(R.string.aqi_label_moderate)
        value <= 80   -> getString(R.string.aqi_label_poor)
        value <= 100  -> getString(R.string.aqi_label_very_poor)
        else          -> getString(R.string.aqi_label_extremely_poor)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val text = WeatherFormatter().formatAqi(42)
        val label = usAqiLabel(42)
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

    override suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val isEu = UserPreferencesStore(applicationContext).getAqiType() == UserPreferencesStore.AQI_EU
        val aqi = if (isEu) data?.current?.europeanAqi else data?.current?.aqi
        val rangeMax = if (isEu) 100f else 300f
        val text = formatter().formatAqi(aqi)
        val label = if (isEu) euAqiLabel(aqi) else usAqiLabel(aqi)
        val description = PlainComplicationText.Builder(getString(R.string.aqi_description, text, label)).build()
        val tapAction = weatherAppTapAction()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder(label).build())
                .setTapAction(tapAction).build()

            ComplicationType.RANGED_VALUE -> {
                val value = (aqi ?: 0).toFloat().coerceIn(0f, rangeMax)
                RangedValueComplicationData.Builder(
                    value = value, min = 0f, max = rangeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(label).build())
                    .setTapAction(tapAction).build()
            }

            else -> null
        }
    }
}
