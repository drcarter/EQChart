package com.magimon.eq.calendarheatmap

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Rectangular region expressed in pixels relative to the chart canvas.
 */
data class CalendarHeatmapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

/**
 * Pixel-based layout inputs consumed by [resolveCalendarHeatmapChartLayout].
 */
data class CalendarHeatmapChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val monthLabelHeightPx: Float,
    val weekdayLabelWidthPx: Float,
    val labelGapPx: Float,
    val cellGapPx: Float,
)

/**
 * Render-ready month label placement produced by [resolveCalendarHeatmapChartLayout].
 */
data class CalendarHeatmapMonthLabelLayout(
    val month: Int,
    val label: String,
    val rect: CalendarHeatmapRect,
)

/**
 * Render-ready weekday label placement produced by [resolveCalendarHeatmapChartLayout].
 */
data class CalendarHeatmapWeekdayLabelLayout(
    val weekdayIndex: Int,
    val label: String,
    val rect: CalendarHeatmapRect,
)

/**
 * Render-ready day placement produced by [resolveCalendarHeatmapChartLayout].
 */
data class CalendarHeatmapDayCellLayout(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val weekIndex: Int,
    val weekdayIndex: Int,
    val value: Double?,
    val day: CalendarHeatmapDay?,
    val rect: CalendarHeatmapRect,
    val fillColor: Int,
)

/**
 * Shared layout result for View and Compose calendar heatmap renderers.
 */
