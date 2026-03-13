package com.magimon.eq.pie

import android.content.Context
import android.util.AttributeSet

/**
 * Standard pie chart view.
 *
 * Uses the shared [BasePieDonutChartView] API (`setData`, `setStyleOptions`,
 * `setPresentationOptions`, `setOnSliceClickListener`) and fixes inner-hole ratio to `0`.
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
