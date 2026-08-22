package com.seleneworlds.client.ui.cef

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class CefHitRegion(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(pointX: Int, pointY: Int): Boolean =
        pointX >= x && pointY >= y && pointX < x + width && pointY < y + height
}

class CefInteractionState {
    private val regions = AtomicReference<List<CefHitRegion>>(emptyList())
    private val editableFocused = AtomicBoolean()

    fun update(newRegions: List<CefHitRegion>, hasEditableFocus: Boolean) {
        regions.set(newRegions)
        editableFocused.set(hasEditableFocus)
    }

    fun isInteractive(x: Int, y: Int): Boolean = regions.get().any { it.contains(x, y) }
    fun hasUi(): Boolean = regions.get().isNotEmpty()
    fun hasEditableFocus(): Boolean = editableFocused.get()
}
