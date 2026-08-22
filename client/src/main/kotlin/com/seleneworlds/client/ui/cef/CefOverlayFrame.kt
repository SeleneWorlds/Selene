package com.seleneworlds.client.ui.cef

import java.nio.ByteBuffer
class CefOverlayFrame internal constructor(
    val width: Int,
    val height: Int,
    val bgra: ByteBuffer,
    internal val slot: Int
)

class CefOverlayFrameMailbox {
    private val buffers = arrayOfNulls<ByteBuffer>(2)
    private val widths = IntArray(2)
    private val heights = IntArray(2)
    private var pendingSlot = NONE
    private var acquiredSlot = NONE

    @Synchronized
    fun publish(buffer: ByteBuffer, width: Int, height: Int) {
        require(width > 0 && height > 0) { "CEF frame dimensions must be positive" }
        val size = Math.multiplyExact(Math.multiplyExact(width, height), BYTES_PER_PIXEL)
        val slot = when {
            pendingSlot != NONE -> pendingSlot
            acquiredSlot != 0 -> 0
            else -> 1
        }
        val target = buffers[slot]?.takeIf { it.capacity() >= size }
            ?: ByteBuffer.allocateDirect(size).also { buffers[slot] = it }
        val source = buffer.duplicate().apply {
            position(0)
            limit(size)
        }
        target.clear()
        target.put(source)
        target.flip()
        widths[slot] = width
        heights[slot] = height
        pendingSlot = slot
    }

    @Synchronized
    fun takeLatest(): CefOverlayFrame? {
        check(acquiredSlot == NONE) { "The previous CEF frame has not been released" }
        val slot = pendingSlot.takeIf { it != NONE } ?: return null
        pendingSlot = NONE
        acquiredSlot = slot
        return CefOverlayFrame(widths[slot], heights[slot], buffers[slot]!!.asReadOnlyBuffer(), slot)
    }

    @Synchronized
    fun release(frame: CefOverlayFrame) {
        check(frame.slot == acquiredSlot) { "Attempted to release a CEF frame that is not acquired" }
        acquiredSlot = NONE
    }

    private companion object {
        const val NONE = -1
        const val BYTES_PER_PIXEL = 4
    }
}
