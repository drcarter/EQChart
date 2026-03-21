# EQChart

EQChart is an Android custom chart library.
It currently provides `Heatmap`, `Bubble`, `Line`, `Area`, `Bar`, `Histogram`, `Waterfall`, `Funnel`, `PCM Waveform`, `Radar`, `Pie`, `Donut`, `Gauge`, `Sankey`, `Cycle`, and `Step Flow` charts.

## Project Structure

- `:EQChart-common`
  - Shared chart models/options/enums used by both View and Compose
- `:EQChart`
  - Android View-based chart components
- `:EQChart-compose`
  - Native Compose chart components
- `:EQChart-bom`
  - Maven BOM for aligning EQChart module versions
- `:app`
  - Sample app for both View and Compose demos

## Supported Charts

- Heatmap: Section-based treemap-style stock heatmap
- Bubble: Scatter / Packed bubble chart
- Line: Multi-series Cartesian line chart with grid / legend / point selection
- Area: Filled line chart variant for trend comparison
- Bar: Grouped / stacked bar chart with vertical / horizontal orientation
- Histogram: Ordered bucket chart for count / frequency distribution
- Waterfall: Ordered cumulative delta chart with subtotal / total bars and connectors
- Funnel: Vertical conversion funnel chart with tapered stages and click callbacks
- PCM Waveform: Real-time 16-bit mono PCM waveform rendering
- Radar: Multi-series radar chart (legend/animation/point click)
- Pie: Ratio-based pie chart (legend/labels/click)
- Donut: Donut chart with center text/labels/click
- Gauge: Semi-circular single-value gauge with ranges/ticks/indicator
- Sankey: Flow diagram with nodes/links, stage inference, and tap highlight
- Cycle: Circular flow diagram with nodes on a ring and directional inner links
- Step Flow: Ordered infographic steps with a hub, curved spine, and right-side pill cards

## Chart Families

- Tiled: Heatmap
- Axis-based: Bubble, Line, Area, Bar, Histogram, Waterfall, Funnel
- Radial: Radar, Pie, Donut, Gauge
- Flow: Sankey, Cycle, Step Flow
- Signal: PCM Waveform

## Development Environment

- Min SDK: 24
- Compile / Target SDK: 36
- Kotlin: 2.2.0
- AGP: 8.11.1
- Java / JVM Target: 11

## Versioning

- Release version format: `YYYY.MM.DD`
- Same-day republish format: `YYYY.MM.DD.N` (`N = 1, 2, 3 ...`)
- Version placeholder used in this README: `latest_version`
- Published versions follow one of these forms:
  - First release of the day: `YYYY.MM.DD`
  - Same-day republish: `YYYY.MM.DD.N`

## Installation

### 1) GitHub Packages repository

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/drcarter/EQChart")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GPR_USER"))
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GPR_KEY"))
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
```

`github.properties` (project root) or `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN_WITH_read:packages
```

### 2) Maven dependencies

```kotlin
dependencies {
    // Align EQChart module versions with a single platform import
    implementation(platform("com.magimon.eq:eqchart-bom:latest_version"))

    // View charts
    implementation("com.magimon.eq:eqchart")

    // Compose charts
    implementation("com.magimon.eq:eqchart-compose")
}
```

Replace `latest_version` with the latest published EQChart version.

`eqchart` and `eqchart-compose` transitively include `eqchart-common`,
so `eqchart-common` usually does not need to be added separately.

If you do not want to use the BOM, you can keep specifying versions on each
artifact individually.

### 3) Local multi-module usage (this repository)

```kotlin
dependencies {
    implementation(platform(project(":EQChart-bom")))
    implementation(project(":EQChart"))
    implementation(project(":EQChart-compose"))
}
```

## Publish

```bash
# First release of the day
./gradlew publish -PpublishDate=2026.03.08

