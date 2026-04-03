# WeatherComplications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a WearOS complications app that displays current weather data (relative humidity, dewpoint, apparent temperature, snow depth, visibility, UV index, AQI) from Open Meteo, with RANGED_VALUE complications where daily data is available, refreshing at most once per hour.

**Architecture:** A singleton `WeatherRepository` fetches and caches Open Meteo weather + air quality data per device location, refreshing only when the cache is older than one hour. Seven `SuspendingComplicationDataSourceService` subclasses each pull from the repository and return typed `ComplicationData`. Complications that have daily min/max data from the API expose both `SHORT_TEXT` and `RANGED_VALUE` types.

**Tech Stack:** Kotlin 2.0, WearOS API 30+ (Wear OS 3+), AndroidX Wear Complications Datasource KTX 1.2.1, Retrofit 2.11 + kotlinx.serialization, DataStore Preferences, FusedLocationProvider, Kotlin Coroutines, MockK + JUnit 4

---

## File Map

```
gradle/
  libs.versions.toml
  wrapper/
    gradle-wrapper.properties
settings.gradle.kts
build.gradle.kts
app/
  build.gradle.kts
  proguard-rules.pro
  src/
    main/
      AndroidManifest.xml
      kotlin/ac/house/studio/weathercomplications/
        data/
          LatLon.kt                         # Simple coordinate data class
          WeatherData.kt                    # Domain models (@Serializable)
          WeatherCache.kt                   # Interface: getCachedData / saveData
          remote/
            WeatherResponse.kt              # Open Meteo weather API DTOs
            AqiResponse.kt                  # Open Meteo AQI API DTOs
            OpenMeteoWeatherApi.kt          # Retrofit interface for weather
            OpenMeteoAqiApi.kt              # Retrofit interface for AQI
            ApiServiceFactory.kt            # Creates Retrofit instances (two base URLs)
          LocationRepository.kt             # Returns LatLon via FusedLocationProvider
          WeatherDataStore.kt               # DataStore implementation of WeatherCache
          WeatherRepository.kt              # Orchestrates fetch + 1-hour cache logic
        util/
          WeatherFormatter.kt               # Formats raw values as display strings
        complications/
          BaseWeatherComplicationService.kt # Shared repository accessor + activation hook
          HumidityComplicationService.kt    # SHORT_TEXT: relative humidity
          DewpointComplicationService.kt    # SHORT_TEXT: dewpoint
          ApparentTemperatureComplicationService.kt  # SHORT_TEXT + RANGED_VALUE (daily min/max)
          SnowDepthComplicationService.kt   # SHORT_TEXT + RANGED_VALUE (0..daily max)
          VisibilityComplicationService.kt  # SHORT_TEXT + RANGED_VALUE (daily min/max)
          UvIndexComplicationService.kt     # SHORT_TEXT + RANGED_VALUE (0..daily max, capped at 11)
          AqiComplicationService.kt         # SHORT_TEXT + RANGED_VALUE (0..300 US AQI scale)
      res/
        drawable/
          ic_weather.xml                    # Monochrome sun vector icon
        values/
          strings.xml                       # App name + complication labels
    test/
      kotlin/ac/house/studio/weathercomplications/
        WeatherFormatterTest.kt             # Pure JVM unit tests
        WeatherRepositoryTest.kt            # JVM tests with MockK + FakeWeatherCache
```

---

### Task 1: Gradle Project Setup

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create version catalog**

Create `gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
wearComplications = "1.2.1"
playServicesLocation = "21.3.0"
retrofit = "2.11.0"
retrofitKotlinxSerializer = "1.0.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"
kotlinxCoroutines = "1.9.0"
datastore = "1.1.1"
junit = "4.13.2"
mockk = "1.13.13"

[libraries]
wear-complications-data = { group = "androidx.wear.watchface", name = "watchface-complications-data", version.ref = "wearComplications" }
wear-complications-datasource = { group = "androidx.wear.watchface", name = "watchface-complications-datasource", version.ref = "wearComplications" }
wear-complications-datasource-ktx = { group = "androidx.wear.watchface", name = "watchface-complications-datasource-ktx", version.ref = "wearComplications" }
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofitKotlinxSerializer" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Create Gradle wrapper properties**

Create `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Create settings.gradle.kts**

