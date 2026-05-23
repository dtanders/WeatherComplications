package app.weathercomplications.util

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureWeightedElementsTest {

    // buildTemperatureElements

    @Test fun `element count matches maxElements parameter`() {
        val elements = buildTemperatureElements(-3f, 2f, 15f, 18f, 7)
        assertEquals(7, elements.size)
    }

    @Test fun `first element is blue`() {
        // stop 0 maps to apparentMin=-3, below airMin=2
        val elements = buildTemperatureElements(-3f, 2f, 15f, 18f, 7)
        assertEquals(Color.BLUE, elements.first().color)
    }

    @Test fun `last element is orange`() {
        // last stop maps to apparentMax=18, above airMax=15
        val elements = buildTemperatureElements(-3f, 2f, 15f, 18f, 7)
        assertEquals(COLOR_ORANGE, elements.last().color)
    }

    @Test fun `middle elements spanning air range are white`() {
        // 7 elements over -3..18 (span=21), step=3.5 per element
        // element 2: -3 + 2*3.5 = 4° (above airMin=2) → white
        // element 4: -3 + 4*3.5 = 11° (below airMax=15) → white
        val elements = buildTemperatureElements(-3f, 2f, 15f, 18f, 7)
        assertEquals(Color.WHITE, elements[2].color)
        assertEquals(Color.WHITE, elements[4].color)
    }

    @Test fun `elements all white when apparent range equals air range`() {
        val elements = buildTemperatureElements(0f, 0f, 10f, 10f, 7)
        assertTrue(elements.all { it.color == Color.WHITE })
    }

    @Test fun `minimum two elements for degenerate range`() {
        val elements = buildTemperatureElements(5f, 5f, 5f, 5f, 1)
        assertEquals(2, elements.size)
    }

    @Test fun `all elements have equal weight`() {
        val elements = buildTemperatureElements(-3f, 2f, 15f, 18f, 7)
        assertTrue(elements.all { it.weight == 1f })
    }

    // computeTemperatureColors

    @Test fun `color stops count is capped at MAX_COLOR_RAMP_STOPS`() {
        // span = 21 degrees would give 22 stops, but cap is MAX_COLOR_RAMP_STOPS
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(MAX_COLOR_RAMP_STOPS, colors.size)
    }

    @Test fun `first stop is blue`() {
        // stop 0 maps to apparentMin=-3, below airMin=2
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(Color.BLUE, colors[0])
    }

    @Test fun `last stop is orange`() {
        // last stop maps to apparentMax=18, above airMax=15
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
