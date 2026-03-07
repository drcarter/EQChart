package com.magimon.eq.bubble

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleLegendResolverTest {

    @Test
    fun autoMode_usesLegendGroupAndKeepsFirstSeenOrder() {
        val data = listOf(
            BubbleDatum(x = 0.0, y = 0.0, size = 10.0, color = RED, legendGroup = "Labor"),
            BubbleDatum(x = 0.0, y = 0.0, size = 8.0, color = BLUE, legendGroup = "Arts"),
            BubbleDatum(x = 0.0, y = 0.0, size = 6.0, color = GREEN, legendGroup = "Labor"),
        )

        val result = BubbleLegendResolver.resolve(
            data = data,
            mode = BubbleLegendMode.AUTO,
            explicitItems = emptyList(),
        )

        assertEquals(listOf("Labor", "Arts"), result.map { it.label })
        assertEquals(RED, result[0].color)
        assertEquals(BLUE, result[1].color)
    }

    @Test
    fun autoMode_fallsBackToColorHexWhenLegendGroupMissing() {
        val data = listOf(
            BubbleDatum(x = 0.0, y = 0.0, size = 10.0, color = ARTS_BLUE),
        )

        val result = BubbleLegendResolver.resolve(
            data = data,
            mode = BubbleLegendMode.AUTO,
            explicitItems = emptyList(),
        )

        assertEquals(1, result.size)
        assertEquals("#4A7FB1", result.first().label)
    }

    @Test
    fun explicitMode_usesOnlyExplicitItems() {
        val data = listOf(
            BubbleDatum(x = 0.0, y = 0.0, size = 10.0, color = RED, legendGroup = "Labor"),
        )
        val explicit = listOf(BubbleLegendItem(label = "Custom", color = BLACK))

        val result = BubbleLegendResolver.resolve(
            data = data,
            mode = BubbleLegendMode.EXPLICIT,
            explicitItems = explicit,
        )

        assertEquals(explicit, result)
    }

    @Test
    fun autoWithOverride_prefersExplicitWhenProvided() {
        val data = listOf(
            BubbleDatum(x = 0.0, y = 0.0, size = 10.0, color = RED, legendGroup = "Labor"),
        )
        val explicit = listOf(BubbleLegendItem(label = "Pinned", color = BLACK))

        val result = BubbleLegendResolver.resolve(
            data = data,
            mode = BubbleLegendMode.AUTO_WITH_OVERRIDE,
            explicitItems = explicit,
        )

        assertEquals(explicit, result)
    }

    @Test
    fun autoWithOverride_usesAutoWhenExplicitIsEmpty() {
        val data = listOf(
            BubbleDatum(x = 0.0, y = 0.0, size = 10.0, color = RED, legendGroup = "Labor"),
        )

        val result = BubbleLegendResolver.resolve(
            data = data,
            mode = BubbleLegendMode.AUTO_WITH_OVERRIDE,
            explicitItems = emptyList(),
        )

        assertEquals(listOf("Labor"), result.map { it.label })
    }

    private companion object {
        const val RED: Int = 0xFFFF0000.toInt()
        const val BLUE: Int = 0xFF0000FF.toInt()
        const val GREEN: Int = 0xFF00FF00.toInt()
        const val BLACK: Int = 0xFF000000.toInt()
        const val ARTS_BLUE: Int = 0xFF4A7FB1.toInt()
    }
}
