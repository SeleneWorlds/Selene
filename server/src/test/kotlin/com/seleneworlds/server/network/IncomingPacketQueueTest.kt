package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IncomingPacketQueueTest {
    @Test
    fun `rejects packets beyond its capacity`() {
        val queue = IncomingPacketQueue(2)
        val first = TestPacket()
        val second = TestPacket()

        assertTrue(queue.offer(first))
        assertTrue(queue.offer(second))
        assertFalse(queue.offer(TestPacket()))
        assertSame(first, queue.poll())
        assertSame(second, queue.poll())
        assertNull(queue.poll())
    }

    private class TestPacket : Packet
}