Create `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "WeatherComplications"
include(":app")
```

- [ ] **Step 4: Create root build.gradle.kts**

Create `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 5: Create app/build.gradle.kts**

Create `app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ac.house.studio.weathercomplications"
    compileSdk = 35

    defaultConfig {
        applicationId = "ac.house.studio.weathercomplications"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.wear.complications.data)
    implementation(libs.wear.complications.datasource)
    implementation(libs.wear.complications.datasource.ktx)
    implementation(libs.play.services.location)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 6: Create app/proguard-rules.pro**

Create `app/proguard-rules.pro`:
```
-keep class ac.house.studio.weathercomplications.data.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
```

- [ ] **Step 7: Generate Gradle wrapper scripts**

Run in the project root (requires a system Gradle installation, or use Android Studio which generates the wrapper on first sync):
```bash
gradle wrapper --gradle-version 8.11.1
```
Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are created.

If `gradle` is not on PATH, open the project in Android Studio — it will generate the wrapper automatically on first Gradle sync.

- [ ] **Step 8: Verify Gradle sync**

```bash
./gradlew tasks
```
Expected: Task list printed, no build errors.

- [ ] **Step 9: Commit**

```bash
git add gradle/ settings.gradle.kts build.gradle.kts app/build.gradle.kts app/proguard-rules.pro
git commit -m "feat: add gradle project configuration for WearOS complications app"
```

---

### Task 2: Domain Data Models and Cache Interface

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/LatLon.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherData.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherCache.kt`

- [ ] **Step 1: Create LatLon coordinate class**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/LatLon.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

data class LatLon(val latitude: Double, val longitude: Double)
```

- [ ] **Step 2: Create domain weather models**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherData.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val current: CurrentWeather,
    val daily: DailyWeather,
    val fetchedAt: Long
)

@Serializable
data class CurrentWeather(
    val relativeHumidity: Int?,
    val dewpoint: Double?,
    val apparentTemperature: Double?,
    val snowDepth: Double?,
    val visibility: Double?,
    val uvIndex: Double?,
    val aqi: Int?
)

@Serializable
data class DailyWeather(
    val apparentTemperatureMax: Double?,
    val apparentTemperatureMin: Double?,
    val uvIndexMax: Double?,
    val snowDepthMax: Double?,
    val visibilityMax: Double?,
    val visibilityMin: Double?
)
```

- [ ] **Step 3: Create WeatherCache interface**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherCache.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

interface WeatherCache {
    suspend fun getCachedData(): WeatherData?
    suspend fun saveData(data: WeatherData)
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/data/
git commit -m "feat: add domain data models and WeatherCache interface"
```

---

### Task 3: API Response Models, Retrofit Interfaces, and Service Factory

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/WeatherResponse.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/AqiResponse.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/OpenMeteoWeatherApi.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/OpenMeteoAqiApi.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/ApiServiceFactory.kt`

- [ ] **Step 1: Create weather API response DTOs**

The `current` object uses snake_case field names that differ from the Kotlin model; `@SerialName` maps them. All fields are nullable with defaults so missing keys don't crash deserialization.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/WeatherResponse.kt`:
```kotlin
package ac.house.studio.weathercomplications.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherResponse,
    val daily: DailyWeatherResponse
)

@Serializable
data class CurrentWeatherResponse(
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("dew_point_2m") val dewPoint2m: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("snow_depth") val snowDepth: Double? = null,
    val visibility: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null
)

@Serializable
data class DailyWeatherResponse(
    val time: List<String> = emptyList(),
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("snow_depth_max") val snowDepthMax: List<Double?> = emptyList(),
    @SerialName("visibility_max") val visibilityMax: List<Double?> = emptyList(),
    @SerialName("visibility_min") val visibilityMin: List<Double?> = emptyList()
)
```

