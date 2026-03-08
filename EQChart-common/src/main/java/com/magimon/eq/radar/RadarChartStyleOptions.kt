package com.magimon.eq.radar

import android.graphics.Color

/**
 * Radar chart rendering style options.
 *
 * @property backgroundColor Chart background color
 * @property gridColor Concentric polygon grid line color
 * @property axisColor Center-to-axis line color
 * @property axisLabelColor Axis label text color
 * @property legendTextColor Legend text color
 * @property polygonStrokeWidthDp Series polygon stroke width (dp)
 * @property gridStrokeWidthDp Grid line stroke width (dp)
 * @property axisStrokeWidthDp Axis line stroke width (dp)
 * @property axisDashLengthDp Dash length for axis dashed line (dp)
 * @property axisDashGapDp Dash gap for axis dashed line (dp)
 * @property fillAlpha Series fill alpha (0..255)
 * @property pointRadiusDp Point marker radius (dp)
 * @property pointCoreRadiusDp Inner core radius of point marker (dp)
 * @property pointGlowRadiusDp Glow radius around point marker (dp)
 * @property pointGlowAlpha Point glow alpha (0..255)
 * @property selectedPointRadiusDp Highlight ring radius for selected point (dp)
 * @property selectedStrokeWidthDp Highlight ring stroke width for selected point (dp)
 * @property contentPaddingDp Outer padding around chart content (dp)
 * @property axisLabelOffsetDp Distance from axis end to label anchor (dp)
 * @property legendMarkerSizeDp Legend color marker size (dp)
 * @property legendItemGapDp Horizontal gap between legend items (dp)
 * @property legendRowGapDp Vertical gap between legend rows (dp)
 * @property legendBottomGapDp Bottom gap between legend and chart body (dp)
 * @property touchHitRadiusDp Touch hit radius for point selection (dp)
 */
data class RadarChartStyleOptions(
    val backgroundColor: Int = Color.WHITE,
    val gridColor: Int = Color.parseColor("#D7DFE8"),
    val axisColor: Int = Color.parseColor("#C8D1DC"),
    val axisLabelColor: Int = Color.parseColor("#5D6675"),
    val legendTextColor: Int = Color.parseColor("#1F2430"),
    val polygonStrokeWidthDp: Float = 1.8f,
    val gridStrokeWidthDp: Float = 1f,
    val axisStrokeWidthDp: Float = 1f,
    val axisDashLengthDp: Float = 4f,
    val axisDashGapDp: Float = 4f,
    val fillAlpha: Int = 78,
    val pointRadiusDp: Float = 4.6f,
    val pointCoreRadiusDp: Float = 2f,
    val pointGlowRadiusDp: Float = 12f,
    val pointGlowAlpha: Int = 72,
    val selectedPointRadiusDp: Float = 7.5f,
    val selectedStrokeWidthDp: Float = 2.4f,
    val contentPaddingDp: Float = 12f,
    val axisLabelOffsetDp: Float = 18f,
    val legendMarkerSizeDp: Float = 10f,
    val legendItemGapDp: Float = 10f,
    val legendRowGapDp: Float = 8f,
    val legendBottomGapDp: Float = 10f,
    val touchHitRadiusDp: Float = 18f,
)