# Same-day republish
./gradlew publish -PpublishDate=2026.03.08 -PpublishIncrement=1
```

Published artifacts: `eqchart-common`, `eqchart`, `eqchart-compose`, `eqchart-bom`

## Reference Docs

Build aggregated Dokka HTML reference docs for the published library modules:

```bash
./gradlew referenceDocs
```

Build, start the local preview server, and open the browser in one step:

```bash
./gradlew referenceDocsPreview
```

Generated site:

- `docs/reference/index.html`

Per-module Dokka tasks are also available:

- `./gradlew :EQChart-common:dokkaHtml`
- `./gradlew :EQChart:dokkaHtml`
- `./gradlew :EQChart-compose:dokkaHtml`

Compatibility aliases are also kept for familiar Dokka task names:

- `./gradlew dokkaHtmlMultiModule`

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
- `LineChart(...)`, `AreaChart(...)`
- `BarChart(...)`
- `HistogramChart(...)`
- `WaterfallChart(...)`
- `FunnelChart(...)`
- `PcmWaveformChart(...)` + `rememberPcmWaveformController(...)`
- `RadarChart(...)`
- `PieChart(...)`, `DonutChart(...)`
- `GaugeChart(...)`
- `SankeyChart(...)`
- `CycleChart(...)`
- `StepFlowChart(...)`

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

### 3) Line / Area

Key classes:
- `LineChartView`, `AreaChartView`
- `LineSeries`, `LineDatum`
- `LineChartStyleOptions`, `LineChartPresentationOptions`

Basic example:

```kotlin
val series = listOf(
    LineSeries(
        name = "Traffic",
        color = Color.parseColor("#2B80FF"),
        points = listOf(
            LineDatum(0.0, 10.0, "Jan"),
            LineDatum(1.0, 14.0, "Feb"),
            LineDatum(2.0, 18.0, "Mar"),
        ),
        payload = "Traffic",
        areaFillColor = Color.parseColor("#2B80FF"),
    ),
    LineSeries(
        name = "Conversion",
        color = Color.parseColor("#13C3A3"),
        points = listOf(
            LineDatum(0.0, 8.0, "Jan"),
            LineDatum(1.0, 11.0, "Feb"),
            LineDatum(2.0, 15.0, "Mar"),
        ),
        payload = "Conversion",
    ),
)

val lineChart = LineChartView(this).apply {
    setStyleOptions(
        LineChartStyleOptions(
            backgroundColor = Color.parseColor("#F7FAFC"),
            axisColor = Color.parseColor("#8D9AA8"),
            axisLabelColor = Color.parseColor("#3B4350"),
            legendTextColor = Color.parseColor("#273447"),
        ),
    )
    setPresentationOptions(
        LineChartPresentationOptions(
            showLegend = true,
            showGrid = true,
            showAxes = true,
            showPoints = true,
        ),
    )
    setSeries(series)
    setOnPointClickListener { _, _, point, payload ->
        // use point.x, point.y, payload
    }
}

