package app.weathercomplications.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherConditionIcon
import app.weathercomplications.util.WeatherFormatter
import app.weathercomplications.util.temperatureWeightedElements

class ApparentTemperatureWeightedComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val formatter = WeatherFormatter()
        val text = formatter.formatApparentTemperature(12.3)
        val title = getString(R.string.apparent_temperature_title)
        val image = monoImage(WeatherConditionIcon.forWmoCode(0))
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder(
                    getString(R.string.apparent_temperature_description, text)
                ).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setMonochromaticImage(image).build()

            ComplicationType.WEIGHTED_ELEMENTS -> WeightedElementsComplicationData.Builder(
                elements = temperatureWeightedElements(-3f, 2f, 15f, 18f),
                contentDescription = PlainComplicationText.Builder(
                    getString(R.string.apparent_temperature_weighted_description)
                ).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(
                    formatter.formatTemperatureRange(-3.0, 18.0)
                ).build())
                .setMonochromaticImage(image).build()

            else -> null
        }
    }

    override suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
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
                contentDescription = PlainComplicationText.Builder(
                    getString(R.string.apparent_temperature_description, text)
                ).build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setMonochromaticImage(image).setTapAction(tapAction).build()

            ComplicationType.WEIGHTED_ELEMENTS -> {
                val apparentMin = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
                val apparentMax = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
                val airMin = data.daily.temperatureMin?.toFloat() ?: return null
                val airMax = data.daily.temperatureMax?.toFloat() ?: return null
                val tempTitle = formatter.formatTemperatureRange(
                    apparentMin.toDouble(), apparentMax.toDouble()
                )
                WeightedElementsComplicationData.Builder(
                    elements = temperatureWeightedElements(apparentMin, airMin, airMax, apparentMax),
                    contentDescription = PlainComplicationText.Builder(
                        getString(R.string.apparent_temperature_weighted_description)
                    ).build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(tempTitle).build())
                    .setMonochromaticImage(image).setTapAction(tapAction).build()
            }

            else -> null
        }
    }

    private fun monoImage(resId: Int) =
        MonochromaticImage.Builder(Icon.createWithResource(this, resId)).build()
}
