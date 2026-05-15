# Temperature Weighted Elements Complication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `WEIGHTED_ELEMENTS` as a third supported type to `ApparentTemperatureComplicationService`, rendering a three-segment arc that shows cold-extension (wind chill), actual air temperature range, and heat-extension (heat index).

**Architecture:** New daily air temperature fields are added to the data model and API call. A pure-Kotlin helper `computeTemperatureWeights` computes the three segment weights (testable with plain JUnit). A thin public wrapper `temperatureWeightedElements` maps those weights to `WeightedElementsComplicationData.Element`s with semantic colors. The existing service handles the new type alongside `SHORT_TEXT` and `RANGED_VALUE`.

**Tech Stack:** Kotlin, AndroidX `wear-complications-data`, Open-Meteo REST API, JUnit 4, MockK

---

### Task 1: Extend data model with actual air temperature fields

**Files:**
- Modify: `app/src/main/kotlin/app/weathercomplications/data/remote/WeatherResponse.kt`
- Modify: `app/src/main/kotlin/app/weathercomplications/data/WeatherData.kt`

- [ ] **Step 1: Add fields to `DailyWeatherResponse`**

In `WeatherResponse.kt`, add two fields to `DailyWeatherResponse` (after `visibilityMin`):

```kotlin
@SerialName("temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
@SerialName("temperature_2m_min") val temperature2mMin: List<Double?> = emptyList()
```

Full updated class:
```kotlin
@Serializable
data class DailyWeatherResponse(
    val time: List<String> = emptyList(),
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("visibility_max") val visibilityMax: List<Double?> = emptyList(),
    @SerialName("visibility_min") val visibilityMin: List<Double?> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double?> = emptyList()
)
```

- [ ] **Step 2: Add fields to `DailyWeather`**

In `WeatherData.kt`, add two optional fields to `DailyWeather` (must default to null to keep existing call sites compiling):

```kotlin
@Serializable
data class DailyWeather(
    val apparentTemperatureMax: Double?,
    val apparentTemperatureMin: Double?,
    val uvIndexMax: Double?,
    val visibilityMax: Double?,
    val visibilityMin: Double?,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null
)
```

- [ ] **Step 3: Verify existing tests still compile and pass**

Run:
```
./gradlew :app:testDebugUnitTest
```
Expected: all tests pass (new fields default to null, no existing call sites break).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/app/weathercomplications/data/remote/WeatherResponse.kt \
        app/src/main/kotlin/app/weathercomplications/data/WeatherData.kt
git commit -m "feat: add air temperature daily fields to data model"
```

---

### Task 2: Update repository to fetch and map air temperature

**Files:**
- Modify: `app/src/main/kotlin/app/weathercomplications/data/WeatherRepository.kt`
- Modify: `app/src/test/kotlin/app/weathercomplications/data/WeatherRepositoryTest.kt`

- [ ] **Step 1: Update daily query string in `WeatherRepository.kt`**

In `fetchAndCache()`, change the `daily` parameter from:
```kotlin
daily = "apparent_temperature_max,apparent_temperature_min,uv_index_max,visibility_max,visibility_min",
```
to:
```kotlin
daily = "apparent_temperature_max,apparent_temperature_min,uv_index_max,visibility_max,visibility_min,temperature_2m_max,temperature_2m_min",
```

- [ ] **Step 2: Map new fields in `fetchAndCache()`**

In the `DailyWeather(...)` constructor call inside `fetchAndCache()`, add:
```kotlin
temperatureMax = weather.daily.temperature2mMax.firstOrNull(),
temperatureMin = weather.daily.temperature2mMin.firstOrNull(),
```

Full updated `DailyWeather(...)` block:
```kotlin
daily = DailyWeather(
    apparentTemperatureMax = weather.daily.apparentTemperatureMax.firstOrNull(),
    apparentTemperatureMin = weather.daily.apparentTemperatureMin.firstOrNull(),
    uvIndexMax = weather.daily.uvIndexMax.firstOrNull(),
    visibilityMax = weather.daily.visibilityMax.firstOrNull(),
    visibilityMin = weather.daily.visibilityMin.firstOrNull(),
    temperatureMax = weather.daily.temperature2mMax.firstOrNull(),
    temperatureMin = weather.daily.temperature2mMin.firstOrNull()
),
```

- [ ] **Step 3: Update `testWeatherResponse()` fixture in `WeatherRepositoryTest.kt`**

Add the new fields to the `DailyWeatherResponse` in `testWeatherResponse()`:
```kotlin
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
```

- [ ] **Step 4: Add assertions to the `maps daily weather values` test**

In `WeatherRepositoryTest.kt`, extend the `maps daily weather values from API response` test to verify the new fields:
```kotlin
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
```

- [ ] **Step 5: Run tests**

```
./gradlew :app:testDebugUnitTest
```
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/app/weathercomplications/data/WeatherRepository.kt \
        app/src/test/kotlin/app/weathercomplications/data/WeatherRepositoryTest.kt
git commit -m "feat: fetch and map temperature_2m daily min/max from Open-Meteo"
```

