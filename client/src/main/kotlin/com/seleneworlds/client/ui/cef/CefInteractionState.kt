package com.seleneworlds.client.ui.cef

import java.util.concurrent.atomic.AtomicReference

data class CefHitRegion(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(pointX: Int, pointY: Int): Boolean =
        pointX >= x && pointY >= y && pointX < x + width && pointY < y + height
}

class CefInteractionState {
    private data class Snapshot(
        val regions: List<CefHitRegion> = emptyList(),
        val editableFocused: Boolean = false,
        val capturedKeys: Set<String> = emptySet(),
        val captureText: Boolean = false,
        val passthroughKeys: Set<String> = emptySet(),
        val active: Boolean = false
    )
    private val snapshot = AtomicReference(Snapshot())

    fun update(newRegions: List<CefHitRegion>, hasEditableFocus: Boolean, capturedKeys: Set<String> = emptySet(),
        captureText: Boolean = false, passthroughKeys: Set<String> = emptySet()) = snapshot.set(
        Snapshot(newRegions, hasEditableFocus, capturedKeys, captureText, passthroughKeys, true))

    fun clear() = snapshot.set(Snapshot())
    fun isInteractive(x: Int, y: Int): Boolean = snapshot.get().regions.any { it.contains(x, y) }
    fun hasUi(): Boolean = snapshot.get().active
    fun hasEditableFocus(): Boolean = snapshot.get().editableFocused
    fun consumesKey(key: String): Boolean = snapshot.get().let {
        key !in it.passthroughKeys && (it.editableFocused || key in it.capturedKeys)
    }
    fun capturesText(): Boolean = snapshot.get().let { it.editableFocused || it.captureText }
}
