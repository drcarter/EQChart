# EQChart

EQChart is an Android custom chart library.
It currently provides `Heatmap`, `Bubble`, `PCM Waveform`, `Radar`, `Pie`, and `Donut` charts.

## Project Structure

- `:EQChart-common`
  - Shared chart models/options/enums used by both View and Compose
- `:EQChart`
  - Android View-based chart components
- `:EQChart-compose`
  - Native Compose chart components
- `:app`
  - Sample app for both View and Compose demos

## Supported Charts

- Heatmap: Section-based treemap-style stock heatmap
- Bubble: Scatter / Packed bubble chart
- PCM Waveform: Real-time 16-bit mono PCM waveform rendering
- Radar: Multi-series radar chart (legend/animation/point click)
- Pie: Ratio-based pie chart (legend/labels/click)
- Donut: Donut chart with center text/labels/click

## Development Environment

- Min SDK: 24
- Compile / Target SDK: 36
- Kotlin: 2.2.0
- AGP: 8.11.1
- Java / JVM Target: 11

## Installation

`app/build.gradle.kts`:

```kotlin
dependencies {
    // Shared models (optional if already transitively included)
    implementation(project(":EQChart-common"))

    // View charts
    implementation(project(":EQChart"))

    // Compose charts
    implementation(project(":EQChart-compose"))
}
```

## Run Sample App

```bash
./gradlew :app:installDebug
```

After launch, open each demo screen from `MainActivity`.

## Compose Quick Start

```kotlin
val slices = listOf(
    PieSlice("Direct", 43.0, Color.parseColor("#2B80FF")),
    PieSlice("Search", 26.0, Color.parseColor("#FF9F1C")),
)

PieChart(
    slices = slices,
    modifier = Modifier
        .fillMaxWidth()
        .height(280.dp),
    presentationOptions = PieDonutPresentationOptions(
        showLabels = true,
        labelPosition = PieLabelPosition.AUTO,
        enableSelectionExpand = true,
    ),
    onSliceClick = { _, slice, _ ->
        // slice click callback
    },
)
```

Compose module exports:
- `StockHeatmapChart(...)`
- `BubbleChart(...)`
- `PcmWaveformChart(...)` + `rememberPcmWaveformController(...)`
- `RadarChart(...)`
- `PieChart(...)`, `DonutChart(...)`

## Usage by Chart

### 1) Heatmap

Key classes:
- `StockHeatmapView`
- `StockHeatmapSection`
- `StockHeatmapItem`

Basic example:

```kotlin
val heatmapView = StockHeatmapView(this).apply {
    setSections(
        listOf(
            StockHeatmapSection(
                name = "Technology",
                color = Color.parseColor("#1E88E5"),
                stocks = listOf(
                    StockHeatmapItem("AAPL", "Apple", "Technology", 200.0, 1.2, 3_000_000_000_000.0, 24.0),
                    StockHeatmapItem("MSFT", "Microsoft", "Technology", 380.0, -0.8, 2_800_000_000_000.0, 22.0),
                ),
            ),
        ),
    )

    setOnItemClickListener { item ->
        // use item.symbol, item.changePct, item.marketCap
    }
}

setContentView(ScrollView(this).apply { addView(heatmapView) })
```

Notes:
- `setData(List<StockHeatmapItem>)` is also supported (backward compatible)
- If `sizeRatio` exists, it is used first for area weighting

### 2) Bubble

Key classes:
- `BubbleChartView`
- `BubbleDatum`
- `BubbleLayoutMode`
- `BubblePresentationOptions`, `BubbleAxisOptions`

Basic example:

```kotlin
val bubbleView = BubbleChartView(this).apply {
    setLayoutMode(BubbleLayoutMode.PACKED) // or SCATTER
    setChartBackgroundColor(Color.parseColor("#E6E6E6"))

    setPresentationOptions(
        BubblePresentationOptions(
            title = "Loan Distribution",
            showLegend = true,
            legendMode = BubbleLegendMode.AUTO_WITH_OVERRIDE,
        ),
    )

    setAxisOptions(
        BubbleAxisOptions(
            showAxes = false,
            showGrid = false,
            showTicks = false,
        ),
    )

    setData(
        listOf(
            BubbleDatum(0.0, 0.0, 120.0, Color.parseColor("#4A7FB1"), "Food"),
            BubbleDatum(0.0, 0.0, 90.0, Color.parseColor("#FF9100"), "Retail"),
        ),
    )

    setOnBubbleClickListener { datum ->
        // use datum.label, datum.payload
    }
}

setContentView(bubbleView)
```

