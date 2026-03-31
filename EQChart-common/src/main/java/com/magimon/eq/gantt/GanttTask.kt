package com.magimon.eq.gantt

/**
 * One Gantt task rendered on its own row.
 *
 * @property id Stable task identifier used by dependency edges.
 * @property label Row label rendered beside the task.
 * @property start Inclusive task start on the shared numeric X axis.
 * @property end Inclusive task end on the shared numeric X axis.
 * @property progress Completion ratio in the `0f..1f` domain.
 * @property color Optional task color override.
 * @property title Optional task text rendered on or near the task.
 * @property isMilestone Whether the task should render as a milestone marker.
 * @property payload Optional source object returned in click callbacks.
 */
data class GanttTask(
    val id: String,
    val label: String,
    val start: Double,
    val end: Double,
    val progress: Float = 0f,
    val color: Int? = null,
    val title: String? = null,
    val isMilestone: Boolean = false,
    val payload: Any? = null,
)
