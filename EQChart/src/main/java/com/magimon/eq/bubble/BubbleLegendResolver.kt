package com.magimon.eq.bubble

import java.util.Locale
import java.util.LinkedHashMap

/**
 * 범례 모드와 데이터/명시 항목을 바탕으로 최종 범례 목록을 계산한다.
 */
internal object BubbleLegendResolver {

    /**
     * 범례 모드에 따라 최종 범례 목록을 반환한다.
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
     * 데이터 순서를 유지하면서 자동 범례를 생성한다.
     *
     * `legendGroup`이 있으면 해당 값을 라벨로 사용하고,
     * 없으면 버블 색상 HEX 문자열을 라벨로 사용한다.
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
     * 정수 색상을 `#RRGGBB` 라벨로 변환한다.
     */
    private fun colorHexLabel(color: Int): String {
        return String.format(Locale.US, "#%06X", color and 0x00FFFFFF)
    }
}
