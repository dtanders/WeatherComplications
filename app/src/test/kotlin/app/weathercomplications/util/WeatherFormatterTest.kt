package app.weathercomplications.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherFormatterTest {

    private val metric = WeatherFormatter(isImperial = false)
    private val imperial = WeatherFormatter(isImperial = true)

    // Humidity (no unit conversion)
    @Test fun `formatHumidity returns percent string`() =
        assertEquals("65%", metric.formatHumidity(65))

    @Test fun `formatHumidity returns dash for null`() =
        assertEquals("--", metric.formatHumidity(null))

    // Dewpoint — metric
    @Test fun `formatDewpoint metric returns celsius`() =
        assertEquals("10.5°", metric.formatDewpoint(10.5))

    @Test fun `formatDewpoint metric returns dash for null`() =
        assertEquals("--", metric.formatDewpoint(null))

    // Dewpoint — imperial
    @Test fun `formatDewpoint imperial converts to fahrenheit`() =
        assertEquals("50.9°", imperial.formatDewpoint(10.5))

    @Test fun `formatDewpoint imperial freezing point`() =
        assertEquals("32.0°", imperial.formatDewpoint(0.0))

    // Apparent temperature — metric
    @Test fun `formatApparentTemperature metric returns celsius`() =
        assertEquals("15.3°", metric.formatApparentTemperature(15.3))

    @Test fun `formatApparentTemperature metric returns dash for null`() =
        assertEquals("--", metric.formatApparentTemperature(null))

    // Apparent temperature — imperial
    @Test fun `formatApparentTemperature imperial converts to fahrenheit`() =
        assertEquals("59.5°", imperial.formatApparentTemperature(15.3))

    // Snow depth — metric
    @Test fun `formatSnowDepth metric converts meters to centimeters`() =
        assertEquals("30cm", metric.formatSnowDepth(0.30))

    @Test fun `formatSnowDepth metric returns 0cm for tiny value`() =
        assertEquals("0cm", metric.formatSnowDepth(0.005))

    @Test fun `formatSnowDepth metric returns dash for null`() =
        assertEquals("--", metric.formatSnowDepth(null))

    // Snow depth — imperial
    @Test fun `formatSnowDepth imperial converts meters to inches`() =
        assertEquals("11.8in", imperial.formatSnowDepth(0.30))

    @Test fun `formatSnowDepth imperial returns 0in for tiny value`() =
        assertEquals("0in", imperial.formatSnowDepth(0.001))

    // Visibility — metric
    @Test fun `formatVisibility metric uses km for 1000m or more`() =
        assertEquals("10km", metric.formatVisibility(10000.0))

    @Test fun `formatVisibility metric uses m for under 1000m`() =
        assertEquals("500m", metric.formatVisibility(500.0))

    @Test fun `formatVisibility metric returns dash for null`() =
        assertEquals("--", metric.formatVisibility(null))

    // Visibility — imperial
    @Test fun `formatVisibility imperial uses mi for 1 mile or more`() =
        assertEquals("6mi", imperial.formatVisibility(10000.0))

    @Test fun `formatVisibility imperial uses fractional miles under 1 mile`() =
        assertEquals("0.3mi", imperial.formatVisibility(500.0))

    // UV Index (no unit conversion)
    @Test fun `formatUvIndex returns one decimal string`() =
        assertEquals("3.5", metric.formatUvIndex(3.5))

    @Test fun `formatUvIndex returns dash for null`() =
        assertEquals("--", metric.formatUvIndex(null))

    // AQI (no unit conversion)
    @Test fun `formatAqi returns integer string`() =
        assertEquals("42", metric.formatAqi(42))

    @Test fun `formatAqi returns dash for null`() =
        assertEquals("--", metric.formatAqi(null))
}
