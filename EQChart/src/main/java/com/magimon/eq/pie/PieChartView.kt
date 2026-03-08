package com.magimon.eq.pie

import android.content.Context
import android.util.AttributeSet

/**
 * 일반 파이 차트 View.
 *
 * [BasePieDonutChartView] 공용 API(`setData`, `setStyleOptions`, `setPresentationOptions`,
 * `setOnSliceClickListener`)를 그대로 사용하며, 내부 홀 반경을 0으로 고정한다.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BasePieDonutChartView(context, attrs, defStyleAttr) {

    init {
        setInnerRadiusRatio(0f)
    }
}
