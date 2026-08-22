package com.seleneworlds.client.ui.cef

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CefOverlayFrameMailboxTest {
    @Test
    fun `keeps only the latest complete CEF frame`() {
        val mailbox = CefOverlayFrameMailbox()
        mailbox.publish(ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)), 1, 1)
        mailbox.publish(ByteBuffer.wrap(byteArrayOf(5, 6, 7, 8)), 1, 1)

        val frame = mailbox.takeLatest()
        assertEquals(1, frame?.width)
        assertEquals(1, frame?.height)
        assertContentEquals(byteArrayOf(5, 6, 7, 8), frame?.bgra?.bytes())
        mailbox.release(frame!!)
        assertNull(mailbox.takeLatest())
    }

    @Test
    fun `copies callback memory before returning`() {
        val source = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        val mailbox = CefOverlayFrameMailbox()
        mailbox.publish(source, 1, 1)
        source.put(0, 9)

        val frame = mailbox.takeLatest()!!
        assertContentEquals(byteArrayOf(1, 2, 3, 4), frame.bgra.bytes())
        mailbox.release(frame)
    }

    @Test
    fun `does not overwrite a frame while the render thread owns it`() {
        val mailbox = CefOverlayFrameMailbox()
        mailbox.publish(ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)), 1, 1)
        val acquired = mailbox.takeLatest()!!

        mailbox.publish(ByteBuffer.wrap(byteArrayOf(5, 6, 7, 8)), 1, 1)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), acquired.bgra.bytes())

        mailbox.release(acquired)
        val latest = mailbox.takeLatest()!!
        assertContentEquals(byteArrayOf(5, 6, 7, 8), latest.bgra.bytes())
        mailbox.release(latest)
    }

    private fun ByteBuffer.bytes(): ByteArray = duplicate().let { buffer ->
        ByteArray(buffer.remaining()).also(buffer::get)
    }
}
