package app.weathercomplications.util

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureWeightedElementsTest {

    // buildTemperatureElements — 4 proportional segments:
    // [apparentMin→airMin] blue | [airMin→current] gray | [current→airMax] white | [airMax→apparentMax] orange

    private val elements = buildTemperatureElements(-3f, 2f, 12.3f, 15f, 18f)

    @Test fun `returns four elements for typical range`() {
        assertEquals(4, elements.size)
    }

    @Test fun `weights sum to 1`() {
        assertEquals(1f, elements.sumOf { it.weight.toDouble() }.toFloat(), 0.001f)
    }

    @Test fun `segment colors are blue gray white orange`() {
        assertEquals(Color.BLUE,   elements[0].color)
        assertEquals(Color.GRAY,   elements[1].color)
        assertEquals(Color.WHITE,  elements[2].color)
        assertEquals(COLOR_ORANGE, elements[3].color)
    }

    @Test fun `weights are proportional to temperature spans`() {
        // span = 18 - (-3) = 21
        // w1 = (2 - -3) / 21 = 5/21
        // w2 = (12.3 - 2) / 21 = 10.3/21
        // w3 = (15 - 12.3) / 21 = 2.7/21
        // w4 = (18 - 15) / 21 = 3/21
        val span = 21f
        assertEquals(5f / span,    elements[0].weight, 0.001f)
        assertEquals(10.3f / span, elements[1].weight, 0.001f)
        assertEquals(2.7f / span,  elements[2].weight, 0.001f)
        assertEquals(3f / span,    elements[3].weight, 0.001f)
    }

    @Test fun `zero-span segments are omitted`() {
        // current at airMin → gray segment collapses
        val result = buildTemperatureElements(-3f, 2f, 2f, 15f, 18f)
        assertEquals(3, result.size)
        assertTrue(result.none { it.color == Color.GRAY })
    }

    @Test fun `current clamped when above apparentMax`() {
        // current clamped to 18 → white segment (current→airMax) collapses
        val result = buildTemperatureElements(-3f, 2f, 25f, 15f, 18f)
        assertEquals(3, result.size)
        assertTrue(result.none { it.color == Color.WHITE })
    }

    @Test fun `current clamped when below apparentMin`() {
        // current clamped to -3 → gray segment (airMin→current) collapses
        val result = buildTemperatureElements(-3f, 2f, -10f, 15f, 18f)
        assertEquals(3, result.size)
        assertTrue(result.none { it.color == Color.GRAY })
    }

    // computeTemperatureColors

    @Test fun `color stops count is capped at MAX_COLOR_RAMP_STOPS`() {
        // span = 21 degrees would give 22 stops, but cap is MAX_COLOR_RAMP_STOPS
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(MAX_COLOR_RAMP_STOPS, colors.size)
    }

    @Test fun `first stop is blue`() {
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(Color.BLUE, colors[0])
    }

    @Test fun `last stop is orange`() {
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(COLOR_ORANGE, colors.last())
    }

    @Test fun `middle stops spanning air range are white`() {
        // with 8 stops over -3..18 (span=21), step=3 degrees per stop
        // stop 2: -3 + 2*3 = 3° (above airMin=2) → white
        // stop 5: -3 + 5*3 = 12° (below airMax=15) → white
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(Color.WHITE, colors[2])
        assertEquals(Color.WHITE, colors[5])
    }

    @Test fun `all white when apparent range equals air range`() {
        val colors = computeTemperatureColors(0f, 0f, 10f, 10f)
        assertTrue(colors.all { it == Color.WHITE })
    }

    @Test fun `minimum two stops for degenerate range`() {
        val colors = computeTemperatureColors(5f, 5f, 5f, 5f)
        assertEquals(2, colors.size)
    }
}
