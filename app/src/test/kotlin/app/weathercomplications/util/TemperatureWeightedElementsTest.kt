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

    // temperatureArc — 3 segments over the widened range min(lows)..max(highs):
    // blue |airMin−apparentMin| | white middle | orange |apparentMax−airMax|,
    // rendered as 7 equal ColorRamp blocks allocated by largest remainder.

    @Test fun `arc spans apparent range when apparent is wider`() {
        val arc = temperatureArc(-3f, 2f, 15f, 18f)
        assertEquals(-3f, arc.min)
        assertEquals(18f, arc.max)
    }

    @Test fun `arc spans air range when air is wider`() {
        // absolute-difference case: apparent range inside air range
        val arc = temperatureArc(5f, 2f, 15f, 13f)
        assertEquals(2f, arc.min)
        assertEquals(15f, arc.max)
    }

    @Test fun `typical input yields seven blocks proportional to spans`() {
        // spans 5 : 13 : 3 over 21 → ideal 1.67 : 4.33 : 1.0 → blocks 2 : 4 : 1
        val colors = temperatureArc(-3f, 2f, 15f, 18f).ramp.colors
        assertEquals(7, colors.size)
        assertEquals(listOf(Color.BLUE, Color.BLUE), colors.take(2))
        assertEquals(List(4) { Color.WHITE }, colors.drop(2).take(4))
        assertEquals(COLOR_ORANGE, colors.last())
    }

    @Test fun `tiny nonzero gaps still get one block each`() {
        val colors = temperatureArc(-3f, -2.9f, 15f, 15.1f).ramp.colors
        assertEquals(7, colors.size)
        assertEquals(Color.BLUE, colors.first())
        assertEquals(COLOR_ORANGE, colors.last())
        assertEquals(5, colors.count { it == Color.WHITE })
    }

    @Test fun `zero cold gap omits blue`() {
        val colors = temperatureArc(2f, 2f, 15f, 18f).ramp.colors
        assertEquals(7, colors.size)
        assertTrue(colors.none { it == Color.BLUE })
        assertEquals(Color.WHITE, colors.first())
        assertEquals(COLOR_ORANGE, colors.last())
    }

    @Test fun `degenerate equal input yields valid all-white ramp`() {
        val arc = temperatureArc(5f, 5f, 5f, 5f)
        assertTrue(arc.max > arc.min)
        assertTrue(arc.ramp.colors.size >= 2)
        assertTrue(arc.ramp.colors.all { it == Color.WHITE })
    }

    // allocateBlocks

    @Test fun `allocates by largest remainder`() {
        assertEquals(listOf(2, 4, 1), allocateBlocks(floatArrayOf(5f, 13f, 3f), 7).toList())
    }

    @Test fun `total blocks always matches request`() {
        assertEquals(7, allocateBlocks(floatArrayOf(0.1f, 20f, 0.1f), 7).sum())
        assertEquals(7, allocateBlocks(floatArrayOf(1f, 1f, 1f), 7).sum())
    }
}
