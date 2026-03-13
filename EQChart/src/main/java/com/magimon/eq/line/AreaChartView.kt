package com.magimon.eq.line

import android.content.Context
import android.util.AttributeSet

/**
 * Area chart view.
 *
 * Reuses line renderer with area fill enabled and can be used for single or multi-series data.
 *
 * Data is supplied with [setSeries].
 */
class AreaChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseLineAreaChartView(context, attrs, defStyleAttr) {

    init {
        setPresentationOptions(LineChartPresentationOptions(showAreaFill = true))
    }

    override fun enableAreaFill(): Boolean = true
}
