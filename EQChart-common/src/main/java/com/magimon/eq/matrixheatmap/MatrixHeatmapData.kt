package com.magimon.eq.matrixheatmap

/**
 * Input data for a categorical matrix heatmap.
 *
 * The axis lists define both display order and valid keys for [cells].
 *
 * @property xLabels Ordered column labels rendered along the X axis.
 * @property yLabels Ordered row labels rendered along the Y axis.
 * @property cells Sparse or dense cell values keyed by axis labels.
 */
data class MatrixHeatmapData(
    val xLabels: List<String>,
    val yLabels: List<String>,
    val cells: List<MatrixHeatmapCell>,
)
