# Fix 3-segment apparent-temperature arc complication

## Context

The goal is an apparent-temperature arc complication whose track shows three colored segments:

1. **First (blue):** the gap between the air low and the apparent low — `|airMin − apparentMin|`
2. **Middle (white):** the rest — from `max(airMin, apparentMin)` to `min(airMax, apparentMax)`
3. **Last (orange):** the gap between the air high and the apparent high — `|apparentMax − airMax|`

Decisions: segments are **absolute differences** (shown whichever side is wider), the final vehicle is the **GOAL_PROGRESS / RANGED_VALUE arc** (not WEIGHTED_ELEMENTS), and the current symptom is that the arc **renders but the segments are wrong**.

### Root causes found (in `app/src/main/kotlin/app/weathercomplications/util/TemperatureWeightedElements.kt`)

1. **Block vs. stop mismatch.** A non-interpolated `ColorRamp` renders each color as an *equal-sized block* of the track (`colors.size` blocks). `goalProgressColorRamp` instead samples colors at point fractions `i/(n−1)`, so every segment boundary is off by up to half a block and the last block is biased.
2. **Wrong size constant.** `goalProgressColorRamp` sizes the ramp with `WeightedElementsComplicationData.getMaxElements()` — that's the WEIGHTED_ELEMENTS cap, unrelated to `ColorRamp`, whose own hard limit is **7 colors**.
3. **`MAX_COLOR_RAMP_STOPS = 8`** used by `temperatureColorRamp` (RANGED_VALUE path) exceeds the 7-color limit.
4. **Range mismatch.** Both services set the arc range to `apparentMin..apparentMax`, but with absolute-difference semantics the apparent low may be *above* the air low (etc.), so the track must span the widened range `min(apparentMin, airMin) .. max(apparentMax, airMax)` or the colors won't line up with the value.
5. **Coarse quantization** can erase a real-but-small segment entirely; each non-zero segment must be guaranteed at least one block.

## Changes

### 1. `util/TemperatureWeightedElements.kt` — replace ramp builders with one 3-zone builder

Add a small result type and one function (replacing `goalProgressColorRamp`, `temperatureColorRamp`, and `computeTemperatureColors`):

```kotlin
data class TemperatureArc(val min: Float, val max: Float, val ramp: ColorRamp)

fun temperatureArc(apparentMin: Float, airMin: Float, airMax: Float, apparentMax: Float): TemperatureArc
```

Logic:
- `lo = min(apparentMin, airMin)`, `loEnd = max(apparentMin, airMin)`,
  `hiStart = min(apparentMax, airMax)`, `hi = max(apparentMax, airMax)` (guard `hi > lo`, and clamp `loEnd/hiStart` so spans are non-negative and ordered).
- Spans: `blue = loEnd − lo`, `white = hiStart − loEnd`, `orange = hi − hiStart`.
- Allocate **7 blocks** proportionally to the three spans using largest-remainder rounding, with a **minimum of 1 block for any span > 0** (drop zero spans entirely; if only one span is non-zero, still emit ≥2 colors — `ColorRamp` needs at least 2 — by repeating the color).
- Colors: `Color.BLUE`, `Color.WHITE`, `COLOR_ORANGE`. No gray/current segment — the arc's own pointer shows the current value.
- Return `ColorRamp(colors, interpolated = false)` plus the widened `lo..hi` range.

Delete `MAX_COLOR_RAMP_STOPS`, `computeTemperatureColors`, `temperatureColorRamp`, `goalProgressColorRamp`. Keep `buildTemperatureElements`/`temperatureWeightedElements` untouched (the WEIGHTED_ELEMENTS side-by-side experiment stays as-is for now).

### 2. `complications/ApparentTemperatureGoalProgressComplicationService.kt`

In the GOAL_PROGRESS branch (both live and preview):
- `val arc = temperatureArc(apparentMin, airMin, airMax, apparentMax)`
- `value = (currentApparent − arc.min).coerceIn(0f, arc.max − arc.min)`, `targetValue = arc.max − arc.min`
- `setColorRamp(arc.ramp)`; title keeps showing the apparent range via `formatter.formatTemperatureRange(...)` as today.
- Preview: use the same sample numbers (−3, 2, 15, 18, current 12.3) through `temperatureArc`.

### 3. `complications/ApparentTemperatureComplicationService.kt` (RANGED_VALUE)

Same substitution: `min = arc.min`, `max = arc.max` (keep the `max > min` guard), `value = current.coerceIn(...)`, `setColorRamp(arc.ramp)` — replacing the `temperatureColorRamp(min, airMin, airMax, safeMax)` calls at both call sites (preview and live).

### 4. `app/src/test/kotlin/app/weathercomplications/util/TemperatureWeightedElementsTest.kt`

Replace the `computeTemperatureColors` tests with `temperatureArc` tests:
- widened range when apparent extends beyond air range, and when air extends beyond apparent range (absolute-difference case)
- exactly 7 colors for a typical 3-zone input; first block blue, last block orange
- a tiny non-zero cold/heat gap still gets ≥1 blue/orange block
- zero gap on one side → that color absent
- degenerate input (all equal) → valid ramp (≥2 colors), no crash
- block counts proportional to spans (largest-remainder expectation for a known input)

Keep the existing `buildTemperatureElements` tests unchanged.

## Files

- `app/src/main/kotlin/app/weathercomplications/util/TemperatureWeightedElements.kt` — rewrite ramp logic
- `app/src/main/kotlin/app/weathercomplications/complications/ApparentTemperatureGoalProgressComplicationService.kt`
- `app/src/main/kotlin/app/weathercomplications/complications/ApparentTemperatureComplicationService.kt`
- `app/src/test/kotlin/app/weathercomplications/util/TemperatureWeightedElementsTest.kt`

No manifest changes needed (services already registered correctly).

## Verification

1. `./gradlew :app:testDebugUnitTest` — new `temperatureArc` tests pass.
2. `./gradlew :app:assembleDebug` and install on the paired watch/emulator (`adb install`).
3. On a watch face slot, add the GOAL_PROGRESS complication: confirm the track shows blue → white → orange blocks whose proportions match today's `|airLow − apparentLow| : middle : |apparentHigh − airHigh|`, and the pointer sits at the current apparent temp within the widened range. Check the picker preview too (fixed sample: blue ≈ 5/21, white ≈ 13/21, orange ≈ 3/21 of the arc → expect 2/4/1 of 7 blocks).
4. Repeat for the RANGED_VALUE version on the original apparent-temp complication.

## Known limitation (by design)

`ColorRamp` caps at 7 equal blocks, so boundaries quantize to ~14% of the arc. Exact proportions are only possible with WEIGHTED_ELEMENTS; the min-1-block rule guarantees small gaps stay visible rather than accurate.