### 3) PCM Waveform

Key classes:
- `PcmWaveFormView`
- `PcmWaveFormStyleOptions`

Basic example:

```kotlin
val waveformView = PcmWaveFormView(this).apply {
    setSampleRateHz(44_100)
    setWindowDurationMs(2_500)
    setStyleOptions(
        PcmWaveFormStyleOptions(
            backgroundColor = Color.parseColor("#0E1620"),
            waveColor = Color.parseColor("#62D5FF"),
            centerLineColor = Color.parseColor("#2D3A46"),
            strokeWidthDp = 1.4f,
        ),
    )
}

// append real-time PCM chunk (16-bit mono)
waveformView.appendPcm16Mono(shortArrayOf(120, -300, 520, -120))
```

Notes:
- Input data type is `ShortArray` (16-bit mono PCM)
- Internally keeps only a recent N-ms window

### 4) Radar

Key classes:
- `RadarChartView`
- `RadarAxis`
- `RadarSeries`
- `RadarChartStyleOptions`, `RadarChartPresentationOptions`

Basic example:

```kotlin
val radarView = RadarChartView(this).apply {
    setAxes(
        listOf(
            RadarAxis("sweet"),
            RadarAxis("price"),
            RadarAxis("color"),
            RadarAxis("fresh"),
            RadarAxis("good"),
        ),
    )

    setSeries(
        listOf(
            RadarSeries("Apple", Color.parseColor("#B899FF"), listOf(48.0, 80.0, 84.0, 34.0, 40.0)),
            RadarSeries("Banana", Color.parseColor("#6F8695"), listOf(30.0, 40.0, 90.0, 82.0, 62.0)),
        ),
    )

    setValueMax(100.0)
    setPresentationOptions(
        RadarChartPresentationOptions(
            showLegend = true,
            animateOnDataChange = true,
            enterAnimationDurationMs = 760L,
        ),
    )

    setOnPointClickListener { seriesIndex, axisIndex, value, payload ->
        // handle selected point info
    }
}

setContentView(radarView)
```

Note:
- Each `RadarSeries.values` size must match axis count to render.

### 5) Pie / Donut

Key classes:
- `PieChartView`, `DonutChartView`
- `PieSlice`
- `PieDonutStyleOptions`, `PieDonutPresentationOptions`

Basic example:

```kotlin
val slices = listOf(
    PieSlice("Direct", 43.0, Color.parseColor("#2B80FF")),
    PieSlice("Social", 18.0, Color.parseColor("#13C3A3")),
    PieSlice("Search", 26.0, Color.parseColor("#FF9F1C")),
    PieSlice("Referral", 13.0, Color.parseColor("#EF476F")),
)

val pieChart = PieChartView(this).apply {
    setPresentationOptions(
        PieDonutPresentationOptions(
            showLegend = true,
            showLabels = true,
            labelPosition = PieLabelPosition.AUTO,
            enableSelectionExpand = true,
            selectedSliceExpandDp = 10f,
        ),
    )
    setData(slices)
}

val donutChart = DonutChartView(this).apply {
    setDonutInnerRadiusRatio(0.58f)
    setPresentationOptions(
        PieDonutPresentationOptions(
            showLegend = true,
            enableSelectionExpand = true,
            centerText = "Total",
            centerSubText = "100",
        ),
    )
    setData(slices)
    setOnSliceClickListener { index, slice, payload ->
        // use slice.label, slice.value, payload
    }
}
```

Notes:
- `PieSlice.value` must be finite and `> 0` to render
- If valid total is 0, `emptyText` is shown
- Selection explode effect is controlled by `enableSelectionExpand`, `selectedSliceExpandDp`, and `selectedSliceExpandAnimMs`

## Test

```bash
./gradlew test
```

Unit tests currently focus on chart math/utility logic.
