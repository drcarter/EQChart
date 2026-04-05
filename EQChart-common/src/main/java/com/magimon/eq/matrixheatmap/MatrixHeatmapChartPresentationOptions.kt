package com.magimon.eq.matrixheatmap

/**
 * Behavioral options for matrix heatmap charts.
 *
 * @property showXAxisLabels Whether X-axis labels are drawn above the heatmap.
 * @property showYAxisLabels Whether Y-axis labels are drawn left of the heatmap.
 * @property showCellText Whether cell text is drawn when the cell is large enough.
 * @property emptyText Message shown when there is no renderable data.
 * @property axisLabelTextSizeSp Text size for X/Y-axis labels.
 * @property cellTextSizeSp Text size for cell labels.
 * @property minCellTextWidthDp Minimum cell width required to draw text.
 * @property minCellTextHeightDp Minimum cell height required to draw text.
 * @property valueFormatter Converts numeric values into fallback cell text.
 */
data class MatrixHeatmapChartPresentationOptions(
    val showXAxisLabels: Boolean = true,
    val showYAxisLabels: Boolean = true,
    val showCellText: Boolean = true,
    val emptyText: String = "No data",
    val axisLabelTextSizeSp: Float = 11.5f,
    val cellTextSizeSp: Float = 11f,
    val minCellTextWidthDp: Float = 36f,
    val minCellTextHeightDp: Float = 22f,
    val valueFormatter: (Double) -> String = ::formatMatrixHeatmapValue,
)
