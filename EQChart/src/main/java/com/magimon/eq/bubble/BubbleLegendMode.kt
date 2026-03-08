package com.magimon.eq.bubble

/**
 * [BubbleChartView]의 범례 항목 생성 방식을 정의한다.
 *
 * 범례 데이터 소스 우선순위를 결정하며, `showLegend=true`일 때만 시각적으로 표시된다.
 */
enum class BubbleLegendMode {
    /**
     * 차트 데이터 기반 자동 생성 모드.
     *
     * - [com.magimon.eq.bubble.BubbleDatum.legendGroup]과 색상을 기준으로 항목을 만든다.
     * - `setLegendItems`로 명시 항목을 전달해도 사용하지 않는다.
     * - 데이터 변경 시 범례도 자동으로 동기화된다.
     */
    AUTO,

    /**
     * 명시 항목 전용 모드.
     *
     * - `setLegendItems`로 전달한 목록만 렌더링한다.
     * - 데이터에 존재하지 않는 그룹도 범례에 표시할 수 있다.
     * - 명시 항목이 비어 있으면 범례도 비어 있을 수 있다.
     */
    EXPLICIT,

    /**
     * 명시 항목 우선 + 자동 생성 fallback 모드.
     *
     * - 명시 항목이 1개 이상이면 [EXPLICIT]처럼 동작한다.
     * - 명시 항목이 없을 때만 [AUTO] 규칙으로 범례를 생성한다.
     * - 운영 환경에서 기본 템플릿과 데이터 자동화를 혼합할 때 유용하다.
     */
    AUTO_WITH_OVERRIDE,
}
