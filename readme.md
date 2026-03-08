# EQChart 진행 현황 정리

기준일: 2026-03-08

## 1) 프로젝트 개요

- Android 멀티 모듈 프로젝트
- `:EQChart` 라이브러리 모듈: 커스텀 차트 컴포넌트 제공
- `:app` 샘플 앱 모듈: 각 차트 데모 실행 화면 제공

## 2) 현재까지 완료된 기능

### 2-1. Heatmap 차트

- `StockHeatmapView` 구현
- 섹션 기반 API(`setSections`) + 하위호환 API(`setData`) 지원
- 종목 클릭 리스너 지원
- 2단계 squarified treemap 레이아웃 적용
- `sizeRatio` 우선, 없으면 `marketCap` 기반 블록 면적 계산
- `StockHeatmapHelper`로 샘플 섹션 데이터/색상 매핑/포맷 유틸 제공

### 2-2. Bubble 차트

- `BubbleChartView` 구현
- 레이아웃 모드 지원: `SCATTER`, `PACKED`
- 축/그리드 옵션(`BubbleAxisOptions`)과 표현 옵션(`BubblePresentationOptions`) 분리
- 범례 모드 지원(`AUTO`, `EXPLICIT`, `AUTO_WITH_OVERRIDE`)
- 스케일 오버라이드, 버블 클릭 리스너 지원
- 데이터 매핑 오버로드(`setData(items, mapper)`) 지원

### 2-3. PCM Waveform

- `PcmWaveFormView` 구현 (16-bit mono PCM 기준)
- 링 버퍼(`PcmRingBuffer`)로 최근 N ms 윈도우 유지
- 픽셀 단위 min/max 다운샘플링(`PcmWaveDownSampler`)으로 피크 보존
- 스타일 옵션(`PcmWaveFormStyleOptions`) 및 샘플레이트/윈도우 길이 제어 API 제공
- `WaveformFileActivity`에서 `MediaExtractor`/`MediaCodec` 디코딩 + `MediaPlayer` 재생 동기화 데모 구현

## 3) 샘플 앱 구성

- `MainActivity`: Heatmap / Bubble / Waveform 진입 버튼 제공
- `HeatmapActivity`: 섹션 히트맵 렌더링 및 아이템 클릭 Toast 데모
- `BubbleActivity`: Packed 버블 + 제목/범례 + 클릭 Toast 데모
- `WaveformFileActivity`: `sample_tone.wav` 재생과 실시간 파형 표시 데모

## 4) 테스트 및 빌드 상태

- 실행 명령: `./gradlew test`
- 실행 일시: 2026-03-08
- 결과: `BUILD SUCCESSFUL`
- 확인된 단위 테스트:
  - `BubbleChartMathTest`
  - `BubbleLegendResolverTest`
  - `PcmRingBufferTest`
  - `PcmWaveDownSamplerTest`