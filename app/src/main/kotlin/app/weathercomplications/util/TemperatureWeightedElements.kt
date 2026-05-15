package app.weathercomplications.util

import android.graphics.Color
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData

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
        WeightedElementsComplicationData.Element(weights[2], Color.rgb(255, 140, 0))
    )
}
