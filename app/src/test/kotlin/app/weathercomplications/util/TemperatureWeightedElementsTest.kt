package app.weathercomplications.util

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureWeightedElementsTest {

    @Test
    fun `normal case - cold extension weight`() {
        // apparent -3, air low 2, air high 15, apparent 18
        // cold: 2 - (-3) = 5
        val weights = computeTemperatureWeights(-3f, 2f, 15f, 18f)
        assertEquals(5f, weights[0], 0.001f)
    }

    @Test
    fun `normal case - air range weight`() {
        // air: 15 - 2 = 13
        val weights = computeTemperatureWeights(-3f, 2f, 15f, 18f)
        assertEquals(13f, weights[1], 0.001f)
    }

    @Test
    fun `normal case - heat extension weight`() {
        // heat: 18 - 15 = 3
        val weights = computeTemperatureWeights(-3f, 2f, 15f, 18f)
        assertEquals(3f, weights[2], 0.001f)
    }

    @Test
    fun `returns exactly three weights`() {
        val weights = computeTemperatureWeights(-3f, 2f, 15f, 18f)
        assertEquals(3, weights.size)
    }

    @Test
    fun `zero cold extension gets minimum sliver`() {
        // apparentMin == airMin, totalSpan = 18 - 2 = 16, minWeight = 0.8
        val weights = computeTemperatureWeights(2f, 2f, 15f, 18f)
        val totalSpan = 18f - 2f
        val minWeight = totalSpan * 0.05f
        assertTrue("cold weight ${weights[0]} should be >= minWeight $minWeight",
            weights[0] >= minWeight)
    }

    @Test
    fun `zero heat extension gets minimum sliver`() {
        // apparentMax == airMax, totalSpan = 18 - (-3) = 21, minWeight = 1.05
        val weights = computeTemperatureWeights(-3f, 2f, 18f, 18f)
        val totalSpan = 18f - (-3f)
        val minWeight = totalSpan * 0.05f
        assertTrue("heat weight ${weights[2]} should be >= minWeight $minWeight",
            weights[2] >= minWeight)
    }

    @Test
    fun `both extensions zero get slivers`() {
        // apparent range equals air range: 2..15
        val weights = computeTemperatureWeights(2f, 2f, 15f, 15f)
        val totalSpan = maxOf(15f - 2f, 1f)
        val minWeight = totalSpan * 0.05f
        assertTrue(weights[0] >= minWeight)
        assertTrue(weights[2] >= minWeight)
    }

    @Test
    fun `inverted cold input - apparent warmer than air low - gets sliver`() {
        // unusual: apparent min 5 > air min 2 (apparent is warmer, no wind chill)
        val weights = computeTemperatureWeights(5f, 2f, 15f, 18f)
        val totalSpan = maxOf(18f - 5f, 1f)
        val minWeight = totalSpan * 0.05f
        assertTrue("cold weight ${weights[0]} should be >= minWeight $minWeight",
            weights[0] >= minWeight)
    }

    // computeTemperatureColors

    @Test
    fun `color stops count equals degree span plus one`() {
        // span = 18 - (-3) = 21 degrees → 22 stops
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(22, colors.size)
    }

    @Test
    fun `cold zone stops are blue`() {
        // stops 0..4 map to temps -3..-1 (below airMin=2)
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(Color.BLUE, colors[0])
        assertEquals(Color.BLUE, colors[4])
    }

    @Test
    fun `air zone stops are white`() {
        // stops 5..18 map to temps 2..15 (airMin..airMax)
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(Color.WHITE, colors[5])
        assertEquals(Color.WHITE, colors[18])
    }

    @Test
    fun `heat zone stops are orange`() {
        // stops 19..21 map to temps 16..18 (above airMax=15)
        val colors = computeTemperatureColors(-3f, 2f, 15f, 18f)
        assertEquals(COLOR_ORANGE, colors[19])
        assertEquals(COLOR_ORANGE, colors[21])
    }

    @Test
    fun `all white when apparent range equals air range`() {
        val colors = computeTemperatureColors(0f, 0f, 10f, 10f)
        assertTrue(colors.all { it == Color.WHITE })
    }

    @Test
    fun `minimum two stops for degenerate range`() {
        val colors = computeTemperatureColors(5f, 5f, 5f, 5f)
        assertEquals(2, colors.size)
    }

    @Test
    fun `all equal temperatures uses minimum 1f span`() {
        // degenerate: all same temperature
        val weights = computeTemperatureWeights(10f, 10f, 10f, 10f)
        // totalSpan = max(0, 1f) = 1f, minWeight = 0.05f
        val minWeight = 1f * 0.05f
        assertTrue(weights[0] >= minWeight)
        assertTrue(weights[1] >= minWeight)
        assertTrue(weights[2] >= minWeight)
    }
}