- [ ] **Step 2: Create AQI response DTOs**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/AqiResponse.kt`:
```kotlin
package ac.house.studio.weathercomplications.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AqiResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentAqiResponse
)

@Serializable
data class CurrentAqiResponse(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null
)
```

- [ ] **Step 3: Create weather Retrofit interface**

Note: default parameter values are NOT used here. Retrofit's Java proxy doesn't invoke Kotlin default methods. All parameters are explicit; the repository passes all values at the call site.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/OpenMeteoWeatherApi.kt`:
```kotlin
package ac.house.studio.weathercomplications.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoWeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("daily") daily: String,
        @Query("models") models: String,
        @Query("forecast_days") forecastDays: Int
    ): WeatherResponse
}
```

- [ ] **Step 4: Create AQI Retrofit interface**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/OpenMeteoAqiApi.kt`:
```kotlin
package ac.house.studio.weathercomplications.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoAqiApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String
    ): AqiResponse
}
```

- [ ] **Step 5: Create API service factory**

Two separate Retrofit instances are required because the weather and AQI endpoints are on different base URLs (`api.open-meteo.com` vs `air-quality-api.open-meteo.com`).

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/ApiServiceFactory.kt`:
```kotlin
package ac.house.studio.weathercomplications.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object ApiServiceFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    val weatherApi: OpenMeteoWeatherApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoWeatherApi::class.java)

    val aqiApi: OpenMeteoAqiApi = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoAqiApi::class.java)
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/data/remote/
git commit -m "feat: add Open Meteo API DTOs, Retrofit interfaces, and service factory"
```

---

### Task 4: Location Repository

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/LocationRepository.kt`

- [ ] **Step 1: Implement LocationRepository**

`lastLocation` is used first (fast, no battery cost). If it returns null (device just booted or location was cleared), a fresh `getCurrentLocation` request is made. `@Suppress("MissingPermission")` is acceptable here because the permission check is enforced at the manifest/OS level — the complication service will not activate without the user granting location.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/LocationRepository.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(private val context: Context) {

    @Suppress("MissingPermission")
    suspend fun getLocation(): LatLon {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        cont.resume(LatLon(loc.latitude, loc.longitude))
                    } else {
                        fusedClient.getCurrentLocation(
                            CurrentLocationRequest.Builder()
                                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                                .build(),
                            null
                        ).addOnSuccessListener { fresh: Location? ->
                            if (fresh != null) cont.resume(LatLon(fresh.latitude, fresh.longitude))
                            else cont.resumeWithException(Exception("Location unavailable"))
                        }.addOnFailureListener { cont.resumeWithException(it) }
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/data/LocationRepository.kt
git commit -m "feat: add location repository using FusedLocationProviderClient"
```

---

### Task 5: WeatherDataStore

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherDataStore.kt`

- [ ] **Step 1: Implement WeatherDataStore**

The entire `WeatherData` graph is serialized as a single JSON string in one DataStore preference key. This avoids managing 10+ individual preference keys and makes atomic save/read trivial.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherDataStore.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WeatherDataStore(context: Context) : WeatherCache {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("weather_cache") }
    )

    private val weatherJsonKey = stringPreferencesKey("weather_json")

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCachedData(): WeatherData? {
        val stored = dataStore.data.first()[weatherJsonKey] ?: return null
        return try { json.decodeFromString(stored) } catch (_: Exception) { null }
    }

    override suspend fun saveData(data: WeatherData) {
        dataStore.edit { it[weatherJsonKey] = json.encodeToString(data) }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherDataStore.kt
git commit -m "feat: add DataStore-backed weather cache"
```

---

### Task 6: WeatherFormatter — TDD

**Files:**
- Create: `app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherFormatterTest.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/util/WeatherFormatter.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherFormatterTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:test --tests "ac.house.studio.weathercomplications.WeatherFormatterTest"
```
Expected: FAILED — `WeatherFormatter` class not found.

- [ ] **Step 3: Implement WeatherFormatter**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/util/WeatherFormatter.kt`:
```kotlin
package ac.house.studio.weathercomplications.util

object WeatherFormatter {