---

### Task 3: TDD the weight computation helper

**Files:**
- Create: `app/src/test/kotlin/app/weathercomplications/util/TemperatureWeightedElementsTest.kt`
- Create: `app/src/main/kotlin/app/weathercomplications/util/TemperatureWeightedElements.kt`

- [ ] **Step 1: Create the stub implementation**

Create `app/src/main/kotlin/app/weathercomplications/util/TemperatureWeightedElements.kt`:

```kotlin
package app.weathercomplications.util

import android.graphics.Color
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData

internal fun computeTemperatureWeights(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float
): FloatArray = throw NotImplementedError()

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
```

- [ ] **Step 2: Write failing tests**

Create `app/src/test/kotlin/app/weathercomplications/util/TemperatureWeightedElementsTest.kt`:

```kotlin
package app.weathercomplications.util

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
```

- [ ] **Step 3: Run tests to verify they fail**

```
./gradlew :app:testDebugUnitTest --tests "app.weathercomplications.util.TemperatureWeightedElementsTest"
```
Expected: tests FAIL with `NotImplementedError`.

- [ ] **Step 4: Implement `computeTemperatureWeights`**

Replace the stub body in `TemperatureWeightedElements.kt`:

```kotlin
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
```

- [ ] **Step 5: Run tests to verify they pass**

```
./gradlew :app:testDebugUnitTest --tests "app.weathercomplications.util.TemperatureWeightedElementsTest"
```
Expected: 8 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/app/weathercomplications/util/TemperatureWeightedElements.kt \
        app/src/test/kotlin/app/weathercomplications/util/TemperatureWeightedElementsTest.kt
git commit -m "feat: add temperatureWeightedElements helper with weight computation"
```

---

### Task 4: Add string resource

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add content description string**

In `strings.xml`, add after the existing `apparent_temperature_range_description` line:
```xml
<string name="apparent_temperature_weighted_description">Temperature range with apparent temperature extremes</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add string resource for weighted temperature complication"
```

---

### Task 5: Update manifest to declare WEIGHTED_ELEMENTS support

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add WEIGHTED_ELEMENTS to supported types**

Find the `ApparentTemperatureComplicationService` entry (around line 84). Change:
```xml
android:value="SHORT_TEXT,RANGED_VALUE"/>
```
to:
```xml
android:value="SHORT_TEXT,RANGED_VALUE,WEIGHTED_ELEMENTS"/>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: declare WEIGHTED_ELEMENTS support in ApparentTemperatureComplicationService manifest"
```

---

### Task 6: Add WEIGHTED_ELEMENTS handling to the service

**Files:**
- Modify: `app/src/main/kotlin/app/weathercomplications/complications/ApparentTemperatureComplicationService.kt`

- [ ] **Step 1: Update the type guard in `buildComplicationData()`**

Change the early-return check from:
```kotlin
if (request.complicationType != ComplicationType.SHORT_TEXT &&
    request.complicationType != ComplicationType.RANGED_VALUE) return null
