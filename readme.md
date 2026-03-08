# EQChart

Android 커스텀 차트 라이브러리입니다.  
현재 `Heatmap`, `Bubble`, `PCM Waveform`, `Radar`, `Pie`, `Donut` 차트를 제공합니다.

## 프로젝트 구성

- `:EQChart`
  - 차트 컴포넌트 라이브러리 모듈
- `:app`
  - 라이브러리 동작 예제를 확인할 수 있는 샘플 앱

## 지원 차트

- Heatmap: 섹션 기반 트리맵 스타일 히트맵
- Bubble: Scatter / Packed 버블 차트
- PCM Waveform: 실시간 16-bit mono PCM 파형 렌더링
- Radar: 다중 시리즈 레이더 차트(범례/애니메이션/포인트 클릭)
- Pie: 비율 기반 파이 차트(범례/레이블/클릭)
- Donut: 중앙 텍스트/레이블/클릭을 지원하는 도넛 차트

## 개발 환경

- Min SDK: 24
- Compile / Target SDK: 36
- Kotlin: 2.2.0
- AGP: 8.11.1
- Java / JVM Target: 11

## 설치/연동

`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":EQChart"))
}
```

## 샘플 앱 실행

```bash
./gradlew :app:installDebug
```

실행 후 `MainActivity`에서 각 차트 데모 화면으로 진입할 수 있습니다.

## 차트별 사용법

### 1) Heatmap

핵심 클래스:
- `StockHeatmapView`
- `StockHeatmapSection`
- `StockHeatmapItem`

기본 사용 예시:

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
        // item.symbol, item.changePct, item.marketCap 사용
    }
}

setContentView(ScrollView(this).apply { addView(heatmapView) })
```

참고:
- `setData(List<StockHeatmapItem>)`도 지원(하위호환)
- `sizeRatio`가 있으면 면적 계산에 우선 사용

### 2) Bubble

핵심 클래스:
- `BubbleChartView`
- `BubbleDatum`
- `BubbleLayoutMode`
- `BubblePresentationOptions`, `BubbleAxisOptions`

기본 사용 예시:

```kotlin
val bubbleView = BubbleChartView(this).apply {
    setLayoutMode(BubbleLayoutMode.PACKED) // 또는 SCATTER
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
        // datum.label, datum.payload 사용
    }
}

setContentView(bubbleView)
```

### 3) PCM Waveform

핵심 클래스:
- `PcmWaveFormView`
- `PcmWaveFormStyleOptions`

기본 사용 예시:

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

// 실시간 PCM 청크 추가 (16-bit mono)
waveformView.appendPcm16Mono(shortArrayOf(120, -300, 520, -120))
```

참고:
- 입력 데이터는 `ShortArray`(16-bit mono PCM)
- 내부적으로 최근 N ms 윈도우만 유지

### 4) Radar

핵심 클래스:
- `RadarChartView`
- `RadarAxis`
- `RadarSeries`
- `RadarChartStyleOptions`, `RadarChartPresentationOptions`

기본 사용 예시:

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
        // 포인트 클릭 정보 처리
    }
}

setContentView(radarView)
```

주의:
- 각 `RadarSeries.values` 개수는 축 개수와 동일해야 렌더링됩니다.

### 5) Pie / Donut

핵심 클래스:
- `PieChartView`, `DonutChartView`
- `PieSlice`
- `PieDonutStyleOptions`, `PieDonutPresentationOptions`

기본 사용 예시:

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
        // slice.label, slice.value, payload 사용
    }
}
```

주의:
- `PieSlice.value`는 `> 0`인 유한값만 렌더링됩니다.
- 유효 데이터 합계가 0이면 `emptyText`가 표시됩니다.
- 선택 확대 효과는 `enableSelectionExpand`, `selectedSliceExpandDp`, `selectedSliceExpandAnimMs`로 제어합니다.

## 테스트

```bash
./gradlew test
```

단위 테스트는 차트 수학/유틸 로직 중심으로 포함되어 있습니다.