val areaChart = AreaChartView(this).apply {
    setPresentationOptions(
        LineChartPresentationOptions(
            showLegend = true,
            showAreaFill = true,
        ),
    )
    setSeries(series)
}
```

Notes:
- `AreaChartView` is the filled variant that reuses the same `LineSeries` / `LineDatum` model
- Only finite `(x, y)` points are rendered

### 4) Bar

Key classes:
- `BarChartView`
- `BarSeries`, `BarDatum`
- `BarChartStyleOptions`, `BarChartPresentationOptions`
- `BarLayoutMode`, `BarOrientation`

Basic example:

```kotlin
val barChart = BarChartView(this).apply {
    setPresentationOptions(
        BarChartPresentationOptions(
            showLegend = true,
            showGrid = true,
            showAxes = true,
            layoutMode = BarLayoutMode.GROUPED,
            orientation = BarOrientation.VERTICAL,
        ),
    )

    setSeries(
        listOf(
            BarSeries(
                name = "Desktop",
                color = Color.parseColor("#2B80FF"),
                points = listOf(
                    BarDatum("Q1", 12.0, "Desktop-Q1"),
                    BarDatum("Q2", 16.0, "Desktop-Q2"),
                ),
            ),
            BarSeries(
                name = "Mobile",
                color = Color.parseColor("#13C3A3"),
                points = listOf(
                    BarDatum("Q1", 9.0, "Mobile-Q1"),
                    BarDatum("Q2", 14.0, "Mobile-Q2"),
                ),
            ),
        ),
    )

    setOnBarClickListener { _, categoryIndex, value, payload ->
        // use categoryIndex, value, payload
    }
}
```

Notes:
- Categories are resolved from the union of `BarDatum.category` values across all series
- `layoutMode` supports `GROUPED` and `STACKED`; `orientation` supports `VERTICAL` and `HORIZONTAL`

### 5) PCM Waveform

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

### 6) Radar

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

### 7) Pie / Donut

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

### 8) Gauge

Key classes:
- `GaugeChartView`
- `GaugeValue`, `GaugeRange`
- `GaugeChartStyleOptions`, `GaugeChartPresentationOptions`

Basic example:

```kotlin
val gaugeView = GaugeChartView(this).apply {
    setRanges(
        listOf(
            GaugeRange(0.0, 50.0, Color.parseColor("#13C3A3")),
            GaugeRange(50.0, 80.0, Color.parseColor("#FF9F1C")),
            GaugeRange(80.0, 100.0, Color.parseColor("#EF476F")),
        ),
    )
    setPresentationOptions(
        GaugeChartPresentationOptions(
            showTicks = true,
            tickCount = 5,
            showMinMaxLabels = true,
            showValueText = true,
            showCenterLabel = true,
        ),
    )
    setValue(
        GaugeValue(
            value = 72.0,
            minValue = 0.0,
            maxValue = 100.0,
            label = "CPU usage",
        ),
    )
}
```

Notes:
- `GaugeValue.maxValue` must be greater than `minValue`
- `value` is clamped into the configured range before rendering
- Invalid ranges (`end <= start`, NaN, infinite) are ignored
- Compose uses `GaugeChart(...)` with the same shared models/options

### 9) Sankey

Key classes:
- `SankeyChartView`
- `SankeyNode`, `SankeyLink`
- `SankeyChartStyleOptions`, `SankeyChartPresentationOptions`

Basic example:

```kotlin
val nodes = listOf(
    SankeyNode("direct", "Direct", Color.parseColor("#2B80FF")),
    SankeyNode("search", "Search", Color.parseColor("#13C3A3")),
    SankeyNode("landing", "Landing", Color.parseColor("#6F8695")),
    SankeyNode("trial", "Trial", Color.parseColor("#8A79FF")),
    SankeyNode("paid", "Paid", Color.parseColor("#2A9D8F")),
)

val links = listOf(
    SankeyLink("direct", "landing", 28.0),
    SankeyLink("search", "landing", 34.0),
    SankeyLink("landing", "trial", 30.0),
    SankeyLink("trial", "paid", 16.0),
)

val sankeyView = SankeyChartView(this).apply {
    setPresentationOptions(
        SankeyChartPresentationOptions(
            showNodeLabels = true,
            showLinkValues = true,
        ),
    )
    setNodes(nodes)
    setLinks(links)
    setOnNodeClickListener { nodeIndex, node, payload ->
        // use node.id, node.label, payload
    }
    setOnLinkClickListener { linkIndex, link, payload ->
        // use link.sourceId, link.targetId, link.value, payload
    }
}
```

Notes:
- `SankeyLink.value` must be finite and `> 0`
- `SankeyNode.stage` is optional; if omitted, stage is inferred from links
- Cycles or backward stage assignments fall back to `emptyText`
- Compose uses `SankeyChart(...)` with the same shared models/options

### 10) Cycle

Key classes:
- `CycleChartView`
- `CycleNode`, `CycleLink`
- `CycleChartStyleOptions`, `CycleChartPresentationOptions`

Basic example:

```kotlin
val nodes = listOf(
    CycleNode("plan", "Plan", Color.parseColor("#2B80FF")),
    CycleNode("build", "Build", Color.parseColor("#13C3A3")),
    CycleNode("launch", "Launch", Color.parseColor("#FF9F1C")),
    CycleNode("measure", "Measure", Color.parseColor("#8A79FF")),
    CycleNode("learn", "Learn", Color.parseColor("#EF476F")),
)

