package com.magimon.eq.pie

/**
 * Data model for a single pie/donut slice.
 *
 * @property label Slice label used for legend/label rendering
 * @property value Value used for ratio calculation (`>0` and finite values are renderable)
 * @property color Slice fill color
 * @property payload Optional source data delivered to click callbacks
 */
data class PieSlice(
    val label: String,
    val value: Double,
    val color: Int,
    val payload: Any? = null,
)
