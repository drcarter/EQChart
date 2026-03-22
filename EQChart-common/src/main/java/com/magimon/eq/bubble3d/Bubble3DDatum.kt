package com.magimon.eq.bubble3d

/**
 * Data model for one 3D bubble rendered in a world-space scatter volume.
 *
 * @property x X value in data space
 * @property y Y value in data space
 * @property z Z value in data space
 * @property size Value used to map bubble radius
 * @property color Bubble fill color
 * @property label Optional label for selection overlays or legends
 * @property legendGroup Optional group label reserved for future legend/grouping use
 * @property payload Optional custom object delivered in click callbacks
 */
data class Bubble3DDatum(
    val x: Double,
    val y: Double,
    val z: Double,
    val size: Double,
    val color: Int,
    val label: String? = null,
    val legendGroup: String? = null,
    val payload: Any? = null,
)
