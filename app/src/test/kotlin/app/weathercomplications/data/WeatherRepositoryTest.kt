package app.weathercomplications.data

import app.weathercomplications.data.*
import app.weathercomplications.data.remote.*
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {

    private val mockWeatherApi = mockk<OpenMeteoWeatherApi>()
    private val mockAqiApi = mockk<OpenMeteoAqiApi>()
    private val mockLocationRepo = mockk<LocationRepository>()
    private val fakeCache = FakeWeatherCache()

    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        repository = WeatherRepository(mockWeatherApi, mockAqiApi, mockLocationRepo, fakeCache)
    }

    @Test
    fun `returns cached data when not stale`() = runTest {
        fakeCache.data = freshWeatherData()

        val result = repository.getWeatherData()

        assertEquals(fakeCache.data, result)
        coVerify(exactly = 0) { mockWeatherApi.getForecast(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `fetches fresh data when cache is null`() = runTest {
        fakeCache.data = null
        coEvery { mockLocationRepo.getLocation() } returns LatLon(47.0, -122.0)
        coEvery { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) } returns testWeatherResponse()
        coEvery { mockAqiApi.getAirQuality(eq(47.0), eq(-122.0), any()) } returns testAqiResponse()

        val result = repository.getWeatherData()

        assertNotNull(result)
        assertEquals(65, result.current.relativeHumidity)
        coVerify(exactly = 1) { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) }
        assertNotNull(fakeCache.data)
    }

    @Test
    fun `fetches fresh data when cache is stale`() = runTest {
        fakeCache.data = staleWeatherData()
        coEvery { mockLocationRepo.getLocation() } returns LatLon(47.0, -122.0)
        coEvery { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) } returns testWeatherResponse()
        coEvery { mockAqiApi.getAirQuality(eq(47.0), eq(-122.0), any()) } returns testAqiResponse()

        repository.getWeatherData()

        coVerify(exactly = 1) { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) }
    }

    @Test
    fun `handles AQI API failure gracefully`() = runTest {
        fakeCache.data = null
        coEvery { mockLocationRepo.getLocation() } returns LatLon(47.0, -122.0)
        coEvery { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) } returns testWeatherResponse()
        coEvery { mockAqiApi.getAirQuality(any(), any(), any()) } throws RuntimeException("Network error")

        val result = repository.getWeatherData()

        assertNotNull(result)
        assertNull(result.current.aqi)
    }

    @Test
    fun `maps daily weather values from API response`() = runTest {
        fakeCache.data = null
        coEvery { mockLocationRepo.getLocation() } returns LatLon(47.0, -122.0)
        coEvery { mockWeatherApi.getForecast(eq(47.0), eq(-122.0), any(), any(), any(), any()) } returns testWeatherResponse()
        coEvery { mockAqiApi.getAirQuality(any(), any(), any()) } returns testAqiResponse()

        val result = repository.getWeatherData()

        assertEquals(18.0, result.daily.apparentTemperatureMax)
        assertEquals(8.0, result.daily.apparentTemperatureMin)
        assertEquals(5.0, result.daily.uvIndexMax)
        assertEquals(24140.0, result.daily.visibilityMax)
        assertEquals(16.0, result.daily.temperatureMax)
        assertEquals(5.0, result.daily.temperatureMin)
    }

    private fun freshWeatherData() = WeatherData(
        current = CurrentWeather(65, 10.0, 15.0, 0.0, 10000.0, 3.0, 42),
        daily = DailyWeather(18.0, 8.0, 5.0, 24140.0, 8000.0),
        fetchedAt = System.currentTimeMillis() - 30 * 60 * 1000L
    )

    private fun staleWeatherData() = WeatherData(
        current = CurrentWeather(65, 10.0, 15.0, 0.0, 10000.0, 3.0, 42),
        daily = DailyWeather(18.0, 8.0, 5.0, 24140.0, 8000.0),
        fetchedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
    )

    private fun testWeatherResponse() = WeatherResponse(
        latitude = 47.0, longitude = -122.0,
        current = CurrentWeatherResponse(
            relativeHumidity2m = 65, dewPoint2m = 10.0, apparentTemperature = 15.0,
            snowDepth = 0.0, visibility = 10000.0, uvIndex = 3.0
        ),
        daily = DailyWeatherResponse(
            time = listOf("2026-04-01"),
            apparentTemperatureMax = listOf(18.0),
            apparentTemperatureMin = listOf(8.0),
            uvIndexMax = listOf(5.0),
            visibilityMax = listOf(24140.0),
            visibilityMin = listOf(8000.0),
            temperature2mMax = listOf(16.0),
            temperature2mMin = listOf(5.0)
        )
    )

    private fun testAqiResponse() = AqiResponse(
        latitude = 47.0, longitude = -122.0,
        current = CurrentAqiResponse(usAqi = 42, europeanAqi = 38)
    )
}

class FakeWeatherCache : WeatherCache {
    var data: WeatherData? = null
    override suspend fun getCachedData(): WeatherData? = data
    override suspend fun saveData(weatherData: WeatherData) { data = weatherData }
}
