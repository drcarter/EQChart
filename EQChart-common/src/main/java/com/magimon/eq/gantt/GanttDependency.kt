package com.magimon.eq.gantt

/**
 * One directed dependency edge between two tasks.
 *
 * @property fromTaskId Upstream task id.
 * @property toTaskId Downstream task id.
 * @property type Dependency type.
 * @property color Optional edge color override.
 * @property payload Optional source object returned to consumers.
 */
data class GanttDependency(
    val fromTaskId: String,
    val toTaskId: String,
    val type: GanttDependencyType = GanttDependencyType.FINISH_TO_START,
    val color: Int? = null,
    val payload: Any? = null,
)
