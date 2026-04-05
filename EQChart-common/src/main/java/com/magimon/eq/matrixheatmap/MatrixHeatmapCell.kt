package com.magimon.eq.matrixheatmap

/**
 * One categorical matrix heatmap cell.
 *
 * @property xKey Column key that must match one entry from [MatrixHeatmapData.xLabels].
 * @property yKey Row key that must match one entry from [MatrixHeatmapData.yLabels].
 * @property value Numeric value used for color mapping.
 * @property label Optional text override rendered inside the cell.
 * @property payload Optional source object returned in click callbacks.
 */
data class MatrixHeatmapCell(
    val xKey: String,
    val yKey: String,
    val value: Double,
    val label: String? = null,
    val payload: Any? = null,
)
