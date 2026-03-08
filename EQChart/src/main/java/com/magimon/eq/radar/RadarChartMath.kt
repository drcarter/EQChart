package com.magimon.eq.radar

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 레이더 차트 좌표/히트테스트 계산 유틸리티.
 *
 * 뷰 렌더링 코드에서 수학 계산을 분리해 테스트 가능성을 높이기 위한 내부 유틸이다.
 */
internal object RadarChartMath {
    /**
     * 2차원 좌표.
     */
    data class Vec2(
        val x: Float,
        val y: Float,
    )

    /**
     * 터치 히트테스트 결과.
     *
     * @property seriesIndex 선택된 시리즈 인덱스
     * @property axisIndex 선택된 축 인덱스
     * @property distance 터치 좌표와 포인트 사이 거리(px)
     */
    data class HitResult(
        val seriesIndex: Int,
        val axisIndex: Int,
        val distance: Float,
    )

    /**
     * 원본 값을 `0..1` 범위로 정규화한다.
     *
     * 잘못된 입력(`NaN`, `Infinity`, `max<=0`)은 0으로 처리한다.
     */
    fun normalize(value: Double, maxValue: Double): Float {
        if (!value.isFinite() || !maxValue.isFinite() || maxValue <= 0.0) return 0f
        val ratio = value / maxValue
        return ratio.coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * 지정한 축 인덱스의 극좌표 정점을 계산한다.
     *
     * @param centerX 차트 중심 X
     * @param centerY 차트 중심 Y
     * @param radius 중심으로부터 반지름(px)
     * @param axisIndex 대상 축 인덱스
     * @param axisCount 전체 축 개수
     * @param startAngleDeg 시작 각도(도)
     */
    fun vertex(
        centerX: Float,
        centerY: Float,
        radius: Float,
        axisIndex: Int,
        axisCount: Int,
        startAngleDeg: Float = -90f,
    ): Vec2 {
        if (axisCount <= 0) return Vec2(centerX, centerY)
        val angleDeg = startAngleDeg + (360f * axisIndex.toFloat() / axisCount.toFloat())
        val angleRad = angleDeg * (PI / 180.0)
        return Vec2(
            x = centerX + (cos(angleRad) * radius).toFloat(),
            y = centerY + (sin(angleRad) * radius).toFloat(),
        )
    }

    /**
     * 시리즈 값을 폴리곤 정점 목록으로 변환한다.
     *
     * @param values 축 순서와 동일한 값 목록
     * @param maxValue 값 정규화 기준 최대값
     * @param centerX 차트 중심 X
     * @param centerY 차트 중심 Y
     * @param radius 차트 최대 반지름(px)
     * @param axisCount 전체 축 개수
     * @param progress 등장 애니메이션 진행률(0..1)
     * @param startAngleDeg 시작 각도(도)
     */
    fun polygonPoints(
        values: List<Double>,
        maxValue: Double,
        centerX: Float,
        centerY: Float,
        radius: Float,
        axisCount: Int,
        progress: Float,
        startAngleDeg: Float = -90f,
    ): List<Vec2> {
        if (axisCount <= 0 || values.isEmpty()) return emptyList()
        val count = min(axisCount, values.size)
        val renderProgress = progress.coerceIn(0f, 1f)
        val points = ArrayList<Vec2>(count)
        for (axisIndex in 0 until count) {
            val norm = normalize(values[axisIndex], maxValue)
            val localRadius = radius * norm * renderProgress
            points += vertex(
                centerX = centerX,
                centerY = centerY,
                radius = localRadius,
                axisIndex = axisIndex,
                axisCount = axisCount,
                startAngleDeg = startAngleDeg,
            )
        }
        return points
    }

    /**
     * 터치 좌표에 가장 가까운 포인트를 반환한다.
     *
     * @param touchX 터치 X
     * @param touchY 터치 Y
     * @param pointsBySeries 시리즈별 정점 목록
     * @param hitRadiusPx 허용 클릭 반경(px)
     * @return 반경 이내의 최근접 포인트. 없으면 `null`
     */
    fun nearestPoint(
        touchX: Float,
        touchY: Float,
        pointsBySeries: List<List<Vec2>>,
        hitRadiusPx: Float,
    ): HitResult? {
        if (pointsBySeries.isEmpty()) return null
        val hitRadius = max(1f, hitRadiusPx)
        var best: HitResult? = null
        var bestDistance = Float.MAX_VALUE

        pointsBySeries.forEachIndexed { seriesIndex, points ->
            points.forEachIndexed { axisIndex, point ->
                val distance = hypot(touchX - point.x, touchY - point.y)
                if (distance <= hitRadius && distance < bestDistance) {
                    bestDistance = distance
                    best = HitResult(seriesIndex = seriesIndex, axisIndex = axisIndex, distance = distance)
                }
            }
        }
        return best
    }
}
