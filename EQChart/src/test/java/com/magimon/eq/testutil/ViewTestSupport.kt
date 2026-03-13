package com.magimon.eq.testutil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View

internal fun layoutAndDraw(view: View, width: Int = 400, height: Int = 400) {
    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    view.measure(widthSpec, heightSpec)
    view.layout(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
}

internal fun touchUp(view: View, x: Float, y: Float) {
    val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, y, 0)
    val up = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_UP, x, y, 0)
    try {
        view.onTouchEvent(down)
        view.onTouchEvent(up)
    } finally {
        down.recycle()
        up.recycle()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Any.readPrivate(name: String): T {
    var current: Class<*>? = javaClass
    while (current != null) {
        try {
            val field = current.getDeclaredField(name)
            field.isAccessible = true
            return field.get(this) as T
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    error("Field not found: $name")
}