```
to:
```kotlin
if (request.complicationType != ComplicationType.SHORT_TEXT &&
    request.complicationType != ComplicationType.RANGED_VALUE &&
    request.complicationType != ComplicationType.WEIGHTED_ELEMENTS) return null
```

- [ ] **Step 2: Add `WEIGHTED_ELEMENTS` branch to `getPreviewData()`**

Add a `WEIGHTED_ELEMENTS` branch to the `when` block in `getPreviewData()`, after the `RANGED_VALUE` branch:

```kotlin
ComplicationType.WEIGHTED_ELEMENTS -> {
    val elements = temperatureWeightedElements(
        apparentMin = -3f, airMin = 2f, airMax = 15f, apparentMax = 18f
    )
    WeightedElementsComplicationData.Builder(
        elements = elements,
        contentDescription = PlainComplicationText.Builder(
            getString(R.string.apparent_temperature_weighted_description)
        ).build()
    ).setText(PlainComplicationText.Builder(text).build())
        .setTitle(PlainComplicationText.Builder(title).build())
        .build()
}
```

Note: `text`, `title`, and `image` are already defined earlier in `getPreviewData()`.

- [ ] **Step 3: Add `WEIGHTED_ELEMENTS` branch to `buildComplicationData()`**

Add a `WEIGHTED_ELEMENTS` branch to the `when` block in `buildComplicationData()`, after the `RANGED_VALUE` branch:

```kotlin
ComplicationType.WEIGHTED_ELEMENTS -> {
    val apparentMin = data?.daily?.apparentTemperatureMin?.toFloat() ?: return null
    val apparentMax = data?.daily?.apparentTemperatureMax?.toFloat() ?: return null
    val airMin = data?.daily?.temperatureMin?.toFloat() ?: return null
    val airMax = data?.daily?.temperatureMax?.toFloat() ?: return null
    val elements = temperatureWeightedElements(apparentMin, airMin, airMax, apparentMax)
    WeightedElementsComplicationData.Builder(
        elements = elements,
        contentDescription = PlainComplicationText.Builder(
            getString(R.string.apparent_temperature_weighted_description)
        ).build()
    ).setText(PlainComplicationText.Builder(text).build())
        .setTitle(PlainComplicationText.Builder(title).build())
        .setTapAction(tapAction)
        .build()
}
```

Note: `text`, `title`, `tapAction`, and `data` are already defined earlier in `buildComplicationData()`.

- [ ] **Step 4: Add the required import**

Add to the imports at the top of `ApparentTemperatureComplicationService.kt`:
```kotlin
import app.weathercomplications.util.temperatureWeightedElements
```

- [ ] **Step 5: If lint reports `@RequiresApi` warning on `WeightedElementsComplicationData`, suppress it**

If the IDE or build shows a lint warning about `WEIGHTED_ELEMENTS` requiring API 33, the `WEIGHTED_ELEMENTS` branches are safe: the platform only sends this request type on API 33+ devices. Suppress the warning by annotating the affected `when` branches or adding `@SuppressLint("NewApi")` to the function:

```kotlin
import android.annotation.SuppressLint
// ...
@SuppressLint("NewApi")
override fun getPreviewData(type: ComplicationType): ComplicationData? { ... }
```
Apply the same to `buildComplicationData()` if needed.

- [ ] **Step 6: Run all tests**

```
./gradlew :app:testDebugUnitTest
```
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/app/weathercomplications/complications/ApparentTemperatureComplicationService.kt
git commit -m "feat: add WEIGHTED_ELEMENTS type to ApparentTemperatureComplicationService"
```

---

### Task 7: Final build verification

- [ ] **Step 1: Clean build**

```
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 2: Run full test suite**

```
./gradlew :app:testDebugUnitTest
```
Expected: all tests pass.

- [ ] **Step 3: Push**

```bash
git push
```
