package app.weathercomplications.complications

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherConditionIcon
import app.weathercomplications.util.WeatherFormatter
import app.weathercomplications.util.temperatureWeightedElements

class ApparentTemperatureComplicationService : BaseWeatherComplicationService() {

    @SuppressLint("NewApi")
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val formatter = WeatherFormatter()
        val text = formatter.formatApparentTemperature(12.3)
        val title = getString(R.string.apparent_temperature_title)
        val image = monoImage(WeatherConditionIcon.forWmoCode(0))
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setMonochromaticImage(image).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 12.3f, min = 5f, max = 20f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_range_description)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(title).build())
                .setMonochromaticImage(image).build()

            ComplicationType.WEIGHTED_ELEMENTS -> {
                val elements = temperatureWeightedElements(
                    apparentMin = -3f, airMin = 2f, airMax = 15f, apparentMax = 18f
                )
                WeightedElementsComplicationData.Builder(
                    elements = elements,
                    contentDescription = PlainComplicationText.Builder(
                        getString(R.string.apparent_temperature_weighted_description)
                    ).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .build()
            }

            else -> null
        }
    }

    @SuppressLint("NewApi")
    override suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE &&
            request.complicationType != ComplicationType.WEIGHTED_ELEMENTS) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val formatter = formatter()
        val text = formatter.formatApparentTemperature(data?.current?.apparentTemperature)
        val title = getString(R.string.apparent_temperature_title)
        val image = monoImage(WeatherConditionIcon.forWmoCode(data?.current?.weatherCode))
        val tapAction = weatherAppTapAction()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_description, text)).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setMonochromaticImage(image).setTapAction(tapAction).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
                val max = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
                val safeMax = if (max > min) max else min + 1f
                val current = (data.current.apparentTemperature?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_range_description)).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setMonochromaticImage(image).setTapAction(tapAction).build()
            }

            ComplicationType.WEIGHTED_ELEMENTS -> {
                val apparentMin = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
                val apparentMax = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
                val airMin = data?.daily?.temperatureMin?.toFloat() ?: return null
                val airMax = data?.daily?.temperatureMax?.toFloat() ?: return null
                val elements = temperatureWeightedElements(apparentMin, airMin, airMax, apparentMax)
                WeightedElementsComplicationData.Builder(
                    elements = elements,
                    contentDescription = PlainComplicationText.Builder(
                        getString(R.string.apparent_temperature_weighted_description)
                    ).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setTapAction(tapAction)
                    .build()
            }

            else -> null
        }
    }

    private fun monoImage(resId: Int) =
        MonochromaticImage.Builder(Icon.createWithResource(this, resId)).build()
}