    fun formatHumidity(value: Int?): String =
        value?.let { "$it%" } ?: "--"

    fun formatDewpoint(valueC: Double?): String =
        valueC?.let { "%.1f°".format(it) } ?: "--"

    fun formatApparentTemperature(valueC: Double?): String =
        valueC?.let { "%.1f°".format(it) } ?: "--"

    fun formatSnowDepth(valueM: Double?): String =
        valueM?.let { if (it < 0.01) "0cm" else "%.0fcm".format(it * 100) } ?: "--"

    fun formatVisibility(valueM: Double?): String =
        valueM?.let { if (it >= 1000) "%.0fkm".format(it / 1000) else "%.0fm".format(it) } ?: "--"

    fun formatUvIndex(value: Double?): String =
        value?.let { "%.1f".format(it) } ?: "--"

    fun formatAqi(value: Int?): String =
        value?.let { "$it" } ?: "--"

    fun aqiLabel(value: Int?): String = when {
        value == null -> "AQI"
        value <= 50   -> "Good"
        value <= 100  -> "Mod"
        value <= 150  -> "USG"
        value <= 200  -> "Unhl"
        value <= 300  -> "VUnhl"
        else          -> "Haz"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "ac.house.studio.weathercomplications.WeatherFormatterTest"
```
Expected: BUILD SUCCESSFUL, all 22 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherFormatterTest.kt
git add app/src/main/kotlin/ac/house/studio/weathercomplications/util/WeatherFormatter.kt
git commit -m "feat: add WeatherFormatter with TDD tests"
```

---

### Task 7: WeatherRepository — TDD

**Files:**
- Create: `app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherRepositoryTest.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherRepository.kt`

- [ ] **Step 1: Write failing tests**

Note: Because `OpenMeteoWeatherApi.getForecast` has no default parameters, MockK requires all 6 arguments to be matched. Mixing literal values and `any()` matchers requires wrapping literals in `eq()`.

Create `app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherRepositoryTest.kt`:
```kotlin
package ac.house.studio.weathercomplications

import ac.house.studio.weathercomplications.data.*
import ac.house.studio.weathercomplications.data.remote.*
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
    }

    private fun freshWeatherData() = WeatherData(
        current = CurrentWeather(65, 10.0, 15.0, 0.0, 10000.0, 3.0, 42),
        daily = DailyWeather(18.0, 8.0, 5.0, 0.0, 24140.0, 8000.0),
        fetchedAt = System.currentTimeMillis() - 30 * 60 * 1000L
    )

    private fun staleWeatherData() = WeatherData(
        current = CurrentWeather(65, 10.0, 15.0, 0.0, 10000.0, 3.0, 42),
        daily = DailyWeather(18.0, 8.0, 5.0, 0.0, 24140.0, 8000.0),
        fetchedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
    )

    private fun testWeatherResponse() = WeatherResponse(
        latitude = 47.0, longitude = -122.0,
        current = CurrentWeatherResponse(
            relativeHumidity2m = 65, dewPoint2m = 10.0, apparentTemperature = 15.0,
            snowDepth = 0.0, visibility = 10000.0, uvIndex = 3.0
        ),
        daily = DailyWeatherResponse(
            time = listOf("2026-03-31"),
            apparentTemperatureMax = listOf(18.0),
            apparentTemperatureMin = listOf(8.0),
            uvIndexMax = listOf(5.0),
            snowDepthMax = listOf(0.0),
            visibilityMax = listOf(24140.0),
            visibilityMin = listOf(8000.0)
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:test --tests "ac.house.studio.weathercomplications.WeatherRepositoryTest"
```
Expected: FAILED — `WeatherRepository` class not found.

- [ ] **Step 3: Implement WeatherRepository**

The public constructor allows direct instantiation in tests. `getInstance` is the production entry point used by complication services.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherRepository.kt`:
```kotlin
package ac.house.studio.weathercomplications.data

import android.content.Context
import ac.house.studio.weathercomplications.data.remote.ApiServiceFactory
import ac.house.studio.weathercomplications.data.remote.OpenMeteoAqiApi
import ac.house.studio.weathercomplications.data.remote.OpenMeteoWeatherApi

class WeatherRepository(
    private val weatherApi: OpenMeteoWeatherApi,
    private val aqiApi: OpenMeteoAqiApi,
    private val locationRepository: LocationRepository,
    private val cache: WeatherCache
) {
    companion object {
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L

        @Volatile private var instance: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository = instance ?: synchronized(this) {
            instance ?: WeatherRepository(
                weatherApi = ApiServiceFactory.weatherApi,
                aqiApi = ApiServiceFactory.aqiApi,
                locationRepository = LocationRepository(context.applicationContext),
                cache = WeatherDataStore(context.applicationContext)
            ).also { instance = it }
        }
    }

    suspend fun getWeatherData(): WeatherData {
        val cached = cache.getCachedData()
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_DURATION_MS) {
            return cached
        }
        return fetchAndCache()
    }

    private suspend fun fetchAndCache(): WeatherData {
        val location = locationRepository.getLocation()
        val weather = weatherApi.getForecast(
            latitude = location.latitude,
            longitude = location.longitude,
            current = "relative_humidity_2m,dew_point_2m,apparent_temperature,snow_depth,visibility,uv_index",
            daily = "apparent_temperature_max,apparent_temperature_min,uv_index_max,snow_depth_max,visibility_max,visibility_min",
            models = "best_match",
            forecastDays = 1
        )
        val aqi = runCatching {
            aqiApi.getAirQuality(
                latitude = location.latitude,
                longitude = location.longitude,
                current = "us_aqi,european_aqi"
            )
        }.getOrNull()

        val data = WeatherData(
            current = CurrentWeather(
                relativeHumidity = weather.current.relativeHumidity2m,
                dewpoint = weather.current.dewPoint2m,
                apparentTemperature = weather.current.apparentTemperature,
                snowDepth = weather.current.snowDepth,
                visibility = weather.current.visibility,
                uvIndex = weather.current.uvIndex,
                aqi = aqi?.current?.usAqi
            ),
            daily = DailyWeather(
                apparentTemperatureMax = weather.daily.apparentTemperatureMax.firstOrNull(),
                apparentTemperatureMin = weather.daily.apparentTemperatureMin.firstOrNull(),
                uvIndexMax = weather.daily.uvIndexMax.firstOrNull(),
                snowDepthMax = weather.daily.snowDepthMax.firstOrNull(),
                visibilityMax = weather.daily.visibilityMax.firstOrNull(),
                visibilityMin = weather.daily.visibilityMin.firstOrNull()
            ),
            fetchedAt = System.currentTimeMillis()
        )
        cache.saveData(data)
        return data
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:test --tests "ac.house.studio.weathercomplications.WeatherRepositoryTest"
```
Expected: BUILD SUCCESSFUL, all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/kotlin/ac/house/studio/weathercomplications/WeatherRepositoryTest.kt
git add app/src/main/kotlin/ac/house/studio/weathercomplications/data/WeatherRepository.kt
git commit -m "feat: add WeatherRepository with 1-hour cache and TDD tests"
```

---

### Task 8: Manifest, Resources, and Base Complication Service

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_weather.xml`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/BaseWeatherComplicationService.kt`

- [ ] **Step 1: Create AndroidManifest.xml**

The `com.google.android.wearable.standalone` meta-data marks this as a standalone WearOS app (no phone companion required). Individual complication `<service>` entries will be added in Tasks 9–14.

Create `app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

    <uses-feature android:name="android.hardware.type.watch"/>

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_weather"
        android:label="@string/app_name"
        android:supportsRtl="true">

        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="true"/>

        <!-- Complication services added in Tasks 9-14 -->

    </application>

</manifest>
```

- [ ] **Step 2: Create string resources**

Create `app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Weather Complications</string>
    <string name="humidity_complication_label">Relative Humidity</string>
    <string name="dewpoint_complication_label">Dewpoint</string>
    <string name="apparent_temperature_complication_label">Apparent Temperature</string>
    <string name="snow_depth_complication_label">Snow Depth</string>
    <string name="visibility_complication_label">Visibility</string>
    <string name="uv_index_complication_label">UV Index</string>
    <string name="aqi_complication_label">Air Quality Index</string>
</resources>
```

- [ ] **Step 3: Create weather icon vector drawable**

Create `app/src/main/res/drawable/ic_weather.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -2.24,-5 -5,-5zM2,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM20,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM11,20v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1z"/>
</vector>
```

- [ ] **Step 4: Implement BaseWeatherComplicationService**

`onComplicationActivated` fires when a user adds the complication to their watch face. Requesting an immediate update ensures the complication shows real data right away rather than waiting up to one hour for the scheduled poll.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/BaseWeatherComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import android.content.ComponentName
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationType
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ac.house.studio.weathercomplications.data.WeatherRepository

abstract class BaseWeatherComplicationService : SuspendingComplicationDataSourceService() {

    protected val repository: WeatherRepository
        get() = WeatherRepository.getInstance(applicationContext)

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        ComplicationDataSourceUpdateRequester
            .create(applicationContext, ComponentName(applicationContext, javaClass))
            .requestUpdate(complicationInstanceId)
    }
}
```

- [ ] **Step 5: Verify build**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/BaseWeatherComplicationService.kt
git commit -m "feat: add manifest, resources, and base complication service"
```

---

### Task 9: Humidity and Dewpoint Complications

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/HumidityComplicationService.kt`
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/DewpointComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement HumidityComplicationService**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/HumidityComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class HumidityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("65%").build(),
            contentDescription = PlainComplicationText.Builder("Relative Humidity 65%").build()
        ).setTitle(PlainComplicationText.Builder("HUMID").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatHumidity(data?.current?.relativeHumidity)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Relative Humidity $text").build()
        ).setTitle(PlainComplicationText.Builder("HUMID").build()).build()
    }
}
```

- [ ] **Step 2: Implement DewpointComplicationService**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/DewpointComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class DewpointComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("10.5°").build(),
            contentDescription = PlainComplicationText.Builder("Dewpoint 10.5°").build()
        ).setTitle(PlainComplicationText.Builder("DEW").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatDewpoint(data?.current?.dewpoint)
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Dewpoint $text").build()
        ).setTitle(PlainComplicationText.Builder("DEW").build()).build()
    }
}
```

- [ ] **Step 3: Register both services in AndroidManifest.xml**

Add inside the `<application>` tag (before the closing `</application>`):
```xml
<service
    android:name=".complications.HumidityComplicationService"
    android:label="@string/humidity_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>

