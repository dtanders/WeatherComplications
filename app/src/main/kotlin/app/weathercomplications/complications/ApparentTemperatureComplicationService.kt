package app.weathercomplications.complications

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.os.Build
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import app.weathercomplications.R
import app.weathercomplications.util.WeatherConditionIcon
import app.weathercomplications.util.WeatherFormatter
import app.weathercomplications.util.temperatureArc

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
                value = 12.3f, min = -3f, max = 18f,
                contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_range_description)).build()
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder(formatter.formatTemperatureRange(-3.0, 18.0)).build())
                .setMonochromaticImage(image)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setColorRamp(temperatureArc(-3f, 2f, 15f, 18f).ramp)
                    }
                }
                .build()

            else -> null
        }
    }

    @SuppressLint("NewApi")
    override suspend fun buildComplicationData(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT &&
            request.complicationType != ComplicationType.RANGED_VALUE) return null
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
                val apparentMin = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
                val apparentMax = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
                val tempTitle = formatter.formatTemperatureRange(
                    apparentMin.toDouble(), apparentMax.toDouble()
                )
                val airMin = data.daily.temperatureMin?.toFloat()
                val airMax = data.daily.temperatureMax?.toFloat()
                val hasRamp = airMin != null && airMax != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                val arc = if (hasRamp) {
                    temperatureArc(apparentMin, airMin!!, airMax!!, apparentMax)
                } else null
                val min = arc?.min ?: apparentMin
                val max = arc?.max ?: maxOf(apparentMax, apparentMin + 1f)
                val current =
                    (data.current.apparentTemperature?.toFloat() ?: min).coerceIn(min, max)
                val builder = RangedValueComplicationData.Builder(
                    value = current, min = min, max = max,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.apparent_temperature_range_description))
                        .build()
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(tempTitle).build())
                    .setMonochromaticImage(image).setTapAction(tapAction)
                if (arc != null) {
                    builder.setColorRamp(arc.ramp)
                }
                builder.build()
            }

            else -> null
        }
    }

    private fun monoImage(resId: Int) =
        MonochromaticImage.Builder(Icon.createWithResource(this, resId)).build()
}
