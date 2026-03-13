package com.magimon.eq.bubble

import java.util.Locale
import java.util.LinkedHashMap

/**
 * Resolves the final legend list from mode, data, and explicit items.
 */
internal object BubbleLegendResolver {

    /**
     * Returns legend items according to the selected legend mode.
     */
    fun resolve(
        data: List<BubbleDatum>,
        mode: BubbleLegendMode,
        explicitItems: List<BubbleLegendItem>,
    ): List<BubbleLegendItem> {
        return when (mode) {
            BubbleLegendMode.AUTO -> autoItems(data)
            BubbleLegendMode.EXPLICIT -> explicitItems
            BubbleLegendMode.AUTO_WITH_OVERRIDE -> {
                if (explicitItems.isNotEmpty()) {
                    explicitItems
                } else {
                    autoItems(data)
                }
            }
        }
    }

    /**
     * Builds auto legend items while preserving data order.
     *
     * Uses `legendGroup` when available; otherwise uses the bubble color in HEX text.
     */
    private fun autoItems(data: List<BubbleDatum>): List<BubbleLegendItem> {
        val entries = LinkedHashMap<String, Int>()
        data.forEach { datum ->
            val label = datum.legendGroup?.takeIf { it.isNotBlank() } ?: colorHexLabel(datum.color)
            if (!entries.containsKey(label)) {
                entries[label] = datum.color
            }
        }
        return entries.map { (label, color) ->
            BubbleLegendItem(label = label, color = color)
        }
    }

    /**
     * Converts an integer color to a `#RRGGBB` label.
     */
    private fun colorHexLabel(color: Int): String {
        return String.format(Locale.US, "#%06X", color and 0x00FFFFFF)
    }
}