<service
    android:name=".complications.DewpointComplicationService"
    android:label="@string/dewpoint_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 4: Verify build**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/HumidityComplicationService.kt
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/DewpointComplicationService.kt
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add humidity and dewpoint complication services"
```

---

### Task 10: Apparent Temperature Complication (SHORT_TEXT + RANGED_VALUE)

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/ApparentTemperatureComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement ApparentTemperatureComplicationService**

For RANGED_VALUE, the current apparent temperature is plotted between the daily low and high. `safeMax` guards against a degenerate range when min == max (e.g., data returned only one value).

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/ApparentTemperatureComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class ApparentTemperatureComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("12.3°").build(),
            contentDescription = PlainComplicationText.Builder("Apparent Temperature 12.3°").build()
        ).setTitle(PlainComplicationText.Builder("FEELS").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 12.3f, min = 5f, max = 20f,
            contentDescription = PlainComplicationText.Builder("Apparent Temperature Range").build()
        ).setText(PlainComplicationText.Builder("12.3°").build())
            .setTitle(PlainComplicationText.Builder("FEELS").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatApparentTemperature(data?.current?.apparentTemperature)
        val description = PlainComplicationText.Builder("Apparent Temperature $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("FEELS").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.apparentTemperatureMin?.toFloat() ?: 0f
                val max = data?.daily?.apparentTemperatureMax?.toFloat() ?: 1f
                val safeMax = if (max > min) max else min + 1f
                val current = (data?.current?.apparentTemperature?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("FEELS").build()).build()
            }

            else -> null
        }
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

Add inside `<application>`:
```xml
<service
    android:name=".complications.ApparentTemperatureComplicationService"
    android:label="@string/apparent_temperature_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 3: Verify build + commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/ApparentTemperatureComplicationService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add apparent temperature complication with SHORT_TEXT and RANGED_VALUE"
