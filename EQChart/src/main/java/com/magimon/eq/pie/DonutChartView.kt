package com.magimon.eq.pie

import android.content.Context
import android.util.AttributeSet

/**
 * 도넛 차트 View.
 *
 * [BasePieDonutChartView] 공용 API를 기반으로 동작하며,
 * 기본 내부 홀 비율은 [DEFAULT_INNER_RATIO]를 사용한다.
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BasePieDonutChartView(context, attrs, defStyleAttr) {

    init {
        setInnerRadiusRatio(DEFAULT_INNER_RATIO)
    }

    /**
     * 도넛 중앙 홀 반경 비율(0..0.92)을 설정한다.
     *
     * 값이 범위를 벗어나면 내부적으로 자동 보정된다.
     */
    fun setDonutInnerRadiusRatio(ratio: Float) {
        setInnerRadiusRatio(ratio)
    }

    /**
     * 현재 도넛 중앙 홀 반경 비율을 반환한다.
     */
    fun getDonutInnerRadiusRatio(): Float = currentInnerRadiusRatio()

    companion object {
        /**
         * 도넛 기본 중앙 홀 반경 비율.
         */
        const val DEFAULT_INNER_RATIO: Float = 0.58f
    }
}
