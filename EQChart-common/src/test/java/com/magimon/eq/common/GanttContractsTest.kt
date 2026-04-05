package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.gantt.GanttChartPresentationOptions
import com.magimon.eq.gantt.GanttChartLayoutConfig
import com.magimon.eq.gantt.GanttChartStyleOptions
import com.magimon.eq.gantt.GanttDependency
import com.magimon.eq.gantt.GanttTask
import com.magimon.eq.gantt.hitTestGanttTask
import com.magimon.eq.gantt.resolveGanttChartLayout
import com.magimon.eq.gantt.resolveGanttChartPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GanttContractsTest {

    @Test
    fun ganttTask_preservesValuesAndDefaults() {
        val payload = mapOf("phase" to "build")
        val task = GanttTask(id = "build", label = "Build", start = 1.0, end = 5.0)
        val milestone = GanttTask(
            id = "launch",
            label = "Launch",
            start = 6.0,
            end = 6.0,
            progress = 2f,
            color = 0xFF123456.toInt(),
            title = "GA",
            isMilestone = true,
            payload = payload,
        )

        assertEquals("build", task.id)
        assertEquals(0f, task.progress, 0f)
        assertNull(task.color)
        assertFalse(task.isMilestone)

        assertEquals("launch", milestone.id)
        assertEquals(0xFF123456.toInt(), milestone.color)
        assertEquals("GA", milestone.title)
        assertTrue(milestone.isMilestone)
        assertSame(payload, milestone.payload)
    }

    @Test
    fun ganttPresentationAndStyleDefaults_areAccessible() {
        val presentation = GanttChartPresentationOptions()
        val style = GanttChartStyleOptions()

        assertTrue(presentation.showGrid)
        assertTrue(presentation.showAxes)
        assertTrue(presentation.showTaskLabels)
        assertTrue(presentation.showProgress)
        assertTrue(presentation.showDependencies)
        assertFalse(presentation.showTodayIndicator)
        assertEquals("1.5K", presentation.xLabelFormatter(1_500.0))
        assertEquals("No data", presentation.emptyText)

        assertEquals(Color.WHITE, style.backgroundColor)
        assertEquals(Color.parseColor("#2563EB"), style.defaultTaskColor)
        assertEquals(Color.parseColor("#93C5FD"), style.progressBarColor)
        assertEquals(Color.parseColor("#64748B"), style.dependencyLineColor)
        assertEquals(10f, style.milestoneSizeDp, 0f)
    }

    @Test
    fun ganttLayout_sanitizesTasksClampsProgressAndResolvesDependencies() {
        val payload = mapOf("id" to 7)
        val layout = resolveGanttChartLayout(
            tasks = listOf(
                GanttTask(id = "plan", label = "Plan", start = 0.0, end = 2.0, progress = 0.25f, payload = payload),
                GanttTask(id = "build", label = "Build", start = 5.0, end = 2.0, progress = 2f, title = "Sprint 1"),
                GanttTask(id = "", label = "Missing", start = 1.0, end = 1.0),
                GanttTask(id = "bad", label = "", start = 1.0, end = 2.0),
            ),
            dependencies = listOf(
                GanttDependency("plan", "build"),
                GanttDependency("plan", "missing"),
                GanttDependency("build", "build"),
            ),
            style = GanttChartStyleOptions(defaultTaskColor = 11),
            tickCount = 5,
        )

        assertEquals(2, layout.tasks.size)
        assertEquals(1, layout.dependencies.size)
        assertEquals("plan", layout.tasks[0].id)
        assertEquals(0.25f, layout.tasks[0].progress, 0f)
        assertSame(payload, layout.tasks[0].payload)
        assertEquals(11, layout.tasks[0].color)

        assertEquals("build", layout.tasks[1].id)
        assertEquals("Sprint 1", layout.tasks[1].title)
        assertEquals(1f, layout.tasks[1].progress, 0f)
        assertEquals(2.0, layout.tasks[1].startValue, 0.0)
        assertEquals(5.0, layout.tasks[1].endValue, 0.0)

        assertEquals(0.0, layout.minValue, 0.0)
        assertEquals(5.0, layout.maxValue, 0.0)
        assertEquals(5, layout.ticks.size)
    }

    @Test
    fun ganttPlacement_resolvesMilestonesTodayLineAndHitTesting() {
        val layout = resolveGanttChartLayout(
            tasks = listOf(
                GanttTask(id = "plan", label = "Plan", start = 0.0, end = 2.0, progress = 0.5f),
                GanttTask(id = "launch", label = "Launch", start = 4.0, end = 4.0, isMilestone = true),
            ),
            dependencies = listOf(GanttDependency("plan", "launch")),
        )
        val placement = resolveGanttChartPlacement(
            layout = layout,
            config = GanttChartLayoutConfig(
                widthPx = 320f,
                heightPx = 200f,
                contentPaddingPx = 12f,
                rowSpacingPx = 10f,
                minTaskWidthPx = 8f,
                rowLabelReservedWidthPx = 56f,
                xAxisLabelReservedHeightPx = 24f,
            ),
            style = GanttChartStyleOptions(milestoneSizeDp = 14f, dependencyArrowSizeDp = 8f),
            presentation = GanttChartPresentationOptions(
                showTodayIndicator = true,
                todayValue = 3.0,
            ),
        )

        assertEquals(2, placement.tasks.size)
        assertEquals(1, placement.dependencies.size)
        assertNotNull(placement.todayIndicatorXPx)
        assertTrue(placement.tasks[0].progressRightPx > placement.tasks[0].leftPx)
        assertEquals(placement.tasks[1].startXPx, placement.tasks[1].centerXPx, 7.1f)

        val hit = hitTestGanttTask(
            placement = placement,
            xPx = placement.tasks[0].centerXPx,
            yPx = placement.tasks[0].centerYPx,
        )
        assertEquals("plan", hit?.task?.id)
        assertNull(hitTestGanttTask(placement, 2f, 2f))
    }
}