```

---

### Task 11: Snow Depth Complication (SHORT_TEXT + RANGED_VALUE)

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/SnowDepthComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement SnowDepthComplicationService**

For RANGED_VALUE, the range is 0 to the daily maximum snow depth. When the daily max is near zero (bare ground), `safeMax` is set to 1.0m to keep the range valid.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/SnowDepthComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class SnowDepthComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("15cm").build(),
            contentDescription = PlainComplicationText.Builder("Snow Depth 15cm").build()
        ).setTitle(PlainComplicationText.Builder("SNOW").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 0.15f, min = 0f, max = 0.50f,
            contentDescription = PlainComplicationText.Builder("Snow Depth Range").build()
        ).setText(PlainComplicationText.Builder("15cm").build())
            .setTitle(PlainComplicationText.Builder("SNOW").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatSnowDepth(data?.current?.snowDepth)
        val description = PlainComplicationText.Builder("Snow Depth $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("SNOW").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val maxM = data?.daily?.snowDepthMax?.toFloat() ?: 1f
                val safeMax = if (maxM > 0.01f) maxM else 1f
                val current = (data?.current?.snowDepth?.toFloat() ?: 0f).coerceIn(0f, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("SNOW").build()).build()
            }

            else -> null
        }
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

Add inside `<application>`:
```xml
<service
    android:name=".complications.SnowDepthComplicationService"
    android:label="@string/snow_depth_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 3: Verify build + commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/SnowDepthComplicationService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add snow depth complication with SHORT_TEXT and RANGED_VALUE"
