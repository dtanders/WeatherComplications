package app.weathercomplications.util

import android.graphics.Color
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import kotlin.math.roundToInt

internal const val COLOR_ORANGE = (255 shl 24) or (255 shl 16) or (140 shl 8)

internal fun computeTemperatureWeights(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float
): FloatArray {
    val totalSpan = maxOf(apparentMax - apparentMin, 1f)
    val minWeight = totalSpan * 0.05f
    return floatArrayOf(
        maxOf(airMin - apparentMin, minWeight),
        maxOf(airMax - airMin, minWeight),
        maxOf(apparentMax - airMax, minWeight)
    )
}

fun temperatureWeightedElements(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float
): List<WeightedElementsComplicationData.Element> {
    val weights = computeTemperatureWeights(apparentMin, airMin, airMax, apparentMax)
    return listOf(
        WeightedElementsComplicationData.Element(weights[0], Color.BLUE),
        WeightedElementsComplicationData.Element(weights[1], Color.WHITE),
        WeightedElementsComplicationData.Element(weights[2], COLOR_ORANGE)
    )
}

internal fun computeTemperatureColors(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): IntArray {
    val totalSpan = maxOf(apparentMax - apparentMin, 1f)
    val numStops = maxOf(totalSpan.roundToInt() + 1, 2)
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
