package com.magimon.eq.bubble

/**
 * [BubbleChartView]의 범례 항목 생성 방식을 정의한다.
 */
enum class BubbleLegendMode {
    /**
     * 차트 데이터만 사용해 범례를 자동 생성한다.
     */
    AUTO,

    /**
     * `setLegendItems`로 제공한 항목만 사용한다.
     */
    EXPLICIT,

    /**
     * 명시 항목이 있으면 우선 사용하고, 없으면 자동 생성한다.
     */
    AUTO_WITH_OVERRIDE,
}
