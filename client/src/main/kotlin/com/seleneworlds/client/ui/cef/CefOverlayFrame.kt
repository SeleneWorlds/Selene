package com.seleneworlds.client.ui.cef

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

data class CefOverlayFrame(val width: Int, val height: Int, val bgra: ByteArray)

class CefOverlayFrameMailbox {
    private val pending = AtomicReference<CefOverlayFrame?>()

    fun publish(buffer: ByteBuffer, width: Int, height: Int) {
        val source = buffer.duplicate().apply {
            position(0)
            limit(width * height * 4)
        }
        val pixels = ByteArray(source.remaining())
        source.get(pixels)
        pending.set(CefOverlayFrame(width, height, pixels))
    }

    fun takeLatest(): CefOverlayFrame? = pending.getAndSet(null)
}
