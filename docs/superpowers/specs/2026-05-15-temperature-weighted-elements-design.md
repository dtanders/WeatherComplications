# Temperature Weighted Elements Complication — Design Spec

**Date:** 2026-05-15
**Status:** Approved

## Goal

Add `WEIGHTED_ELEMENTS` as a third supported type to `ApparentTemperatureComplicationService`, displaying three proportional arc segments that visualize the relationship between actual air temperature range and apparent (feels-like) temperature extremes. Gracefully degrades to `RANGED_VALUE` / `SHORT_TEXT` on API 32 and older watch faces.

## Segments

The arc spans from `apparentTemperatureMin` to `apparentTemperatureMax` and is divided into three segments:

| Segment | Range | Color |
|---|---|---|
| Cold extension | `apparentMin` → `airMin` | `Color.BLUE` |
| Air temp range | `airMin` → `airMax` | `Color.WHITE` |
| Heat extension | `airMax` → `apparentMax` | `Color.rgb(255, 140, 0)` (dark orange) |

Each segment has a minimum weight of `totalSpan * 0.05f` (sliver clamping), where `totalSpan = max(apparentMax - apparentMin, 1f)`. This ensures all three segments are always visible even when wind chill or heat index is negligible.

Text field: current apparent temperature (same string as `SHORT_TEXT`).
Title: `R.string.apparent_temperature_title` ("FEELS").
Tap action: same as other types.

## Data Layer Changes

### `WeatherResponse.kt` — `DailyWeatherResponse`

Add two fields:
```kotlin
@SerialName("temperature_2m_max") val temperature2mMax: List<Double?> = emptyList()
@SerialName("temperature_2m_min") val temperature2mMin: List<Double?> = emptyList()
```

### `WeatherData.kt` — `DailyWeather`

Add two fields:
```kotlin
val temperatureMax: Double? = null
val temperatureMin: Double? = null
```

### `WeatherRepository.kt` — `fetchAndCache()`

Add `"temperature_2m_max,temperature_2m_min"` to the `daily` query parameter string.

Map in `DailyWeather(...)`:
```kotlin
temperatureMax = weather.daily.temperature2mMax.firstOrNull(),
temperatureMin = weather.daily.temperature2mMin.firstOrNull(),
```

## New File: `util/TemperatureWeightedElements.kt`

Top-level function — no class, no Android dependencies except `Color` and the complications data type.

```kotlin
fun temperatureWeightedElements(
    apparentMin: Float,
    airMin: Float,
    airMax: Float,
    apparentMax: Float
): List<WeightedElementsComplicationData.Element>
```

**Algorithm:**
1. `totalSpan = max(apparentMax - apparentMin, 1f)`
2. `minWeight = totalSpan * 0.05f`
3. Raw weights: `coldRaw = airMin - apparentMin`, `airRaw = airMax - airMin`, `heatRaw = apparentMax - airMax`
4. Clamped weights: `max(rawWeight, minWeight)` for each
5. Return three `Element`s: cold (blue), air (white), heat (orange)

Unit-testable with plain JUnit — verify sliver clamping, inverted inputs, equal temperatures.

## `ApparentTemperatureComplicationService` Changes

### `buildComplicationData()`

- Add `ComplicationType.WEIGHTED_ELEMENTS` to the early-return type guard
- Add a `WEIGHTED_ELEMENTS` branch:
  - Return `null` if `data?.daily?.temperatureMin`, `data?.daily?.temperatureMax`, `data?.daily?.apparentTemperatureMin`, or `data?.daily?.apparentTemperatureMax` is null
  - Call `temperatureWeightedElements(apparentMin, airMin, airMax, apparentMax)`
  - Build `WeightedElementsComplicationData` with `setText`, `setTitle`, `setTapAction`, `setElementBackgroundColor(Color.TRANSPARENT)`

### `getPreviewData()`

Add `WEIGHTED_ELEMENTS` branch using hardcoded preview values:
- `apparentMin = -3f`, `airMin = 2f`, `airMax = 15f`, `apparentMax = 18f`
- Preview text: `formatter.formatApparentTemperature(12.3)`

## Manifest Change

`ApparentTemperatureComplicationService` supported types:

```
SHORT_TEXT,RANGED_VALUE,WEIGHTED_ELEMENTS
```

## Strings

Add one new string resource for the weighted elements content description:
```xml
<string name="apparent_temperature_weighted_description">Temperature range with apparent temperature extremes</string>
```

## Testing

- Unit tests for `temperatureWeightedElements`: normal case, zero cold extension, zero heat extension, both extensions zero, inverted inputs (apparent warmer than air on cold end)
- Existing `ApparentTemperatureComplicationService` tests unaffected
