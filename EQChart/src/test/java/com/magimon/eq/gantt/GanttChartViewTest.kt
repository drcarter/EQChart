package com.magimon.eq.gantt

import android.graphics.Color
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GanttChartViewTest {

    @Test
    fun ganttChart_drawsTasksAndDispatchesTaskCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = GanttChartView(context)
        var callbackIndex: Int? = null
        var callbackTask: GanttTask? = null

        view.setStyleOptions(
            GanttChartStyleOptions(
                backgroundColor = Color.WHITE,
                defaultTaskColor = Color.parseColor("#2563EB"),
                selectedTaskStrokeColor = Color.BLACK,
            ),
        )
        view.setPresentationOptions(
            GanttChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showTaskLabels = true,
                showDependencies = true,
            ),
        )
        view.setOnTaskClickListener { index, task ->
            callbackIndex = index
            callbackTask = task
        }
        view.setTasks(
            listOf(
                GanttTask(id = "plan", label = "Plan", start = 0.0, end = 2.0),
                GanttTask(id = "build", label = "Build", start = 2.0, end = 6.0, progress = 0.5f),
                GanttTask(id = "launch", label = "Launch", start = 6.0, end = 6.0, isMilestone = true),
            ),
        )
        view.setDependencies(listOf(GanttDependency("build", "launch")))

        layoutAndDraw(view, width = 540, height = 320)

        val tasks = view.readPrivate<List<GanttPlacedTask>>("placedTasks")
        assertEquals(3, tasks.size)
        touchUp(view, tasks[1].centerXPx, tasks[1].centerYPx)

        assertEquals(1, callbackIndex)
        assertEquals("build", callbackTask?.id)
        assertEquals("Build", callbackTask?.label)
        assertTrue(view.performClick())
    }

    @Test
    fun ganttChart_filtersInvalidTasksAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = GanttChartView(context)

        view.setPresentationOptions(
            GanttChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setTasks(
            listOf(
                GanttTask(id = "", label = "Missing", start = 0.0, end = 1.0),
                GanttTask(id = "bad", label = "", start = 0.0, end = 1.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("placedTasks").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedTaskIndex"))
    }
}
