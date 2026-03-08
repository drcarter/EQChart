package com.magimon.eq.bubble

/**
 * 버블 좌표 계산 방식.
 *
 * [BubbleChartView.setLayoutMode]로 변경하며, 모드에 따라 데이터 해석 방식과
 * 축/그리드 활용 방식이 달라진다.
 */
enum class BubbleLayoutMode {
    /**
     * `x/y` 값을 플롯 영역에 선형 매핑하는 산점도 모드.
     *
     * - [com.magimon.eq.bubble.BubbleDatum.x], [com.magimon.eq.bubble.BubbleDatum.y]를 직접 사용한다.
     * - [BubbleAxisOptions]의 축/그리드/틱 설정이 실제 렌더링에 반영된다.
     * - 동일한 좌표에 데이터가 겹치면 버블이 중첩될 수 있다.
     */
    SCATTER,

    /**
     * 버블을 중심 주변에 충돌 없이 모아 배치하는 packed 모드.
     *
     * - `x/y` 값은 무시하고 `size`만 사용해 반지름을 계산한다.
     * - 초기 나선 배치 후 충돌 해소 반복으로 겹침을 줄인다.
     * - 축/틱은 의미가 없으므로 일반적으로 비활성화해 사용한다.
     */
    PACKED,
}
