package com.magimon.eq.boxplot

/**
 * One categorical box plot entry.
 *
 * @property label Category label rendered on the X axis.
 * @property min Lower whisker value.
 * @property q1 First quartile value.
 * @property median Median value.
 * @property q3 Third quartile value.
 * @property max Upper whisker value.
 * @property outliers Optional outlier values rendered as separate points.
 * @property color Optional box color override.
 * @property title Optional display label rendered above the box.
 * @property payload Optional source object returned in click callbacks.
 */
data class BoxPlotEntry(
    val label: String,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val outliers: List<Double> = emptyList(),
    val color: Int? = null,
    val title: String? = null,
    val payload: Any? = null,
)