data class CalendarHeatmapChartLayoutResult(
    val chartRect: CalendarHeatmapRect,
    val monthLabels: List<CalendarHeatmapMonthLabelLayout>,
    val weekdayLabels: List<CalendarHeatmapWeekdayLabelLayout>,
    val dayCells: List<CalendarHeatmapDayCellLayout>,
    val minPositiveValue: Double?,
    val maxPositiveValue: Double?,
    val weekCount: Int,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

private const val MIN_YEAR = 1
private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
private val VISIBLE_WEEKDAY_LABELS = listOf(1, 3, 5) // Monday, Wednesday, Friday in Sunday-first grid.
private val MONTH_LABELS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val WEEKDAY_LABELS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/**
 * Resolves validated day placements and fill colors for a GitHub-style calendar heatmap.
 */
fun resolveCalendarHeatmapChartLayout(
    data: CalendarHeatmapData,
    config: CalendarHeatmapChartLayoutConfig,
    style: CalendarHeatmapChartStyleOptions = CalendarHeatmapChartStyleOptions(),
    presentation: CalendarHeatmapChartPresentationOptions = CalendarHeatmapChartPresentationOptions(),
): CalendarHeatmapChartLayoutResult {
    val emptyRect = CalendarHeatmapRect(0f, 0f, 0f, 0f)
    if (config.widthPx <= 0f || config.heightPx <= 0f) {
        return CalendarHeatmapChartLayoutResult(
            chartRect = emptyRect,
            monthLabels = emptyList(),
            weekdayLabels = emptyList(),
            dayCells = emptyList(),
            minPositiveValue = null,
            maxPositiveValue = null,
            weekCount = 0,
            isRenderable = false,
            emptyReason = "non-positive size",
        )
    }
    if (data.year < MIN_YEAR) {
        return CalendarHeatmapChartLayoutResult(
            chartRect = emptyRect,
            monthLabels = emptyList(),
            weekdayLabels = emptyList(),
            dayCells = emptyList(),
            minPositiveValue = null,
            maxPositiveValue = null,
            weekCount = 0,
            isRenderable = false,
            emptyReason = "invalid year",
        )
    }

    val daysInYear = daysInYear(data.year)
    val jan1WeekdayIndex = weekdayIndex(data.year, 1, 1)
    val weekCount = (jan1WeekdayIndex + daysInYear + 6) / 7
    val monthLabelGap = if (config.monthLabelHeightPx > 0f) config.labelGapPx else 0f
    val weekdayLabelGap = if (config.weekdayLabelWidthPx > 0f) config.labelGapPx else 0f
    val chartRect = CalendarHeatmapRect(
        left = config.contentPaddingPx + config.weekdayLabelWidthPx + weekdayLabelGap,
        top = config.contentPaddingPx + config.monthLabelHeightPx + monthLabelGap,
        right = config.widthPx - config.contentPaddingPx,
        bottom = config.heightPx - config.contentPaddingPx,
    )
    if (chartRect.width <= 0f || chartRect.height <= 0f || weekCount <= 0) {
        return CalendarHeatmapChartLayoutResult(
            chartRect = chartRect,
            monthLabels = emptyList(),
            weekdayLabels = emptyList(),
            dayCells = emptyList(),
            minPositiveValue = null,
            maxPositiveValue = null,
            weekCount = weekCount,
            isRenderable = false,
            emptyReason = "no drawable bounds",
        )
    }

    val columnWidth = chartRect.width / weekCount.toFloat()
    val rowHeight = chartRect.height / 7f
    if (columnWidth <= 0f || rowHeight <= 0f) {
        return CalendarHeatmapChartLayoutResult(
            chartRect = chartRect,
            monthLabels = emptyList(),
            weekdayLabels = emptyList(),
            dayCells = emptyList(),
            minPositiveValue = null,
            maxPositiveValue = null,
            weekCount = weekCount,
            isRenderable = false,
            emptyReason = "non-positive cell size",
        )
    }

    val sanitized = LinkedHashMap<Pair<Int, Int>, CalendarHeatmapDay>()
    data.days.forEach { day ->
        if (!day.value.isFinite() || day.value < 0.0) return@forEach
        if (!isValidDay(data.year, day.month, day.dayOfMonth)) return@forEach
        sanitized[day.month to day.dayOfMonth] = day.copy(
            label = day.label?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    val positiveValues = sanitized.values.mapNotNull { entry ->
        entry.value.takeIf { it > 0.0 }
    }
    val minPositive = positiveValues.minOrNull()
    val maxPositive = positiveValues.maxOrNull()

    val monthLabels = buildMonthLabels(
        year = data.year,
        weekCount = weekCount,
        chartRect = chartRect,
        config = config,
        columnWidth = columnWidth,
    )
    val weekdayLabels = buildWeekdayLabels(
        chartRect = chartRect,
        config = config,
        rowHeight = rowHeight,
    )
    val dayCells = buildDayCells(
        data = data,
        chartRect = chartRect,
        columnWidth = columnWidth,
        rowHeight = rowHeight,
        cellGapPx = config.cellGapPx,
        sanitized = sanitized,
        style = style,
        minPositive = minPositive,
        maxPositive = maxPositive,
    )

    return CalendarHeatmapChartLayoutResult(
        chartRect = chartRect,
        monthLabels = monthLabels,
        weekdayLabels = weekdayLabels,
        dayCells = dayCells,
        minPositiveValue = minPositive,
        maxPositiveValue = maxPositive,
        weekCount = weekCount,
        isRenderable = true,
    )
}

/**
 * Finds the topmost populated day cell that contains the given point.
 */
fun findCalendarHeatmapHit(
    layout: CalendarHeatmapChartLayoutResult,
    x: Float,
    y: Float,
): CalendarHeatmapDayCellLayout? {
    return layout.dayCells.lastOrNull { cell ->
        cell.day != null && cell.rect.contains(x, y)
    }
}

/**
 * Default numeric formatter used by sample screens.
 */
fun formatCalendarHeatmapValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val formatted = String.format(Locale.US, "%.2f", value)
    return formatted.trimEnd('0').trimEnd('.')
}

private fun buildMonthLabels(
    year: Int,
    weekCount: Int,
    chartRect: CalendarHeatmapRect,
    config: CalendarHeatmapChartLayoutConfig,
    columnWidth: Float,
): List<CalendarHeatmapMonthLabelLayout> {
    if (config.monthLabelHeightPx <= 0f) return emptyList()
    return (1..12).map { month ->
        val startWeek = weekIndex(year, month, 1)
        val endWeekExclusive = if (month == 12) {
            weekCount
        } else {
            max(startWeek + 1, weekIndex(year, month + 1, 1))
        }
        CalendarHeatmapMonthLabelLayout(
            month = month,
            label = MONTH_LABELS[month - 1],
            rect = CalendarHeatmapRect(
                left = chartRect.left + startWeek * columnWidth,
                top = config.contentPaddingPx,
                right = chartRect.left + endWeekExclusive * columnWidth,
                bottom = config.contentPaddingPx + config.monthLabelHeightPx,
            ),
        )
    }
}

private fun buildWeekdayLabels(
    chartRect: CalendarHeatmapRect,
    config: CalendarHeatmapChartLayoutConfig,
    rowHeight: Float,
): List<CalendarHeatmapWeekdayLabelLayout> {
    if (config.weekdayLabelWidthPx <= 0f) return emptyList()
    return VISIBLE_WEEKDAY_LABELS.map { index ->
        CalendarHeatmapWeekdayLabelLayout(
            weekdayIndex = index,
            label = WEEKDAY_LABELS[index],
            rect = CalendarHeatmapRect(
                left = config.contentPaddingPx,
                top = chartRect.top + index * rowHeight,
                right = config.contentPaddingPx + config.weekdayLabelWidthPx,
                bottom = chartRect.top + (index + 1) * rowHeight,
            ),
        )
    }
}

private fun buildDayCells(
    data: CalendarHeatmapData,
    chartRect: CalendarHeatmapRect,
    columnWidth: Float,
    rowHeight: Float,
    cellGapPx: Float,
    sanitized: Map<Pair<Int, Int>, CalendarHeatmapDay>,
    style: CalendarHeatmapChartStyleOptions,
    minPositive: Double?,
    maxPositive: Double?,
): List<CalendarHeatmapDayCellLayout> {
    val cells = ArrayList<CalendarHeatmapDayCellLayout>(daysInYear(data.year))
    val calendar = utcCalendar(data.year, 1, 1)
    val totalDays = daysInYear(data.year)
    repeat(totalDays) {
        val month = calendar.get(Calendar.MONTH) + 1
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val weekIndex = weekIndex(data.year, month, dayOfMonth)
        val weekdayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val rawDay = sanitized[month to dayOfMonth]
        val resolvedValue = rawDay?.value?.takeIf { it > 0.0 }
        val rect = CalendarHeatmapRect(
            left = chartRect.left + weekIndex * columnWidth + cellGapPx * 0.5f,
            top = chartRect.top + weekdayIndex * rowHeight + cellGapPx * 0.5f,
            right = chartRect.left + (weekIndex + 1) * columnWidth - cellGapPx * 0.5f,
            bottom = chartRect.top + (weekdayIndex + 1) * rowHeight - cellGapPx * 0.5f,
        )
        cells += CalendarHeatmapDayCellLayout(
            year = data.year,
            month = month,
            dayOfMonth = dayOfMonth,
            weekIndex = weekIndex,
            weekdayIndex = weekdayIndex,
            value = resolvedValue,
            day = rawDay?.takeIf { resolvedValue != null },
            rect = rect,
            fillColor = resolveFillColor(
                value = resolvedValue,
                style = style,
                minPositive = minPositive,
                maxPositive = maxPositive,
            ),
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cells
}

private fun resolveFillColor(
    value: Double?,
    style: CalendarHeatmapChartStyleOptions,
    minPositive: Double?,
    maxPositive: Double?,
): Int {
    if (value == null || minPositive == null || maxPositive == null) {
        return style.emptyCellColor
    }
    if (maxPositive - minPositive <= 1e-12) {
        return style.level4Color
    }
    val normalized = ((value - minPositive) / (maxPositive - minPositive)).coerceIn(0.0, 1.0)
    return when {
        normalized < 0.25 -> style.level1Color
        normalized < 0.5 -> style.level2Color
        normalized < 0.75 -> style.level3Color
        else -> style.level4Color
    }
}

private fun weekIndex(year: Int, month: Int, dayOfMonth: Int): Int {
    val dayOfYear = dayOfYear(year, month, dayOfMonth)
    val jan1WeekdayIndex = weekdayIndex(year, 1, 1)
    return (jan1WeekdayIndex + dayOfYear - 1) / 7
}

private fun dayOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
    val calendar = utcCalendar(year, month, dayOfMonth)
    return calendar.get(Calendar.DAY_OF_YEAR)
}

private fun weekdayIndex(year: Int, month: Int, dayOfMonth: Int): Int {
    return utcCalendar(year, month, dayOfMonth).get(Calendar.DAY_OF_WEEK) - 1
}

private fun daysInYear(year: Int): Int {
    return if (utcCalendar(year, 1, 1).getActualMaximum(Calendar.DAY_OF_YEAR) == 366) 366 else 365
}

private fun isValidDay(year: Int, month: Int, dayOfMonth: Int): Boolean {
    if (month !in 1..12 || dayOfMonth <= 0) return false
    val calendar = utcCalendar(year, month, 1)
    return dayOfMonth <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun utcCalendar(year: Int, month: Int, dayOfMonth: Int): GregorianCalendar {
    return GregorianCalendar(UTC).apply {
        isLenient = false
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        timeInMillis
    }
}
