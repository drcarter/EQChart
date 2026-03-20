package com.magimon.eq.bar

import android.graphics.Color
import android.graphics.RectF
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
class BarChartViewTest {

    @Test
    fun barChart_drawsGroupedBars_andDispatchesSeriesPayloadFallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = BarChartView(context)
        var callbackSeriesIndex: Int? = null
        var callbackCategoryIndex: Int? = null
        var callbackValue: Double? = null
        var callbackPayload: Any? = null

        view.setStyleOptions(BarChartStyleOptions(backgroundColor = Color.WHITE))
        view.setPresentationOptions(
            BarChartPresentationOptions(
                animateOnDataChange = false,
                showLegend = true,
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
                layoutMode = BarLayoutMode.GROUPED,
                orientation = BarOrientation.VERTICAL,
            ),
        )
        view.setOnBarClickListener { seriesIndex, categoryIndex, value, payload ->
            callbackSeriesIndex = seriesIndex
            callbackCategoryIndex = categoryIndex
            callbackValue = value
            callbackPayload = payload
        }
        view.setSeries(
            listOf(
                BarSeries(
                    name = "Q1",
                    color = Color.RED,
                    payload = "series-q1",
                    points = listOf(
                        BarDatum(category = "Jan", value = 12.0),
                        BarDatum(category = "Feb", value = 18.0, payload = "point-feb"),
                        BarDatum(category = "", value = 99.0),
                    ),
                ),
                BarSeries(
                    name = "Q2",
                    color = Color.BLUE,
                    payload = "series-q2",
                    points = listOf(
                        BarDatum(category = "Jan", value = 8.0),
                        BarDatum(category = "Feb", value = Double.NaN),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 460, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        assertEquals(3, bars.size)

        val firstBar = bars.first()
        val firstRect = firstBar.readPrivate<RectF>("rect")
        touchUp(view, firstRect.centerX(), firstRect.centerY())

        assertEquals(0, callbackSeriesIndex)
        assertEquals(0, callbackCategoryIndex)
        assertEquals(12.0, callbackValue ?: Double.NaN, 0.0)
        assertEquals("series-q1", callbackPayload)
        assertTrue(view.performClick())
    }

    @Test
    fun barChart_buildsHorizontalStackedBars_fromMapperInput() {
        val context = RuntimeEnvironment.getApplication()
        val view = BarChartView(context)

        view.setPresentationOptions(
            BarChartPresentationOptions(
                animateOnDataChange = false,
                orientation = BarOrientation.HORIZONTAL,
                layoutMode = BarLayoutMode.STACKED,
                showLegend = false,
            ),
        )

        data class SampleBar(
            val name: String,
            val color: Int,
            val points: List<BarDatum>,
            val payload: String,
        )

        view.setSeries(
            listOf(
                SampleBar(
                    name = "North",
                    color = Color.MAGENTA,
                    payload = "north",
                    points = listOf(
                        BarDatum(category = "A", value = 10.0),
                        BarDatum(category = "B", value = -4.0),
                    ),
                ),
                SampleBar(
                    name = "South",
                    color = Color.CYAN,
                    payload = "south",
                    points = listOf(
                        BarDatum(category = "A", value = 6.0),
                        BarDatum(category = "B", value = 3.0),
                    ),
                ),
            ),
        ) { sample ->
            BarSeries(
                name = sample.name,
                color = sample.color,
                points = sample.points,
                payload = sample.payload,
            )
        }

        layoutAndDraw(view, width = 480, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        assertEquals(4, bars.size)

        val first = bars[0]
        val second = bars[1]
        val third = bars[2]
        val firstRect = first.readPrivate<RectF>("rect")
        val secondRect = second.readPrivate<RectF>("rect")
        val thirdRect = third.readPrivate<RectF>("rect")

        assertEquals(0, first.readPrivate<Int>("seriesIndex"))
        assertEquals(1, second.readPrivate<Int>("seriesIndex"))
        assertEquals(0, second.readPrivate<Int>("categoryIndex"))
        assertEquals(1, third.readPrivate<Int>("categoryIndex"))
        assertTrue(secondRect.left >= firstRect.right)
        assertTrue(thirdRect.width() > 0f)
    }

    @Test
    fun barChart_clearsSelection_whenTouchMisses_andRendersEmptyStateForInvalidData() {
        val context = RuntimeEnvironment.getApplication()
        val view = BarChartView(context)

        view.setPresentationOptions(
            BarChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setSeries(
            listOf(
                BarSeries(
                    name = "invalid",
                    color = Color.GRAY,
                    points = listOf(
                        BarDatum(category = "", value = 3.0),
                        BarDatum(category = "bad", value = Double.NaN),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("bars").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedBar"))
    }

    @Test
    fun barChart_drawsHorizontalGroupedBars_withNegativeLabelsAndPointPayload() {
        val context = RuntimeEnvironment.getApplication()
        val view = BarChartView(context)
        var callbackPayload: Any? = null

        view.setPresentationOptions(
            BarChartPresentationOptions(
                animateOnDataChange = false,
                orientation = BarOrientation.HORIZONTAL,
                layoutMode = BarLayoutMode.GROUPED,
                showLegend = true,
                showBarLabels = true,
                showGrid = true,
            ),
        )
        view.setOnBarClickListener { _, _, _, payload ->
            callbackPayload = payload
        }
        view.setSeries(
            listOf(
                BarSeries(
                    name = "North",
                    color = Color.MAGENTA,
                    points = listOf(
                        BarDatum(category = "A", value = -6.0, payload = "north-a"),
                        BarDatum(category = "B", value = 9.0),
                    ),
                ),
                BarSeries(
                    name = "South",
                    color = Color.CYAN,
                    points = listOf(
                        BarDatum(category = "A", value = 4.0),
                        BarDatum(category = "B", value = -3.0),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 480, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        val firstRect = bars.first().readPrivate<RectF>("rect")
        touchUp(view, firstRect.centerX(), firstRect.centerY())

        assertEquals("north-a", callbackPayload)
        assertTrue(firstRect.width() > 0f)
        assertTrue(firstRect.height() > 0f)
    }

    @Test
    fun barChart_buildsVerticalStackedBars_forMixedValues() {
        val context = RuntimeEnvironment.getApplication()
        val view = BarChartView(context)

        view.setPresentationOptions(
            BarChartPresentationOptions(
                animateOnDataChange = false,
                orientation = BarOrientation.VERTICAL,
                layoutMode = BarLayoutMode.STACKED,
                showLegend = false,
                showBarLabels = true,
            ),
        )
        view.setSeries(
            listOf(
                BarSeries(
                    name = "A",
                    color = Color.RED,
                    points = listOf(
                        BarDatum(category = "Jan", value = 6.0),
                        BarDatum(category = "Feb", value = -2.0),
                    ),
                ),
                BarSeries(
                    name = "B",
                    color = Color.BLUE,
                    points = listOf(
                        BarDatum(category = "Jan", value = 4.0),
                        BarDatum(category = "Feb", value = 5.0),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 420, height = 300)

        val bars = view.readPrivate<List<Any>>("bars")
        val firstRect = bars[0].readPrivate<RectF>("rect")
        val secondRect = bars[1].readPrivate<RectF>("rect")

        assertEquals(4, bars.size)
        assertTrue(secondRect.bottom <= firstRect.bottom)
        assertTrue(firstRect.height() > 0f)
    }
}