val links = listOf(
    CycleLink("plan", "build", 18.0, label = "18"),
    CycleLink("build", "launch", 14.0, label = "14"),
    CycleLink("launch", "measure", 11.0, label = "11"),
    CycleLink("measure", "learn", 16.0, label = "16"),
    CycleLink("learn", "plan", 20.0, label = "20"),
    CycleLink("measure", "plan", 7.0, label = "7"),
)

val cycleView = CycleChartView(this).apply {
    setStyleOptions(
        CycleChartStyleOptions(
            backgroundColor = Color.parseColor("#F7FAFC"),
            nodeStrokeColor = Color.WHITE,
            selectedStrokeColor = Color.parseColor("#0F172A"),
        ),
    )
    setPresentationOptions(
        CycleChartPresentationOptions(
            showNodeLabels = true,
            showLinkLabels = true,
            animateOnDataChange = true,
        ),
    )
    setNodes(nodes)
    setLinks(links)
    setOnNodeClickListener { _, node, _ ->
        // use node.label / node.payload
    }
    setOnLinkClickListener { _, link, _ ->
        // use link.sourceId / link.targetId / link.value
    }
}

setContentView(cycleView)
```

Notes:
- Nodes are arranged around the ring in input order
- `CycleLink.value` must be finite and `> 0`
- Self-links are ignored in the current MVP implementation
- `CycleChartPresentationOptions.startAngleDeg` and `clockwise` control ring ordering
- Compose uses `CycleChart(...)` with the same shared models/options

### 11) Step Flow

Key classes:
- `StepFlowChartView`
- `StepFlowHubContent`, `StepFlowStep`
- `StepFlowChartStyleOptions`, `StepFlowChartPresentationOptions`

Basic example:

```kotlin
val hubContent = StepFlowHubContent(
    eyebrow = "INFOGRAPHIC",
    title = "STEPS",
    description = "Show the process clearly",
)

val steps = listOf(
    StepFlowStep("discover", "STEP 01", "Discover", "Collect inputs", Color.parseColor("#D946EF"), "!"),
    StepFlowStep("design", "STEP 02", "Design", "Shape the plan", Color.parseColor("#8B5CF6"), "#"),
    StepFlowStep("build", "STEP 03", "Build", "Implement the work", Color.parseColor("#60A5FA"), "*"),
    StepFlowStep("launch", "STEP 04", "Launch", "Release to users", Color.parseColor("#FBBF24"), "$"),
    StepFlowStep("measure", "STEP 05", "Measure", "Track outcomes", Color.parseColor("#A3E635"), "+"),
)

val stepFlowView = StepFlowChartView(this).apply {
    setStyleOptions(
        StepFlowChartStyleOptions(
            backgroundColor = Color.parseColor("#2B2B2B"),
        ),
    )
    setPresentationOptions(
        StepFlowChartPresentationOptions(
            showStepDescriptions = true,
            showHubDescription = true,
        ),
    )
    setHubContent(hubContent)
    setSteps(steps)
    setOnStepClickListener { _, step, _ ->
        // use step.title / step.payload
    }
}

setContentView(stepFlowView)
```

Notes:
- Steps are rendered in input order from top to bottom along the curved spine
- `StepFlowHubContent` is optional; the chart can render steps without the hub block
- The shared layout engine computes hub, ring, spine, badge, card, and icon-slot geometry for both UI stacks
- Compose uses `StepFlowChart(hubContent = ..., steps = ...)` with the same shared models/options
- This chart is diagram-oriented and does not use axis or free-form graph links

## Test

```bash
./gradlew test
```

Unit tests currently focus on chart math/utility logic.
