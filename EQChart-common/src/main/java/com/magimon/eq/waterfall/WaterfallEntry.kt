package com.magimon.eq.waterfall

/**
 * One ordered step in a waterfall chart.
 *
 * For [WaterfallEntryKind.DELTA], [value] is applied to the running total.
 * For [WaterfallEntryKind.SUBTOTAL] and [WaterfallEntryKind.TOTAL], [value] is ignored and the
 * current running total is rendered as a summary bar.
 *
 * @property label Category label shown under the bar.
 * @property value Delta value or ignored summary placeholder.
 * @property color Optional bar color override.
 * @property kind Semantic role of this entry.
 * @property payload Optional source object returned in click callbacks.
 */
data class WaterfallEntry(
    val label: String,
    val value: Double,
    val color: Int? = null,
    val kind: WaterfallEntryKind = WaterfallEntryKind.DELTA,
    val payload: Any? = null,
)
