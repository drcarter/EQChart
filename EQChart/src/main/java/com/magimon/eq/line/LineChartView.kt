package com.magimon.eq.line

import android.content.Context
import android.util.AttributeSet

/**
 * Line chart view.
 *
 * Data is supplied with [setSeries].
 *
 * Use [setOnPointClickListener] to receive point interactions:
 * `(seriesIndex, pointIndex, point, payload)`.
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseLineAreaChartView(context, attrs, defStyleAttr) {

    override fun enableAreaFill(): Boolean = false
}

