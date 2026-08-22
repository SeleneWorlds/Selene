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
        assertContentEquals(byteArrayOf(5, 6, 7, 8), frame?.bgra)
        assertNull(mailbox.takeLatest())
    }

    @Test
    fun `copies callback memory before returning`() {
        val source = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        val mailbox = CefOverlayFrameMailbox()
        mailbox.publish(source, 1, 1)
        source.put(0, 9)

        assertContentEquals(byteArrayOf(1, 2, 3, 4), mailbox.takeLatest()?.bgra)
    }
}
