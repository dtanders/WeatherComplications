package app.weathercomplications.util

import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import kotlin.math.roundToInt

internal const val COLOR_ORANGE = (255 shl 24) or (255 shl 16) or (140 shl 8)

// Four proportional segments: [apparentMin→airMin] [airMin→currentApparent]
// [currentApparent→airMax] [airMax→apparentMax]. Zero-span segments are omitted.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun buildTemperatureElements(
    apparentMin: Float,
    airMin: Float,
    currentApparent: Float,
    airMax: Float,
    apparentMax: Float
): List<WeightedElementsComplicationData.Element> {
    val current = currentApparent.coerceIn(apparentMin, apparentMax)
    val spans = floatArrayOf(
        maxOf(airMin - apparentMin, 0f),
        maxOf(current - airMin, 0f),
        maxOf(airMax - current, 0f),
        maxOf(apparentMax - airMax, 0f)
    )
    val colors = intArrayOf(Color.BLUE, Color.GRAY, Color.WHITE, COLOR_ORANGE)
    val total = spans.sum().coerceAtLeast(1f)
    return spans.zip(colors.toList())
        .filter { (w, _) -> w > 0f }
        .map { (w, c) -> WeightedElementsComplicationData.Element(w / total, c) }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun temperatureWeightedElements(
    apparentMin: Float,
    airMin: Float,
    currentApparent: Float,
    airMax: Float,
    apparentMax: Float
): List<WeightedElementsComplicationData.Element> =
    buildTemperatureElements(apparentMin, airMin, currentApparent, airMax, apparentMax)

internal const val MAX_COLOR_RAMP_STOPS = 8

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun computeTemperatureColors(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float,
    maxStops: Int = MAX_COLOR_RAMP_STOPS
): IntArray {
    val totalSpan = maxOf(apparentMax - apparentMin, 1f)
    val numStops = minOf(maxOf(totalSpan.roundToInt() + 1, 2), maxStops)
    Log.d(LOG_TAG, "totalSteps=${totalSpan} numStops=${numStops}")
    return IntArray(numStops) { i ->
        val temp = apparentMin + i * totalSpan / (numStops - 1)
        Log.d(LOG_TAG, "temp=${temp}")
        when {
            temp < airMin -> Color.BLUE
            temp > airMax -> COLOR_ORANGE
            else -> Color.WHITE
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun temperatureColorRamp(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): ColorRamp = ColorRamp(
    colors = computeTemperatureColors(apparentMin, airMin, airMax, apparentMax),
    interpolated = false
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun goalProgressColorRamp(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): ColorRamp = ColorRamp(
    colors = computeTemperatureColors(apparentMin, airMin, airMax, apparentMax,
        WeightedElementsComplicationData.getMaxElements()),
    interpolated = false
)
