package com.magimon.eq.bubble

/**
 * 버블 좌표 계산 방식.
 */
enum class BubbleLayoutMode {
    /**
     * `x/y` 값을 그대로 축에 투영하는 산점도 형태.
     */
    SCATTER,

    /**
     * 버블 크기 기반으로 중앙에 모으는 packed bubble 형태.
     */
    PACKED,
}