```

---

### Task 12: Visibility Complication (SHORT_TEXT + RANGED_VALUE)

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/VisibilityComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement VisibilityComplicationService**

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/VisibilityComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class VisibilityComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("10km").build(),
            contentDescription = PlainComplicationText.Builder("Visibility 10km").build()
        ).setTitle(PlainComplicationText.Builder("VIS").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 10000f, min = 0f, max = 24140f,
            contentDescription = PlainComplicationText.Builder("Visibility Range").build()
        ).setText(PlainComplicationText.Builder("10km").build())
            .setTitle(PlainComplicationText.Builder("VIS").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatVisibility(data?.current?.visibility)
        val description = PlainComplicationText.Builder("Visibility $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("VIS").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val min = data?.daily?.visibilityMin?.toFloat() ?: 0f
                val max = data?.daily?.visibilityMax?.toFloat() ?: 1f
                val safeMax = if (max > min) max else min + 1f
                val current = (data?.current?.visibility?.toFloat() ?: min).coerceIn(min, safeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = min, max = safeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("VIS").build()).build()
            }

            else -> null
        }
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

Add inside `<application>`:
```xml
<service
    android:name=".complications.VisibilityComplicationService"
    android:label="@string/visibility_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 3: Verify build + commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/VisibilityComplicationService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add visibility complication with SHORT_TEXT and RANGED_VALUE"
