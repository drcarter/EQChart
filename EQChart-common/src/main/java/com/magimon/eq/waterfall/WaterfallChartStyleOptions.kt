package com.magimon.eq.waterfall

import android.graphics.Color

/**
 * Visual styling for waterfall charts.
 *
 * @property backgroundColor Chart background color.
 * @property gridColor Grid line color.
 * @property axisColor Axis line color.
 * @property axisLabelColor Axis and category label color.
 * @property connectorColor Connector stroke color.
 * @property barValueTextColor Value label color.
 * @property positiveBarColor Default fill for positive deltas.
 * @property negativeBarColor Default fill for negative deltas.
 * @property subtotalBarColor Default fill for subtotal bars.
 * @property totalBarColor Default fill for total bars.
 * @property categorySpacingDp Gap between category slots.
 * @property barWidthRatio Ratio of each slot occupied by the bar body.
 * @property barCornerRadiusDp Corner radius for bar rectangles.
 * @property selectedBarPaddingDp Extra inset for selected outlines.
 * @property contentPaddingDp Outer chart padding.
 * @property connectorStrokeWidthDp Connector line width.
 */
data class WaterfallChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#8F9CAB"),
    val axisLabelColor: Int = Color.parseColor("#5E6878"),
    val connectorColor: Int = Color.parseColor("#93A1B2"),
    val barValueTextColor: Int = Color.parseColor("#263244"),
    val positiveBarColor: Int = Color.parseColor("#13C3A3"),
    val negativeBarColor: Int = Color.parseColor("#EF476F"),
    val subtotalBarColor: Int = Color.parseColor("#FF9F1C"),
    val totalBarColor: Int = Color.parseColor("#2B80FF"),
    val categorySpacingDp: Float = 12f,
    val barWidthRatio: Float = 0.62f,
    val barCornerRadiusDp: Float = 3f,
    val selectedBarPaddingDp: Float = 1.5f,
    val contentPaddingDp: Float = 14f,
    val connectorStrokeWidthDp: Float = 1.5f,
)
