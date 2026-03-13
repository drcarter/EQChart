package com.magimon.eq.pie

import android.content.Context
import android.util.AttributeSet

/**
 * Donut chart view.
 *
 * Built on top of [BasePieDonutChartView].
 * Uses [DEFAULT_INNER_RATIO] as the default inner-hole ratio.
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
     * Sets the donut inner-hole radius ratio (`0..0.92`).
     *
     * Out-of-range values are automatically clamped.
     */
    fun setDonutInnerRadiusRatio(ratio: Float) {
        setInnerRadiusRatio(ratio)
    }

    /**
     * Returns the current donut inner-hole radius ratio.
     */
    fun getDonutInnerRadiusRatio(): Float = currentInnerRadiusRatio()

    companion object {
        /**
         * Default donut inner-hole radius ratio.
         */
        const val DEFAULT_INNER_RATIO: Float = 0.58f
    }
}
