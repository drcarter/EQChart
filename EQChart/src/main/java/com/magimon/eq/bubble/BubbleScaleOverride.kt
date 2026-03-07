package com.magimon.eq.bubble

/**
 * 축/크기 스케일 수동 오버라이드 옵션.
 *
 * 각 값이 `null`이면 해당 축은 자동 min-max 범위를 사용한다.
 */
data class BubbleScaleOverride(
    val xMin: Double? = null,
    val xMax: Double? = null,
    val yMin: Double? = null,
    val yMax: Double? = null,
    val sizeMin: Double? = null,
    val sizeMax: Double? = null,
)