```

---

### Task 13: UV Index Complication (SHORT_TEXT + RANGED_VALUE)

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/UvIndexComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement UvIndexComplicationService**

For RANGED_VALUE, the scale is capped at 11 (the top of the standard UV index scale). The `value` is the current reading plotted against the daily maximum, which itself is capped at 11.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/UvIndexComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class UvIndexComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("3.5").build(),
            contentDescription = PlainComplicationText.Builder("UV Index 3.5").build()
        ).setTitle(PlainComplicationText.Builder("UV").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 3.5f, min = 0f, max = 11f,
            contentDescription = PlainComplicationText.Builder("UV Index Range").build()
        ).setText(PlainComplicationText.Builder("3.5").build())
            .setTitle(PlainComplicationText.Builder("UV").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val text = WeatherFormatter.formatUvIndex(data?.current?.uvIndex)
        val description = PlainComplicationText.Builder("UV Index $text").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder("UV").build()).build()

            ComplicationType.RANGED_VALUE -> {
                val dailyMax = (data?.daily?.uvIndexMax?.toFloat() ?: 11f).coerceAtMost(11f)
                val rangeMax = maxOf(dailyMax, 1f)
                val current = (data?.current?.uvIndex?.toFloat() ?: 0f).coerceIn(0f, rangeMax)
                RangedValueComplicationData.Builder(
                    value = current, min = 0f, max = rangeMax,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("UV").build()).build()
            }

            else -> null
        }
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

Add inside `<application>`:
```xml
<service
    android:name=".complications.UvIndexComplicationService"
    android:label="@string/uv_index_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 3: Verify build + commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/UvIndexComplicationService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add UV index complication with SHORT_TEXT and RANGED_VALUE"
```

---

### Task 14: AQI Complication (SHORT_TEXT + RANGED_VALUE)

**Files:**
- Create: `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/AqiComplicationService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement AqiComplicationService**

For RANGED_VALUE, the current US AQI is placed on a fixed 0–300 scale (300 covers "Very Unhealthy"; values above this are rare and clamp to the end of the arc). The title shows the AQI category label so the watch face gets meaningful text even in compact layouts.

Create `app/src/main/kotlin/ac/house/studio/weathercomplications/complications/AqiComplicationService.kt`:
```kotlin
package ac.house.studio.weathercomplications.complications

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import ac.house.studio.weathercomplications.util.WeatherFormatter

class AqiComplicationService : BaseWeatherComplicationService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("42").build(),
            contentDescription = PlainComplicationText.Builder("Air Quality Index 42 Good").build()
        ).setTitle(PlainComplicationText.Builder("Good").build()).build()

        ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
            value = 42f, min = 0f, max = 300f,
            contentDescription = PlainComplicationText.Builder("Air Quality Index 42 Good").build()
        ).setText(PlainComplicationText.Builder("42").build())
            .setTitle(PlainComplicationText.Builder("Good").build()).build()

        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = runCatching { repository.getWeatherData() }.getOrNull()
        val aqi = data?.current?.aqi
        val text = WeatherFormatter.formatAqi(aqi)
        val label = WeatherFormatter.aqiLabel(aqi)
        val description = PlainComplicationText.Builder("Air Quality Index $text $label").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder(label).build()).build()

            ComplicationType.RANGED_VALUE -> {
                val value = (aqi ?: 0).toFloat().coerceIn(0f, 300f)
                RangedValueComplicationData.Builder(
                    value = value, min = 0f, max = 300f,
                    contentDescription = description
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder(label).build()).build()
            }

            else -> null
        }
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

Add inside `<application>`:
```xml
<service
    android:name=".complications.AqiComplicationService"
    android:label="@string/aqi_complication_label"
    android:icon="@drawable/ic_weather"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"/>
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT,RANGED_VALUE"/>
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600"/>
</service>
```

- [ ] **Step 3: Run all unit tests**

```bash
./gradlew :app:test
```
Expected: BUILD SUCCESSFUL — WeatherFormatterTest (22 tests), WeatherRepositoryTest (5 tests), all pass.

- [ ] **Step 4: Full debug build**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/ac/house/studio/weathercomplications/complications/AqiComplicationService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add AQI complication with SHORT_TEXT and RANGED_VALUE"
```

---

## Post-Implementation Checklist

- [ ] Sideload `app-debug.apk` to WearOS device or emulator
- [ ] Grant location permission when prompted on first complication activation
- [ ] Add each of the 7 complications to a watch face via the picker — verify preview data appears
- [ ] Confirm complication values update after one hour (or force-refresh by clearing app data)
- [ ] Confirm AQI complication shows "--" / "AQI" gracefully when device has no internet
- [ ] Confirm no crash when location permission is denied (complication shows "--" for all values)
