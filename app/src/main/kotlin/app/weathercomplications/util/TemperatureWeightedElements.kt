package app.weathercomplications.util

import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData

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

// ColorRamp allows at most 7 colors; non-interpolated ramps render as equal-sized blocks.
internal const val MAX_COLOR_RAMP_BLOCKS = 7

data class TemperatureArc(val min: Float, val max: Float, val ramp: ColorRamp)

// Three segments over the widened range min(lows)..max(highs):
// blue = |airMin − apparentMin| | white = middle | orange = |apparentMax − airMax|.
// The 7 equal ramp blocks are allocated proportionally to the segment spans
// (largest-remainder rounding), with at least one block per non-zero segment.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun temperatureArc(
    apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float
): TemperatureArc {
    val lo = minOf(apparentMin, airMin)
    val rawHi = maxOf(apparentMax, airMax)
    val hi = maxOf(rawHi, lo + 1f)
    val loEnd = maxOf(apparentMin, airMin).coerceIn(lo, hi)
    // If the range had to be padded open, the padding reads as neutral middle, not heat.
    val hiStart = if (hi > rawHi) hi else minOf(apparentMax, airMax).coerceIn(loEnd, hi)
    val spans = floatArrayOf(loEnd - lo, hiStart - loEnd, hi - hiStart)
    val segmentColors = intArrayOf(Color.BLUE, Color.WHITE, COLOR_ORANGE)
    val blocks = allocateBlocks(spans, MAX_COLOR_RAMP_BLOCKS)
    val colors = segmentColors.toList()
        .flatMapIndexed { i, color -> List(blocks[i]) { color } }
        .toIntArray()
    Log.d(LOG_TAG, "arc lo=$lo loEnd=$loEnd hiStart=$hiStart hi=$hi blocks=${blocks.toList()}")
    return TemperatureArc(lo, hi, ColorRamp(colors, interpolated = false))
}

// Splits totalBlocks across spans proportionally: zero spans get zero blocks,
// non-zero spans get at least one, remaining blocks go to the largest remainders.
internal fun allocateBlocks(spans: FloatArray, totalBlocks: Int): IntArray {
    val total = spans.sum()
    if (total <= 0f) {
        // Degenerate input: fall back to an all-white ramp (ColorRamp needs ≥2 colors).
        return spans.indices.map { if (it == 1) 2 else 0 }.toIntArray()
    }
    val ideal = spans.map { it / total * totalBlocks }
    val blocks = IntArray(spans.size) { i ->
        if (spans[i] > 0f) maxOf(ideal[i].toInt(), 1) else 0
    }
    var remaining = totalBlocks - blocks.sum()
    val byRemainder = ideal.indices
        .filter { spans[it] > 0f }
        .sortedByDescending { ideal[it] - ideal[it].toInt() }
    var idx = 0
    while (remaining > 0 && byRemainder.isNotEmpty()) {
        blocks[byRemainder[idx % byRemainder.size]]++
        remaining--
        idx++
    }
    // Guaranteed minimums may have overshot the total; shrink the largest segments back.
    while (remaining < 0) {
        val largest = blocks.indices.filter { blocks[it] > 1 }.maxByOrNull { blocks[it] } ?: break
        blocks[largest]--
        remaining++
    }
    // ColorRamp requires at least 2 colors; pad the only non-zero segment if needed.
    if (blocks.sum() < 2) {
        val only = blocks.indices.first { blocks[it] > 0 }
        blocks[only] = 2
    }
    return blocks
}
