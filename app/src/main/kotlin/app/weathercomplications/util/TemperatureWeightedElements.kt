package app.weathercomplications.util

import android.graphics.Color
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import kotlin.math.roundToInt

internal const val COLOR_ORANGE = (255 shl 24) or (255 shl 16) or (140 shl 8)

internal fun buildTemperatureElements(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float,
    maxElements: Int
): List<WeightedElementsComplicationData.Element> {
    val numElements = maxOf(maxElements, 2)
    val totalSpan = maxOf(apparentMax - apparentMin, 1f)
    return List(numElements) { i ->
        val temp = apparentMin + i * totalSpan / (numElements - 1)
        val color = when {
            temp < airMin -> Color.BLUE
            temp > airMax -> COLOR_ORANGE
            else -> Color.WHITE
        }
        WeightedElementsComplicationData.Element(1f, color)
    }
}

fun temperatureWeightedElements(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float
): List<WeightedElementsComplicationData.Element> =
    buildTemperatureElements(
        apparentMin, airMin, airMax, apparentMax,
        WeightedElementsComplicationData.getMaxElements()
    )

internal const val MAX_COLOR_RAMP_STOPS = 8

internal fun computeTemperatureColors(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): IntArray {
    val totalSpan = maxOf(apparentMax - apparentMin, 1f)
    val numStops = minOf(maxOf(totalSpan.roundToInt() + 1, 2), MAX_COLOR_RAMP_STOPS)
    return IntArray(numStops) { i ->
        val temp = apparentMin + i * totalSpan / (numStops - 1)
        when {
            temp < airMin -> Color.BLUE
            temp > airMax -> COLOR_ORANGE
            else -> Color.WHITE
        }
    }
}

fun temperatureColorRamp(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): ColorRamp = ColorRamp(
    colors = computeTemperatureColors(apparentMin, airMin, airMax, apparentMax),
    interpolated = false
)
