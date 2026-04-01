package ac.house.studio.weathercomplications

import ac.house.studio.weathercomplications.util.WeatherFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherFormatterTest {

    @Test fun `formatHumidity returns percent string`() =
        assertEquals("65%", WeatherFormatter.formatHumidity(65))

    @Test fun `formatHumidity returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatHumidity(null))

    @Test fun `formatDewpoint returns one decimal degree string`() =
        assertEquals("10.5°", WeatherFormatter.formatDewpoint(10.5))

    @Test fun `formatDewpoint returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatDewpoint(null))

    @Test fun `formatApparentTemperature returns one decimal degree`() =
        assertEquals("15.3°", WeatherFormatter.formatApparentTemperature(15.3))

    @Test fun `formatApparentTemperature returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatApparentTemperature(null))

    @Test fun `formatSnowDepth converts meters to centimeters`() =
        assertEquals("30cm", WeatherFormatter.formatSnowDepth(0.30))

    @Test fun `formatSnowDepth returns 0cm for tiny value`() =
        assertEquals("0cm", WeatherFormatter.formatSnowDepth(0.005))

    @Test fun `formatSnowDepth returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatSnowDepth(null))

    @Test fun `formatVisibility uses km for 1000m or more`() =
        assertEquals("10km", WeatherFormatter.formatVisibility(10000.0))

    @Test fun `formatVisibility uses m for under 1000m`() =
        assertEquals("500m", WeatherFormatter.formatVisibility(500.0))

    @Test fun `formatVisibility returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatVisibility(null))

    @Test fun `formatUvIndex returns one decimal string`() =
        assertEquals("3.5", WeatherFormatter.formatUvIndex(3.5))

    @Test fun `formatUvIndex returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatUvIndex(null))

    @Test fun `formatAqi returns integer string`() =
        assertEquals("42", WeatherFormatter.formatAqi(42))

    @Test fun `formatAqi returns dash for null`() =
        assertEquals("--", WeatherFormatter.formatAqi(null))

    @Test fun `aqiLabel Good for 0-50`() = assertEquals("Good", WeatherFormatter.aqiLabel(25))
    @Test fun `aqiLabel Mod for 51-100`() = assertEquals("Mod", WeatherFormatter.aqiLabel(75))
    @Test fun `aqiLabel USG for 101-150`() = assertEquals("USG", WeatherFormatter.aqiLabel(125))
    @Test fun `aqiLabel Unhl for 151-200`() = assertEquals("Unhl", WeatherFormatter.aqiLabel(175))
    @Test fun `aqiLabel VUnhl for 201-300`() = assertEquals("VUnhl", WeatherFormatter.aqiLabel(250))
    @Test fun `aqiLabel Haz for over 300`() = assertEquals("Haz", WeatherFormatter.aqiLabel(400))
    @Test fun `aqiLabel AQI for null`() = assertEquals("AQI", WeatherFormatter.aqiLabel(null))
}
